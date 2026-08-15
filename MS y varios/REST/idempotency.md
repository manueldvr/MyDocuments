# ¿Por qué se necesita implementar la idempotencia en el servidor?

Porque enviar un header como:

```http
Idempotency-Key: abc-123
```

**no vuelve idempotente al `POST` automáticamente**.

El header solo comunica la intención del cliente:

> “Esta petición pertenece a la operación lógica `abc-123`. Si vuelve a llegar, no la proceses otra vez”.

Pero alguien debe:

* leer esa clave;
* comprobar si ya fue utilizada;
* evitar una segunda ejecución;
* recuperar la respuesta anterior;
* controlar peticiones simultáneas;
* detectar si la clave se reutilizó con otro contenido.

Ese “alguien” es el servidor que recibe el `POST`.

---

# 1. Sin implementación en el servidor

El cliente envía:

```http
POST /api/payments
Idempotency-Key: abc-123
```

```json
{
  "accountId": 100,
  "amount": 5000
}
```

El controlador ignora el header:

```java
@PostMapping("/payments")
public ResponseEntity<PaymentDTO> create(
        @RequestBody PaymentRequest request
) {
    PaymentDTO payment = service.create(request);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(payment);
}
```

Primera petición:

```text
Se crea paymentId = 9001
```

El cliente no recibe la respuesta por un timeout y reintenta:

```http
POST /api/payments
Idempotency-Key: abc-123
```

El servidor vuelve a ejecutar:

```java
service.create(request);
```

Resultado:

```text
Se crea paymentId = 9002
```

Aunque el cliente envió la misma clave, el servidor creó dos pagos.

La clave no tuvo ningún efecto porque nadie la procesó.

---

# 2. Con implementación en el servidor

El controlador recibe la clave:

```java
@PostMapping("/payments")
public ResponseEntity<PaymentDTO> create(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody PaymentRequest request
) {
    PaymentDTO payment = service.create(
            idempotencyKey,
            request
    );

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(payment);
}
```

El servicio consulta si esa clave ya existe:

```text
Idempotency-Key = abc-123
```

## Primera petición

```text
La clave no existe
        │
        ▼
Se registra como PROCESSING
        │
        ▼
Se crea el pago 9001
        │
        ▼
Se guarda la respuesta
        │
        ▼
Se marca como COMPLETED
```

## Segunda petición

```text
La clave abc-123 ya existe
        │
        ▼
La operación está COMPLETED
        │
        ▼
No se crea otro pago
        │
        ▼
Se devuelve nuevamente paymentId 9001
```

Ese comportamiento es el que convierte al `POST` en idempotente.

---

# 3. Qué debe recordar el servidor

El servidor necesita guardar algún registro parecido a este:

| Idempotency key | Estado      | Request hash | Recurso creado | Respuesta     |
| --------------- | ----------- | ------------ | -------------: | ------------- |
| `abc-123`       | `COMPLETED` | `f7a91...`   |         `9001` | JSON del pago |

Por ejemplo:

```text
idempotency_key = abc-123
status          = COMPLETED
request_hash    = f7a91...
resource_id     = 9001
response_status = 201
response_body   = {"id":9001,...}
```

Cuando llega nuevamente `abc-123`, el servidor consulta este registro.

---

# 4. ¿Por qué no alcanza con buscar el pago?

Podríamos pensar:

```java
Payment payment =
        paymentRepository.findByAccountIdAndAmount(
                request.accountId(),
                request.amount()
        );
```

Pero eso no identifica necesariamente la misma operación.

Un cliente podría realizar legítimamente dos pagos iguales:

```text
Pago 1: cuenta 100, importe 5000
Pago 2: cuenta 100, importe 5000
```

Los datos son iguales, pero son operaciones distintas.

La clave idempotente identifica la **intención lógica de una petición concreta**, no solamente su contenido.

```text
Idempotency-Key A → primer pago de $5000
Idempotency-Key B → segundo pago legítimo de $5000
```

---

# 5. ¿Por qué guardar el hash del request?

Para detectar este problema:

Primera petición:

```http
Idempotency-Key: abc-123
```

```json
{
  "amount": 5000
}
```

Segunda petición:

```http
Idempotency-Key: abc-123
```

```json
{
  "amount": 9000
}
```

La misma clave no debería representar dos operaciones diferentes.

Por eso el servidor almacena un hash del primer request:

```text
abc-123 → hash(request de 5000)
```

Cuando llega la segunda petición, calcula otro hash.

```text
hash anterior != hash nuevo
```

Entonces responde, por ejemplo:

```http
409 Conflict
```

Sin ese control, podría devolver el resultado de un pago de `$5000` frente a una petición que solicitaba `$9000`.

---

# 6. ¿Por qué guardar el estado `PROCESSING`?

Porque pueden llegar dos peticiones casi al mismo tiempo.

```text
Request A ─────┐
               ├── Idempotency-Key: abc-123
Request B ─────┘
```

Ambas podrían consultar:

```text
¿Existe abc-123?
```

Y ambas obtener:

```text
No
```

Luego ambas crearían el pago.

Por eso la clave debe reservarse antes de ejecutar la operación:

```text
Insertar abc-123 como PROCESSING
```

Solo una petición debe poder insertar esa clave.

La otra encontrará que ya existe.

---

# 7. La importancia de la restricción única

La tabla debe tener una restricción:

```sql
ALTER TABLE idempotency_requests
ADD CONSTRAINT uk_idempotency_key
UNIQUE (idempotency_key);
```

O mediante JPA:

```java
@Entity
@Table(
    name = "idempotency_requests",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_key",
        columnNames = "idempotency_key"
    )
)
public class IdempotencyRequest {
}
```

¿Por qué?

Porque esto no es completamente seguro:

```java
if (!repository.existsByIdempotencyKey(key)) {
    repository.save(new IdempotencyRequest(key));
}
```

Dos threads pueden hacer:

```text
Thread A: no existe
Thread B: no existe
Thread A: inserta
Thread B: inserta
```

La restricción única hace que la base de datos sea la última garantía:

```text
Thread A inserta correctamente
Thread B recibe duplicate key
```

---

# 8. Ejemplo simplificado del servidor

## Entidad

```java
@Entity
@Table(
    name = "idempotency_request",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_key",
        columnNames = "idempotencyKey"
    )
)
public class IdempotencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    private Long resourceId;

    @Lob
    private String responseBody;

    private Integer responseStatus;

    // getters y setters
}
```

```java
public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
```

---

## Controller

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentDTO> create(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @RequestBody
            PaymentRequest request
    ) {
        PaymentDTO result =
                paymentService.create(
                        idempotencyKey,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }
}
```

---

## Servicio conceptual

```java
@Transactional
public PaymentDTO create(
        String idempotencyKey,
        PaymentRequest request
) {
    String requestHash = hash(request);

    Optional<IdempotencyRequest> existing =
            idempotencyRepository
                    .findByIdempotencyKey(
                            idempotencyKey
                    );

    if (existing.isPresent()) {
        return resolveExisting(
                existing.get(),
                requestHash
        );
    }

    IdempotencyRequest record =
            new IdempotencyRequest();

    record.setIdempotencyKey(idempotencyKey);
    record.setRequestHash(requestHash);
    record.setStatus(
            IdempotencyStatus.PROCESSING
    );

    idempotencyRepository.saveAndFlush(record);

    Payment payment =
            paymentRepository.save(
                    Payment.from(request)
            );

    PaymentDTO response =
            PaymentDTO.from(payment);

    record.setStatus(
            IdempotencyStatus.COMPLETED
    );

    record.setResourceId(payment.getId());
    record.setResponseStatus(201);
    record.setResponseBody(
            serialize(response)
    );

    return response;
}
```

---

## Resolver una clave existente

```java
private PaymentDTO resolveExisting(
        IdempotencyRequest existing,
        String requestHash
) {
    if (!existing.getRequestHash()
            .equals(requestHash)) {

        throw new IdempotencyConflictException(
                "The Idempotency-Key was already used "
                + "with a different request"
        );
    }

    return switch (existing.getStatus()) {

        case COMPLETED ->
                deserialize(
                        existing.getResponseBody(),
                        PaymentDTO.class
                );

        case PROCESSING ->
                throw new OperationInProgressException(
                        "The operation is already being processed"
                );

        case FAILED ->
                throw new PreviousOperationFailedException(
                        "The previous operation failed"
                );
    };
}
```

---

# 9. ¿Quién implementa cada parte?

## Cliente

El cliente debe:

* generar la clave;
* conservar la misma clave en los retries;
* enviarla en el header;
* no reutilizarla para otra operación.

```java
String key = UUID.randomUUID().toString();

restClient.post()
        .uri("/api/payments")
        .header("Idempotency-Key", key)
        .body(request)
        .retrieve()
        .body(PaymentDTO.class);
```

## Servidor

El servidor debe:

* leer la clave;
* reservarla de forma atómica;
* validar el request;
* evitar duplicados;
* guardar el resultado;
* devolver el resultado anterior cuando se repite.

```text
Cliente identifica la operación
Servidor garantiza que se ejecute una sola vez
```

Ambos lados participan, pero la garantía real está del lado del servidor.

---

# 10. Analogía sencilla

La `Idempotency-Key` puede compararse con el número de una transferencia bancaria.

El cliente dice:

```text
Transferencia número TX-123
```

Si el banco recibe dos veces:

```text
Procesar TX-123
```

no debería realizar dos transferencias.

Debe consultar:

```text
¿TX-123 ya fue procesada?
```

* Si no fue procesada, la ejecuta.
* Si ya fue procesada, devuelve el resultado anterior.
* Si está en proceso, informa que todavía está siendo procesada.
* Si llega con datos diferentes, rechaza la solicitud.

El número por sí solo no evita el duplicado. El banco debe **registrarlo y comprobarlo**.

---

# Idea central

El punto “Implementación del servidor” es necesario porque:

```text
Idempotency-Key
        │
        ▼
es solo un identificador enviado por el cliente
```

Para convertirlo en una garantía real:

```text
Servidor
   │
   ├── guarda la clave
   ├── impide duplicados
   ├── controla concurrencia
   ├── valida el request
   └── recupera la respuesta original
```

Sin esta implementación, el header es solamente texto y el `POST` sigue siendo no idempotente.
