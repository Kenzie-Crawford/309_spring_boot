# Day 3 — Layered Architecture

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Explain the three-layer architecture (Controller → Service → Repository)
2. Describe why separation of concerns matters
3. Create a service layer and move business logic out of the controller
4. Use constructor injection with Spring's `@Service` annotation
5. Write unit tests for the service layer using Mockito

---

## 1. The Problem: Fat Controllers

Look at our current controller:

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
                return ResponseEntity.ok(carRepository.save(car));
            })
            .orElse(ResponseEntity.notFound().build());
}
```

### What's wrong?

- The controller is doing **business logic** (finding, updating, saving)
- The controller talks **directly** to the repository
- If we need the same logic elsewhere, we'd have to **duplicate** it
- **Testing** becomes harder — you can't test business logic without HTTP

---

## 2. Three-Layer Architecture

```
┌──────────────────────────┐
│      Controller          │  ← Handles HTTP (request/response)
│  @RestController         │
└──────────┬───────────────┘
           │ calls
┌──────────▼───────────────┐
│       Service            │  ← Business logic
│  @Service                │
└──────────┬───────────────┘
           │ calls
┌──────────▼───────────────┐
│      Repository          │  ← Data access
│  JpaRepository           │
└──────────────────────────┘
```

### Responsibilities

| Layer | Responsibility | Knows about |
|-------|---------------|-------------|
| **Controller** | HTTP concerns — parse request, return response, set status codes | Service |
| **Service** | Business logic — validation, transformation, orchestration | Repository |
| **Repository** | Data access — CRUD operations on the database | Database |

### Key Rules

1. **Controllers never touch repositories directly**
2. **Services contain all business logic**
3. **Each layer only talks to the layer below it**

---

## 3. Creating the Service Layer

### Step 1: Create the Service Interface (Optional but recommended)

```java
package com.example.cardealership.service;

import com.example.cardealership.entity.Car;
import java.util.List;

public interface CarService {
    List<Car> getAllCars();
    Car getCarById(Long id);
    Car createCar(Car car);
    Car updateCar(Long id, Car carDetails);
    void deleteCar(Long id);
}
```

### Step 2: Create the Service Implementation

```java
package com.example.cardealership.service;

import com.example.cardealership.entity.Car;
import com.example.cardealership.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;

    public CarServiceImpl(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Override
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Override
    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
    }

    @Override
    public Car createCar(Car car) {
        return carRepository.save(car);
    }

    @Override
    public Car updateCar(Long id, Car carDetails) {
        Car car = getCarById(id);
        car.setMake(carDetails.getMake());
        car.setModel(carDetails.getModel());
        car.setYear(carDetails.getYear());
        car.setColor(carDetails.getColor());
        car.setPrice(carDetails.getPrice());
        return carRepository.save(car);
    }

    @Override
    public void deleteCar(Long id) {
        Car car = getCarById(id);
        carRepository.delete(car);
    }
}
```

### Key Annotations

| Annotation | Purpose |
|-----------|---------|
| `@Service` | Marks this class as a Spring-managed service bean |
| Constructor injection | Spring auto-injects `CarRepository` |

---

## 4. Refactoring the Controller

**Before (fat controller):**
```java
@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarRepository carRepository;  // ← talks to repo directly
    // ...
}
```

**After (thin controller):**
```java
@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarService carService;  // ← talks to service only

    public CarController(CarService carService) {
        this.carService = carService;
    }
}
```

### Complete Refactored Controller

```java
package com.example.cardealership.controller;

import com.example.cardealership.entity.Car;
import com.example.cardealership.service.CarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public List<Car> getAllCars() {
        return carService.getAllCars();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable Long id) {
        Car car = carService.getCarById(id);
        return ResponseEntity.ok(car);
    }

    @PostMapping
    public ResponseEntity<Car> createCar(@RequestBody Car car) {
        Car saved = carService.createCar(car);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Car> updateCar(@PathVariable Long id, @RequestBody Car carDetails) {
        Car updated = carService.updateCar(id, carDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }
}
```

### What Changed?

| Before | After |
|--------|-------|
| Controller had `CarRepository` | Controller has `CarService` |
| find/save/delete logic in controller | One-line delegation to service |
| `Optional` handling in controller | Service throws exception if not found |
| Business logic mixed with HTTP logic | Clean separation |

---

## 5. Why This Pattern Matters

### Reusability
The same service can be used by:
- REST controllers
- Scheduled tasks
- Message listeners
- Other services

### Testability
You can test:
- **Controller:** mock the service, test HTTP behavior
- **Service:** mock the repository, test business logic
- **Repository:** test data access independently

### Maintainability
- Adding business rules? → Change the **service**
- Changing the API shape? → Change the **controller**
- Switching databases? → Change the **repository**

---

## 6. Dependency Injection in Spring

Spring manages object creation and wiring through **dependency injection**.

### Constructor Injection (Recommended)

```java
@Service
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;

    // Spring auto-injects CarRepository
    public CarServiceImpl(CarRepository carRepository) {
        this.carRepository = carRepository;
    }
}
```

### Why Constructor Injection?
- Fields can be `final` (immutable)
- All dependencies are clear and required
- Easy to test — just pass mocks in the constructor
- No need for `@Autowired` when there's only one constructor

---

## 7. Adding Business Logic to the Service

The service layer is where you add logic that goes **beyond simple CRUD**:

```java
@Override
public Car createCar(Car car) {
    // Business rule: price cannot be negative
    if (car.getPrice() < 0) {
        throw new IllegalArgumentException("Price cannot be negative");
    }

    // Business rule: year must be reasonable
    if (car.getYear() < 1886 || car.getYear() > 2026) {
        throw new IllegalArgumentException("Invalid year: " + car.getYear());
    }

    return carRepository.save(car);
}
```

This validation belongs in the **service**, not the controller, because:
- It's a business rule, not an HTTP concern
- It applies regardless of how the car is created (API, batch import, etc.)

---

## 8. Unit Testing the Service Layer

One of the biggest benefits of layered architecture: **each layer can be tested independently.**

You already know JUnit. In Spring Boot, the new concept is **mocking** — replacing real dependencies with fakes so you can test one class in isolation.

### Setup

```java
@ExtendWith(MockitoExtension.class)   // Enables Mockito in JUnit 5
class CarServiceImplTest {

    @Mock                              // Fake repository — no database needed
    private CarRepository carRepository;

    @InjectMocks                       // Creates CarServiceImpl with the mock injected
    private CarServiceImpl carService;
}
```

### Writing a Test

```java
@Test
void getCarById_found() {
    // Arrange — tell mock what to return
    Car car = new Car("Toyota", "Camry", 2023, "Silver", 28000);
    when(carRepository.findById(1L)).thenReturn(Optional.of(car));

    // Act — call the method under test
    Car result = carService.getCarById(1L);

    // Assert — verify the result
    assertEquals("Toyota", result.getMake());
}
```

### Key Mockito Methods

| Method | Purpose |
|--------|---------|
| `when(...).thenReturn(...)` | Define return value for a mock call |
| `verify(mock).method()` | Assert a mock method was called |
| `verify(mock, never()).method()` | Assert a mock method was NOT called |
| `any(Class.class)` | Matcher — accepts any argument of that type |

> **Why not just hit the real database?** Unit tests should be fast and isolated. If the test fails, you know the bug is in your service logic — not in the database, network, or Spring wiring.

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| Three layers | Controller → Service → Repository |
| `@Service` | Marks a class as a service bean |
| Controller role | HTTP concerns only — delegate to service |
| Service role | Business logic — orchestrate repository calls |
| Constructor injection | Preferred DI method — clean, testable, immutable |
| Unit testing | Mock dependencies with `@Mock` and `@InjectMocks` — test logic in isolation |

---

## Next: Day 4 — DTOs & Mapping

Tomorrow we'll stop exposing our entities directly in API responses and introduce DTOs.
