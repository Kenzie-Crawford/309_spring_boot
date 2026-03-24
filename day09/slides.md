# Day 9 — Security Basics

---

## Learning Objectives

By the end of this lesson students will be able to:

1. Explain authentication vs. authorization
2. Add Spring Security to a Spring Boot application
3. Configure basic HTTP authentication
4. Protect specific endpoints
5. Understand the basics of JWT (conceptual)

---

## 1. Authentication vs. Authorization

| Concept | Question it answers | Example |
|---------|-------------------|---------|
| **Authentication** | "Who are you?" | Login with username/password |
| **Authorization** | "What are you allowed to do?" | Admin can delete; User can only read |

```
Request → Authentication (who?) → Authorization (allowed?) → Controller
```

---

## 2. Adding Spring Security

Add the dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### What Happens Immediately

After adding this dependency and restarting:

- **All endpoints are blocked** (return 401 Unauthorized)
- A default user is created: `user`
- A random password is printed in the console:
  ```
  Using generated security password: a1b2c3d4-e5f6-...
  ```
- A login form appears at `http://localhost:8080/login`

> This is Spring Security's **secure by default** philosophy.

---

## 3. Security Configuration

We need to configure which endpoints are public and which require authentication.

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
            .csrf(csrf -> csrf.disable())  // Disable CSRF for API (no browser forms)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.GET, "/api/cars/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/owners/**").permitAll()

                // Protected endpoints (require authentication)
                .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults());  // Enable HTTP Basic auth

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

---

## 4. Understanding the Configuration

### CSRF

```java
.csrf(csrf -> csrf.disable())
```
CSRF protection is for browser-based forms. REST APIs use tokens, so we disable it.

### Session Management

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
REST APIs are stateless — no server-side sessions.

### Authorization Rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/cars/**").permitAll()    // public
    .requestMatchers(HttpMethod.POST, "/api/**").authenticated()   // protected
    .anyRequest().authenticated()                                  // default
)
```

| Rule | Meaning |
|------|---------|
| `.permitAll()` | Anyone can access (no auth needed) |
| `.authenticated()` | Must be logged in |
| `.hasRole("ADMIN")` | Must have ADMIN role |
| `.anyRequest()` | Catch-all for unmatched URLs |

### HTTP Basic Auth

```java
.httpBasic(withDefaults())
```
Sends credentials as `Authorization: Basic base64(username:password)` header.

---

## 5. In-Memory Users

For learning purposes, we define users in code:

```java
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
```

> **Production apps** store users in a database — never hardcoded.

### Password Encoding

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- Passwords are **never stored in plain text**
- BCrypt is a one-way hash — can't reverse it
- Even in-memory users need encoded passwords

---

## 6. Testing Authentication

### Public Endpoint (no auth needed)

```
GET /api/cars
→ 200 OK (no credentials required)
```

### Protected Endpoint Without Auth

```
POST /api/cars
{ "make": "Toyota", ... }
→ 401 Unauthorized
```

### Protected Endpoint With Auth

In Postman:
1. Go to the **Authorization** tab
2. Select **Basic Auth**
3. Username: `user`, Password: `password`

```
POST /api/cars
Authorization: Basic dXNlcjpwYXNzd29yZA==
{ "make": "Toyota", ... }
→ 201 Created
```

---

## 7. Role-Based Authorization

Restrict certain actions to specific roles:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

Now:
- Anyone can **read** data
- Only **admins** can create, update, or delete

### Testing Roles

```
POST /api/cars (with user:password) → 403 Forbidden
POST /api/cars (with admin:admin123) → 201 Created
```

| Status | Meaning |
|--------|---------|
| **401 Unauthorized** | No credentials provided |
| **403 Forbidden** | Authenticated but not authorized (wrong role) |

---

## 8. Handling 401/403 in GlobalExceptionHandler

Add handlers for security-related errors:

```java
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    ErrorResponse error = new ErrorResponse(
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        "You do not have permission to perform this action"
    );
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

> Note: Spring Security may handle these before reaching your controller advice. The default responses work for most cases.

---

## 9. JWT (Conceptual Overview)

**JSON Web Tokens (JWT)** are the industry standard for API authentication.

### How JWT Works

```
1. Client sends credentials (username + password)
2. Server validates → generates a JWT token
3. Client stores the token
4. Client sends token in every request header
5. Server validates the token → allows/denies access
```

### JWT Structure

```
eyJhbGciOiJIUzI1NiJ9.          ← Header (algorithm)
eyJzdWIiOiJ1c2VyMSJ9.          ← Payload (claims: user, roles, expiry)
SflKxwRJSMeKKF2QT4fw            ← Signature (prevents tampering)
```

### Basic Auth vs. JWT

| Feature | Basic Auth | JWT |
|---------|-----------|-----|
| Credentials sent | Every request | Once (login), then token |
| Stateless | Yes | Yes |
| Security | Low (credentials in every request) | Higher (token-based) |
| Scalability | Simple | Better for distributed systems |
| Production use | Development/testing | Production APIs |

> Full JWT implementation is beyond the scope of this course, but understanding the concept is important.

---

## 10. Security Best Practices

1. **Never store plain-text passwords** — always use BCrypt or similar
2. **Use HTTPS in production** — Basic Auth over HTTP is insecure
3. **Principle of least privilege** — give minimum required permissions
4. **Don't expose security details** in error messages
5. **Validate input** even on authenticated endpoints
6. **Use environment variables** for secrets, never hardcode

---

## Summary

| Concept | Key Takeaway |
|---------|-------------|
| `spring-boot-starter-security` | Secures all endpoints by default |
| `SecurityFilterChain` | Configures which endpoints need auth |
| `.permitAll()` | Public access |
| `.authenticated()` | Requires login |
| `.hasRole("ADMIN")` | Requires specific role |
| HTTP Basic Auth | Simple auth for development/learning |
| BCrypt | Password hashing — never store plain text |
| 401 vs 403 | Unauthenticated vs unauthorized |

---

## Next: Day 10 — Capstone Project

Tomorrow you'll build a complete REST API integrating everything from the last 9 days.
