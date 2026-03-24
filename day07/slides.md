# Day 7 — Exception Handling

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Create custom exception classes
2. Build a global exception handler with `@ControllerAdvice`
3. Return consistent, structured error responses
4. Handle different error scenarios (not found, validation, generic)
5. Prevent stack traces from leaking to the client

---

## 1. The Problem: Inconsistent Errors

Right now, our API returns errors in different formats:

### RuntimeException (e.g., "Car not found")
```json
{
  "timestamp": "2025-01-15T10:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/cars/999"
}
```

### Validation error (from Day 5)
```json
{
  "status": 400,
  "error": "Validation Failed",
  "errors": { "make": "Make is required" }
}
```

### Problems:
- Different structure depending on error type
- 500 status for "not found" (should be 404)
- Stack traces may leak in some scenarios
- No consistent error contract for frontend developers

---

## 2. The Solution: Standardized Error Response

Create a consistent error format for **all** errors:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Car not found with id: 999",
  "timestamp": "2025-01-15T10:30:00"
}
```

---

## 3. Error Response DTO

```java
package com.example.cardealership.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

### For validation errors (with field details):

```java
package com.example.cardealership.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ValidationErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;
    private LocalDateTime timestamp;

    public ValidationErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

---

## 4. Custom Exceptions

### ResourceNotFoundException

```java
package com.example.cardealership.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
```

### DuplicateResourceException

```java
package com.example.cardealership.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

---

## 5. Global Exception Handler

```java
package com.example.cardealership.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle "not found" errors
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Handle duplicate resource errors
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "One or more fields are invalid",
            fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }

    // Handle illegal argument errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // Catch-all: handle any unexpected error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred"
        );
        // Log the actual error (don't expose to client)
        // logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### Key Points

| Feature | Purpose |
|---------|---------|
| `@RestControllerAdvice` | Applies to all controllers globally |
| `@ExceptionHandler` | Catches a specific exception type |
| Catch-all handler | Prevents stack traces from leaking |
| Custom error response | Consistent format for all errors |

---

## 6. Updating the Service to Use Custom Exceptions

Replace `RuntimeException` with `ResourceNotFoundException`:

```java
@Override
public CarResponse getCarById(Long id) {
    Car car = carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car", id));
    return carMapper.toResponse(car);
}

@Override
public CarResponse updateCar(Long id, CarRequest request) {
    Car car = carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car", id));
    carMapper.updateEntity(car, request);
    return carMapper.toResponse(carRepository.save(car));
}

@Override
public void deleteCar(Long id) {
    Car car = carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car", id));
    carRepository.delete(car);
}
```

---

## 7. Simplifying the Controller

Now that the service throws specific exceptions (caught by the global handler), the controller becomes very clean:

```java
@GetMapping("/{id}")
public ResponseEntity<CarResponse> getCarById(@PathVariable Long id) {
    return ResponseEntity.ok(carService.getCarById(id));
    // If not found, service throws ResourceNotFoundException
    // GlobalExceptionHandler catches it → returns 404
}
```

No more try-catch or if-else in the controller!

---

## 8. Error Response Examples

### 404 Not Found
```
GET /api/cars/999
```
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Car not found with id: 999",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 400 Validation Error
```
POST /api/cars  { "make": "", "price": -100 }
```
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "fieldErrors": {
    "make": "Make is required",
    "price": "Price must be greater than zero"
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

### 409 Conflict
```
POST /api/owners  { "email": "john@example.com" }  (already exists)
```
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Owner with email john@example.com already exists",
  "timestamp": "2025-01-15T10:30:00"
}
```

### 500 Internal Server Error (no details leaked)
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2025-01-15T10:30:00"
}
```

---

## 9. Why This Pattern Matters

### Without Global Exception Handler
- Controllers need try-catch everywhere
- Error formats are inconsistent
- Stack traces may leak to clients
- Every developer handles errors differently

### With Global Exception Handler
- Controllers are clean — just delegate to service
- All errors follow the same format
- Stack traces are logged server-side only
- Frontend developers have a predictable error contract

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| `@RestControllerAdvice` | Global handler for all controller exceptions |
| `@ExceptionHandler` | Catches and handles specific exception types |
| Custom exceptions | `ResourceNotFoundException`, `DuplicateResourceException` |
| Error response DTO | Consistent `{ status, error, message, timestamp }` |
| Catch-all handler | Prevents stack traces from reaching the client |
| Service layer | Throws custom exceptions instead of generic ones |

---

## Next: Day 8 — Advanced Queries

Tomorrow we'll add filtering, pagination, and sorting to our API.
