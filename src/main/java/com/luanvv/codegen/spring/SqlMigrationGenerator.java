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
        
        // Write SQL content
        String sqlContent = config.getSqlFileContent();
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            sqlContent = generateDefaultSqlContent();
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

    private String generateDefaultSqlContent() {
        StringBuilder sql = new StringBuilder();
        sql.append("--liquibase formatted sql\n\n");
        sql.append("--changeset author:1\n");
        sql.append("CREATE TABLE ").append(camelToSnakeCase(config.getEntityName())).append(" (\n");
        
        for (CodeGenConfig.Field field : config.getFields()) {
            sql.append("    ").append(camelToSnakeCase(field.getName()));
            sql.append(" ").append(mapJavaTypeToSqlType(field.getType()));
            
            if (!field.isNullable()) {
                sql.append(" NOT NULL");
            }
            
            if (config.getIdFields() != null && config.getIdFields().contains(field.getName())) {
                sql.append(" PRIMARY KEY");
            }
            
            sql.append(",\n");
        }
        
        // Remove last comma
        if (sql.length() > 2) {
            sql.setLength(sql.length() - 2);
            sql.append("\n");
        }
        
        sql.append(");\n\n");
        sql.append("--rollback DROP TABLE ").append(camelToSnakeCase(config.getEntityName())).append(";");
        
        return sql.toString();
    }

    private String mapJavaTypeToSqlType(String javaType) {
        switch (javaType) {
            case "String":
                return "VARCHAR(255)";
            case "Long":
                return "BIGINT";
            case "Integer":
                return "INTEGER";
            case "Short":
                return "SMALLINT";
            case "Byte":
                return "TINYINT";
            case "Double":
                return "DOUBLE";
            case "Float":
                return "FLOAT";
            case "BigDecimal":
                return "DECIMAL(19,2)";
            case "Boolean":
                return "BOOLEAN";
            case "LocalDate":
                return "DATE";
            case "LocalDateTime":
                return "TIMESTAMP";
            case "LocalTime":
                return "TIME";
            case "UUID":
                return "UUID";
            default:
                return "VARCHAR(255)";
        }
    }

    private String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
