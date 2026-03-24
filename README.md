# 309 — Spring Boot Module (10 Days)

## Prerequisites
- Java fundamentals
- JPA / Hibernate basics (entities, repositories, CRUD)

## Outcome
Students build a **production-style REST API** using Spring Boot, layered architecture, DTOs, validation, relationships, exception handling, advanced queries, and security.

---

## Module Structure

| Day | Topic | Key Deliverable |
|-----|-------|-----------------|
| 1 | Spring Boot Foundations + Project Setup | Running app with DB connection |
| 2 | REST Controllers | CRUD endpoints returning JSON |
| 3 | Layered Architecture | Service layer extracted |
| 4 | DTOs & Mapping | Clean request/response models |
| 5 | Validation | Bean validation on inputs |
| 6 | Entity Relationships | OneToMany / ManyToOne working |
| 7 | Exception Handling | Global error handler |
| 8 | Advanced Queries | Filtering + pagination |
| 9 | Security Basics | Protected endpoints |
| 10 | Capstone Project | Full REST API integrating all concepts |

---

## Folder Layout

```
309_spring_boot/
├── README.md                  ← You are here
├── day01/
│   ├── slides.md              ← Lecture content
│   └── lab.md                 ← Guided in-class lab
├── day02/ … day10/            ← Same structure
└── project/                   ← Starter Spring Boot project
    └── cardealership/         ← Maven project root
```

## Running the Starter Project

```bash
cd project/cardealership
./mvnw spring-boot:run
```

The app runs on `http://localhost:8080` and uses a **MySQL database**.

> **Prerequisites:** Install MySQL and create the database:
> ```sql
> CREATE DATABASE cardealership;
> ```

> **Note:** The starter project is intentionally minimal — just a Maven project with the main class and MySQL configuration. Students build all entities, repositories, services, controllers, and other layers incrementally during the daily labs.

---

## Domain Model (Used Throughout)

The course uses a **Car Dealership** domain:

- **Car** — make, model, year, color, price
- **Owner** — firstName, lastName, email, phone (introduced Day 6)
- Relationship: One Owner → Many Cars

This domain is simple enough to teach concepts without domain-knowledge overhead.
