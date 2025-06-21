package com.luanvv.codegen.spring;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Maven plugin to generate Spring Boot boilerplate code from YAML configuration
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodeGeneratorMojo extends AbstractMojo {

    /**
     * The Maven project
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;    /**
     * Path to the YAML configuration file (for single file - backward compatibility)
     */
    @Parameter(property = "configFile", defaultValue = "src/main/resources/codegen.yaml")
    private File configFile;

    /**
     * List of YAML configuration files (for multiple files)
     */
    @Parameter(property = "configFiles")
    private File[] configFiles;

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
    private File resourceOutputDirectory;    /**
     * Skip code generation
     */
    @Parameter(property = "skipCodeGen", defaultValue = "false")
    private boolean skip;

    /**
     * Skip generation if target files already exist (allows manual overrides)
     */
    @Parameter(property = "skipIfExists", defaultValue = "false")
    private boolean skipIfExists;

    /**
     * Force regeneration even if files exist
     */
    @Parameter(property = "forceRegenerate", defaultValue = "false")
    private boolean forceRegenerate;@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Code generation is skipped.");
            return;
        }

        getLog().info("Starting Spring Boot code generation...");
        
        try {
            // Determine which config files to process
            File[] filesToProcess = getConfigFilesToProcess();
            
            getLog().info("Processing " + filesToProcess.length + " configuration file(s)");
            getLog().info("Output directory: " + outputDirectory.getAbsolutePath());
            getLog().info("Test output directory: " + testOutputDirectory.getAbsolutePath());
            getLog().info("Resource output directory: " + resourceOutputDirectory.getAbsolutePath());

            // Create output directories
            createDirectories();

            // Process each configuration file
            for (File configFile : filesToProcess) {
                processConfigFile(configFile);
            }

            // Add generated sources to Maven project
            addGeneratedSourcesToProject();
            
            // Copy generated resources to standard Maven directories
            copyGeneratedResources();

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
            }        } catch (Exception e) {
            throw new MojoExecutionException("Error creating directories", e);
        }
    }

    private File[] getConfigFilesToProcess() throws MojoExecutionException {
        // If configFiles array is specified, use it
        if (configFiles != null && configFiles.length > 0) {
            // Validate all config files exist
            for (File file : configFiles) {
                if (!file.exists()) {
                    throw new MojoExecutionException("Configuration file not found: " + file.getAbsolutePath());
                }
            }
            return configFiles;
        }
        
        // Otherwise, use the single configFile (backward compatibility)
        if (!configFile.exists()) {
            throw new MojoExecutionException("Configuration file not found: " + configFile.getAbsolutePath());
        }
        return new File[]{configFile};
    }

    private void processConfigFile(File configFile) throws MojoExecutionException {
        getLog().info("Processing configuration file: " + configFile.getAbsolutePath());
        
        try {
            // Parse YAML configuration
            CodeGenConfig config = parseConfiguration(configFile);
            
            // Generate code
            generateCode(config);
            
            getLog().info("Generated code for entity: " + config.getEntityName());
            
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to process config file: " + configFile.getAbsolutePath(), e);
        }
    }

    private CodeGenConfig parseConfiguration(File configFile) throws MojoExecutionException {
        try {
            YamlConfigParser parser = new YamlConfigParser();
            return parser.parse(configFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to parse configuration file: " + configFile, e);
        }
    }    private void generateCode(CodeGenConfig config) throws MojoExecutionException {
        try {
            CodeGenerator generator = new CodeGenerator(config, outputDirectory, testOutputDirectory, resourceOutputDirectory);
            
            // Set override behavior
            generator.setOverrideBehavior(skipIfExists, forceRegenerate);
            
            generator.generateAll();
            getLog().info("Generated code for entity: " + config.getEntityName());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate code", e);
        }
    }private void addGeneratedSourcesToProject() {
        // Add generated sources to Maven project so they're compiled
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        project.addTestCompileSourceRoot(testOutputDirectory.getAbsolutePath());
        
        getLog().info("Added compile source root: " + outputDirectory.getAbsolutePath());
        getLog().info("Added test compile source root: " + testOutputDirectory.getAbsolutePath());
    }

    private void copyGeneratedResources() throws MojoExecutionException {
        try {
            // Standard Maven resource directory
            File standardResourceDir = new File(project.getBasedir(), "src/main/resources");
            
            // Ensure the standard resource directory exists
            if (!standardResourceDir.exists()) {
                standardResourceDir.mkdirs();
            }
            
            // Copy all files from generated-resources to src/main/resources
            if (resourceOutputDirectory.exists()) {
                copyDirectoryContents(resourceOutputDirectory.toPath(), standardResourceDir.toPath());
                getLog().info("Copied generated resources to: " + standardResourceDir.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy generated resources", e);
        }
    }

    private void copyDirectoryContents(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + sourcePath, e);
                }
            });
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
