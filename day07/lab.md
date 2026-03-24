# Day 7 Lab — Global Exception Handling

## Objective

Implement standardized error handling across the entire API.

---

## Part 1: Create Error Response Classes (10 min)

Create `com.example.cardealership.exception.ErrorResponse`:

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

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

Create `com.example.cardealership.exception.ValidationErrorResponse`:

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

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

---

## Part 2: Create Custom Exceptions (5 min)

Create `com.example.cardealership.exception.ResourceNotFoundException`:

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

---

## Part 3: Create the Global Exception Handler (10 min)

Replace the `ValidationExceptionHandler` from Day 5 with a comprehensive `GlobalExceptionHandler`:

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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
            fieldErrors.put(e.getField(), e.getDefaultMessage())
        );
        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "One or more fields are invalid",
            fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

> **Delete** the old `ValidationExceptionHandler` if you created one in Day 5.

---

## Part 4: Update Services to Use Custom Exceptions (10 min)

Replace all `RuntimeException("... not found ...")` with `ResourceNotFoundException`:

**CarServiceImpl:**
```java
import com.example.cardealership.exception.ResourceNotFoundException;

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

Do the same for `OwnerServiceImpl`.

### Checkpoint ✅

App compiles and runs with custom exceptions.

---

## Part 5: Test All Error Scenarios (15 min)

### Test 1: Resource Not Found (404)
```
GET /api/cars/999
```
**Expected:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Car not found with id: 999",
  "timestamp": "..."
}
```

### Test 2: Validation Error (400)
```
POST /api/cars
{ "make": "", "model": "", "year": 0, "color": "", "price": 0 }
```
**Expected:**
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "fieldErrors": {
    "make": "Make is required",
    "color": "Color is required",
    "price": "Price must be greater than zero"
  },
  "timestamp": "..."
}
```

### Test 3: Update Non-Existent Resource (404)
```
PUT /api/cars/999
{ "make": "Toyota", "model": "Test", "year": 2024, "color": "Red", "price": 30000 }
```
**Expected:** 404 with "Car not found with id: 999"

### Test 4: Delete Non-Existent Resource (404)
```
DELETE /api/cars/999
```
**Expected:** 404

### Test 5: Valid Request (200/201)
```
POST /api/cars
{ "make": "Toyota", "model": "Camry", "year": 2024, "color": "Silver", "price": 28000 }
```
**Expected:** 201 Created (errors don't break valid requests)

### Checkpoint ✅

All scenarios return the correct status code and consistent error format.

---

## Part 6: Verify No Stack Traces Leaked (5 min)

Check that **no error response** includes:
- Java class names
- Package paths
- Stack trace lines
- Internal implementation details

The catch-all handler ensures unexpected errors return a generic message.

---

## Deliverables

- [ ] `ErrorResponse` and `ValidationErrorResponse` DTOs
- [ ] `ResourceNotFoundException` custom exception
- [ ] `GlobalExceptionHandler` with handlers for all error types
- [ ] Services updated to throw `ResourceNotFoundException`
- [ ] Old `ValidationExceptionHandler` removed
- [ ] All 5 test scenarios produce correct responses
- [ ] No stack traces leaked to the client
