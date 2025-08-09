# Controller Layer Architecture

This document describes the Controller layer implementation that aligns with the ideal architecture pattern for Issue #112:

**External Systems → Controller → StreamConverter → Commands**

## Overview

The Controller layer provides a clean separation of concerns by:

- **Controllers** manage command configuration and external I/O
- **StreamConverter** handles command orchestration and stream processing  
- **Commands** focus purely on stream transformation logic

This architecture replaces the direct usage patterns seen in Main.java and Examples, providing better abstraction and easier integration with external systems.

## Architecture Components

### 1. IStreamController Interface

```java
public interface IStreamController {
    List<CommandResult> process(InputStream inputStream, OutputStream outputStream) throws IOException;
    boolean isConfigured();
    String getConfigurationDescription();
    String getInputDataType();
    String getOutputDataType();
}
```

**Purpose**: Defines the contract for all controllers, ensuring consistent behavior across different processing scenarios.

**Key Benefits**:
- Standardized interface for external systems
- Clear separation from StreamConverter internals
- Type-safe input/output specifications

### 2. AbstractStreamController Base Class

```java
public abstract class AbstractStreamController implements IStreamController {
    protected abstract CommandConfig[] configureCommands();
    // Common functionality: lifecycle management, error handling, logging
}
```

**Purpose**: Provides common controller functionality while allowing specialized command configuration.

**Key Features**:
- Lazy configuration with validation
- Automatic StreamConverter lifecycle management
- Comprehensive error handling and logging
- Command pipeline creation using CommandFactory

### 3. Concrete Controller Implementations

#### CsvProcessingController
```java
// Column extraction
CsvProcessingController controller = CsvProcessingController.forColumnExtraction("name");

// Complex processing pipeline  
CsvProcessingController controller = CsvProcessingController.forComplexProcessing("email", "validator");

// Pass-through processing
CsvProcessingController controller = CsvProcessingController.forPassThrough();
```

#### JsonProcessingController
```java
// Property extraction with validation
JsonProcessingController controller = JsonProcessingController.forPropertyExtraction("user.name", true);

// Multi-stage transformation
JsonProcessingController controller = JsonProcessingController.forTransformation(
    "data.items", "processor-1", "processor-2", "formatter"
);

// JSON formatting only
JsonProcessingController controller = JsonProcessingController.forFormatting();
```

### 4. ControllerFactory for Advanced Management

```java
// Get controller by data types
IStreamController controller = ControllerFactory.getController("CSV", "JSON_PROPERTY");

// Use builders for specific configuration
IStreamController controller = ControllerFactory.getCsvController()
    .forColumnExtraction("name");

// Register custom controllers
ControllerFactory.registerController("CUSTOM_CSV", "PROCESSED", customController);
```

## Architecture Benefits

### 1. Clean Separation of Concerns

**Before (Main.java approach)**:
```java
// External systems directly create StreamConverter
IStreamCommand[] commands = {new CsvNavigateCommand("name")};
StreamConverter converter = new StreamConverter(commands);
converter.run(inputStream, outputStream);
```

**After (Controller approach)**:
```java
// Controllers encapsulate configuration complexity
IStreamController controller = CsvProcessingController.forColumnExtraction("name");
List<CommandResult> results = controller.process(inputStream, outputStream);
```

### 2. Complex Configuration Management

Controllers hide complex configuration rules and provide simple, use-case-focused interfaces:

```java
// Complex multi-stage JSON processing made simple
JsonProcessingController controller = JsonProcessingController.forTransformation(
    "users[*].profile", "validator", "enricher", "formatter"
);
```

Internally, this creates a sophisticated command pipeline:
- JsonNavigateCommand for property extraction
- ValidationDecorator for data validation  
- Multiple SampleStreamCommand instances for processing stages
- Automatic error handling and logging

### 3. External System Integration

The Controller layer provides multiple integration patterns:

**File-based Integration**:
```java
try (FileInputStream input = new FileInputStream("data.csv");
     FileOutputStream output = new FileOutputStream("result.txt")) {
    controller.process(input, output);
}
```

**Stream-based Integration**:
```java
try (InputStream input = externalSystem.getDataStream();
     OutputStream output = externalSystem.getOutputSink()) {
    List<CommandResult> results = controller.process(input, output);
    externalSystem.handleResults(results);
}
```

**Type-based Routing**:
```java
// External systems can route based on data types
String inputType = detectDataType(inputStream);
IStreamController controller = ControllerFactory.getController(inputType, "PROCESSED_DATA");
if (controller != null) {
    controller.process(inputStream, outputStream);
}
```

## Usage Examples

### Basic CSV Processing
```java
// Extract a specific column from CSV data
IStreamController controller = CsvProcessingController.forColumnExtraction("email");
controller.process(csvInputStream, outputStream);
```

### Advanced JSON Transformation
```java
// Multi-stage JSON processing with validation
IStreamController controller = JsonProcessingController.forTransformation(
    "data.users", "user-validator", "role-extractor", "output-formatter"
);
List<CommandResult> results = controller.process(jsonInputStream, outputStream);
```

### External System Integration
```java
// Factory-based controller creation
IStreamController controller = ControllerFactory.getController("CSV", "JSON_PROPERTY");
if (controller.isConfigured()) {
    List<CommandResult> results = controller.process(inputStream, outputStream);
    // Process results...
}
```

## Migration from Current Approach

### Old Pattern (Main.java/Examples)
1. External systems directly instantiate commands
2. External systems create StreamConverter instances  
3. Configuration logic scattered across client code
4. No standardized error handling or logging
5. Tight coupling between external systems and StreamConverter

### New Pattern (Controller Layer)
1. External systems use Controller interfaces
2. Controllers encapsulate command configuration
3. Controllers manage StreamConverter lifecycle
4. Standardized error handling and result reporting
5. Clean separation: External → Controller → StreamConverter → Commands

## Implementation Details

### Command Configuration
Controllers use the existing CommandFactory and CommandConfig infrastructure:

```java
@Override
protected CommandConfig[] configureCommands() {
    return new CommandConfig[] {
        new CommandConfig(CsvNavigateCommand.class, "Extract column: " + columnSelector, columnSelector),
        new CommandConfig(SampleStreamCommand.class, "Process with: " + processorId, processorId)
    };
}
```

### Error Handling
Controllers provide comprehensive error handling:
- Input validation with meaningful error messages
- StreamConverter exception handling and rethrowing
- Detailed logging for debugging and monitoring
- Graceful degradation for configuration issues

### Performance Considerations
- Lazy configuration reduces startup overhead
- Controller caching in ControllerFactory for reuse
- Minimal overhead over direct StreamConverter usage
- Leverages existing StreamConverter optimization features

## Testing and Validation

The Controller layer includes comprehensive validation:

1. **Configuration Validation**: Ensures all required parameters are provided
2. **Type Compatibility**: Validates input/output type combinations
3. **Command Pipeline Validation**: Verifies command configurations before execution
4. **Integration Testing**: Demonstrates various external system integration patterns

## Future Extensions

The Controller architecture supports easy extension:

1. **New Data Types**: Add new controller implementations and builders
2. **Custom Processing Scenarios**: Extend existing controllers or create new ones
3. **External System Adapters**: Create specialized controllers for specific integration patterns
4. **Advanced Configuration**: Add configuration files, dependency injection, etc.

## Conclusion

The Controller layer successfully implements the ideal architecture pattern while maintaining backward compatibility with existing StreamConverter functionality. It provides:

- **Clean Architecture**: Clear separation of concerns with well-defined boundaries
- **Easy Integration**: Simple interfaces for external systems
- **Configuration Management**: Centralized, validated command configuration
- **Extensibility**: Easy to add new controllers and processing scenarios
- **Maintainability**: Reduced coupling and improved code organization

This implementation demonstrates how architectural patterns can improve code quality while maintaining the powerful stream processing capabilities of the existing system.