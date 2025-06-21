package com.luanvv.codegen.spring.integration;

import com.luanvv.codegen.spring.CodeGenConfig;
import com.luanvv.codegen.spring.CodeGenerator;
import com.luanvv.codegen.spring.YamlConfigParser;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Integration test for the complete code generation flow
 */
public class CodeGenerationIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testCompleteCodeGeneration() throws Exception {
        // Create temporary directories for output
        File outputDir = tempFolder.newFolder("generated-sources");
        File testOutputDir = tempFolder.newFolder("generated-test-sources");
        File resourceOutputDir = tempFolder.newFolder("generated-resources");

        // Parse the sample configuration
        YamlConfigParser parser = new YamlConfigParser();
        File configFile = new File("src/test/resources/sample-config.yaml");
        CodeGenConfig config = parser.parse(configFile);

        // Generate code
        CodeGenerator generator = new CodeGenerator(config, outputDir, testOutputDir, resourceOutputDir);
        generator.generateAll();

        // Verify that the SQL migration file was created
        File migrationDir = new File(resourceOutputDir, "db/migration");
        assertTrue("Migration directory should exist", migrationDir.exists());
        
        File[] sqlFiles = migrationDir.listFiles((dir, name) -> name.endsWith(".sql"));
        assertNotNull("SQL files array should not be null", sqlFiles);
        assertEquals("Should have exactly one SQL file", 1, sqlFiles.length);
        
        String sqlContent = new String(Files.readAllBytes(sqlFiles[0].toPath()));
        assertTrue("SQL content should contain CREATE TABLE", sqlContent.contains("CREATE TABLE"));
        assertTrue("SQL content should contain user table", sqlContent.toLowerCase().contains("user"));

        // Verify that output directories were created
        String packagePath = config.getPackageName().replace('.', File.separatorChar);
        File mainPackageDir = new File(outputDir, packagePath);
        File testPackageDir = new File(testOutputDir, packagePath);

        assertTrue("Main package directory should exist", mainPackageDir.exists());
        assertTrue("Test package directory should exist", testPackageDir.exists());

        System.out.println("Integration test completed successfully!");
        System.out.println("Generated files in: " + outputDir.getAbsolutePath());
        System.out.println("Generated test files in: " + testOutputDir.getAbsolutePath());
        System.out.println("Generated resources in: " + resourceOutputDir.getAbsolutePath());
    }
}
