# Spring Security 6.x Checklist

## Configuration — Bean-based only
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // replaces @EnableGlobalMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())  // for stateless APIs with JWT
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

## Method Security
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }

@PreAuthorize("hasAuthority('order:read') and #userId == authentication.principal.id")
public List<Order> getUserOrders(Long userId) { ... }
```

## Password Encoding
```java
// ✅ BCrypt (default strength 10)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// ✅ Argon2 (stronger, memory-hard)
@Bean
public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
}
```

## JWT Validation Checklist
- [ ] Algorithm pinned — never `alg: none`
- [ ] Signature verified with secret or public key
- [ ] `exp` (expiry) checked
- [ ] `iss` (issuer) validated
- [ ] `aud` (audience) validated if multi-tenant
- [ ] Token stored in `Authorization: Bearer` header — never in cookies (CSRF risk) or localStorage (XSS risk)

## CORS Configuration
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.mycompany.com")); // never "*" in prod
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

## Actuator Security
```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.endpoints.web.base-path=/actuator
```
Protect `/actuator/**` with ADMIN role in `SecurityFilterChain`.

## Common Vulnerabilities to Check
| Vuln | What to look for |
|---|---|
| SQL Injection | String-concatenated JPQL/SQL, not parameterized |
| XSS | Unescaped user input rendered in responses |
| IDOR | Missing ownership check on resource access |
| Mass Assignment | Entity bound directly from request body |
| Hardcoded secrets | Passwords/keys in source, not env/vault |
| Insecure deserialization | Jackson polymorphism without type filtering |
