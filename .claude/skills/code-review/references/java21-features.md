# Java 17–21 Feature Reference

## Java 16 — Records (Finalized)
Immutable data carriers. Use for DTOs, commands, events, value objects.
- Auto-generates constructor, accessors, equals, hashCode, toString
- Can implement interfaces, have custom methods
- Cannot extend classes (implicitly extends Record)

## Java 17 — Sealed Classes (Finalized)
Restrict which classes can extend/implement a type. Perfect for domain hierarchies.
```java
public sealed interface Shape permits Circle, Rectangle, Triangle {}
public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
```

## Java 17 — Pattern Matching for instanceof
```java
// No explicit cast needed
if (obj instanceof String s && s.length() > 5) { ... }
```

## Java 21 — Pattern Matching for switch (Finalized)
```java
String result = switch (obj) {
    case Integer i -> "int: " + i;
    case String s  -> "str: " + s;
    case null      -> "null!";
    default        -> "other";
};
```

## Java 21 — Virtual Threads (Project Loom, Finalized)
Lightweight threads managed by JVM, not OS. Massive concurrency for I/O-bound work.
```java
// Spring Boot 3.2+ auto-configures if enabled
spring.threads.virtual.enabled=true

// Or manually
Thread.ofVirtual().start(() -> handleRequest());
ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
```

## Java 15 — Text Blocks (Finalized)
```java
String sql = """
        SELECT u.id, u.name
        FROM users u
        WHERE u.active = true
        ORDER BY u.created_at DESC
        """;
```

## Java 21 — Sequenced Collections
New interfaces: SequencedCollection, SequencedSet, SequencedMap
```java
list.getFirst(); list.getLast();
list.reversed();
```

## Java 14+ — Helpful NullPointerExceptions
JVM now tells you exactly which variable was null. No config needed in Java 21.

## Java 21 — Record Patterns (Finalized)
Destructure records directly in pattern matching:
```java
if (obj instanceof Point(int x, int y)) {
    System.out.println(x + ", " + y);
}
```
