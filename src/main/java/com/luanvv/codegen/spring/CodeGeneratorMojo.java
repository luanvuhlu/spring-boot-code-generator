package com.luanvv.codegen.spring;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Maven plugin to generate Spring Boot boilerplate code from YAML configuration
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodeGeneratorMojo extends AbstractMojo {

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Path to the YAML configuration file
     */
    @Parameter(property = "configFile", defaultValue = "src/main/resources/codegen.yaml")
    private File configFile;

    /**
     * Output directory for generated Java sources
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/java")
    private File outputDirectory;

    /**
     * Output directory for generated test sources
     */
    @Parameter(property = "testOutputDirectory", defaultValue = "${project.build.directory}/generated-test-sources/java")
    private File testOutputDirectory;

    /**
     * Output directory for generated resources (SQL files)
     */
    @Parameter(property = "resourceOutputDirectory", defaultValue = "${project.build.directory}/generated-resources")
    private File resourceOutputDirectory;

    /**
     * Skip code generation
     */
    @Parameter(property = "skipCodeGen", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Code generation is skipped.");
            return;
        }

        getLog().info("Starting Spring Boot code generation...");
        
        try {
            // Validate configuration file exists
            if (!configFile.exists()) {
                throw new MojoExecutionException("Configuration file not found: " + configFile.getAbsolutePath());
            }

            getLog().info("Reading configuration from: " + configFile.getAbsolutePath());
            getLog().info("Output directory: " + outputDirectory.getAbsolutePath());
            getLog().info("Test output directory: " + testOutputDirectory.getAbsolutePath());
            getLog().info("Resource output directory: " + resourceOutputDirectory.getAbsolutePath());

            // Create output directories
            createDirectories();

            // Parse YAML configuration
            CodeGenConfig config = parseConfiguration();
            
            // Generate code
            generateCode(config);

            // Add generated sources to Maven project
            addGeneratedSourcesToProject();

            getLog().info("Code generation completed successfully!");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate code", e);
        }
    }

    private void createDirectories() throws MojoExecutionException {
        try {
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
            }
            if (!testOutputDirectory.exists() && !testOutputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create test output directory: " + testOutputDirectory);
            }
            if (!resourceOutputDirectory.exists() && !resourceOutputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create resource output directory: " + resourceOutputDirectory);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating directories", e);
        }
    }

    private CodeGenConfig parseConfiguration() throws MojoExecutionException {
        try {
            YamlConfigParser parser = new YamlConfigParser();
            return parser.parse(configFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to parse configuration file: " + configFile, e);
        }
    }

    private void generateCode(CodeGenConfig config) throws MojoExecutionException {
        try {
            CodeGenerator generator = new CodeGenerator(config, outputDirectory, testOutputDirectory, resourceOutputDirectory);
            generator.generateAll();
            getLog().info("Generated code for entity: " + config.getEntityName());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate code", e);
        }
    }

    private void addGeneratedSourcesToProject() {
        // Add generated sources to Maven project so they're compiled
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        project.addTestCompileSourceRoot(testOutputDirectory.getAbsolutePath());
        
        getLog().debug("Added compile source root: " + outputDirectory.getAbsolutePath());
        getLog().debug("Added test compile source root: " + testOutputDirectory.getAbsolutePath());
    }

    // Getters for testing
    public File getConfigFile() {
        return configFile;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public File getTestOutputDirectory() {
        return testOutputDirectory;
    }

    public File getResourceOutputDirectory() {
        return resourceOutputDirectory;
    }

    public boolean isSkip() {
        return skip;
    }
}
