# Day 4 — DTOs & Mapping

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Explain why exposing entities directly in APIs is problematic
2. Create DTO (Data Transfer Object) classes for requests and responses
3. Map between entities and DTOs manually
4. Separate internal data model from external API contract

---

## 1. The Problem: Exposing Entities

Currently our API returns the **entity** directly:

```java
@GetMapping
public List<Car> getAllCars() {  // ← returns JPA entity
    return carService.getAllCars();
}
```

### Why is this bad?

| Problem | Example |
|---------|---------|
| **Leaking internal structure** | Database column names, IDs, internal fields visible |
| **Tight coupling** | Changing the entity breaks the API |
| **Security risk** | Sensitive fields (password, internal flags) exposed |
| **No control over shape** | Can't customize what the API returns vs. what's stored |
| **Serialization issues** | Lazy-loaded relationships can cause errors |

### Real-World Example

```java
@Entity
public class User {
    private Long id;
    private String username;
    private String passwordHash;    // ← DO NOT expose this
    private String internalRole;    // ← internal use only
    private LocalDateTime createdAt;
    // ...
}
```

If you return this entity, the API exposes `passwordHash` and `internalRole`.

---

## 2. What Is a DTO?

A **Data Transfer Object (DTO)** is a plain Java class that defines the shape of data sent to/from the API.

```
Client  ←→  Controller  ←→  Service  ←→  Repository  ←→  Database
         ↑               ↑
      DTOs used       Entities used
      (external)      (internal)
```

### Two Types of DTOs

| DTO Type | Purpose | Example |
|----------|---------|---------|
| **Request DTO** | Data the client sends to create/update | `CarRequest` |
| **Response DTO** | Data the API returns to the client | `CarResponse` |

---

## 3. Creating DTOs

### CarRequest — what the client sends

```java
package com.example.cardealership.dto;

public class CarRequest {
    private String make;
    private String model;
    private int year;
    private String color;
    private double price;

    // Default constructor
    public CarRequest() {}

    public CarRequest(String make, String model, int year, String color, double price) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
    }

    // Getters and setters
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
```

### CarResponse — what the API returns

```java
package com.example.cardealership.dto;

public class CarResponse {
    private Long id;
    private String make;
    private String model;
    private int year;
    private String color;
    private double price;

    public CarResponse() {}

    public CarResponse(Long id, String make, String model, int year, String color, double price) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
```

### Key Differences

| | Entity (Car) | Request DTO | Response DTO |
|-|-------------|-------------|--------------|
| `id` | Yes | **No** (server generates it) | Yes |
| JPA annotations | Yes | No | No |
| Internal fields | Can have | Never | Never |
| Used by | Repository | Controller input | Controller output |

---

## 4. Mapping: Entity ↔ DTO

Mapping is converting between entities and DTOs. We'll do it **manually** (no library needed).

### Entity → Response DTO

```java
// In the service or a dedicated mapper class
public static CarResponse toResponse(Car car) {
    return new CarResponse(
        car.getId(),
        car.getMake(),
        car.getModel(),
        car.getYear(),
        car.getColor(),
        car.getPrice()
    );
}
```

### Request DTO → Entity

```java
public static Car toEntity(CarRequest request) {
    return new Car(
        request.getMake(),
        request.getModel(),
        request.getYear(),
        request.getColor(),
        request.getPrice()
    );
}
```

### Where to Put Mapping Logic

**Option A: Static methods on the DTO** (simple and works well)

```java
public class CarResponse {
    // ... fields, getters, setters ...

    public static CarResponse fromEntity(Car car) {
        return new CarResponse(
            car.getId(), car.getMake(), car.getModel(),
            car.getYear(), car.getColor(), car.getPrice()
        );
    }
}
```

**Option B: Dedicated mapper class** (scales better)

```java
package com.example.cardealership.mapper;

import com.example.cardealership.dto.*;
import com.example.cardealership.entity.Car;
import org.springframework.stereotype.Component;

@Component
public class CarMapper {

    public CarResponse toResponse(Car car) {
        CarResponse response = new CarResponse();
        response.setId(car.getId());
        response.setMake(car.getMake());
        response.setModel(car.getModel());
        response.setYear(car.getYear());
        response.setColor(car.getColor());
        response.setPrice(car.getPrice());
        return response;
    }

    public Car toEntity(CarRequest request) {
        Car car = new Car();
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setYear(request.getYear());
        car.setColor(request.getColor());
        car.setPrice(request.getPrice());
        return car;
    }
}
```

---

## 5. Updating the Service

The service now accepts DTOs from the controller and returns DTOs:

```java
@Service
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    public CarServiceImpl(CarRepository carRepository, CarMapper carMapper) {
        this.carRepository = carRepository;
        this.carMapper = carMapper;
    }

    @Override
    public List<CarResponse> getAllCars() {
        return carRepository.findAll()
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    @Override
    public CarResponse getCarById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        return carMapper.toResponse(car);
    }

    @Override
    public CarResponse createCar(CarRequest request) {
        Car car = carMapper.toEntity(request);
        Car saved = carRepository.save(car);
        return carMapper.toResponse(saved);
    }

    @Override
    public CarResponse updateCar(Long id, CarRequest request) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setYear(request.getYear());
        car.setColor(request.getColor());
        car.setPrice(request.getPrice());
        Car updated = carRepository.save(car);
        return carMapper.toResponse(updated);
    }

    @Override
    public void deleteCar(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        carRepository.delete(car);
    }
}
```

---

## 6. Updating the Controller

The controller now works entirely with DTOs:

```java
@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping
    public List<CarResponse> getAllCars() {
        return carService.getAllCars();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getCarById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getCarById(id));
    }

    @PostMapping
    public ResponseEntity<CarResponse> createCar(@RequestBody CarRequest request) {
        CarResponse response = carService.createCar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> updateCar(@PathVariable Long id, @RequestBody CarRequest request) {
        return ResponseEntity.ok(carService.updateCar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
        return ResponseEntity.noContent().build();
    }
}
```

### What Changed

- Input parameter: `Car` → `CarRequest`
- Return type: `Car` → `CarResponse`
- **No entity classes referenced in the controller at all**

---

## 7. The Data Flow

```
POST /api/cars  { "make": "Toyota", ... }
     │
     ▼
  Controller receives CarRequest
     │
     ▼
  Service calls carMapper.toEntity(request) → Car entity
     │
     ▼
  Repository saves Car entity to DB
     │
     ▼
  Service calls carMapper.toResponse(saved) → CarResponse
     │
     ▼
  Controller returns CarResponse as JSON
     │
     ▼
  { "id": 1, "make": "Toyota", ... }
```

---

## 8. When DTOs Differ from Entities

DTOs really shine when the API shape differs from the database:

```java
// Entity has internal fields
@Entity
public class Car {
    private Long id;
    private String make;
    private String model;
    private int year;
    private String color;
    private double price;
    private double dealerCost;      // internal only
    private String internalNotes;   // internal only
    private LocalDateTime createdAt;
}

// Response only shows what the client needs
public class CarResponse {
    private Long id;
    private String make;
    private String model;
    private int year;
    private String color;
    private double price;
    // no dealerCost, no internalNotes
    // createdAt could be included or excluded
}
```

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| DTO | Plain Java class that defines API shape |
| Request DTO | What the client sends (no ID) |
| Response DTO | What the API returns (includes ID) |
| Mapper | Converts between entity and DTOs |
| Why DTOs | Security, decoupling, flexibility |

---

## Next: Day 5 — Validation

Tomorrow we'll add validation to our DTOs to reject invalid input.
