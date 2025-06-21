# Demo Spring Boot Application

This is an example Spring Boot project that demonstrates how to use the Spring Boot Code Generator Maven Plugin.

## Overview

This project shows how to:
- Configure the code generator plugin in your `pom.xml`
- Create a YAML configuration file to define your entities
- Generate Spring Boot boilerplate code automatically
- Integrate generated code with your Spring Boot application

## Project Structure

```
example/
├── pom.xml                                    # Maven configuration with code generator plugin
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   └── DemoApplication.java          # Main Spring Boot application
│   │   └── resources/
│   │       ├── codegen.yaml                  # Code generator configuration
│   │       ├── application.properties        # Spring Boot configuration
│   │       └── db/changelog/
│   │           └── db.changelog-master.xml   # Liquibase master changelog
│   └── test/
│       └── java/                             # Test files (generated and manual)
└── target/
    ├── generated-sources/java/               # Generated Java source files
    ├── generated-test-sources/java/          # Generated test files
    └── generated-resources/db/migration/     # Generated SQL migration files
```

## Configuration

### Code Generator Configuration (`src/main/resources/codegen.yaml`)

The YAML file defines:
- **packageName**: Base package for generated classes
- **entityName**: Name of the entity (e.g., "User")
- **idFields**: List of fields that make up the primary key
- **fields**: List of entity fields with their properties
- **sqlFileContent**: Liquibase SQL migration content

### Example Configuration

```yaml
packageName: com.example.demo
entityName: User
idFields:
  - id
fields:
  - name: id
    type: Long
    nullable: false
  - name: username
    type: String
    nullable: false
    length: 50
  - name: email
    type: String
    nullable: false
    length: 100
sqlFileContent: |
  CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL
  );
```

## Generated Components

When you run the code generator, it will create:

### Main Sources (`target/generated-sources/java/`)
- **User.java** - JPA Entity with annotations
- **UserRepository.java** - Spring Data JPA Repository interface
- **UserService.java** - Service interface
- **UserServiceImpl.java** - Service implementation
- **UserController.java** - REST Controller with CRUD endpoints

### Test Sources (`target/generated-test-sources/java/`)
- **UserControllerTest.java** - Controller unit tests
- **UserServiceTest.java** - Service unit tests
- **UserRepositoryTest.java** - Repository unit tests

### Resources (`target/generated-resources/`)
- **db/migration/V{timestamp}__Create_user_table.sql** - Liquibase migration

## How to Run

### 1. Generate Code

```bash
# Navigate to example directory
cd example

# Generate code using the plugin
mvn com.example:spring-boot-code-generator:generate

# Or use the generate-sources phase
mvn generate-sources
```

### 2. Build the Project

```bash
# Compile the project (includes generated sources)
mvn clean compile

# Run tests (includes generated tests)
mvn test

# Package the application
mvn clean package
```

### 3. Run the Application

```bash
# Run the Spring Boot application
mvn spring-boot:run

# Or run the JAR file
java -jar target/demo-app-1.0.0-SNAPSHOT.jar
```

## API Endpoints (Generated)

Once code generation is complete, the following REST endpoints will be available:

- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create a new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

## Development Workflow

1. **Modify Configuration**: Update `src/main/resources/codegen.yaml`
2. **Regenerate Code**: Run `mvn generate-sources`
3. **Review Generated Code**: Check `target/generated-sources/`
4. **Test**: Run `mvn test` to verify generated tests pass
5. **Build**: Run `mvn clean package`
6. **Run**: Start the application with `mvn spring-boot:run`

## Customization

You can customize the generated code by:
1. Modifying the YAML configuration
2. Extending the code generator plugin
3. Adding custom templates
4. Implementing additional generators
