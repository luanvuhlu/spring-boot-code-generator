package com.example.codegen;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for YAML configuration files
 */
public class YamlConfigParser {    /**
     * Parse the YAML configuration file
     */
    public CodeGenConfig parse(File configFile) throws IOException {
        if (!configFile.exists()) {
            throw new IOException("Configuration file not found: " + configFile.getAbsolutePath());
        }

        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(CodeGenConfig.class, loaderOptions);
        TypeDescription configTypeDescription = new TypeDescription(CodeGenConfig.class);
        configTypeDescription.addPropertyParameters("fields", CodeGenConfig.Field.class);
        constructor.addTypeDescription(configTypeDescription);
        
        Yaml yaml = new Yaml(constructor);
        
        try (InputStream inputStream = new FileInputStream(configFile)) {
            CodeGenConfig config = yaml.load(inputStream);
            
            // Validate the configuration
            validateConfig(config);
            
            return config;
        }
    }

    /**
     * Validate the parsed configuration
     */
    private void validateConfig(CodeGenConfig config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration is null");
        }

        if (config.getPackageName() == null || config.getPackageName().trim().isEmpty()) {
            throw new IllegalArgumentException("Package name is required");
        }

        if (config.getEntityName() == null || config.getEntityName().trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name is required");
        }

        if (config.getFields() == null || config.getFields().isEmpty()) {
            throw new IllegalArgumentException("At least one field is required");
        }

        // Validate package name format
        if (!isValidPackageName(config.getPackageName())) {
            throw new IllegalArgumentException("Invalid package name: " + config.getPackageName());
        }

        // Validate entity name format
        if (!isValidJavaIdentifier(config.getEntityName())) {
            throw new IllegalArgumentException("Invalid entity name: " + config.getEntityName());
        }

        // Validate fields
        for (CodeGenConfig.Field field : config.getFields()) {
            if (field.getName() == null || field.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Field name is required");
            }
            if (field.getType() == null || field.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Field type is required for field: " + field.getName());
            }
            if (!isValidJavaIdentifier(field.getName())) {
                throw new IllegalArgumentException("Invalid field name: " + field.getName());
            }
        }

        // Validate ID fields exist in fields list
        if (config.getIdFields() != null && !config.getIdFields().isEmpty()) {
            for (String idField : config.getIdFields()) {
                boolean found = config.getFields().stream()
                        .anyMatch(field -> field.getName().equals(idField));
                if (!found) {
                    throw new IllegalArgumentException("ID field '" + idField + "' not found in fields list");
                }
            }
        }
    }

    /**
     * Check if a string is a valid Java package name
     */
    private boolean isValidPackageName(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }

        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (!isValidJavaIdentifier(part)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a string is a valid Java identifier
     */
    private boolean isValidJavaIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }

        for (int i = 1; i < identifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }

        // Check if it's a reserved keyword
        return !isJavaKeyword(identifier);
    }

    /**
     * Check if a string is a Java reserved keyword
     */
    private boolean isJavaKeyword(String word) {
        String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "true", "false", "null"
        };

        for (String keyword : keywords) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }
}
