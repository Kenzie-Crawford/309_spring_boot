# Day 5 — Validation

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Add Bean Validation annotations to DTOs
2. Enable validation in controllers with `@Valid`
3. Understand common validation constraints
4. Handle validation errors and return meaningful messages

---

## 1. Why Validate at the API Layer?

Without validation, your API accepts **anything**:

```json
{
  "make": "",
  "model": null,
  "year": -5,
  "color": "",
  "price": -99999
}
```

This bad data would hit your database. Validation prevents this **before** business logic runs.

### Where Validation Lives

```
Client sends JSON → Controller validates DTO → Service processes → Repository saves
                     ↑
                  Validation happens HERE
```

---

## 2. Adding the Dependency

Bean Validation is included with `spring-boot-starter-web`, but you need the validation starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

> Add this to your `pom.xml` if it's not already there.

---

## 3. Validation Annotations

### Common Annotations

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@NotNull` | Field cannot be null | `@NotNull private String make;` |
| `@NotBlank` | String not null/empty/whitespace | `@NotBlank private String make;` |
| `@NotEmpty` | Collection/String not null/empty | `@NotEmpty private List<String> tags;` |
| `@Size` | String/Collection length bounds | `@Size(min = 2, max = 50)` |
| `@Min` / `@Max` | Numeric minimum/maximum | `@Min(0) private double price;` |
| `@Positive` | Must be > 0 | `@Positive private double price;` |
| `@Email` | Must be valid email format | `@Email private String email;` |
| `@Pattern` | Must match regex | `@Pattern(regexp = "^[A-Z].*")` |

All annotations accept a `message` parameter for custom error messages.

---

## 4. Validating CarRequest

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

    // constructors, getters, setters...
}
```

---

## 5. Enabling Validation with @Valid

Add `@Valid` to the controller parameter:

```java
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

### What Happens Without @Valid

Without `@Valid`, the annotations on the DTO are **ignored**. Validation only triggers when `@Valid` is present on the parameter.

---

## 6. What Happens When Validation Fails

When `@Valid` detects invalid data, Spring throws `MethodArgumentNotValidException`.

By default, Spring returns a **400 Bad Request** with a response like:

```json
{
  "timestamp": "2025-01-15T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/cars"
}
```

The default response isn't very helpful — we'll customize it.

---

## 7. Custom Validation Error Response

We can catch validation errors and return a clean response using `@RestControllerAdvice`:

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

### Now the error response looks like:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "make": "Make is required",
    "price": "Price must be greater than zero",
    "year": "Year must be 1886 or later"
  }
}
```

---

## 8. Testing Validation

### Valid Request (should succeed)

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
→ **201 Created**

### Invalid Request (should fail)

```json
POST /api/cars
{
  "make": "",
  "model": "",
  "year": 1800,
  "color": "",
  "price": -5000
}
```
→ **400 Bad Request** with field-level errors

### Partially Invalid (should fail)

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
→ **400 Bad Request** — only `price` error

---

## 9. Validation on Nested Objects

If your DTO contains another object, use `@Valid` on the nested field:

```java
public class OrderRequest {
    @Valid
    @NotNull
    private CustomerRequest customer;

    @Valid
    @NotNull
    private List<ItemRequest> items;
}
```

---

## 10. Custom Validation (Preview)

You can create custom validators for complex rules:

```java
// Custom annotation
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidYearValidator.class)
public @interface ValidYear {
    String message() default "Invalid year";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class ValidYearValidator implements ConstraintValidator<ValidYear, Integer> {
    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        int currentYear = Year.now().getValue();
        return year >= 1886 && year <= currentYear + 1;
    }
}
```

> Custom validators are optional — built-in annotations cover most cases.

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| `@Valid` | Triggers validation on a controller parameter |
| `@NotBlank` | String required and not empty |
| `@Min` / `@Max` | Numeric range bounds |
| `@Positive` | Must be greater than zero |
| `@Size` | String/Collection length bounds |
| `message` | Custom error message for each constraint |
| `@RestControllerAdvice` | Global handler for validation errors |

---

## Next: Day 6 — Entity Relationships

Tomorrow we'll add a second entity and create relationships between them.
