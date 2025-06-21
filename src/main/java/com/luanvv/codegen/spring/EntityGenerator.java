package com.luanvv.codegen.spring;

import com.squareup.javapoet.*;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Generator for JPA Entity classes
 */
public class EntityGenerator extends BaseGenerator {

    public EntityGenerator(CodeGenConfig config, File outputDirectory) {
        super(config, outputDirectory);
    }    @Override
    public void generate() throws IOException {
        createPackageDirectory();
        
        // Create the entity class
        TypeSpec.Builder entityBuilder = TypeSpec.classBuilder(config.getEntityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("jakarta.persistence", "Entity"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Table"))
                        .addMember("name", "$S", config.getEntityName().toLowerCase() + "s")
                        .build());

        // Add fields, getters, setters
        for (CodeGenConfig.Field field : config.getFields()) {
            addFieldToEntity(entityBuilder, field);
        }

        // Add default constructor
        entityBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build());

        // Add toString method
        addToStringMethod(entityBuilder);

        // Add equals method
        addEqualsMethod(entityBuilder);

        // Add hashCode method
        addHashCodeMethod(entityBuilder);        // Build the Java file
        String entityPackage = config.getPackageName() + ".entity";
        JavaFile javaFile = JavaFile.builder(entityPackage, entityBuilder.build())
                .build();// Write to file
        javaFile.writeTo(outputDirectory);
        
        System.out.println("Generated Entity class: " + config.getEntityName());
    }

    private void addFieldToEntity(TypeSpec.Builder entityBuilder, CodeGenConfig.Field field) {
        // Determine the Java type
        TypeName fieldType = getJavaType(field.getType());
        
        // Create field with annotations
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, field.getName(), Modifier.PRIVATE);

        // Add JPA annotations
        if (config.getIdFields().contains(field.getName())) {
            fieldBuilder.addAnnotation(ClassName.get("jakarta.persistence", "Id"));
            if ("Long".equals(field.getType()) || "Integer".equals(field.getType())) {
                fieldBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.persistence", "GeneratedValue"))
                        .addMember("strategy", "$T.IDENTITY", ClassName.get("jakarta.persistence", "GenerationType"))
                        .build());
            }
        }

        // Add Column annotation
        AnnotationSpec.Builder columnBuilder = AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Column"));
        columnBuilder.addMember("name", "$S", camelToSnakeCase(field.getName()));
        columnBuilder.addMember("nullable", "$L", field.isNullable());
        
        if (field.getLength() != null && ("String".equals(field.getType()))) {
            columnBuilder.addMember("length", "$L", field.getLength());
        }
        
        fieldBuilder.addAnnotation(columnBuilder.build());

        entityBuilder.addField(fieldBuilder.build());

        // Add getter
        entityBuilder.addMethod(MethodSpec.methodBuilder("get" + capitalize(field.getName()))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return this.$N", field.getName())
                .build());

        // Add setter
        entityBuilder.addMethod(MethodSpec.methodBuilder("set" + capitalize(field.getName()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, field.getName())
                .addStatement("this.$N = $N", field.getName(), field.getName())
                .build());
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
            case "LocalDateTime":
                return ClassName.get(LocalDateTime.class);
            default:
                return ClassName.get(String.class); // Default to String
        }
    }    private void addToStringMethod(TypeSpec.Builder entityBuilder) {
        MethodSpec.Builder toStringMethod = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class);

        StringBuilder returnStatement = new StringBuilder();
        returnStatement.append("return \"").append(config.getEntityName()).append("{\"");
        
        for (int i = 0; i < config.getFields().size(); i++) {
            CodeGenConfig.Field field = config.getFields().get(i);
            if (i > 0) {
                returnStatement.append(" + \", ");
            } else {
                returnStatement.append(" + \"");
            }
            returnStatement.append(field.getName()).append("=\" + ").append(field.getName());
        }
        returnStatement.append(" + \"}\"");

        toStringMethod.addStatement(returnStatement.toString());
        entityBuilder.addMethod(toStringMethod.build());
    }private void addEqualsMethod(TypeSpec.Builder entityBuilder) {
        MethodSpec.Builder equalsBuilder = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(boolean.class)
                .addParameter(Object.class, "o");

        equalsBuilder.addStatement("if (this == o) return true");
        equalsBuilder.addStatement("if (o == null || getClass() != o.getClass()) return false");        equalsBuilder.addStatement("$T that = ($T) o", ClassName.get(config.getPackageName() + ".entity", config.getEntityName()), 
                ClassName.get(config.getPackageName() + ".entity", config.getEntityName()));

        // Build a single return statement with all field comparisons
        if (config.getFields().size() == 1) {
            CodeGenConfig.Field field = config.getFields().get(0);
            equalsBuilder.addStatement("return $T.equals($N, that.$N)", Objects.class, field.getName(), field.getName());
        } else if (config.getFields().size() > 1) {
            StringBuilder returnStatement = new StringBuilder("return ");
            for (int i = 0; i < config.getFields().size(); i++) {
                CodeGenConfig.Field field = config.getFields().get(i);
                if (i > 0) {
                    returnStatement.append(" && ");
                }
                returnStatement.append("Objects.equals(").append(field.getName()).append(", that.").append(field.getName()).append(")");
            }
            equalsBuilder.addStatement(returnStatement.toString());
        } else {
            equalsBuilder.addStatement("return true");
        }

        entityBuilder.addMethod(equalsBuilder.build());
    }

    private void addHashCodeMethod(TypeSpec.Builder entityBuilder) {
        StringBuilder hashFields = new StringBuilder();
        for (int i = 0; i < config.getFields().size(); i++) {
            if (i > 0) {
                hashFields.append(", ");
            }
            hashFields.append(config.getFields().get(i).getName());
        }

        entityBuilder.addMethod(MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(int.class)
                .addStatement("return $T.hash($L)", Objects.class, hashFields.toString())                .build());
    }
}
