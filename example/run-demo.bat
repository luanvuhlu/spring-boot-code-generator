@echo off
echo ========================================
echo Spring Boot Code Generator Demo
echo ========================================
echo.

echo Step 1: Installing the code generator plugin...
cd ..
call mvnw.cmd clean install -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to install the code generator plugin
    pause
    exit /b 1
)
echo ✓ Code generator plugin installed successfully
echo.

echo Step 2: Moving to example project...
cd example
echo Current directory: %cd%
echo.

echo Step 3: Generating code from YAML configuration...
call ..\mvnw.cmd com.example:spring-boot-code-generator:generate
if %ERRORLEVEL% neq 0 (
    echo ERROR: Code generation failed
    pause
    exit /b 1
)
echo ✓ Code generation completed
echo.

echo Step 4: Listing generated files...
echo Generated Java sources:
if exist "target\generated-sources\java" (
    dir /s /b target\generated-sources\java\*.java
) else (
    echo No Java sources generated yet (stubs only)
)
echo.

echo Generated SQL migrations:
if exist "target\generated-resources\db\migration" (
    dir /s /b target\generated-resources\db\migration\*.sql
) else (
    echo No SQL migrations found
)
echo.

echo Step 5: Compiling the project...
call ..\mvnw.cmd clean compile
if %ERRORLEVEL% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)
echo ✓ Project compiled successfully
echo.

echo Step 6: Running tests...
call ..\mvnw.cmd test
if %ERRORLEVEL% neq 0 (
    echo ERROR: Tests failed
    pause
    exit /b 1
)
echo ✓ All tests passed
echo.

echo ========================================
echo Demo completed successfully!
echo ========================================
echo.
echo Generated files are located in:
echo - target\generated-sources\java\
echo - target\generated-test-sources\java\
echo - target\generated-resources\db\migration\
echo.
echo To run the application:
echo   ..\mvnw.cmd spring-boot:run
echo.
pause
