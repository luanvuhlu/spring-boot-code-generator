package com.luanvv.codegen.spring;

import java.io.File;
import java.io.IOException;

/**
 * Base class for all code generators
 */
public abstract class BaseGenerator {
    
    protected final CodeGenConfig config;
    protected final File outputDirectory;

    public BaseGenerator(CodeGenConfig config, File outputDirectory) {
        this.config = config;
        this.outputDirectory = outputDirectory;
    }

    /**
     * Generate the component
     */
    public abstract void generate() throws IOException;

    /**
     * Get the package directory for the configured package
     */
    protected File getPackageDirectory() {
        String packagePath = config.getPackageName().replace('.', File.separatorChar);
        return new File(outputDirectory, packagePath);
    }

    /**
     * Ensure the package directory exists
     */
    protected void createPackageDirectory() {
        File packageDir = getPackageDirectory();
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
    }

    /**
     * Convert camelCase to snake_case
     */
    protected String camelToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Convert snake_case to camelCase
     */
    protected String snakeToCamelCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Capitalize first letter
     */
    protected String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Convert first letter to lowercase
     */
    protected String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
