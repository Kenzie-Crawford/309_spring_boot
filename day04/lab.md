# Day 4 Lab — Implementing DTOs & Mapping

## Objective

Refactor the Car API to use DTOs instead of exposing entities directly.

---

## Part 1: Create the DTO Classes (10 min)

Create a `dto` package: `com.example.cardealership.dto`

### CarRequest.java

```java
package com.example.cardealership.dto;

public class CarRequest {
    private String make;
    private String model;
    private int year;
    private String color;
    private double price;

    public CarRequest() {}

    public CarRequest(String make, String model, int year, String color, double price) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
    }

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

### CarResponse.java

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

---

## Part 2: Create the Mapper (10 min)

Create `com.example.cardealership.mapper.CarMapper`:

```java
package com.example.cardealership.mapper;

import com.example.cardealership.dto.CarRequest;
import com.example.cardealership.dto.CarResponse;
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

    public void updateEntity(Car car, CarRequest request) {
        car.setMake(request.getMake());
        car.setModel(request.getModel());
        car.setYear(request.getYear());
        car.setColor(request.getColor());
        car.setPrice(request.getPrice());
    }
}
```

### Checkpoint ✅

Project compiles with the new DTO and mapper classes.

---

## Part 3: Update the Service Interface (5 min)

Update `CarService` to use DTOs:

```java
package com.example.cardealership.service;

import com.example.cardealership.dto.CarRequest;
import com.example.cardealership.dto.CarResponse;
import java.util.List;

public interface CarService {
    List<CarResponse> getAllCars();
    CarResponse getCarById(Long id);
    CarResponse createCar(CarRequest request);
    CarResponse updateCar(Long id, CarRequest request);
    void deleteCar(Long id);
}
```

---

## Part 4: Update the Service Implementation (15 min)

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
        carMapper.updateEntity(car, request);
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

## Part 5: Update the Controller (5 min)

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

### Checkpoint ✅

Run the app and test all endpoints with Postman. The JSON shape should be the same, but now:
- Controller references only DTOs
- Entities are only used inside the service and repository layers

---

## Part 6: Verify No Entities in Controller (5 min)

Open `CarController.java` and verify:
- [ ] No imports from `com.example.cardealership.entity`
- [ ] No `Car` entity references
- [ ] Only `CarRequest` and `CarResponse` used

---

## Deliverables

- [ ] `CarRequest` DTO class
- [ ] `CarResponse` DTO class
- [ ] `CarMapper` with `toResponse`, `toEntity`, `updateEntity`
- [ ] Service interface and implementation updated for DTOs
- [ ] Controller updated — no entity references
- [ ] All endpoints tested and returning correct JSON
