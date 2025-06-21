package com.example.codegen;

import com.squareup.javapoet.*;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Generator for Spring Data JPA Repository interfaces
 */
public class RepositoryGenerator extends BaseGenerator {

    public RepositoryGenerator(CodeGenConfig config, File outputDirectory) {
        super(config, outputDirectory);
    }    @Override
    public void generate() throws IOException {
        createPackageDirectory();
        
        String repositoryName = config.getEntityName() + "Repository";
        
        // Determine the ID type from the first ID field
        TypeName idType = getIdType();
        
        // Create the repository interface
        TypeSpec.Builder repositoryBuilder = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Repository"))
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
                        ClassName.get(config.getPackageName(), config.getEntityName()),
                        idType
                ));

        // Add custom query methods
        addCustomQueryMethods(repositoryBuilder);

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(config.getPackageName(), repositoryBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory.getParentFile());
        
        System.out.println("Generated Repository interface: " + repositoryName);
    }

    private TypeName getIdType() {
        // Find the first ID field and return its type
        for (CodeGenConfig.Field field : config.getFields()) {
            if (config.getIdFields().contains(field.getName())) {
                return getJavaType(field.getType());
            }
        }
        return ClassName.get(Long.class); // Default to Long
    }

    private TypeName getJavaType(String type) {
        switch (type) {
            case "String":
                return ClassName.get(String.class);
            case "Long":
                return ClassName.get(Long.class);
            case "Integer":
                return ClassName.get(Integer.class);
            case "Boolean":
                return ClassName.get(Boolean.class);
            default:
                return ClassName.get(String.class);
        }
    }

    private void addCustomQueryMethods(TypeSpec.Builder repositoryBuilder) {
        ClassName entityClass = ClassName.get(config.getPackageName(), config.getEntityName());
        
        // Add findBy methods for unique fields
        for (CodeGenConfig.Field field : config.getFields()) {
            if (!config.getIdFields().contains(field.getName()) && 
                ("String".equals(field.getType()) || "email".equals(field.getName()) || "username".equals(field.getName()))) {
                
                // Add findByFieldName method
                MethodSpec findByMethod = MethodSpec.methodBuilder("findBy" + capitalize(field.getName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), entityClass))
                        .addParameter(getJavaType(field.getType()), field.getName())
                        .build();
                
                repositoryBuilder.addMethod(findByMethod);

                // Add existsByFieldName method for unique fields
                if ("email".equals(field.getName()) || "username".equals(field.getName())) {
                    MethodSpec existsByMethod = MethodSpec.methodBuilder("existsBy" + capitalize(field.getName()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(boolean.class)
                            .addParameter(getJavaType(field.getType()), field.getName())
                            .build();
                    
                    repositoryBuilder.addMethod(existsByMethod);
                }
            }
        }

        // Add findAllByActive method if there's an active field
        boolean hasActiveField = config.getFields().stream()
                .anyMatch(field -> "active".equals(field.getName()) && "Boolean".equals(field.getType()));
        
        if (hasActiveField) {
            MethodSpec findAllByActiveMethod = MethodSpec.methodBuilder("findAllByActive")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityClass))
                    .addParameter(Boolean.class, "active")
                    .build();
            
            repositoryBuilder.addMethod(findAllByActiveMethod);
        }
    }
}
