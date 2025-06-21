package com.luanvv.codegen.spring;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit tests for CodeGeneratorMojo
 */
public class CodeGeneratorMojoTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
            // Empty implementation
        }

        @Override
        protected void after() {
            // Empty implementation
        }
    };

    /**
     * Test configuration validation
     */
    @Test
    @WithoutMojo
    public void testConfigurationValidation() throws Exception {
        // Test that the mojo can be instantiated
        CodeGeneratorMojo mojo = new CodeGeneratorMojo();
        assertNotNull(mojo);
    }

    /**
     * Test YAML parsing
     */
    @Test
    @WithoutMojo
    public void testYamlParsing() throws Exception {
        YamlConfigParser parser = new YamlConfigParser();
        
        // Get the sample config file
        File configFile = new File("src/test/resources/sample-config.yaml");
        assertTrue("Sample config file should exist", configFile.exists());
        
        // Parse the configuration
        CodeGenConfig config = parser.parse(configFile);
        
        // Verify the parsed configuration
        assertNotNull("Config should not be null", config);
        assertEquals("Package name should match", "com.example.demo", config.getPackageName());
        assertEquals("Entity name should match", "User", config.getEntityName());
        assertNotNull("ID fields should not be null", config.getIdFields());
        assertEquals("Should have one ID field", 1, config.getIdFields().size());
        assertEquals("ID field should be 'id'", "id", config.getIdFields().get(0));
        assertNotNull("Fields should not be null", config.getFields());
        assertTrue("Should have multiple fields", config.getFields().size() > 0);
        assertNotNull("SQL content should not be null", config.getSqlFileContent());
        assertTrue("SQL content should not be empty", !config.getSqlFileContent().trim().isEmpty());
    }

    /**
     * Test field validation
     */
    @Test
    @WithoutMojo
    public void testFieldValidation() throws Exception {
        YamlConfigParser parser = new YamlConfigParser();
        File configFile = new File("src/test/resources/sample-config.yaml");
        CodeGenConfig config = parser.parse(configFile);

        // Test that ID field exists in fields list
        boolean idFieldFound = config.getFields().stream()
                .anyMatch(field -> field.getName().equals("id"));
        assertTrue("ID field should exist in fields list", idFieldFound);

        // Test field properties
        CodeGenConfig.Field idField = config.getFields().stream()
                .filter(field -> field.getName().equals("id"))
                .findFirst()
                .orElse(null);
        
        assertNotNull("ID field should be found", idField);
        assertEquals("ID field type should be Long", "Long", idField.getType());
        assertFalse("ID field should not be nullable", idField.isNullable());
    }
}
