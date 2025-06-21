package com.luanvv.codegen.spring;

import java.io.File;
import java.io.IOException;

/**
 * Main code generator that orchestrates the generation of all components
 */
public class CodeGenerator {
    
    private final CodeGenConfig config;
    private final File outputDirectory;
    private final File testOutputDirectory;
    private final File resourceOutputDirectory;

    public CodeGenerator(CodeGenConfig config, File outputDirectory, File testOutputDirectory, File resourceOutputDirectory) {
        this.config = config;
        this.outputDirectory = outputDirectory;
        this.testOutputDirectory = testOutputDirectory;
        this.resourceOutputDirectory = resourceOutputDirectory;
    }

    /**
     * Generate all components
     */
    public void generateAll() throws IOException {
        // Create package directories
        createPackageDirectories();

        // Generate SQL migration file
        generateSqlMigration();

        // Generate Entity class
        generateEntity();

        // Generate Repository interface
        generateRepository();

        // Generate Service interface and implementation
        generateService();        // Generate REST Controller
        generateController();
    }

    private void createPackageDirectories() {
        String packagePath = config.getPackageName().replace('.', File.separatorChar);
        
        // Create main source package directory
        File mainPackageDir = new File(outputDirectory, packagePath);
        mainPackageDir.mkdirs();

        // Create test source package directory
        File testPackageDir = new File(testOutputDirectory, packagePath);
        testPackageDir.mkdirs();
    }

    private void generateSqlMigration() throws IOException {
        SqlMigrationGenerator generator = new SqlMigrationGenerator(config, resourceOutputDirectory);
        generator.generate();
    }

    private void generateEntity() throws IOException {
        EntityGenerator generator = new EntityGenerator(config, outputDirectory);
        generator.generate();
    }

    private void generateRepository() throws IOException {
        RepositoryGenerator generator = new RepositoryGenerator(config, outputDirectory);
        generator.generate();
    }

    private void generateService() throws IOException {
        ServiceGenerator generator = new ServiceGenerator(config, outputDirectory);
        generator.generate();
    }    private void generateController() throws IOException {
        ControllerGenerator generator = new ControllerGenerator(config, outputDirectory);
        generator.generate();
    }
}
