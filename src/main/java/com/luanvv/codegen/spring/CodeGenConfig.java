package com.luanvv.codegen.spring;

import java.util.List;

/**
 * Configuration model for code generation
 */
public class CodeGenConfig {
      private String packageName;
    private String entityName;
    private String tableName;
    private List<String> idFields;
    private List<Field> fields;
    private String sqlFileContent;

    // Default constructor
    public CodeGenConfig() {}

    // Getters and setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getEntityName() {
        return entityName;
    }    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getIdFields() {
        return idFields;
    }

    public void setIdFields(List<String> idFields) {
        this.idFields = idFields;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String getSqlFileContent() {
        return sqlFileContent;
    }    public void setSqlFileContent(String sqlFileContent) {
        this.sqlFileContent = sqlFileContent;
    }

    /**
     * Get the effective table name to use.
     * Returns the custom tableName if provided, otherwise defaults to entityName + "s" in lowercase.
     */
    public String getEffectiveTableName() {
        if (tableName != null && !tableName.trim().isEmpty()) {
            return tableName;
        }
        return entityName != null ? entityName.toLowerCase() + "s" : null;
    }

    /**
     * Represents a field in the entity
     */
    public static class Field {
        private String name;
        private String type;
        private boolean nullable = true;
        private Integer length;
        private String defaultValue;

        // Default constructor
        public Field() {}

        public Field(String name, String type) {
            this.name = name;
            this.type = type;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * Check if this field is an ID field
         */
        public boolean isIdField(List<String> idFields) {
            return idFields != null && idFields.contains(this.name);
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", nullable=" + nullable +
                    ", length=" + length +
                    ", defaultValue='" + defaultValue + '\'' +
                    '}';
        }
    }    @Override
    public String toString() {
        return "CodeGenConfig{" +
                "packageName='" + packageName + '\'' +
                ", entityName='" + entityName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", idFields=" + idFields +
                ", fields=" + fields +
                ", sqlFileContent='" + sqlFileContent + '\'' +
                '}';
    }
}
