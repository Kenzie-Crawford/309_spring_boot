# Day 2 Lab — Building REST Endpoints

## Objective

Create a full CRUD REST controller for the `Car` entity and test it using Postman or Thunder Client.

---

## Part 1: Create the Controller (15 min)

Create `com.example.cardealership.controller.CarController`:

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

    // GET all cars
    @GetMapping
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    // GET car by ID
    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable Long id) {
        return carRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create car
    @PostMapping
    public ResponseEntity<Car> createCar(@RequestBody Car car) {
        Car saved = carRepository.save(car);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
```

### Checkpoint ✅

Run the app and hit `GET http://localhost:8080/api/cars` — you should see the seeded cars from `DataLoader`.

---

## Part 2: Add PUT and DELETE (10 min)

Add these methods to your controller:

```java
// PUT update car
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

// DELETE car
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
    if (carRepository.existsById(id)) {
        carRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
}
```

---

## Part 3: Test All Endpoints (15 min)

Open **Postman** or **Thunder Client** and test each endpoint:

### 1. GET all cars
- **Method:** GET
- **URL:** `http://localhost:8080/api/cars`
- **Expected:** 200 OK + array of cars

### 2. GET car by ID
- **Method:** GET
- **URL:** `http://localhost:8080/api/cars/1`
- **Expected:** 200 OK + single car

### 3. GET non-existent car
- **Method:** GET
- **URL:** `http://localhost:8080/api/cars/999`
- **Expected:** 404 Not Found

### 4. POST create car
- **Method:** POST
- **URL:** `http://localhost:8080/api/cars`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "make": "Chevrolet",
  "model": "Corvette",
  "year": 2024,
  "color": "Yellow",
  "price": 65000
}
```
- **Expected:** 201 Created + car with generated ID

### 5. PUT update car
- **Method:** PUT
- **URL:** `http://localhost:8080/api/cars/1`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "make": "Toyota",
  "model": "Camry",
  "year": 2025,
  "color": "Midnight Blue",
  "price": 32000
}
```
- **Expected:** 200 OK + updated car

### 6. DELETE car
- **Method:** DELETE
- **URL:** `http://localhost:8080/api/cars/1`
- **Expected:** 204 No Content

### 7. Verify deletion
- **Method:** GET
- **URL:** `http://localhost:8080/api/cars/1`
- **Expected:** 404 Not Found

### Checkpoint ✅

All 7 tests produce the expected status codes and responses.

---

## Part 4: Add a Search Endpoint (10 min)

First, add a query method to `CarRepository`:

```java
List<Car> findByMake(String make);
```

Then add a search endpoint to the controller:

```java
@GetMapping("/search")
public List<Car> searchByMake(@RequestParam String make) {
    return carRepository.findByMake(make);
}
```

**Test:** `GET http://localhost:8080/api/cars/search?make=Toyota`

### Checkpoint ✅

Search returns only matching cars.

---

## Deliverables

- [ ] `CarController` with GET all, GET by ID, POST, PUT, DELETE
- [ ] All endpoints return correct HTTP status codes
- [ ] Search endpoint with `@RequestParam`
- [ ] All endpoints tested via Postman/Thunder Client
