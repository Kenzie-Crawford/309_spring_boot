# Day 3 Lab — Extracting the Service Layer

## Objective

Refactor the Car API to use a proper three-layer architecture: Controller → Service → Repository.

---

## Part 1: Create the Service Interface (5 min)

Create `com.example.cardealership.service.CarService`:

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

---

## Part 2: Create the Service Implementation (15 min)

Create `com.example.cardealership.service.CarServiceImpl`:

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

### Checkpoint ✅

Your project should compile with the new service class.

---

## Part 3: Refactor the Controller (10 min)

Update `CarController` to use the service instead of the repository:

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

### Checkpoint ✅

Run the app. All endpoints should work exactly as before. Test with Postman to confirm.

---

## Part 4: Add Business Logic (10 min)

Add validation logic to the service's `createCar` method:

```java
@Override
public Car createCar(Car car) {
    // Business rule: price must be positive
    if (car.getPrice() <= 0) {
        throw new IllegalArgumentException("Price must be greater than zero");
    }

    // Business rule: year must be valid
    int currentYear = java.time.Year.now().getValue();
    if (car.getYear() < 1886 || car.getYear() > currentYear + 1) {
        throw new IllegalArgumentException("Year must be between 1886 and " + (currentYear + 1));
    }

    return carRepository.save(car);
}
```

### Test it

Send a POST with invalid data:

```json
{
  "make": "Test",
  "model": "Car",
  "year": 1800,
  "color": "Red",
  "price": -5000
}
```

You should get a 500 error (we'll handle this properly on Day 7).

### Checkpoint ✅

Invalid data is rejected by the service layer.

---

## Part 5: Verify Architecture (5 min)

Check your project structure:

```
src/main/java/com/example/cardealership/
├── CardealershipApplication.java
├── DataLoader.java
├── controller/
│   └── CarController.java         ← depends on CarService only
├── entity/
│   └── Car.java
├── repository/
│   └── CarRepository.java
└── service/
    ├── CarService.java            ← interface
    └── CarServiceImpl.java        ← implementation
```

Verify:
- [ ] `CarController` has **no imports** from `repository` package
- [ ] `CarController` does **not** reference `CarRepository`
- [ ] All business logic is in `CarServiceImpl`
- [ ] `CarServiceImpl` is annotated with `@Service`

---

## Part 6: Unit Test the Service Layer (15 min)

Now let's test `CarServiceImpl` using JUnit and Mockito. Since you already know JUnit, the new concepts here are **mocking the repository** so tests run without a database.

### Add Test Class

Create `src/test/java/com/example/cardealership/service/CarServiceImplTest.java`:

```java
package com.example.cardealership.service;

import com.example.cardealership.entity.Car;
import com.example.cardealership.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarServiceImpl carService;

    private Car sampleCar;

    @BeforeEach
    void setUp() {
        sampleCar = new Car("Toyota", "Camry", 2023, "Silver", 28000);
        sampleCar.setId(1L);
    }

    @Test
    void getAllCars_returnsList() {
        // Arrange
        List<Car> cars = Arrays.asList(sampleCar, new Car("Honda", "Civic", 2022, "Blue", 24000));
        when(carRepository.findAll()).thenReturn(cars);

        // Act
        List<Car> result = carService.getAllCars();

        // Assert
        assertEquals(2, result.size());
        verify(carRepository, times(1)).findAll();
    }

    @Test
    void getCarById_found() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(sampleCar));

        Car result = carService.getCarById(1L);

        assertEquals("Toyota", result.getMake());
        assertEquals("Camry", result.getModel());
    }

    @Test
    void getCarById_notFound_throwsException() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> carService.getCarById(99L));
    }

    @Test
    void createCar_validCar_saves() {
        when(carRepository.save(any(Car.class))).thenReturn(sampleCar);

        Car result = carService.createCar(sampleCar);

        assertNotNull(result);
        assertEquals("Toyota", result.getMake());
        verify(carRepository).save(sampleCar);
    }

    @Test
    void createCar_negativePrice_throwsException() {
        Car badCar = new Car("Test", "Car", 2023, "Red", -5000);

        assertThrows(IllegalArgumentException.class, () -> carService.createCar(badCar));
        verify(carRepository, never()).save(any());
    }

    @Test
    void createCar_invalidYear_throwsException() {
        Car badCar = new Car("Test", "Car", 1800, "Red", 25000);

        assertThrows(IllegalArgumentException.class, () -> carService.createCar(badCar));
        verify(carRepository, never()).save(any());
    }

    @Test
    void deleteCar_found_deletes() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(sampleCar));

        carService.deleteCar(1L);

        verify(carRepository).delete(sampleCar);
    }
}
```

### Key Concepts

| Annotation | Purpose |
|-----------|---------|
| `@ExtendWith(MockitoExtension.class)` | Enables Mockito annotations in JUnit 5 |
| `@Mock` | Creates a fake (mock) version of a dependency |
| `@InjectMocks` | Creates the class under test and injects the mocks |
| `when(...).thenReturn(...)` | Defines what a mock returns when called |
| `verify(...)` | Confirms a mock method was called |
| `never()` | Asserts a method was NOT called |

> **Why mock?** We want to test `CarServiceImpl` logic in isolation — without starting Spring or connecting to MySQL. Mocks let us control exactly what the repository returns.

### Run the Tests

Right-click the test class → **Run Tests**, or from terminal:

```bash
./mvnw test
```

### Checkpoint ✅

All 7 tests pass. No database connection needed.

---

## Deliverables

- [ ] `CarService` interface with 5 methods
- [ ] `CarServiceImpl` with all implementations
- [ ] `CarController` refactored to use `CarService`
- [ ] At least one business validation rule in the service
- [ ] All endpoints still work correctly
- [ ] Controller contains no repository imports
- [ ] `CarServiceImplTest` with 7 passing unit tests
