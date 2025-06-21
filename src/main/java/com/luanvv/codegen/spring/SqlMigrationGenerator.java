package com.luanvv.codegen.spring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generator for Liquibase SQL migration files
 */
public class SqlMigrationGenerator {
    
    private final CodeGenConfig config;
    private final File resourceOutputDirectory;

    public SqlMigrationGenerator(CodeGenConfig config, File resourceOutputDirectory) {
        this.config = config;
        this.resourceOutputDirectory = resourceOutputDirectory;
    }    public void generate() throws IOException {
        // Create db/migration directory
        File migrationDir = new File(resourceOutputDirectory, "db" + File.separator + "migration");
        if (!migrationDir.exists()) {
            migrationDir.mkdirs();
        }

        String entityNameSnakeCase = camelToSnakeCase(config.getEntityName());
        String expectedFilePattern = "_Create_" + entityNameSnakeCase + "_table.sql";
        
        // Check if a migration file for this entity already exists
        File existingMigrationFile = findExistingMigrationFile(migrationDir, expectedFilePattern);
        
        if (existingMigrationFile != null) {
            System.out.println("Migration file already exists: " + existingMigrationFile.getName() + " (skipping generation)");
            return;
        }

        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = String.format("V%s__Create_%s_table.sql", timestamp, entityNameSnakeCase);
          File sqlFile = new File(migrationDir, filename);
        
        // Use only user-provided SQL content
        String sqlContent = config.getSqlFileContent();
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            throw new IOException("SQL content is required in the 'sqlFileContent' section of the configuration file. " +
                    "The plugin does not generate SQL automatically - please provide your own SQL migration content.");
        }
        
        Files.write(sqlFile.toPath(), sqlContent.getBytes());
        System.out.println("Generated SQL migration file: " + filename);
    }

    private File findExistingMigrationFile(File migrationDir, String expectedFilePattern) {
        if (!migrationDir.exists()) {
            return null;
        }
        
        File[] files = migrationDir.listFiles();
        if (files == null) {
            return null;
        }
        
        for (File file : files) {
            if (file.getName().contains(expectedFilePattern)) {
                return file;
            }
        }        
        return null;
    }

    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
