# Day 1 Lab — Spring Boot Project Setup

## Objective

Set up a Spring Boot project, connect to a database, create an entity and repository, and verify data persistence using `CommandLineRunner`.

---

## Part 1: Generate the Project (10 min)

1. Go to [start.spring.io](https://start.spring.io)
2. Configure:
   - **Project:** Maven
   - **Language:** Java
   - **Spring Boot:** latest stable 3.x
   - **Group:** `com.example`
   - **Artifact:** `cardealership`
   - **Package name:** `com.example.cardealership`
   - **Packaging:** Jar
   - **Java:** 17
3. Add these dependencies:
   - `Spring Web`
   - `Spring Data JPA`
   - `MySQL Driver`
4. Click **Generate**, download the ZIP, extract it, and open in your IDE.

### Checkpoint ✅

You should see `CardealershipApplication.java` with a `main()` method.

---

## Part 2: Configure the Database (5 min)

### Prerequisites

Make sure MySQL is running and create the database:

```sql
CREATE DATABASE cardealership;
```

Open `src/main/resources/application.properties` and add:

```properties
# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/cardealership
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=root

# JPA settings
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

---

## Part 3: Create the Car Entity (10 min)

Create a new package `com.example.cardealership.entity` and add:

**Car.java**

```java
package com.example.cardealership.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    private int year;
    private String color;
    private double price;

    public Car() {}

    public Car(String make, String model, int year, String color, double price) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
    }

    // Generate getters and setters for all fields
    // Generate toString()

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

    @Override
    public String toString() {
        return year + " " + make + " " + model + " (" + color + ") - $" + price;
    }
}
```

---

## Part 4: Create the Repository (5 min)

Create a new package `com.example.cardealership.repository` and add:

**CarRepository.java**

```java
package com.example.cardealership.repository;

import com.example.cardealership.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
}
```

### Checkpoint ✅

Your project structure should look like:

```
src/main/java/com/example/cardealership/
├── CardealershipApplication.java
├── entity/
│   └── Car.java
└── repository/
    └── CarRepository.java
```

---

## Part 5: Seed Data with CommandLineRunner (10 min)

Create a new class in the root package:

**DataLoader.java**

```java
package com.example.cardealership;

import com.example.cardealership.entity.Car;
import com.example.cardealership.repository.CarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final CarRepository carRepository;

    public DataLoader(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Override
    public void run(String... args) {
        // Save some cars
        carRepository.save(new Car("Toyota", "Camry", 2023, "Silver", 28000));
        carRepository.save(new Car("Honda", "Civic", 2022, "Blue", 24000));
        carRepository.save(new Car("Ford", "Mustang", 2024, "Red", 45000));
        carRepository.save(new Car("Tesla", "Model 3", 2024, "White", 42000));
        carRepository.save(new Car("BMW", "330i", 2023, "Black", 46000));

        // Print all cars
        System.out.println("\n=== All Cars in Database ===");
        carRepository.findAll().forEach(System.out::println);
        System.out.println("Total cars: " + carRepository.count());
    }
}
```

---

## Part 6: Run and Verify (10 min)

### 1. Run the application

Right-click `CardealershipApplication.java` → **Run**

Or from terminal:

```bash
./mvnw spring-boot:run
```

### 2. Check the console output

You should see:
- Hibernate SQL statements (because `show-sql=true`)
- Your printed car list
- `Started CardealershipApplication in X.X seconds`

### 3. Verify in MySQL

1. Open MySQL Workbench (or use the MySQL CLI: `mysql -u root -p`)
2. Connect to `localhost:3306`
3. Select the `cardealership` database
4. Run: `SELECT * FROM cars;`

### Checkpoint ✅

You should see all 5 cars in the MySQL query results.

---

## Part 7: Experiment (remaining time)

Try these on your own:

1. **Add more cars** to the `DataLoader` and re-run
2. **Find a car by ID:**
   ```java
   carRepository.findById(1L).ifPresent(car ->
       System.out.println("Found: " + car)
   );
   ```
3. **Delete a car:**
   ```java
   carRepository.deleteById(2L);
   System.out.println("After delete: " + carRepository.count() + " cars");
   ```
4. **Change the port** to 9090 by adding `server.port=9090` to `application.properties`

---

## Deliverables

By the end of this lab you should have:

- [ ] A running Spring Boot application
- [ ] MySQL database configured and verified with a client
- [ ] `Car` entity mapped to a `cars` table
- [ ] `CarRepository` providing CRUD operations
- [ ] `DataLoader` seeding and printing data on startup
