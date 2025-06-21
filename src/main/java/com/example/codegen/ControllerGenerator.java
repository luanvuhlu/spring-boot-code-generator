package com.example.codegen;

import com.squareup.javapoet.*;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Generator for REST Controller classes
 */
public class ControllerGenerator extends BaseGenerator {

    public ControllerGenerator(CodeGenConfig config, File outputDirectory) {
        super(config, outputDirectory);
    }    @Override
    public void generate() throws IOException {
        createPackageDirectory();
        
        String controllerName = config.getEntityName() + "Controller";
        String serviceName = config.getEntityName() + "Service";
        
        ClassName entityClass = ClassName.get(config.getPackageName(), config.getEntityName());
        ClassName serviceClass = ClassName.get(config.getPackageName(), serviceName);
        TypeName idType = getIdType();

        // Create the controller class
        TypeSpec.Builder controllerBuilder = TypeSpec.classBuilder(controllerName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                        .addMember("value", "$S", "/api/" + config.getEntityName().toLowerCase() + "s")
                        .build());

        // Add service field
        FieldSpec serviceField = FieldSpec.builder(serviceClass, uncapitalize(serviceName))
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();
        controllerBuilder.addField(serviceField);

        // Add constructor
        controllerBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(serviceClass, uncapitalize(serviceName))
                .addStatement("this.$N = $N", uncapitalize(serviceName), uncapitalize(serviceName))
                .build());

        // Add CRUD endpoints
        addCrudEndpoints(controllerBuilder, entityClass, idType, serviceName);

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(config.getPackageName(), controllerBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory.getParentFile());
        
        System.out.println("Generated Controller class: " + controllerName);
    }

    private void addCrudEndpoints(TypeSpec.Builder controllerBuilder, ClassName entityClass, TypeName idType, String serviceName) {
        String entityVar = uncapitalize(config.getEntityName());
        String serviceVar = uncapitalize(serviceName);

        // GET /api/entities - Get all entities
        controllerBuilder.addMethod(MethodSpec.methodBuilder("getAll" + config.getEntityName() + "s")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityClass))
                .addStatement("return $N.findAll()", serviceVar)
                .build());        // GET /api/entities/{id} - Get entity by id
        controllerBuilder.addMethod(MethodSpec.methodBuilder("get" + config.getEntityName() + "ById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .returns(ParameterizedTypeName.get(ClassName.get("org.springframework.http", "ResponseEntity"), entityClass))
                .addParameter(ParameterSpec.builder(idType, "id")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                        .build())
                .addStatement("$T<$T> entity = $N.findById(id)", Optional.class, entityClass, serviceVar)
                .addStatement("return entity.map($T::ok).orElse($T.notFound().build())", 
                        ClassName.get("org.springframework.http", "ResponseEntity"),
                        ClassName.get("org.springframework.http", "ResponseEntity"))
                .build());        // POST /api/entities - Create new entity
        controllerBuilder.addMethod(MethodSpec.methodBuilder("create" + config.getEntityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                .returns(ParameterizedTypeName.get(ClassName.get("org.springframework.http", "ResponseEntity"), entityClass))
                .addParameter(ParameterSpec.builder(entityClass, entityVar)
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestBody"))
                        .addAnnotation(ClassName.get("jakarta.validation", "Valid"))
                        .build())
                .addStatement("$T created = $N.create($N)", entityClass, serviceVar, entityVar)
                .addStatement("return $T.status($T.CREATED).body(created)", 
                        ClassName.get("org.springframework.http", "ResponseEntity"),
                        ClassName.get("org.springframework.http", "HttpStatus"))
                .build());        // PUT /api/entities/{id} - Update entity
        controllerBuilder.addMethod(MethodSpec.methodBuilder("update" + config.getEntityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PutMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .returns(ParameterizedTypeName.get(ClassName.get("org.springframework.http", "ResponseEntity"), entityClass))
                .addParameter(ParameterSpec.builder(idType, "id")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                        .build())
                .addParameter(ParameterSpec.builder(entityClass, entityVar)
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestBody"))
                        .addAnnotation(ClassName.get("jakarta.validation", "Valid"))
                        .build())
                .beginControlFlow("try")
                .addStatement("$T updated = $N.update(id, $N)", entityClass, serviceVar, entityVar)
                .addStatement("return $T.ok(updated)", ClassName.get("org.springframework.http", "ResponseEntity"))
                .nextControlFlow("catch ($T e)", RuntimeException.class)
                .addStatement("return $T.notFound().build()", ClassName.get("org.springframework.http", "ResponseEntity"))
                .endControlFlow()
                .build());        // DELETE /api/entities/{id} - Delete entity
        controllerBuilder.addMethod(MethodSpec.methodBuilder("delete" + config.getEntityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping"))
                        .addMember("value", "$S", "/{id}")
                        .build())
                .returns(ParameterizedTypeName.get(ClassName.get("org.springframework.http", "ResponseEntity"), WildcardTypeName.subtypeOf(Object.class)))
                .addParameter(ParameterSpec.builder(idType, "id")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                        .build())
                .beginControlFlow("try")
                .addStatement("$N.deleteById(id)", serviceVar)
                .addStatement("return $T.noContent().build()", ClassName.get("org.springframework.http", "ResponseEntity"))
                .nextControlFlow("catch ($T e)", RuntimeException.class)
                .addStatement("return $T.notFound().build()", ClassName.get("org.springframework.http", "ResponseEntity"))
                .endControlFlow()
                .build());
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
