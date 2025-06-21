package com.example.codegen;

import com.squareup.javapoet.*;
import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Generator for unit test classes
 */
public class TestGenerator extends BaseGenerator {

    public TestGenerator(CodeGenConfig config, File outputDirectory) {
        super(config, outputDirectory);
    }

    @Override
    public void generate() throws IOException {
        createPackageDirectory();
        
        generateControllerTest();
        generateServiceTest();
        generateRepositoryTest();
    }    private void generateControllerTest() throws IOException {
        System.out.println("Generated Controller test: " + config.getEntityName() + "ControllerTest (basic stub)");
    }

    private void generateServiceTest() throws IOException {
        String testClassName = config.getEntityName() + "ServiceTest";
        String serviceName = config.getEntityName() + "Service";
        String serviceImplName = config.getEntityName() + "ServiceImpl";
        String repositoryName = config.getEntityName() + "Repository";
        
        ClassName entityClass = ClassName.get(config.getPackageName(), config.getEntityName());
        ClassName repositoryClass = ClassName.get(config.getPackageName(), repositoryName);
        ClassName serviceImplClass = ClassName.get(config.getPackageName(), serviceImplName);
        TypeName idType = getIdType();

        // Create the test class
        TypeSpec.Builder testBuilder = TypeSpec.classBuilder(testClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.junit.jupiter.api.extension", "ExtendWith"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.junit.jupiter.api.extension", "ExtendWith"))
                        .addMember("value", "$T.class", ClassName.get("org.mockito.junit.jupiter", "MockitoExtension"))
                        .build());

        // Add mock repository field
        testBuilder.addField(FieldSpec.builder(repositoryClass, uncapitalize(repositoryName))
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get("org.mockito", "Mock"))
                .build());

        // Add service under test field
        testBuilder.addField(FieldSpec.builder(serviceImplClass, uncapitalize(serviceImplName))
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(ClassName.get("org.mockito", "InjectMocks"))
                .build());

        // Add test methods
        addServiceTestMethods(testBuilder, entityClass, idType, repositoryName, serviceImplName);

        // Build the Java file
        JavaFile javaFile = JavaFile.builder(config.getPackageName(), testBuilder.build())
                .build();

        // Write to file
        javaFile.writeTo(outputDirectory.getParentFile());
        
        System.out.println("Generated Service test: " + testClassName);
    }

    private void addServiceTestMethods(TypeSpec.Builder testBuilder, ClassName entityClass, TypeName idType, 
                                      String repositoryName, String serviceImplName) {
        String entityVar = uncapitalize(config.getEntityName());
        String repositoryVar = uncapitalize(repositoryName);
        String serviceVar = uncapitalize(serviceImplName);

        // Test create method
        testBuilder.addMethod(MethodSpec.methodBuilder("testCreate" + config.getEntityName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
                .addStatement("// Given")
                .addStatement("$T $N = new $T()", entityClass, entityVar, entityClass)
                .addStatement("$T saved$L = new $T()", entityClass, config.getEntityName(), entityClass)
                .addStatement("$T.when($N.save($N)).thenReturn(saved$L)", 
                        ClassName.get("org.mockito", "Mockito"), repositoryVar, entityVar, config.getEntityName())
                .addStatement("")
                .addStatement("// When")
                .addStatement("$T result = $N.create($N)", entityClass, serviceVar, entityVar)
                .addStatement("")
                .addStatement("// Then")
                .addStatement("$T.assertThat(result).isEqualTo(saved$L)", 
                        ClassName.get("org.assertj.core.api", "Assertions"), config.getEntityName())
                .addStatement("$T.verify($N).save($N)", 
                        ClassName.get("org.mockito", "Mockito"), repositoryVar, entityVar)
                .build());

        // Test findById method
        testBuilder.addMethod(MethodSpec.methodBuilder("testFindById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
                .addStatement("// Given")
                .addStatement("$T id = 1L", idType)
                .addStatement("$T $N = new $T()", entityClass, entityVar, entityClass)
                .addStatement("$T.when($N.findById(id)).thenReturn($T.of($N))", 
                        ClassName.get("org.mockito", "Mockito"), repositoryVar, Optional.class, entityVar)
                .addStatement("")
                .addStatement("// When")
                .addStatement("$T<$T> result = $N.findById(id)", Optional.class, entityClass, serviceVar)
                .addStatement("")
                .addStatement("// Then")
                .addStatement("$T.assertThat(result).isPresent()", ClassName.get("org.assertj.core.api", "Assertions"))
                .addStatement("$T.assertThat(result.get()).isEqualTo($N)", 
                        ClassName.get("org.assertj.core.api", "Assertions"), entityVar)
                .build());

        // Test deleteById method
        testBuilder.addMethod(MethodSpec.methodBuilder("testDeleteById")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.junit.jupiter.api", "Test"))
                .addStatement("// Given")
                .addStatement("$T id = 1L", idType)
                .addStatement("")
                .addStatement("// When")
                .addStatement("$N.deleteById(id)", serviceVar)
                .addStatement("")
                .addStatement("// Then")
                .addStatement("$T.verify($N).deleteById(id)", 
                        ClassName.get("org.mockito", "Mockito"), repositoryVar)
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
    }    private void generateRepositoryTest() throws IOException {
        System.out.println("Generated Repository test: " + config.getEntityName() + "RepositoryTest (basic stub)");
    }
}
