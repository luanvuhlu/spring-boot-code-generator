# Spring Boot Code Generator Maven Plugin

A Maven plugin that generates Spring Boot boilerplate code from YAML configuration files with extensive customization and override capabilities.

## Features

This plugin generates the following components:
- Liquibase migration SQL files
- JPA Entity classes (with proper package structure: `.entity`)
- Repository interfaces (with proper package structure: `.repository`)
- Service interfaces and implementations (with proper package structure: `.service`)
- REST Controllers (with proper package structure: `.controller`)

### Key Features
- ✅ **Multiple Configuration Files**: Generate code for multiple entities at once
- ✅ **Override Protection**: Skip generation if files already exist to protect manual customizations
- ✅ **Force Regeneration**: Force regenerate files even if they exist
- ✅ **Integrated Build**: No need for additional Maven plugins (build-helper, resources)
- ✅ **Package Organization**: Generates classes in proper subpackages
- ✅ **User-Controlled SQL**: You provide your own SQL migrations

## Configuration

You can generate code for multiple entities by specifying multiple config files:

```xml
<plugin>
    <groupId>com.luanvv.codegen.spring</groupId>
    <artifactId>spring-boot-code-generator</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <configuration>
        <configFiles>
            <configFile>src/main/resources/user-config.yaml</configFile>
            <configFile>src/main/resources/product-config.yaml</configFile>
            <configFile>src/main/resources/order-config.yaml</configFile>
        </configFiles>
    </configuration>
</plugin>
```

## Usage

### Add to your project's pom.xml

**Simple Setup (Recommended):**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.luanvv.codegen.spring</groupId>
            <artifactId>spring-boot-code-generator</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <phase>generate-sources</phase>
                </execution>
            </executions>
            <configuration>
                <configFile>src/main/resources/codegen.yaml</configFile>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Run the plugin

```bash
mvn com.luanvv.codegen.spring:spring-boot-code-generator:generate
```

Or if added to your project's build lifecycle:

```bash
mvn generate-sources
```

⚠️ **Important**: After generation, you must create custom controllers to get working REST endpoints. See the "Custom Controllers Required" section below for details.

## Plugin Parameters

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| `configFile` | `src/main/resources/codegen.yaml` | Path to single YAML configuration file |
| `configFiles` | - | Array of paths to multiple YAML configuration files |
| `outputDirectory` | `${project.build.directory}/generated-sources/java` | Output directory for generated Java sources |
| `testOutputDirectory` | `${project.build.directory}/generated-test-sources/java` | Output directory for generated test sources |
| `resourceOutputDirectory` | `${project.build.directory}/generated-resources` | Output directory for generated resources (SQL files) |
| `skipCodeGen` | `false` | Skip code generation entirely |
| `skipIfExists` | `false` | Skip generation if target files already exist (protect manual changes) |
| `forceRegenerate` | `false` | Force regeneration even if files exist |

## Generated Files

For an entity named `User` with package `com.example.demo`, the plugin generates:

### Main Sources (in proper subpackages)
- `com/example/demo/entity/User.java` - JPA Entity
- `com/example/demo/repository/UserRepository.java` - Spring Data JPA Repository
- `com/example/demo/service/UserService.java` - Service interface
- `com/example/demo/service/base/BaseUserServiceImpl.java` - Base service implementation (always regenerated)
- `com/example/demo/service/UserServiceImpl.java` - Extensible service implementation (generated once)
- `com/example/demo/controller/base/BaseUserController.java` - Base controller (always regenerated, no annotations)
- `com/example/demo/controller/UserController.java` - Extensible controller (generated once, mapped to `/api/default/users`)

⚠️ **Note**: The generated controllers do not provide standard API endpoints. You must create custom controllers for working REST APIs.

### Resources (automatically copied to src/main/resources)
- `src/main/resources/db/migration/V{timestamp}__Create_user_table.sql` - Liquibase migration file

## Customization and Override Behavior

### Override Protection
Use `skipIfExists=true` to protect manually modified files:

```bash
mvn generate-sources -DskipIfExists=true
```

This will skip generation for any files that already exist, allowing you to:
- Manually customize generated classes
- Add custom methods and fields
- Modify generated code without losing changes

### Force Regeneration
Use `forceRegenerate=true` to regenerate all files:

```bash
mvn generate-sources -DforceRegenerate=true
```

### Typical Workflow
1. **Initial Generation**: Run `mvn generate-sources` to generate initial boilerplate
2. **Customize**: Manually modify generated classes as needed
3. **Protected Updates**: Use `skipIfExists=true` for subsequent runs to protect your changes
4. **Full Regeneration**: Use `forceRegenerate=true` when you want to reset everything

## Extensible Code Generation

The plugin generates **extensible** code that allows you to customize business logic without losing changes when regenerating:

### Generated Structure
```
target/generated-sources/java/
└── com/example/demo/
    ├── service/
    │   ├── base/BaseUserServiceImpl.java      # Always regenerated
    │   └── UserServiceImpl.java               # Generated once, then preserved
    └── controller/
        ├── base/BaseUserController.java       # Always regenerated  
        └── UserController.java                # Generated once, then preserved
```

### Key Benefits
- **Base classes** are always regenerated with schema changes
- **Implementation classes** are generated once and preserved
- **Protected access** allows subclasses to access repositories/services
- **Full customization** of business logic and endpoints

### Example: Custom Service Logic
```java
@Service
@Primary
@Transactional  
public class UserServiceImpl extends BaseUserServiceImpl {
    
    @Override
    public User create(User user) {
        validateUser(user);
        user.setCreatedAt(LocalDateTime.now());
        return super.create(user);
    }
    
    // Custom business methods
    public List<User> findActiveUsers() {
        return userRepository.findByActiveTrue();
    }
}
```

## Important: Custom Controllers Required

⚠️ **The generated extensible controllers do not provide working REST endpoints by default.** You MUST create custom controllers to access your API endpoints.

### Why Custom Controllers are Required

The plugin generates:
- **Base controllers** (e.g., `BaseUserController`) - Contains CRUD logic but no annotations
- **Extensible controllers** (e.g., `UserController`) - Has `@RestController` but mapped to `/api/default/{entity}` path

To get working API endpoints at the standard paths (e.g., `/api/users`, `/api/products`), you must create custom controllers.

### Creating Custom Controllers

**Step 1: Create a custom controller class**
```java
package com.example.demo.controller;

import com.example.demo.controller.base.BaseUserController;
import com.example.demo.service.UserService;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")  // Standard API path
@Primary  // Takes precedence over generated controller
public class CustomUserController extends BaseUserController {

    public CustomUserController(UserService userService) {
        super(userService);
    }

    // All CRUD endpoints are inherited from BaseUserController
    // Add custom endpoints here as needed
}
```

**Step 2: Test your endpoints**
```bash
# Now you can access:
curl http://localhost:8080/api/users          # GET all users
curl http://localhost:8080/api/users/1        # GET user by ID
curl -X POST http://localhost:8080/api/users  # POST create user
curl -X PUT http://localhost:8080/api/users/1 # PUT update user
curl -X DELETE http://localhost:8080/api/users/1  # DELETE user
```

### Available Endpoints from Base Controller

When you extend `BaseUserController`, you automatically get:
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID  
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update existing user
- `DELETE /api/users/{id}` - Delete user by ID

### Custom Endpoints Example
```java
@RestController
@RequestMapping("/api/users")
@Primary
public class CustomUserController extends BaseUserController {

    public CustomUserController(UserService userService) {
        super(userService);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        // Your custom search logic
        return ResponseEntity.ok("Custom search results");
    }

    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        // Your custom business logic
        return ResponseEntity.ok(customBusinessLogic());
    }
}
```

## Best Practices

### Transaction Management
The generated base service classes follow Spring transaction best practices:
- **Class-level `@Transactional(readOnly=true)`** for optimal read performance
- **Method-level `@Transactional(readOnly=false)`** for write operations (create, update, delete)
- Read operations inherit the readOnly=true transaction for better performance
- Write operations override with readOnly=false to allow database modifications

When creating custom service methods, follow the same pattern:
```java
@Service
@Primary
public class CustomUserServiceImpl extends BaseUserServiceImpl {
    
    @Transactional(readOnly = false)
    public void customWriteOperation() {
        // Your write logic here
    }
    
    @Transactional(readOnly = true)  // Optional, inherited from class level
    public List<User> customReadOperation() {
        // Your read logic here  
    }
}
```

### Required Custom Controllers
- **Always create custom controllers** for working API endpoints
- Map to standard API paths like `/api/users`, `/api/products`
- Extend base controllers to inherit CRUD operations

### SQL Management
- Always provide your own `sqlFileContent` in the configuration
- The plugin will NOT generate SQL automatically - you have full control
- Use proper Liquibase changesets with author and id
- Include rollback statements when possible

### File Organization
- Generated files are organized in proper Maven package structure
- Entity classes go in `.entity` package
- Repository classes go in `.repository` package  
- Service classes go in `.service` package
- Controller classes go in `.controller` package

### Multiple Entities
- Create separate YAML config files for each entity
- Use the `configFiles` parameter to process multiple entities at once
- Each entity gets its own migration file with timestamp

## Building the Plugin

To build and install the plugin locally:

```bash
mvn clean install
```

## Testing

Run the tests:

```bash
mvn test
```

## Configuration Schema

### Root Properties
- `packageName` (required): Base package name for generated classes
- `entityName` (required): Name of the entity class
- `tableName` (optional): Custom table name (defaults to `entityName.toLowerCase() + "s"`)
- `idFields` (optional): List of field names that represent the primary key
- `fields` (required): List of entity fields
- `sqlFileContent` (required): SQL content for Liquibase migration - YOU must provide this

## Requirements

- Java 17 or higher
- Maven 3.8.1 or higher

## Examples

### Example 1: Simple Entity Generation
```yaml
# user-config.yaml
packageName: com.example.demo
entityName: User
tableName: users
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
  --liquibase formatted sql
  --changeset demo:create-user-table-1
  CREATE TABLE users (
      id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      username VARCHAR(50) NOT NULL UNIQUE,
      email VARCHAR(100) NOT NULL UNIQUE
  );
```

### Example 2: Development Workflow
```bash
# Initial generation
mvn generate-sources

# Customize generated files manually
# Edit UserService.java, add custom methods

# Subsequent generations (protects your changes)
mvn generate-sources -DskipIfExists=true

# Force regeneration (overwrites everything)
mvn generate-sources -DforceRegenerate=true
```

## Repository
GitHub: [spring-boot-code-generator](https://github.com/luanvuhlu/spring-boot-code-generator)
