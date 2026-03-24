# Day 10 — Capstone Project

---

## Overview

Build a complete REST API that integrates **all concepts** from Days 1–9. Work independently or in pairs.

---

## Requirements

Your API **must** include all of the following:

### 1. Entities & Relationships
- At least **2 entities** with a `@OneToMany` / `@ManyToOne` relationship
- Proper JPA annotations

### 2. Layered Architecture
- **Controller** — HTTP concerns only
- **Service** — business logic
- **Repository** — data access
- Controllers must not reference repositories directly

### 3. DTOs & Mapping
- **Request DTOs** for create/update (no entity exposed)
- **Response DTOs** for API output
- **Mapper** class(es) for entity ↔ DTO conversion

### 4. Validation
- Bean Validation annotations on request DTOs
- `@Valid` on controller endpoints
- Meaningful error messages

### 5. Exception Handling
- Global exception handler (`@RestControllerAdvice`)
- Custom exceptions (at least `ResourceNotFoundException`)
- Consistent error response format
- No stack traces leaked

### 6. Advanced Queries
- At least **one** custom query (derived method or `@Query`)
- Pagination on at least one list endpoint
- At least **one** filter/search endpoint

### 7. Security
- Spring Security with HTTP Basic (or JWT if ambitious)
- GET endpoints are public
- Write operations require authentication
- At least 2 roles with different permissions

---

## Suggested Project Ideas

Choose one or propose your own:

### Option A: Car Inventory System
- **Car** — make, model, year, color, price, mileage, status (available/sold)
- **Owner** — firstName, lastName, email, phone
- Owner has many Cars
- Filter by make, year range, price range, status
- Only ADMIN can mark a car as sold

### Option B: Task Manager API
- **Project** — name, description, startDate, status
- **Task** — title, description, priority, status, dueDate
- Project has many Tasks
- Filter tasks by priority, status, due date
- Search tasks by keyword
- Only MANAGER+ can create projects

### Option C: Library System
- **Author** — firstName, lastName, bio
- **Book** — title, isbn, genre, publishedYear, available
- Author has many Books
- Search by title, genre, author name
- Only ADMIN can add/remove books

### Option D: Your Own Idea
- Must meet all requirements above
- Get instructor approval before starting

---

## Evaluation Rubric

| Category | Points | Criteria |
|----------|--------|----------|
| **Entities & Relationships** | 15 | 2+ entities, proper JPA annotations, relationship works |
| **Layered Architecture** | 15 | Clear Controller → Service → Repository separation |
| **DTOs & Mapping** | 15 | Request/Response DTOs, mapper class, no entity leaks |
| **CRUD Endpoints** | 10 | GET all, GET by ID, POST, PUT, DELETE for primary entity |
| **Validation** | 10 | Bean validation on DTOs, meaningful messages, 400 on invalid |
| **Exception Handling** | 10 | Global handler, custom exceptions, consistent format |
| **Advanced Queries** | 10 | Pagination + at least 1 filter/search endpoint |
| **Security** | 10 | Public reads, protected writes, role-based access |
| **Code Quality** | 5 | Clean code, consistent naming, proper packages |
| **Total** | **100** | |

---

## Project Structure (Expected)

```
src/main/java/com/example/yourproject/
├── YourProjectApplication.java
├── DataLoader.java
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── PrimaryController.java
│   └── SecondaryController.java
├── dto/
│   ├── PrimaryRequest.java
│   ├── PrimaryResponse.java
│   ├── SecondaryRequest.java
│   └── SecondaryResponse.java
├── entity/
│   ├── PrimaryEntity.java
│   └── SecondaryEntity.java
├── exception/
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ValidationErrorResponse.java
├── mapper/
│   ├── PrimaryMapper.java
│   └── SecondaryMapper.java
├── repository/
│   ├── PrimaryRepository.java
│   └── SecondaryRepository.java
└── service/
    ├── PrimaryService.java
    ├── PrimaryServiceImpl.java
    ├── SecondaryService.java
    └── SecondaryServiceImpl.java
```

---

## API Endpoints (Minimum)

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/primary` | Public | List all (paginated) |
| GET | `/api/primary/{id}` | Public | Get by ID |
| GET | `/api/primary/search?q=...` | Public | Search |
| GET | `/api/primary/filter?...` | Public | Filter |
| POST | `/api/primary` | Authenticated | Create |
| PUT | `/api/primary/{id}` | Authenticated | Update |
| DELETE | `/api/primary/{id}` | ADMIN | Delete |
| GET | `/api/secondary` | Public | List all |
| GET | `/api/secondary/{id}` | Public | Get by ID |
| POST | `/api/secondary` | Authenticated | Create |

---

## Timeline

| Time | Activity |
|------|----------|
| 0:00–0:15 | Choose project, plan entities and endpoints |
| 0:15–0:45 | Create entities, repositories, DTOs, mappers |
| 0:45–1:15 | Build services and controllers (CRUD) |
| 1:15–1:30 | Add validation and exception handling |
| 1:30–1:45 | Add advanced queries (pagination, search, filter) |
| 1:45–2:00 | Add security |
| 2:00–2:15 | Test all endpoints, fix bugs |
| 2:15–2:30 | Code review / demo |

---

## Submission

1. Push your project to GitHub (or zip and submit)
2. Include a `README.md` with:
   - Project description
   - How to run the app
   - List of all endpoints with example requests
   - Test credentials (username/password for each role)
3. Be ready to demo your API

---

## Tips

- **Start simple** — get basic CRUD working first, then add features
- **Test as you go** — don't build everything before testing
- **Use your Day 1–9 code as reference** — you've built all of this before
- **Commit often** — save your progress in case you need to roll back
- **Ask for help** if you're stuck on a specific concept
