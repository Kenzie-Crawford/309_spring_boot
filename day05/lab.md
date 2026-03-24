# Day 5 Lab — Adding Validation

## Objective

Add Bean Validation to the Car API and handle validation errors with clean responses.

---

## Part 1: Add the Dependency (2 min)

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Part 2: Add Validation Annotations to CarRequest (10 min)

Update `CarRequest`:

```java
package com.example.cardealership.dto;

import jakarta.validation.constraints.*;

public class CarRequest {

    @NotBlank(message = "Make is required")
    @Size(min = 2, max = 50, message = "Make must be between 2 and 50 characters")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    private String model;

    @Min(value = 1886, message = "Year must be 1886 or later")
    @Max(value = 2026, message = "Year cannot be more than 1 year in the future")
    private int year;

    @NotBlank(message = "Color is required")
    private String color;

    @Positive(message = "Price must be greater than zero")
    private double price;

    // Keep existing constructors, getters, setters
}
```

---

## Part 3: Enable @Valid in the Controller (5 min)

Add `@Valid` to the POST and PUT endpoints:

```java
import jakarta.validation.Valid;

@PostMapping
public ResponseEntity<CarResponse> createCar(@Valid @RequestBody CarRequest request) {
    CarResponse response = carService.createCar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

@PutMapping("/{id}")
public ResponseEntity<CarResponse> updateCar(
        @PathVariable Long id,
        @Valid @RequestBody CarRequest request) {
    return ResponseEntity.ok(carService.updateCar(id, request));
}
```

### Checkpoint ✅

Send an invalid POST request and confirm you get a 400 error.

---

## Part 4: Create the Validation Error Handler (10 min)

Create `com.example.cardealership.exception.ValidationExceptionHandler`:

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
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("error", "Validation Failed");
        response.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }
}
```

### Checkpoint ✅

Invalid requests now return a clean JSON error like:
```json
{
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "make": "Make is required",
    "price": "Price must be greater than zero"
  }
}
```

---

## Part 5: Test All Validation Scenarios (15 min)

Test each scenario with Postman:

### Test 1: All fields empty
```json
POST /api/cars
{
  "make": "",
  "model": "",
  "year": 0,
  "color": "",
  "price": 0
}
```
**Expected:** 400 with multiple errors

### Test 2: Only one field invalid
```json
POST /api/cars
{
  "make": "Toyota",
  "model": "Camry",
  "year": 2024,
  "color": "Silver",
  "price": -100
}
```
**Expected:** 400 with only `price` error

### Test 3: Year too old
```json
POST /api/cars
{
  "make": "Ford",
  "model": "Model T",
  "year": 1800,
  "color": "Black",
  "price": 5000
}
```
**Expected:** 400 with `year` error

### Test 4: Make too short
```json
POST /api/cars
{
  "make": "X",
  "model": "Test",
  "year": 2024,
  "color": "Red",
  "price": 20000
}
```
**Expected:** 400 with `make` error

### Test 5: Valid request still works
```json
POST /api/cars
{
  "make": "Toyota",
  "model": "Camry",
  "year": 2024,
  "color": "Silver",
  "price": 28000
}
```
**Expected:** 201 Created

### Test 6: PUT with invalid data
```json
PUT /api/cars/1
{
  "make": "",
  "model": "Camry",
  "year": 2024,
  "color": "Silver",
  "price": 28000
}
```
**Expected:** 400 with `make` error

### Checkpoint ✅

All 6 tests produce expected results.

---

## Part 6: Remove Service-Layer Validation (5 min)

Now that the controller validates via `@Valid`, you can **remove** the manual validation from the service layer (the if-statements from Day 3).

The service can trust that incoming data is already validated.

> **Note:** Keep any business rules that go beyond simple field validation (e.g., checking for duplicates).

---

## Deliverables

- [ ] `spring-boot-starter-validation` dependency added
- [ ] `CarRequest` annotated with validation constraints
- [ ] `@Valid` added to POST and PUT endpoints
- [ ] `ValidationExceptionHandler` returns clean error JSON
- [ ] All 6 test scenarios produce expected results
- [ ] Valid requests still succeed with 201/200
