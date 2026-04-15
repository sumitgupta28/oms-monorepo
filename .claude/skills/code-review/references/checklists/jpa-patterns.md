# JPA / Hibernate 6 Patterns Checklist

## ✅ DO

### Fetch Strategy
- Default collections to `FetchType.LAZY`
- Use `@EntityGraph` or `JOIN FETCH` in repository methods that need associations
- Use projections (interfaces or records) for read-only queries

### Transactions
- Place `@Transactional` at the **service layer only**
- Use `@Transactional(readOnly = true)` for read operations (enables Hibernate optimizations)
- Never make `@Transactional` methods `private` or `final` (proxy won't intercept)

### Relationships
- Always define `mappedBy` on the inverse side of bidirectional relationships
- Use helper methods to keep both sides of bidirectional relationships in sync
- Prefer `Set` over `List` for `@ManyToMany` (avoids duplicate join rows)

### Auditing
```java
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class BaseEntity {
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

### Migrations
- Use **Flyway** or **Liquibase** — never `spring.jpa.hibernate.ddl-auto=create` in production
- Version scripts: `V1__create_users.sql`, `V2__add_index_on_email.sql`

---

## ❌ AVOID

### N+1 Problem
```java
// ❌ This fires N queries for N orders
List<Order> orders = orderRepo.findAll();
orders.forEach(o -> System.out.println(o.getCustomer().getName())); // N extra queries

// ✅ Fix with JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();
```

### Open Session in View
```properties
# ❌ Default is true — disables lazy loading protection
spring.jpa.open-in-view=false  # Always set to false
```

### Mutable Entities as DTOs
```java
// ❌ Exposing entity directly in REST response leaks schema and breaks encapsulation
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

// ✅ Map to a record DTO
public record UserResponse(Long id, String name, String email) {}
```

### Bidirectional without sync
```java
// ❌ Only sets one side — inconsistent in-memory state
order.getItems().add(item);

// ✅ Sync both sides
public void addItem(OrderItem item) {
    this.items.add(item);
    item.setOrder(this);
}
```
