# Day 6 Lab — Entity Relationships

## Objective

Add an `Owner` entity with a OneToMany relationship to `Car`, and expose nested data through the API.

---

## Part 1: Create the Owner Entity (10 min)

Create `com.example.cardealership.entity.Owner`:

```java
package com.example.cardealership.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "owners")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phone;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();

    public Owner() {}

    public Owner(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    // Generate all getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public List<Car> getCars() { return cars; }
    public void setCars(List<Car> cars) { this.cars = cars; }
}
```

---

## Part 2: Add Relationship to Car (5 min)

Add these to `Car.java`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id")
private Owner owner;

public Owner getOwner() { return owner; }
public void setOwner(Owner owner) { this.owner = owner; }
```

### Checkpoint ✅

Run the app. Check MySQL — you should see both `CARS` and `OWNERS` tables, with `CARS` having an `OWNER_ID` column.

---

## Part 3: Create Owner DTOs (10 min)

### OwnerRequest.java

```java
package com.example.cardealership.dto;

import jakarta.validation.constraints.*;

public class OwnerRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    public OwnerRequest() {}
    // Add getters and setters for all fields
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
```

### OwnerResponse.java

```java
package com.example.cardealership.dto;

import java.util.List;

public class OwnerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private List<CarResponse> cars;

    public OwnerResponse() {}
    // Add getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public List<CarResponse> getCars() { return cars; }
    public void setCars(List<CarResponse> cars) { this.cars = cars; }
}
```

---

## Part 4: Create Owner Repository, Mapper, Service, and Controller (15 min)

### OwnerRepository.java

```java
package com.example.cardealership.repository;

import com.example.cardealership.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
}
```

### OwnerMapper.java

```java
package com.example.cardealership.mapper;

import com.example.cardealership.dto.OwnerRequest;
import com.example.cardealership.dto.OwnerResponse;
import com.example.cardealership.entity.Owner;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OwnerMapper {

    private final CarMapper carMapper;

    public OwnerMapper(CarMapper carMapper) {
        this.carMapper = carMapper;
    }

    public OwnerResponse toResponse(Owner owner) {
        OwnerResponse response = new OwnerResponse();
        response.setId(owner.getId());
        response.setFirstName(owner.getFirstName());
        response.setLastName(owner.getLastName());
        response.setEmail(owner.getEmail());
        response.setPhone(owner.getPhone());
        response.setCars(owner.getCars().stream()
                .map(carMapper::toResponse)
                .collect(Collectors.toList()));
        return response;
    }

    public Owner toEntity(OwnerRequest request) {
        Owner owner = new Owner();
        owner.setFirstName(request.getFirstName());
        owner.setLastName(request.getLastName());
        owner.setEmail(request.getEmail());
        owner.setPhone(request.getPhone());
        return owner;
    }
}
```

> **Key point:** `OwnerMapper` depends on `CarMapper` to convert nested cars. This is why we use constructor injection — Spring wires it automatically.

### OwnerService.java (interface)

```java
package com.example.cardealership.service;

import com.example.cardealership.dto.OwnerRequest;
import com.example.cardealership.dto.OwnerResponse;

import java.util.List;

public interface OwnerService {
    List<OwnerResponse> getAllOwners();
    OwnerResponse getOwnerById(Long id);
    OwnerResponse createOwner(OwnerRequest request);
}
```

### OwnerServiceImpl.java

```java
package com.example.cardealership.service;

import com.example.cardealership.dto.OwnerRequest;
import com.example.cardealership.dto.OwnerResponse;
import com.example.cardealership.entity.Owner;
import com.example.cardealership.mapper.OwnerMapper;
import com.example.cardealership.repository.OwnerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;

    public OwnerServiceImpl(OwnerRepository ownerRepository, OwnerMapper ownerMapper) {
        this.ownerRepository = ownerRepository;
        this.ownerMapper = ownerMapper;
    }

    @Override
    public List<OwnerResponse> getAllOwners() {
        return ownerRepository.findAll().stream()
                .map(ownerMapper::toResponse)
                .toList();
    }

    @Override
    public OwnerResponse getOwnerById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Owner not found with id: " + id));
        return ownerMapper.toResponse(owner);
    }

    @Override
    public OwnerResponse createOwner(OwnerRequest request) {
        Owner owner = ownerMapper.toEntity(request);
        Owner saved = ownerRepository.save(owner);
        return ownerMapper.toResponse(saved);
    }
}
```

### OwnerController.java

```java
package com.example.cardealership.controller;

import com.example.cardealership.dto.OwnerRequest;
import com.example.cardealership.dto.OwnerResponse;
import com.example.cardealership.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    public List<OwnerResponse> getAllOwners() {
        return ownerService.getAllOwners();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerResponse> getOwnerById(@PathVariable Long id) {
        return ResponseEntity.ok(ownerService.getOwnerById(id));
    }

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerRequest request) {
        OwnerResponse response = ownerService.createOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Checkpoint ✅

- `POST /api/owners` creates an owner
- `GET /api/owners` returns all owners
- `GET /api/owners/1` returns one owner with an empty `cars` list

---

## Part 5: Assign Cars to Owners (10 min)

Add an endpoint to assign a car to an owner. In `CarController`:

```java
@PutMapping("/{carId}/owner/{ownerId}")
public ResponseEntity<CarResponse> assignOwner(
        @PathVariable Long carId,
        @PathVariable Long ownerId) {
    CarResponse response = carService.assignOwner(carId, ownerId);
    return ResponseEntity.ok(response);
}
```

Add to your `CarService` and implement:

```java
public CarResponse assignOwner(Long carId, Long ownerId) {
    Car car = carRepository.findById(carId)
            .orElseThrow(() -> new RuntimeException("Car not found"));
    Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("Owner not found"));
    car.setOwner(owner);
    Car saved = carRepository.save(car);
    return carMapper.toResponse(saved);
}
```

> You'll need to inject `OwnerRepository` into the `CarServiceImpl`.

---

## Part 6: Seed and Test (10 min)

Update `DataLoader` to create owners and assign cars:

```java
// Create owners
Owner john = ownerRepository.save(new Owner("John", "Smith", "john@example.com", "555-0100"));
Owner jane = ownerRepository.save(new Owner("Jane", "Doe", "jane@example.com", "555-0200"));

// Create cars with owners
Car car1 = new Car("Toyota", "Camry", 2023, "Silver", 28000);
car1.setOwner(john);
carRepository.save(car1);

Car car2 = new Car("Honda", "Civic", 2022, "Blue", 24000);
car2.setOwner(john);
carRepository.save(car2);

Car car3 = new Car("Ford", "Mustang", 2024, "Red", 45000);
car3.setOwner(jane);
carRepository.save(car3);
```

### Test

1. `GET /api/owners/1` → Should show John with 2 cars
2. `GET /api/owners/2` → Should show Jane with 1 car
3. `GET /api/cars` → Should show all cars

### Checkpoint ✅

Owner responses include nested car data. No infinite recursion.

---

## Deliverables

- [ ] `Owner` entity with `@OneToMany` relationship
- [ ] `Car` entity updated with `@ManyToOne` relationship
- [ ] Owner DTOs, Mapper, Repository, Service, Controller
- [ ] Assign car to owner endpoint
- [ ] Nested data returned correctly (owner → cars)
- [ ] No infinite recursion in JSON responses
- [ ] DataLoader seeds owners and assigns cars
