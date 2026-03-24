# Day 6 — Entity Relationships

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Model OneToMany and ManyToOne relationships in JPA
2. Handle lazy vs. eager loading
3. Avoid infinite recursion in JSON serialization
4. Create and retrieve nested data through the API

---

## 1. The Domain: Owner → Cars

An **Owner** can have many **Cars**. A **Car** belongs to one **Owner**.

```
┌──────────┐         ┌──────────┐
│  Owner   │ 1 ────* │   Car    │
│──────────│         │──────────│
│ id       │         │ id       │
│ firstName│         │ make     │
│ lastName │         │ model    │
│ email    │         │ year     │
│ phone    │         │ color    │
│          │         │ price    │
│ cars []  │         │ owner    │
└──────────┘         └──────────┘
```

---

## 2. Creating the Owner Entity

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

    // Getters and setters
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

## 3. Adding the Relationship to Car

```java
@Entity
@Table(name = "cars")
public class Car {
    // ... existing fields ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    // Getter and setter
    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }
}
```

### Annotation Breakdown

| Annotation | Meaning |
|-----------|---------|
| `@OneToMany(mappedBy = "owner")` | Owner side — "I have many Cars, mapped by the `owner` field in Car" |
| `@ManyToOne` | Car side — "I belong to one Owner" |
| `@JoinColumn(name = "owner_id")` | The FK column in the `cars` table |
| `cascade = CascadeType.ALL` | Operations on Owner cascade to Cars |
| `orphanRemoval = true` | Removing a car from the list deletes it from DB |
| `fetch = FetchType.LAZY` | Don't load the owner until accessed |

---

## 4. Lazy vs. Eager Loading

### Eager Loading (`FetchType.EAGER`)
- Related data is loaded **immediately** with the parent
- Can cause performance issues (loading entire object graphs)
- Default for `@ManyToOne` and `@OneToOne`

### Lazy Loading (`FetchType.LAZY`)
- Related data is loaded **only when accessed**
- Better performance — loads data on demand
- Default for `@OneToMany` and `@ManyToMany`
- Can cause `LazyInitializationException` if accessed outside a transaction

### Best Practice
- Use `LAZY` by default
- Use DTOs to control what data is returned (avoids lazy loading issues)

---

## 5. The Infinite Recursion Problem

If you return entities directly, JSON serialization enters an **infinite loop**:

```
Owner → cars → [Car → owner → Owner → cars → [Car → owner → ...]]
```

### Solutions

| Solution | How |
|----------|-----|
| **DTOs (recommended)** | Only include the fields you want — no circular references |
| `@JsonManagedReference` / `@JsonBackReference` | Jackson annotations to break the cycle |
| `@JsonIgnore` | Skip the field during serialization |

**We're already using DTOs**, so we control the shape. This is the cleanest solution.

---

## 6. DTOs for Owner

### OwnerRequest

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

    // constructors, getters, setters
    public OwnerRequest() {}

    public OwnerRequest(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

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

### OwnerResponse

```java
package com.example.cardealership.dto;

import java.util.List;

public class OwnerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private List<CarResponse> cars;  // Nested car data

    // constructors, getters, setters
    public OwnerResponse() {}

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

### Updated CarResponse (optional: include owner info)

```java
public class CarResponse {
    // ... existing fields ...
    private String ownerName;  // just the name, not the full owner object

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}
```

> Notice: `OwnerResponse` includes `List<CarResponse>`, but `CarResponse` only includes `ownerName` (a String) — **no circular reference**.

---

## 7. Owner Repository, Service, Controller

### OwnerRepository

```java
package com.example.cardealership.repository;

import com.example.cardealership.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
}
```

### OwnerMapper

```java
package com.example.cardealership.mapper;

import com.example.cardealership.dto.*;
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
        return new Owner(
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getPhone()
        );
    }
}
```

### OwnerService

```java
package com.example.cardealership.service;

import com.example.cardealership.dto.*;
import java.util.List;

public interface OwnerService {
    List<OwnerResponse> getAllOwners();
    OwnerResponse getOwnerById(Long id);
    OwnerResponse createOwner(OwnerRequest request);
    OwnerResponse updateOwner(Long id, OwnerRequest request);
    void deleteOwner(Long id);
}
```

### OwnerController

```java
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

---

## 8. Assigning a Car to an Owner

Add an endpoint to assign an existing car to an owner:

```java
// In CarController or a dedicated endpoint
@PutMapping("/{carId}/owner/{ownerId}")
public ResponseEntity<CarResponse> assignOwner(
        @PathVariable Long carId,
        @PathVariable Long ownerId) {
    CarResponse response = carService.assignOwner(carId, ownerId);
    return ResponseEntity.ok(response);
}
```

Service implementation:

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

---

## 9. Fetching Nested Data

### GET /api/owners/1 Response:

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Smith",
  "email": "john@example.com",
  "phone": "555-0100",
  "cars": [
    { "id": 1, "make": "Toyota", "model": "Camry", "year": 2023, "color": "Silver", "price": 28000 },
    { "id": 3, "make": "Ford", "model": "Mustang", "year": 2024, "color": "Red", "price": 45000 }
  ]
}
```

No infinite recursion — the DTOs control the shape.

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| `@OneToMany` | Parent side — "I have many children" |
| `@ManyToOne` | Child side — "I belong to one parent" |
| `mappedBy` | Indicates the owning side of the relationship |
| `FetchType.LAZY` | Load related data only when accessed |
| `cascade` | Propagate operations to related entities |
| Infinite recursion | Prevented by using DTOs |
| Nested DTOs | `OwnerResponse` contains `List<CarResponse>` |

---

## Next: Day 7 — Exception Handling

Tomorrow we'll standardize our error responses across the entire API.
