package com.luanvv.codegen.spring;

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
    }    @Override
    public void generate() throws IOException {
        createPackageDirectory();
        
        generateServiceInterface();
        generateBaseServiceImplementation();
        generateExtensibleServiceImplementation();
    }

    private void generateServiceInterface() throws IOException {
        String serviceName = config.getEntityName() + "Service";
        ClassName entityClass = ClassName.get(config.getPackageName() + ".entity", config.getEntityName());
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
                        .build());            }
        }

        // Build the Java file
        String servicePackage = config.getPackageName() + ".service";
        JavaFile javaFile = JavaFile.builder(servicePackage, serviceBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory);
        
        System.out.println("Generated Service interface: " + serviceName);
    }

    private void generateBaseServiceImplementation() throws IOException {
        String serviceName = config.getEntityName() + "Service";
        String baseServiceImplName = "Base" + config.getEntityName() + "ServiceImpl";
        String repositoryName = config.getEntityName() + "Repository";
        String baseServicePackage = config.getPackageName() + ".service.base";
        
        ClassName entityClass = ClassName.get(config.getPackageName() + ".entity", config.getEntityName());
        ClassName serviceInterface = ClassName.get(config.getPackageName() + ".service", serviceName);
        ClassName repositoryClass = ClassName.get(config.getPackageName() + ".repository", repositoryName);
        TypeName idType = getIdType();        // Create the base service implementation class (abstract)
        TypeSpec.Builder baseServiceImplBuilder = TypeSpec.classBuilder(baseServiceImplName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                        .addMember("readOnly", "true")
                        .build())
                .addSuperinterface(serviceInterface)
                .addJavadoc("""
                        Base service implementation for $L.
                        
                        This abstract class provides common CRUD operations with proper transaction management:
                        - Class-level @Transactional(readOnly=true) for optimal read performance
                        - Method-level @Transactional(readOnly=false) for write operations (create, update, delete)
                        
                        Read operations (findById, findAll, findByXxx) inherit the readOnly=true transaction,
                        which provides better performance and prevents accidental writes.
                        
                        Write operations (create, update, delete) override with readOnly=false to allow
                        database modifications within the transaction.
                        
                        This class is always regenerated - do not modify directly.
                        Extend this class in your service implementations to inherit this behavior.
                        """, config.getEntityName());

        // Add repository field
        FieldSpec repositoryField = FieldSpec.builder(repositoryClass, uncapitalize(repositoryName))
                .addModifiers(Modifier.PROTECTED, Modifier.FINAL) // Protected so subclasses can access
                .build();
        baseServiceImplBuilder.addField(repositoryField);

        // Add constructor
        baseServiceImplBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryClass, uncapitalize(repositoryName))
                .addStatement("this.$N = $N", uncapitalize(repositoryName), uncapitalize(repositoryName))
                .build());

        // Implement CRUD methods
        addCrudMethods(baseServiceImplBuilder, entityClass, idType, repositoryName);

        // Create base package directory
        String basePackagePath = config.getPackageName().replace('.', File.separatorChar) + File.separatorChar + "service" + File.separatorChar + "base";
        File basePackageDir = new File(outputDirectory, basePackagePath);
        if (!basePackageDir.exists()) {
            basePackageDir.mkdirs();
        }

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(baseServicePackage, baseServiceImplBuilder.build())
                .build();

        // Write to file (always regenerate base classes)
        javaFile.writeTo(outputDirectory);
        
        System.out.println("Generated Base Service implementation: " + baseServiceImplName);
    }    private void generateExtensibleServiceImplementation() throws IOException {
        String serviceImplName = config.getEntityName() + "ServiceImpl";
        String baseServiceImplName = "Base" + config.getEntityName() + "ServiceImpl";
        String repositoryName = config.getEntityName() + "Repository";
        String servicePackage = config.getPackageName() + ".service";
        
        ClassName baseServiceClass = ClassName.get(config.getPackageName() + ".service.base", baseServiceImplName);
        ClassName repositoryClass = ClassName.get(config.getPackageName() + ".repository", repositoryName);

        // Check if we should skip generation of the extensible implementation
        File targetFile = getTargetFile(serviceImplName, "service");
        if (shouldSkipFile(targetFile)) {
            return;
        }        // Create the extensible service implementation class
        TypeSpec.Builder serviceImplBuilder = TypeSpec.classBuilder(serviceImplName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.stereotype", "Service"))
                        .addMember("value", "$S", "default" + config.getEntityName() + "Service")
                        .build())
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .superclass(baseServiceClass);

        // Add constructor that calls super
        serviceImplBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(repositoryClass, uncapitalize(repositoryName))
                .addStatement("super($N)", uncapitalize(repositoryName))
                .build());        // Add comment explaining extensibility
        serviceImplBuilder.addJavadoc("""
                Default service implementation for $L.
                
                This class extends $L and provides a default implementation that can be replaced
                by creating a custom service with @Primary annotation.
                
                Transaction Management:
                - Inherits @Transactional(readOnly=true) from base class for read operations
                - Write operations have @Transactional(readOnly=false) for database modifications
                - Add @Transactional annotations to custom methods as needed
                
                To create a custom implementation:
                1. Create a new class that extends $L
                2. Add @Service and @Primary annotations 
                3. Add @Transactional for custom transaction management if needed
                4. Your custom service will automatically override this default one
                
                Example:
                @Service
                @Primary
                public class Custom$LImpl extends $L {
                    
                    @Transactional(readOnly = false)
                    public void customWriteOperation() {
                        // Your custom write logic
                    }
                    
                    @Transactional(readOnly = true)
                    public CustomEntity customReadOperation() {
                        // Your custom read logic
                    }
                }
                
                This file is generated only once - subsequent generations will skip this file
                if it already exists, allowing you to maintain your customizations.
                """, config.getEntityName(), baseServiceImplName, baseServiceImplName, 
                config.getEntityName(), baseServiceImplName);

        // Add example custom method template (commented out)
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("customBusinessLogic")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addJavadoc("Example custom method - implement your business logic here")
                .addComment("TODO: Implement custom business logic")
                .addComment("Example: validation, complex queries, business rules, etc.")
                .build());

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(servicePackage, serviceImplBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory);
          System.out.println("Generated Service implementation: " + serviceImplName);
    }    private void addCrudMethods(TypeSpec.Builder serviceImplBuilder, ClassName entityClass, TypeName idType, String repositoryName) {
        String entityVar = uncapitalize(config.getEntityName());
        String repositoryVar = uncapitalize(repositoryName);

        // Create method - write operation, override readOnly=true
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                        .addMember("readOnly", "false")
                        .build())
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
                .build());        // Update method - write operation, override readOnly=true
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                        .addMember("readOnly", "false")
                        .build())
                .returns(entityClass)
                .addParameter(idType, "id")
                .addParameter(entityClass, entityVar)
                .addStatement("$T existing = $N.findById(id).orElseThrow(() -> new $T(\"$L not found with id: \" + id))",
                        entityClass, repositoryVar, RuntimeException.class, config.getEntityName())
                .addCode(generateUpdateCode(entityVar))
                .addStatement("return $N.save(existing)", repositoryVar)
                .build());        // DeleteById method - write operation, override readOnly=true
        serviceImplBuilder.addMethod(MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                        .addMember("readOnly", "false")
                        .build())
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
