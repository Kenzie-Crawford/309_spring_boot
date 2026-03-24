# Day 9 Lab — Securing the API

## Objective

Add Spring Security with HTTP Basic authentication and role-based access control.

---

## Part 1: Add the Dependency (2 min)

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Quick Test

1. Restart the app
2. Try `GET /api/cars` → Should get 401 (everything locked down)
3. Check the console for the generated password
4. **Don't panic** — we'll configure it next

---

## Part 2: Create Security Configuration (15 min)

Create `com.example.cardealership.config.SecurityConfig`:

```java
package com.example.cardealership.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: anyone can read
                .requestMatchers(HttpMethod.GET, "/api/cars/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/owners/**").permitAll()

                // Protected: must be authenticated to write
                .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()

                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Checkpoint ✅

- `GET /api/cars` → 200 OK (no auth needed)
- `POST /api/cars` without auth → 401 Unauthorized

---

## Part 3: Test Public vs. Protected Endpoints (10 min)

### Test 1: Public GET (no auth)
```
GET /api/cars
→ 200 OK
```

### Test 2: Protected POST without auth
```
POST /api/cars
{ "make": "Toyota", "model": "Camry", "year": 2024, "color": "Silver", "price": 28000 }
→ 401 Unauthorized
```

### Test 3: Protected POST with auth
In Postman:
1. Go to **Authorization** tab
2. Type: **Basic Auth**
3. Username: `user`
4. Password: `password`

```
POST /api/cars
{ "make": "Toyota", "model": "Camry", "year": 2024, "color": "Silver", "price": 28000 }
→ 201 Created
```

### Test 4: DELETE with auth
```
DELETE /api/cars/1
Authorization: Basic (user:password)
→ 204 No Content
```

### Test 5: Wrong credentials
```
POST /api/cars
Authorization: Basic (user:wrongpassword)
→ 401 Unauthorized
```

### Checkpoint ✅

Public endpoints are accessible; protected endpoints require valid credentials.

---

## Part 4: Add Role-Based Access (10 min)

Update the security configuration to require ADMIN role for delete:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
    .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")  // ← Only admins
    .anyRequest().authenticated()
)
```

### Test:

```
DELETE /api/cars/1 (with user:password)   → 403 Forbidden
DELETE /api/cars/1 (with admin:admin123)  → 204 No Content
```

### Checkpoint ✅

- USER can create and update
- Only ADMIN can delete
- 403 returned for insufficient role (not 401)

---

## Part 5: Test the Full Flow (10 min)

Run through this complete scenario:

1. `GET /api/cars` (no auth) → See seeded cars ✅
2. `POST /api/cars` (no auth) → 401 ✅
3. `POST /api/cars` (user:password) → 201 ✅
4. `PUT /api/cars/1` (user:password) → 200 ✅
5. `DELETE /api/cars/1` (user:password) → 403 ✅
6. `DELETE /api/cars/1` (admin:admin123) → 204 ✅
7. `GET /api/cars/1` → 404 (deleted) ✅

---

## Deliverables

- [ ] `spring-boot-starter-security` dependency added
- [ ] `SecurityConfig` with filter chain and user details
- [ ] GET endpoints are public
- [ ] POST/PUT require authentication
- [ ] DELETE requires ADMIN role
- [ ] BCrypt password encoding used
- [ ] All 7 test scenarios pass
