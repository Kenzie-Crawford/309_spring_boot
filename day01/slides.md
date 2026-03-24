# Day 1 — Spring Boot Foundations + Project Setup

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Explain what Spring Boot does and why it exists
2. Generate and run a Spring Boot application
3. Configure a database connection using `application.properties`
4. Create a JPA entity and repository inside a Spring Boot project
5. Use `CommandLineRunner` to seed and retrieve data at startup

---

## 1. What Is Spring Boot?

Spring Boot is an **opinionated framework** built on top of the Spring Framework. It removes boilerplate so you can focus on business logic.

### What Spring Boot gives you:

| Feature | What it does |
|---------|-------------|
| **Auto-configuration** | Detects your dependencies and configures beans automatically |
| **Embedded server** | Ships with Apache Tomcat — no external server needed |
| **Starter dependencies** | Curated dependency bundles (`spring-boot-starter-web`, etc.) |
| **Opinionated defaults** | Sensible defaults that work out of the box |

### Without Spring Boot vs. With Spring Boot

**Without:**
- Manually configure DataSource, EntityManagerFactory, TransactionManager
- Deploy WAR to external Tomcat
- Write XML configuration files

**With:**
- Add `spring-boot-starter-data-jpa` + a DB driver → JPA auto-configured
- Run `main()` → Embedded Tomcat starts
- Write `application.properties` → Everything wired up

---

## 2. Spring Initializr

Spring Initializr ([start.spring.io](https://start.spring.io)) generates a ready-to-run project.

### Steps:

1. Go to [start.spring.io](https://start.spring.io)
2. Configure:
   - **Project:** Maven
   - **Language:** Java
   - **Spring Boot:** 3.x (latest stable)
   - **Group:** `com.example`
   - **Artifact:** `cardealership`
   - **Packaging:** Jar
   - **Java:** 17+
3. Add dependencies:
   - **Spring Web**
   - **Spring Data JPA**
   - **MySQL Driver**
4. Click **Generate** → Download ZIP → Extract → Open in IDE

---

## 3. Project Structure

```
cardealership/
├── src/
│   ├── main/
│   │   ├── java/com/example/cardealership/
│   │   │   └── CardealershipApplication.java    ← Entry point
│   │   └── resources/
│   │       ├── application.properties            ← Configuration
│   │       └── static/                           ← Static files (unused for APIs)
│   └── test/
│       └── java/com/example/cardealership/
│           └── CardealershipApplicationTests.java
├── pom.xml                                       ← Maven dependencies
└── mvnw / mvnw.cmd                               ← Maven wrapper
```

### The Entry Point

```java
@SpringBootApplication
public class CardealershipApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardealershipApplication.class, args);
    }
}
```

`@SpringBootApplication` combines three annotations:
- `@Configuration` — marks this class as a source of bean definitions
- `@EnableAutoConfiguration` — tells Spring Boot to auto-configure
- `@ComponentScan` — scans for components in this package and sub-packages

---

## 4. application.properties

This file is how you configure your Spring Boot app.

### MySQL Database

> **Prerequisites:** Install MySQL and create a database before running the app.
>
> ```sql
> CREATE DATABASE cardealership;
> ```

```properties
# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/cardealership
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=root

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

### Key Properties Explained

| Property | Purpose |
|----------|---------|
| `spring.datasource.url` | JDBC URL to your MySQL database |
| `spring.datasource.driver-class-name` | MySQL JDBC driver class |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` (dev), `update` (prod), `none` (manual) |
| `spring.jpa.show-sql` | Prints SQL to console for debugging |

---

## 5. Dependency Management (pom.xml)

Spring Boot uses **starter** dependencies — curated bundles.

```xml
<dependencies>
    <!-- REST API support -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JPA + Hibernate -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

You don't specify versions — Spring Boot's parent POM manages them.

---

## 6. Embedded Server (Tomcat)

Spring Boot includes **Apache Tomcat** as an embedded server.

- No need to install or configure Tomcat separately
- The app starts as a regular Java `main()` method
- Default port: **8080**
- Change port: `server.port=9090` in `application.properties`

When you run the app, you'll see:

```
Tomcat started on port(s): 8080 (http)
Started CardealershipApplication in 2.3 seconds
```

---

## 7. Creating an Entity (Review + Spring Boot Context)

Students already know JPA. In Spring Boot, the setup is simpler:

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

    // Default constructor (required by JPA)
    public Car() {}

    public Car(String make, String model, int year, String color, double price) {
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

    @Override
    public String toString() {
        return year + " " + make + " " + model + " (" + color + ") - $" + price;
    }
}
```

> **Note:** No `persistence.xml` needed — Spring Boot auto-detects `@Entity` classes.

---

## 8. Creating a Repository

```java
package com.example.cardealership.repository;

import com.example.cardealership.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarRepository extends JpaRepository<Car, Long> {
    // JpaRepository provides: save, findById, findAll, deleteById, count, etc.
}
```

> **No implementation class needed.** Spring Data JPA generates one at runtime.

---

## 9. CommandLineRunner — Running Code at Startup

`CommandLineRunner` is a Spring Boot interface that runs code after the app starts.

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
        carRepository.save(new Car("Toyota", "Camry", 2023, "Silver", 28000));
        carRepository.save(new Car("Honda", "Civic", 2022, "Blue", 24000));
        carRepository.save(new Car("Ford", "Mustang", 2024, "Red", 45000));

        System.out.println("=== All Cars ===");
        carRepository.findAll().forEach(System.out::println);
    }
}
```

When the app starts, you'll see:

```
=== All Cars ===
2023 Toyota Camry (Silver) - $28000.0
2022 Honda Civic (Blue) - $24000.0
2024 Ford Mustang (Red) - $45000.0
```

---

## 10. Running the App

### From IDE
Right-click `CardealershipApplication.java` → Run

### From Terminal
```bash
./mvnw spring-boot:run
```

### Verify
1. Console shows `Started CardealershipApplication`
2. Open MySQL Workbench (or your MySQL client)
3. Connect to `localhost:3306` → Run `SELECT * FROM cars;` on the `cardealership` database

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| Spring Boot | Opinionated framework — auto-configures everything |
| Embedded Tomcat | No external server setup needed |
| application.properties | Centralized configuration |
| Starters | `spring-boot-starter-web`, `spring-boot-starter-data-jpa` |
| CommandLineRunner | Runs code at startup — great for seeding data |
| MySQL | Relational database — verify data with MySQL Workbench or CLI |

---

## Next: Day 2 — REST Controllers

Tomorrow we'll expose our data through HTTP endpoints using `@RestController`.
