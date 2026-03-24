# Day 2 — REST Controllers

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Explain what a REST API is and how it maps to HTTP methods
2. Create a controller using `@RestController`
3. Map endpoints with `@GetMapping`, `@PostMapping`
4. Use path variables and query parameters
5. Return proper HTTP status codes

---

## 1. What Is a REST API?

REST (Representational State Transfer) is an architectural style for web services.

### Core Idea

Each **resource** (e.g., a Car) gets a URL, and you use **HTTP methods** to interact with it:

| HTTP Method | Purpose | Example URL | Description |
|-------------|---------|-------------|-------------|
| `GET` | Read | `/api/cars` | Get all cars |
| `GET` | Read one | `/api/cars/1` | Get car with ID 1 |
| `POST` | Create | `/api/cars` | Create a new car |
| `PUT` | Update | `/api/cars/1` | Update car with ID 1 |
| `DELETE` | Delete | `/api/cars/1` | Delete car with ID 1 |

### JSON

REST APIs communicate with **JSON** (JavaScript Object Notation):

```json
{
  "id": 1,
  "make": "Toyota",
  "model": "Camry",
  "year": 2023,
  "color": "Silver",
  "price": 28000.0
}
```

Spring Boot automatically converts Java objects to/from JSON using **Jackson**.

---

## 2. @RestController

`@RestController` marks a class as a REST API controller.

```java
package com.example.cardealership.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    // endpoints go here
}
```

### Key Annotations

| Annotation | Purpose |
|-----------|---------|
| `@RestController` | Combines `@Controller` + `@ResponseBody` — returns JSON, not HTML |
| `@RequestMapping("/api/cars")` | Base URL path for all endpoints in this controller |

---

## 3. GET All

```java
@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarRepository carRepository;

    public CarController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @GetMapping
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }
}
```

**Request:** `GET http://localhost:8080/api/cars`

**Response:**
```json
[
  { "id": 1, "make": "Toyota", "model": "Camry", "year": 2023, "color": "Silver", "price": 28000.0 },
  { "id": 2, "make": "Honda", "model": "Civic", "year": 2022, "color": "Blue", "price": 24000.0 }
]
```

---

## 4. GET by ID — Path Variables

Use `@PathVariable` to capture part of the URL:

```java
@GetMapping("/{id}")
public ResponseEntity<Car> getCarById(@PathVariable Long id) {
    return carRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

**Request:** `GET http://localhost:8080/api/cars/1`

**Response:** Single car JSON (200 OK) or 404 Not Found.

### ResponseEntity

`ResponseEntity` lets you control:
- The response body
- The HTTP status code
- Response headers

```java
ResponseEntity.ok(car);              // 200 + body
ResponseEntity.notFound().build();   // 404, no body
ResponseEntity.status(201).body(car); // 201 + body
```

---

## 5. POST — Creating Resources

Use `@PostMapping` and `@RequestBody` to accept JSON input:

```java
@PostMapping
public ResponseEntity<Car> createCar(@RequestBody Car car) {
    Car saved = carRepository.save(car);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

**Request:** `POST http://localhost:8080/api/cars`

**Body (JSON):**
```json
{
  "make": "Chevrolet",
  "model": "Corvette",
  "year": 2024,
  "color": "Yellow",
  "price": 65000
}
```

**Response:** 201 Created + saved car (with generated ID)

### @RequestBody

- Tells Spring to read the HTTP request body
- Jackson automatically converts JSON → Java object
- The `Content-Type` header must be `application/json`

---

## 6. PUT — Updating Resources

```java
@PutMapping("/{id}")
public ResponseEntity<Car> updateCar(@PathVariable Long id, @RequestBody Car carDetails) {
    return carRepository.findById(id)
            .map(car -> {
                car.setMake(carDetails.getMake());
                car.setModel(carDetails.getModel());
                car.setYear(carDetails.getYear());
                car.setColor(carDetails.getColor());
                car.setPrice(carDetails.getPrice());
                Car updated = carRepository.save(car);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
}
```

---

## 7. DELETE

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
    if (carRepository.existsById(id)) {
        carRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // 204
    }
    return ResponseEntity.notFound().build(); // 404
}
```

---

## 8. Path Variables vs. Query Parameters

### Path Variable — identifies a specific resource

```java
@GetMapping("/{id}")
public Car getById(@PathVariable Long id) { ... }
// GET /api/cars/5
```

### Query Parameter — filters or modifies the request

```java
@GetMapping("/search")
public List<Car> searchByMake(@RequestParam String make) {
    return carRepository.findByMake(make);
}
// GET /api/cars/search?make=Toyota
```

| Feature | Path Variable | Query Parameter |
|---------|--------------|-----------------|
| Syntax | `/cars/{id}` | `/cars?make=Toyota` |
| Annotation | `@PathVariable` | `@RequestParam` |
| Use case | Identify resource | Filter/search |
| Required by default | Yes | Yes (use `required = false` to make optional) |

---

## 9. HTTP Status Codes Reference

| Code | Meaning | When to use |
|------|---------|------------|
| `200 OK` | Success | GET, PUT success |
| `201 Created` | Resource created | POST success |
| `204 No Content` | Success, no body | DELETE success |
| `400 Bad Request` | Invalid input | Validation errors |
| `404 Not Found` | Resource doesn't exist | GET/PUT/DELETE with invalid ID |
| `500 Internal Server Error` | Server error | Unhandled exceptions |

---

## 10. Complete Controller

```java
package com.example.cardealership.controller;

import com.example.cardealership.entity.Car;
import com.example.cardealership.repository.CarRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarRepository carRepository;

    public CarController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @GetMapping
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable Long id) {
        return carRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Car> createCar(@RequestBody Car car) {
        Car saved = carRepository.save(car);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Car> updateCar(@PathVariable Long id, @RequestBody Car carDetails) {
        return carRepository.findById(id)
                .map(car -> {
                    car.setMake(carDetails.getMake());
                    car.setModel(carDetails.getModel());
                    car.setYear(carDetails.getYear());
                    car.setColor(carDetails.getColor());
                    car.setPrice(carDetails.getPrice());
                    return ResponseEntity.ok(carRepository.save(car));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        if (carRepository.existsById(id)) {
            carRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

---

## 11. Testing with Postman / Thunder Client

### Install

- **Postman:** [postman.com](https://www.postman.com/downloads/)
- **Thunder Client:** VS Code extension

### Test Sequence

1. **GET all:** `GET http://localhost:8080/api/cars` → Should return array
2. **GET by ID:** `GET http://localhost:8080/api/cars/1` → Should return one car
3. **POST create:** `POST http://localhost:8080/api/cars` with JSON body → Should return 201
4. **PUT update:** `PUT http://localhost:8080/api/cars/1` with JSON body → Should return updated car
5. **DELETE:** `DELETE http://localhost:8080/api/cars/1` → Should return 204
6. **GET deleted:** `GET http://localhost:8080/api/cars/1` → Should return 404

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| `@RestController` | Returns JSON automatically |
| `@RequestMapping` | Sets base URL path |
| `@GetMapping` / `@PostMapping` | Maps HTTP methods to Java methods |
| `@PathVariable` | Captures URL segments |
| `@RequestParam` | Captures query string values |
| `@RequestBody` | Reads JSON from request body |
| `ResponseEntity` | Controls status code + body |

---

## Next: Day 3 — Layered Architecture

Tomorrow we'll extract business logic into a **service layer** for clean separation of concerns.
