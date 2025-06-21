package com.example.codegen;

import com.squareup.javapoet.*;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Generator for Service interface and implementation classes
 */
public class ServiceGenerator extends BaseGenerator {

    public ServiceGenerator(CodeGenConfig config, File outputDirectory) {
        super(config, outputDirectory);
    }

    @Override
    public void generate() throws IOException {
        createPackageDirectory();
        
        generateServiceInterface();
        generateServiceImplementation();
    }    private void generateServiceInterface() throws IOException {
        String serviceName = config.getEntityName() + "Service";
        ClassName entityClass = ClassName.get(config.getPackageName(), config.getEntityName());
        TypeName idType = getIdType();

        // Create the service interface
        TypeSpec.Builder serviceBuilder = TypeSpec.interfaceBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC);

        // Add CRUD methods
        serviceBuilder.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(entityClass)
                .addParameter(entityClass, uncapitalize(config.getEntityName()))
                .build());

        serviceBuilder.addMethod(MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), entityClass))
                .addParameter(idType, "id")
                .build());

        serviceBuilder.addMethod(MethodSpec.methodBuilder("findAll")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityClass))
                .build());

        serviceBuilder.addMethod(MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(entityClass)
                .addParameter(idType, "id")
                .addParameter(entityClass, uncapitalize(config.getEntityName()))
                .build());

        serviceBuilder.addMethod(MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(void.class)
                .addParameter(idType, "id")
                .build());

        // Add custom methods for unique fields
        for (CodeGenConfig.Field field : config.getFields()) {
            if ("email".equals(field.getName()) || "username".equals(field.getName())) {
                serviceBuilder.addMethod(MethodSpec.methodBuilder("findBy" + capitalize(field.getName()))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), entityClass))
                        .addParameter(getJavaType(field.getType()), field.getName())
                        .build());
            }
        }

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(config.getPackageName(), serviceBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory.getParentFile());
        
        System.out.println("Generated Service interface: " + serviceName);
    }

    private void generateServiceImplementation() throws IOException {
        String serviceName = config.getEntityName() + "Service";
        String serviceImplName = config.getEntityName() + "ServiceImpl";
        String repositoryName = config.getEntityName() + "Repository";
        
        ClassName entityClass = ClassName.get(config.getPackageName(), config.getEntityName());
        ClassName serviceInterface = ClassName.get(config.getPackageName(), serviceName);
        ClassName repositoryClass = ClassName.get(config.getPackageName(), repositoryName);
        TypeName idType = getIdType();

        // Create the service implementation class
        TypeSpec.Builder serviceImplBuilder = TypeSpec.classBuilder(serviceImplName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .addSuperinterface(serviceInterface);

        // Add repository field
        FieldSpec repositoryField = FieldSpec.builder(repositoryClass, uncapitalize(repositoryName))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
        serviceImplBuilder.addField(repositoryField);

        // Add constructor
        serviceImplBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryClass, uncapitalize(repositoryName))
                .addStatement("this.$N = $N", uncapitalize(repositoryName), uncapitalize(repositoryName))
                .build());

        // Implement CRUD methods
        addCrudMethods(serviceImplBuilder, entityClass, idType, repositoryName);

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(config.getPackageName(), serviceImplBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory.getParentFile());
        
        System.out.println("Generated Service implementation: " + serviceImplName);
    }

    private void addCrudMethods(TypeSpec.Builder serviceImplBuilder, ClassName entityClass, TypeName idType, String repositoryName) {
        String entityVar = uncapitalize(config.getEntityName());
        String repositoryVar = uncapitalize(repositoryName);

        // Create method
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(entityClass)
                .addParameter(entityClass, entityVar)
                .addStatement("return $N.save($N)", repositoryVar, entityVar)
                .build());

        // FindById method
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), entityClass))
                .addParameter(idType, "id")
                .addStatement("return $N.findById(id)", repositoryVar)
                .build());

        // FindAll method
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("findAll")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityClass))
                .addStatement("return $N.findAll()", repositoryVar)
                .build());

        // Update method
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(entityClass)
                .addParameter(idType, "id")
                .addParameter(entityClass, entityVar)
                .addStatement("$T existing = $N.findById(id).orElseThrow(() -> new $T(\"$L not found with id: \" + id))",
                        entityClass, repositoryVar, RuntimeException.class, config.getEntityName())
                .addCode(generateUpdateCode(entityVar))
                .addStatement("return $N.save(existing)", repositoryVar)
                .build());

        // DeleteById method
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(void.class)
                .addParameter(idType, "id")
                .addStatement("$N.deleteById(id)", repositoryVar)
                .build());

        // Add custom methods for unique fields
        for (CodeGenConfig.Field field : config.getFields()) {
            if ("email".equals(field.getName()) || "username".equals(field.getName())) {
                serviceImplBuilder.addMethod(MethodSpec.methodBuilder("findBy" + capitalize(field.getName()))
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), entityClass))
                        .addParameter(getJavaType(field.getType()), field.getName())
                        .addStatement("return $N.findBy$L($N)", repositoryVar, capitalize(field.getName()), field.getName())
                        .build());
            }
        }
    }

    private CodeBlock generateUpdateCode(String entityVar) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        
        for (CodeGenConfig.Field field : config.getFields()) {
            if (!config.getIdFields().contains(field.getName())) {
                codeBuilder.addStatement("existing.set$L($N.get$L())", 
                        capitalize(field.getName()), entityVar, capitalize(field.getName()));
            }
        }
        
        return codeBuilder.build();
    }

    private TypeName getIdType() {
        for (CodeGenConfig.Field field : config.getFields()) {
            if (config.getIdFields().contains(field.getName())) {
                return getJavaType(field.getType());
            }
        }
        return ClassName.get(Long.class);
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
}
