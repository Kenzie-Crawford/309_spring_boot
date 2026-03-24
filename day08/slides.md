# Day 8 — Advanced Queries

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Write derived query methods in Spring Data JPA repositories
2. Write custom JPQL queries with `@Query`
3. Implement pagination and sorting
4. Build flexible filtering endpoints

---

## 1. Spring Data Query Methods

Spring Data JPA generates queries **from method names**. You write the method signature; Spring writes the SQL.

### Naming Convention

```
findBy + FieldName + Condition
```

### Examples

```java
public interface CarRepository extends JpaRepository<Car, Long> {

    // WHERE make = ?
    List<Car> findByMake(String make);

    // WHERE color = ?
    List<Car> findByColor(String color);

    // WHERE year = ?
    List<Car> findByYear(int year);

    // WHERE price < ?
    List<Car> findByPriceLessThan(double price);

    // WHERE price > ?
    List<Car> findByPriceGreaterThan(double price);

    // WHERE price BETWEEN ? AND ?
    List<Car> findByPriceBetween(double min, double max);

    // WHERE make = ? AND color = ?
    List<Car> findByMakeAndColor(String make, String color);

    // WHERE make = ? OR make = ?
    List<Car> findByMakeOrColor(String make, String color);

    // WHERE make LIKE '%toyota%' (case-insensitive)
    List<Car> findByMakeContainingIgnoreCase(String make);

    // WHERE year > ? ORDER BY price ASC
    List<Car> findByYearGreaterThanOrderByPriceAsc(int year);
}
```

### Query Method Keywords

| Keyword | SQL | Example |
|---------|-----|---------|
| `findBy` | `WHERE` | `findByMake(String make)` |
| `And` | `AND` | `findByMakeAndColor(...)` |
| `Or` | `OR` | `findByMakeOrColor(...)` |
| `LessThan` | `<` | `findByPriceLessThan(...)` |
| `GreaterThan` | `>` | `findByPriceGreaterThan(...)` |
| `Between` | `BETWEEN` | `findByPriceBetween(...)` |
| `Like` | `LIKE` | `findByMakeLike(...)` |
| `Containing` | `LIKE %...%` | `findByMakeContaining(...)` |
| `IgnoreCase` | case-insensitive | `findByMakeIgnoreCase(...)` |
| `OrderBy` | `ORDER BY` | `findByYearOrderByPriceAsc(...)` |
| `IsNull` | `IS NULL` | `findByOwnerIsNull()` |
| `IsNotNull` | `IS NOT NULL` | `findByOwnerIsNotNull()` |

---

## 2. Custom Queries with @Query

For complex queries, use `@Query` with JPQL:

```java
public interface CarRepository extends JpaRepository<Car, Long> {

    // JPQL (uses entity/field names, not table/column names)
    @Query("SELECT c FROM Car c WHERE c.make = :make AND c.year >= :year")
    List<Car> findByMakeAndMinYear(@Param("make") String make, @Param("year") int year);

    // JPQL with LIKE
    @Query("SELECT c FROM Car c WHERE LOWER(c.make) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Car> searchByKeyword(@Param("keyword") String keyword);

    // Native SQL (uses table/column names)
    @Query(value = "SELECT * FROM cars WHERE price > :minPrice ORDER BY price DESC",
           nativeQuery = true)
    List<Car> findExpensiveCars(@Param("minPrice") double minPrice);
}
```

### JPQL vs. Native SQL

| Feature | JPQL | Native SQL |
|---------|------|-----------|
| Syntax | Uses entity names (`Car`) | Uses table names (`cars`) |
| Portable | Yes (works across DBs) | No (DB-specific) |
| When to use | Most cases | DB-specific features |

---

## 3. Pagination

Spring Data provides built-in pagination through `Pageable`.

### Repository

No changes needed — `JpaRepository` already supports `Pageable`:

```java
// This method is inherited from JpaRepository
Page<Car> findAll(Pageable pageable);

// You can also add Pageable to custom methods
Page<Car> findByMake(String make, Pageable pageable);
```

### Service

```java
@Override
public Page<CarResponse> getAllCars(int page, int size, String sortBy, String direction) {
    Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return carRepository.findAll(pageable)
            .map(carMapper::toResponse);
}
```

### Controller

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

### Pagination Response

```
GET /api/cars?page=0&size=2&sortBy=price&direction=desc
```

```json
{
  "content": [
    { "id": 5, "make": "BMW", "model": "330i", "price": 46000 },
    { "id": 3, "make": "Ford", "model": "Mustang", "price": 45000 }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 2,
    "sort": { "sorted": true, "direction": "DESC", "property": "price" }
  },
  "totalElements": 5,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### Key Pagination Properties

| Property | Meaning |
|----------|---------|
| `content` | The data for this page |
| `totalElements` | Total records across all pages |
| `totalPages` | Total number of pages |
| `pageNumber` | Current page (0-indexed) |
| `pageSize` | Records per page |
| `first` / `last` | Is this the first/last page? |

---

## 4. Building a Filter Endpoint

Create a flexible search endpoint that accepts multiple optional filters:

### Controller

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

### Service

```java
@Override
public List<CarResponse> filterCars(String make, String color,
        Integer minYear, Integer maxYear, Double minPrice, Double maxPrice) {

    List<Car> cars = carRepository.findAll();

    // Apply filters
    if (make != null && !make.isBlank()) {
        cars = cars.stream()
                .filter(c -> c.getMake().equalsIgnoreCase(make))
                .toList();
    }
    if (color != null && !color.isBlank()) {
        cars = cars.stream()
                .filter(c -> c.getColor().equalsIgnoreCase(color))
                .toList();
    }
    if (minYear != null) {
        cars = cars.stream()
                .filter(c -> c.getYear() >= minYear)
                .toList();
    }
    if (maxYear != null) {
        cars = cars.stream()
                .filter(c -> c.getYear() <= maxYear)
                .toList();
    }
    if (minPrice != null) {
        cars = cars.stream()
                .filter(c -> c.getPrice() >= minPrice)
                .toList();
    }
    if (maxPrice != null) {
        cars = cars.stream()
                .filter(c -> c.getPrice() <= maxPrice)
                .toList();
    }

    return cars.stream()
            .map(carMapper::toResponse)
            .toList();
}
```

### Usage

```
GET /api/cars/filter?make=Toyota
GET /api/cars/filter?minPrice=30000&maxPrice=50000
GET /api/cars/filter?make=Toyota&color=Silver&minYear=2023
GET /api/cars/filter  (no filters → returns all)
```

---

## 5. Better Filtering with Repository Queries

Instead of filtering in Java (loads all data first), push filters to the database:

```java
// In CarRepository
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

Then the service becomes:

```java
@Override
public List<CarResponse> filterCars(String make, String color,
        Integer minYear, Integer maxYear, Double minPrice, Double maxPrice) {
    return carRepository.filterCars(make, color, minYear, maxYear, minPrice, maxPrice)
            .stream()
            .map(carMapper::toResponse)
            .toList();
}
```

---

## 6. Counting and Existence Checks

```java
public interface CarRepository extends JpaRepository<Car, Long> {
    // Count
    long countByMake(String make);

    // Exists
    boolean existsByMakeAndModel(String make, String model);
}
```

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| Derived queries | Method name → SQL: `findByMakeAndColor(...)` |
| `@Query` | Custom JPQL/SQL for complex queries |
| `Pageable` | `PageRequest.of(page, size, sort)` |
| `Page<T>` | Response with data + pagination metadata |
| Filtering | Use `@RequestParam(required = false)` for optional filters |
| `@Param` | Named parameters in `@Query` |

---

## Next: Day 9 — Security Basics

Tomorrow we'll add authentication and authorization to protect our endpoints.
