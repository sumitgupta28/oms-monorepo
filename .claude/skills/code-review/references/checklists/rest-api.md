# REST API Design Checklist — Spring Boot 3+

## Resource Naming
| Rule | ❌ Wrong | ✅ Correct |
|---|---|---|
| Plural nouns | `/getUser` | `/users` |
| Lowercase | `/UserOrders` | `/user-orders` |
| Hierarchical | `/ordersForUser/1` | `/users/1/orders` |
| No verbs | `/createProduct` | `POST /products` |

## HTTP Verbs & Status Codes
| Action | Verb | Success | Error |
|---|---|---|---|
| List | GET | 200 | 400, 404 |
| Get one | GET | 200 | 404 |
| Create | POST | 201 + Location header | 400, 409, 422 |
| Full update | PUT | 200 or 204 | 400, 404, 409 |
| Partial update | PATCH | 200 or 204 | 400, 404 |
| Delete | DELETE | 204 | 404 |
| Search/filter | GET with query params | 200 | 400 |

## Pagination (Spring Data)
```java
// Controller
@GetMapping("/orders")
public Page<OrderResponse> list(Pageable pageable) {
    return orderService.findAll(pageable).map(OrderResponse::from);
}

// Request: GET /orders?page=0&size=20&sort=createdAt,desc
```

## Versioning Options
- **Path-based** (simplest): `/api/v1/users`
- **Header-based**: `Accept: application/vnd.myapp.v1+json`
- **Query param**: `/users?version=1` (least preferred)

Always version from day 1. Changing it later is painful.

## Error Response — Problem Details (RFC 7807)
Spring Boot 3+ includes built-in support. Enable with:
```properties
spring.mvc.problemdetails.enabled=true
```

Response shape:
```json
{
  "type": "https://api.example.com/errors/order-not-found",
  "title": "Order Not Found",
  "status": 404,
  "detail": "Order with id 123 does not exist",
  "instance": "/orders/123",
  "orderId": 123
}
```

## Validation Response (422)
```json
{
  "status": 422,
  "title": "Validation Failed",
  "violations": [
    { "field": "email", "message": "must be a valid email" },
    { "field": "name",  "message": "must not be blank" }
  ]
}
```

## Controller Best Practices
```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor  // constructor injection
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
```
