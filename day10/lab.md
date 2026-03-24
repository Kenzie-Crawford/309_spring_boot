# Day 10 Lab — Capstone Build Day

## Objective

Build a complete REST API integrating all concepts from the module. This is your lab and the main deliverable.

---

## Planning Phase (15 min)

Before writing any code, plan your project:

### 1. Choose your project (or use the car dealership)

### 2. Define your entities

| Entity | Fields | Relationship |
|--------|--------|-------------|
| | | |
| | | |

### 3. List your endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| | | | |

### 4. List your validation rules

| DTO Field | Constraint | Message |
|-----------|-----------|---------|
| | | |

---

## Build Checklist

Work through this checklist in order:

### Phase 1: Foundation
- [ ] Generate project (or use your project from Days 1-9)
- [ ] Create entities with JPA annotations
- [ ] Create relationship (`@OneToMany` / `@ManyToOne`)
- [ ] Create repositories
- [ ] Create `DataLoader` to seed data
- [ ] **TEST:** Run app, check MySQL for tables and data

### Phase 2: DTOs & Mapping
- [ ] Create Request DTOs (with validation annotations)
- [ ] Create Response DTOs
- [ ] Create Mapper classes
- [ ] **TEST:** Classes compile correctly

### Phase 3: Service Layer
- [ ] Create service interfaces
- [ ] Create service implementations
- [ ] Add business logic (validation, etc.)
- [ ] **TEST:** Inject and call from a CommandLineRunner (optional)

### Phase 4: Controllers
- [ ] Create REST controllers with all CRUD endpoints
- [ ] Add `@Valid` to POST/PUT
- [ ] Add search/filter endpoint
- [ ] Add pagination to list endpoint
- [ ] **TEST:** Test all endpoints with Postman

### Phase 5: Exception Handling
- [ ] Create `ErrorResponse` and custom exceptions
- [ ] Create `GlobalExceptionHandler`
- [ ] **TEST:** 404 for missing resources, 400 for invalid input

### Phase 6: Security
- [ ] Add Spring Security dependency
- [ ] Create `SecurityConfig`
- [ ] Define users and roles
- [ ] Configure public vs. protected endpoints
- [ ] **TEST:** Public GET works, POST/PUT/DELETE require auth

### Phase 7: Polish
- [ ] Remove any hardcoded values
- [ ] Consistent naming across all files
- [ ] Proper package structure
- [ ] Write README with endpoint documentation

---

## Demo Prep

Be ready to demonstrate:

1. **GET all** (paginated) — show pagination metadata
2. **GET by ID** — show a single resource
3. **Search/filter** — show query results
4. **POST** without auth — show 401
5. **POST** with auth — show 201 + created resource
6. **POST** with invalid data — show 400 + validation errors
7. **DELETE** with non-admin — show 403
8. **DELETE** with admin — show 204
9. **GET** deleted resource — show 404 with error response
10. **Nested data** — show relationship in response (e.g., owner with cars)

---

## Deliverables

- [ ] Working REST API meeting all requirements
- [ ] Clean layered architecture
- [ ] All endpoints tested
- [ ] README with documentation
- [ ] Ready for demo

---

## Submission Requirements

### Final Checklist

If you didn't finish during class, complete the remaining items:

- [ ] All CRUD endpoints working
- [ ] DTOs (no entities in API responses)
- [ ] Validation with meaningful error messages
- [ ] Entity relationship working (nested data)
- [ ] Global exception handler (consistent errors)
- [ ] At least one advanced query (search, filter, or custom)
- [ ] Pagination on at least one list endpoint
- [ ] Spring Security (public reads, protected writes)
- [ ] Data seeder (`DataLoader`)

### README Requirements

Your project must include a `README.md` with:

#### 1. Project Description
2-3 sentences about what the API does.

#### 2. How to Run
```bash
cd your-project
./mvnw spring-boot:run
```

#### 3. Test Credentials
| Username | Password | Role |
|----------|----------|------|
| user | password | USER |
| admin | admin123 | ADMIN |

#### 4. API Endpoints
Document every endpoint with:
- HTTP method and URL
- Auth required?
- Example request body (for POST/PUT)
- Example response

#### 5. Technologies Used
- Spring Boot 3.x
- Spring Data JPA
- Spring Security
- MySQL
- Bean Validation

### Reflection Questions

Answer these at the bottom of your README:

1. What was the most challenging concept to implement? Why?
2. What would you add to this API if you had more time?
3. How does the layered architecture help when working on a team?
4. What's the difference between how you handled errors on Day 2 vs. Day 7?
5. Why is it important to use DTOs instead of returning entities directly?

### Submission Steps

1. Push your completed project to GitHub
2. Include `README.md` with all sections above
3. Ensure the app starts without errors
4. Submit the GitHub link

---

## Congratulations!

You've built a production-style REST API with:
- Clean architecture
- Proper data modeling
- Input validation
- Standardized error handling
- Flexible queries
- Authentication & authorization

These are the same patterns used in professional Spring Boot applications.
