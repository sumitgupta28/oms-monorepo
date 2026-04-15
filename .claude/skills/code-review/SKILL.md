---
name: code-review
description: >
  Perform deep, structured code reviews for Java 21+ and Spring Boot 3+ projects,
  enforcing modern Java standards, idiomatic patterns, and production-grade quality.
  Trigger this skill whenever the user asks to "review my code", "check this PR",
  "give me feedback on this diff", "is this good Java?", "review my service/controller/entity",
  pastes Java or Spring Boot code for evaluation, or asks about best practices in
  Java 21+, Spring Boot 3+, JPA, REST API design, or security. Also trigger for
  any request involving virtual threads, records, sealed classes, pattern matching,
  or other modern Java features introduced in Java 17–21.
---

# Code Review Skill — Java 21+ & Spring Boot 3+

## Scope

This skill reviews Java 21+ and Spring Boot 3+ codebases against:
- Modern Java language standards (records, sealed classes, pattern matching, virtual threads)
- Spring Boot 3.x best practices (native image, observability, problem details, security 6.x)
- REST API design, JPA/Hibernate 6, validation, error handling, testing

## Review Dimensions

Always evaluate code across ALL of the following axes. Skip none.

### 1. Modern Java Usage
- Are **records** used for DTOs, value objects, and command objects instead of mutable POJOs?
- Are **sealed interfaces/classes** used for exhaustive domain hierarchies?
- Is **pattern matching** (`instanceof`, `switch` expressions) used instead of old-style casting?
- Are **text blocks** used for multi-line strings (SQL, JSON, HTML)?
- Are **virtual threads** (Project Loom) considered for I/O-bound workloads?
- Is the **Stream API** used idiomatically without anti-patterns (e.g., no `forEach` for side effects on parallel streams)?
- Is `Optional` used correctly — only as a return type, never as a field or parameter?

### 2. Spring Boot 3+ Standards
- Are **constructor injection** used everywhere — never `@Autowired` on fields?
- Is `@ConfigurationProperties` used instead of `@Value` for structured config?
- Are **functional endpoints** (RouterFunction) considered for lightweight routes?
- Is **Spring Security 6.x** applied correctly (no deprecated `WebSecurityConfigurerAdapter`)?
- Is `SecurityFilterChain` bean-based configuration used?
- Are **Problem Details** (`ProblemDetail`, RFC 7807) used for error responses?
- Is `@ControllerAdvice` + `@ExceptionHandler` centralizing error handling?
- Are **Spring Boot Actuator** endpoints secured and sensibly exposed?
- Is `spring.jpa.open-in-view=false` set? (Avoid Open Session in View anti-pattern)

### 3. REST API Design
- Are HTTP verbs semantically correct (GET is idempotent, POST creates, PUT replaces, PATCH partially updates)?
- Are status codes precise (201 for creation, 204 for no-content, 409 for conflict, 422 for validation errors)?
- Is API versioning present (path-based `/v1/`, header-based, or content negotiation)?
- Are resource names plural nouns, not verbs (`/users` not `/getUsers`)?
- Is pagination implemented for collection endpoints (Pageable, Page<T>)?
- Are responses wrapped in a consistent envelope or using Spring HATEOAS?

### 4. JPA / Hibernate 6 & Data Layer
- Are **bidirectional relationships** correctly managed (owning side, `mappedBy`)?
- Is **fetch type** correct — `LAZY` by default for collections, avoiding N+1 queries?
- Are **projections** or DTOs used for read-only queries instead of loading full entities?
- Is `@Transactional` placed at the service layer, not DAO or controller?
- Are **Spring Data Specifications** or **QueryDSL** used for dynamic queries (not concatenated JPQL strings)?
- Are **database migrations** handled by Flyway or Liquibase?
- Are `@CreatedDate`, `@LastModifiedDate` via `@EnableJpaAuditing` used for audit fields?

### 5. Validation & Error Handling
- Is **Bean Validation 3.0** (`@Valid`, `@NotNull`, `@Size`, custom constraints) applied at the controller boundary?
- Are validation errors surfaced as 422 with field-level detail, not 400 with a generic message?
- Are custom exceptions domain-meaningful (`OrderNotFoundException` not `RuntimeException`)?
- Is error propagation clean — no swallowed exceptions or empty catch blocks?
- Are external API calls wrapped with proper timeout, retry, and circuit breaker patterns?

### 6. Security
- Is input sanitized against injection attacks (SQL, XSS, XXE)?
- Are passwords hashed with BCrypt/Argon2 — never MD5/SHA1?
- Is JWT validation robust (algorithm pinning, expiry check, signature verification)?
- Are endpoints that expose sensitive data protected with proper authority checks (`@PreAuthorize`)?
- Is CORS configured explicitly, not with `*` in production?
- Are secrets externalized (env vars, Vault) — never hardcoded in source?

### 7. Performance
- Are queries analyzed for N+1 (use `@EntityGraph` or JOIN FETCH where appropriate)?
- Is caching applied sensibly (`@Cacheable` for stable data, with eviction policy)?
- Are responses compressed (gzip via `server.compression.enabled=true`)?
- Are async operations (`@Async`, `CompletableFuture`, virtual threads) used for parallelizable work?
- Are database indexes present for all frequently queried columns?

### 8. Testing
- Are **unit tests** using JUnit 5 + Mockito, with no Spring context (fast)?
- Are **integration tests** using `@SpringBootTest` + `@Testcontainers` for real DB?
- Are **slice tests** used (`@WebMvcTest`, `@DataJpaTest`) to isolate layers?
- Is test coverage meaningful — edge cases, error paths, not just happy path?
- Are tests using AssertJ (`assertThat`) not JUnit's older `assertEquals`?

### 9. Code Clarity & Architecture
- Are layers cleanly separated (Controller → Service → Repository — no business logic in controllers)?
- Are package names domain-driven, not layer-driven (prefer `order/` over `service/`)?
- Are classes and methods small and focused (Single Responsibility)?
- Are magic strings and numbers extracted to constants or enums?
- Is Lombok used minimally — prefer records over `@Data` for immutable types?

---

## Severity Scale

| Level | Label | Meaning | Example |
|---|---|---|---|
| 🔴 | **P0 — Blocker** | Security vuln, data corruption, crash risk | Hardcoded secret, SQL injection, missing `@Transactional` on write |
| 🟠 | **P1 — Major** | Logic bug, design flaw, broken edge case | N+1 query, field injection, Open Session in View enabled |
| 🟡 | **P2 — Minor** | Violation of standards, poor pattern | Mutable DTO where record fits, `@Value` instead of `@ConfigurationProperties` |
| 🟢 | **P3 — Suggestion** | Optional improvement, style | Rename variable, add missing `@EntityGraph`, extract constant |

---

## Output Format

Always produce a review in this exact structure:

```
## 📋 Code Summary
[2–3 sentences: what this code does and its role in the system]

## 🔍 Findings

### 🔴 P0 — Blockers
[List each with: file/line if available, what's wrong, why it matters, corrected code snippet]

### 🟠 P1 — Major Issues
[Same format]

### 🟡 P2 — Minor Issues
[Same format]

### 🟢 P3 — Suggestions
[Same format, can be brief]

## ✅ What's Done Well
[At least 2 genuine positives — not filler]

## 🏁 Verdict
[ Approve | Approve with comments | Request changes ]
[One sentence rationale]
```

---

## Java 21+ Pattern Examples

When flagging issues, always show the corrected idiomatic version.

### DTO: Use Record, not POJO
```java
// ❌ Old style
public class UserDto {
    @NotNull private String name;
    private String email;
    // getters, setters, equals, hashCode, toString...
}

// ✅ Java 21
public record UserDto(
    @NotNull String name,
    String email
) {}
```

### Pattern Matching: switch expression
```java
// ❌ Old style
if (shape instanceof Circle) {
    Circle c = (Circle) shape;
    return Math.PI * c.radius() * c.radius();
} else if (shape instanceof Rectangle r) { ... }

// ✅ Java 21
return switch (shape) {
    case Circle c    -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.width() * r.height();
    default          -> throw new IllegalArgumentException("Unknown shape");
};
```

### Spring Security 6.x
```java
// ❌ Deprecated
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter { ... }

// ✅ Spring Boot 3+
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .build();
    }
}
```

### Problem Details (RFC 7807)
```java
// ✅ Spring Boot 3+ built-in
@ExceptionHandler(OrderNotFoundException.class)
public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Order Not Found");
    pd.setProperty("orderId", ex.getOrderId());
    return pd;
}
```

---

## Reference Files

Load these when reviewing code in the relevant domain:

- `references/checklists/spring-security.md` — Deep security checklist
- `references/checklists/jpa-patterns.md` — JPA/Hibernate anti-patterns
- `references/checklists/rest-api.md` — REST design rules
- `references/java21-features.md` — Full Java 17–21 feature guide

---

## Tone Guidelines

- Be direct and specific. Vague feedback ("this could be better") is useless.
- Always provide the corrected code, not just the problem.
- Praise genuinely good patterns — do not skip the positives section.
- Frame uncertain findings as questions: "Consider whether X might cause Y in Z scenario."
- Never be condescending. Assume competence, flag the issue, show the fix.
