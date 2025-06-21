#!/bin/bash

echo "========================================"
echo "Spring Boot Code Generator Demo"
echo "========================================"
echo

echo "Step 1: Installing the code generator plugin..."
cd ..
./mvnw clean install -q
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to install the code generator plugin"
    exit 1
fi
echo "✓ Code generator plugin installed successfully"
echo

echo "Step 2: Moving to example project..."
cd example
echo "Current directory: $(pwd)"
echo

echo "Step 3: Generating code from YAML configuration..."
../mvnw com.example:spring-boot-code-generator:generate
if [ $? -ne 0 ]; then
    echo "ERROR: Code generation failed"
    exit 1
fi
echo "✓ Code generation completed"
echo

echo "Step 4: Listing generated files..."
echo "Generated Java sources:"
if [ -d "target/generated-sources/java" ]; then
    find target/generated-sources/java -name "*.java" -type f
else
    echo "No Java sources generated yet (stubs only)"
fi
echo

echo "Generated SQL migrations:"
if [ -d "target/generated-resources/db/migration" ]; then
    find target/generated-resources/db/migration -name "*.sql" -type f
else
    echo "No SQL migrations found"
fi
echo

echo "Step 5: Compiling the project..."
../mvnw clean compile
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed"
    exit 1
fi
echo "✓ Project compiled successfully"
echo

echo "Step 6: Running tests..."
../mvnw test
if [ $? -ne 0 ]; then
    echo "ERROR: Tests failed"
    exit 1
fi
echo "✓ All tests passed"
echo

echo "========================================"
echo "Demo completed successfully!"
echo "========================================"
echo
echo "Generated files are located in:"
echo "- target/generated-sources/java/"
echo "- target/generated-test-sources/java/"
echo "- target/generated-resources/db/migration/"
echo
echo "To run the application:"
echo "  ../mvnw spring-boot:run"
echo
