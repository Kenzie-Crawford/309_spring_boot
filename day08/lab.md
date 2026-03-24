# Day 8 Lab — Advanced Queries & Pagination

## Objective

Add query methods, pagination, sorting, and a filtering endpoint to the Car API.

---

## Part 1: Add Query Methods to CarRepository (10 min)

Add these to `CarRepository`:

```java
List<Car> findByMake(String make);
List<Car> findByColor(String color);
List<Car> findByYear(int year);
List<Car> findByPriceBetween(double min, double max);
List<Car> findByMakeContainingIgnoreCase(String keyword);
List<Car> findByOwnerIsNull();
List<Car> findByOwnerIsNotNull();
```

### Checkpoint ✅

App compiles — Spring Data will auto-generate implementations.

---

## Part 2: Add a Keyword Search (10 min)

Add a custom JPQL query:

```java
@Query("SELECT c FROM Car c WHERE LOWER(c.make) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
       "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :keyword, '%'))")
List<Car> searchByKeyword(@Param("keyword") String keyword);
```

Add a search endpoint to the controller:

```java
@GetMapping("/search")
public List<CarResponse> search(@RequestParam String q) {
    return carService.searchCars(q);
}
```

Add to service:

```java
public List<CarResponse> searchCars(String keyword) {
    return carRepository.searchByKeyword(keyword)
            .stream()
            .map(carMapper::toResponse)
            .toList();
}
```

**Test:**
```
GET /api/cars/search?q=toy    → finds Toyota
GET /api/cars/search?q=camry  → finds Camry
GET /api/cars/search?q=xyz    → empty array
```

### Checkpoint ✅

Search returns matching cars.

---

## Part 3: Add Pagination (15 min)

### Update the service:

```java
public Page<CarResponse> getAllCars(int page, int size, String sortBy, String direction) {
    Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return carRepository.findAll(pageable)
            .map(carMapper::toResponse);
}
```

### Update the controller:

```java
@GetMapping
public Page<CarResponse> getAllCars(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
    return carService.getAllCars(page, size, sortBy, direction);
}
```

> Don't forget to update the `CarService` interface as well.

### Test:

```
GET /api/cars?page=0&size=2                    → first 2 cars
GET /api/cars?page=1&size=2                    → next 2 cars
GET /api/cars?page=0&size=3&sortBy=price&direction=desc  → top 3 most expensive
GET /api/cars?sortBy=make                      → sorted alphabetically by make
```

### Checkpoint ✅

- Response includes `content`, `totalElements`, `totalPages`
- Changing `page` and `size` returns different subsets
- Sorting works by different fields

---

## Part 4: Build a Filter Endpoint (15 min)

### Add a filter query to CarRepository:

```java
@Query("SELECT c FROM Car c WHERE " +
       "(:make IS NULL OR c.make = :make) AND " +
       "(:color IS NULL OR c.color = :color) AND " +
       "(:minYear IS NULL OR c.year >= :minYear) AND " +
       "(:maxYear IS NULL OR c.year <= :maxYear) AND " +
       "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
       "(:maxPrice IS NULL OR c.price <= :maxPrice)")
List<Car> filterCars(
    @Param("make") String make,
    @Param("color") String color,
    @Param("minYear") Integer minYear,
    @Param("maxYear") Integer maxYear,
    @Param("minPrice") Double minPrice,
    @Param("maxPrice") Double maxPrice
);
```

### Add to service:

```java
public List<CarResponse> filterCars(String make, String color,
        Integer minYear, Integer maxYear, Double minPrice, Double maxPrice) {
    return carRepository.filterCars(make, color, minYear, maxYear, minPrice, maxPrice)
            .stream()
            .map(carMapper::toResponse)
            .toList();
}
```

### Add to controller:

```java
@GetMapping("/filter")
public List<CarResponse> filterCars(
        @RequestParam(required = false) String make,
        @RequestParam(required = false) String color,
        @RequestParam(required = false) Integer minYear,
        @RequestParam(required = false) Integer maxYear,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice) {
    return carService.filterCars(make, color, minYear, maxYear, minPrice, maxPrice);
}
```

### Test:

```
GET /api/cars/filter?make=Toyota
GET /api/cars/filter?minPrice=30000&maxPrice=50000
GET /api/cars/filter?color=Red&minYear=2023
GET /api/cars/filter  → returns all (no filters)
```

### Checkpoint ✅

Filters work individually and combined.

---

## Part 5: Seed More Data (5 min)

Update `DataLoader` with more cars for better pagination/filter testing:

```java
carRepository.save(new Car("Toyota", "Camry", 2023, "Silver", 28000));
carRepository.save(new Car("Toyota", "Corolla", 2022, "White", 22000));
carRepository.save(new Car("Honda", "Civic", 2022, "Blue", 24000));
carRepository.save(new Car("Honda", "Accord", 2023, "Black", 32000));
carRepository.save(new Car("Ford", "Mustang", 2024, "Red", 45000));
carRepository.save(new Car("Ford", "F-150", 2023, "White", 38000));
carRepository.save(new Car("BMW", "330i", 2023, "Black", 46000));
carRepository.save(new Car("BMW", "X5", 2024, "Blue", 62000));
carRepository.save(new Car("Tesla", "Model 3", 2024, "White", 42000));
carRepository.save(new Car("Tesla", "Model Y", 2024, "Red", 52000));
```

---

## Deliverables

- [ ] Query methods added to `CarRepository`
- [ ] Keyword search endpoint (`/search?q=...`)
- [ ] Pagination working with `page`, `size`, `sortBy`, `direction`
- [ ] Filter endpoint with multiple optional parameters
- [ ] All endpoints tested with various parameter combinations
- [ ] Sufficient seed data for meaningful results
