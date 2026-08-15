# Tutorial de patrones de resiliencia

## Retry, Timeout, Circuit Breaker, Bulkhead y Rate Limiter con Java 21 + Spring Boot

En una arquitectura de microservicios, una llamada remota puede fallar por múltiples razones:

* el servicio está caído;
* la red está lenta;
* se agotó el pool de conexiones;
* el servicio responde con `503`;
* hay demasiadas solicitudes simultáneas;
* el consumidor supera la cuota permitida;
* la respuesta tarda demasiado.

Los patrones de resiliencia no evitan todos esos fallos. Su objetivo es impedir que un fallo local se convierta en un **fallo en cascada**.

Utilizaremos:

* Java 21;
* Spring Boot 3;
* `RestClient`;
* Resilience4j;
* Spring Boot Actuator;
* Micrometer.

Resilience4j ofrece módulos específicos para Circuit Breaker, Retry, Rate Limiter, Bulkhead y Time Limiter, además de integración con Spring Boot 3 mediante configuración y anotaciones. ([resilience4j][1])

---

# 1. Escenario del tutorial

Tenemos un microservicio de seguros:

```text
claims-service
```

que consume:

```text
customer-service
```

para obtener datos del asegurado.

```text
Cliente
   │
   ▼
claims-service
   │
   ▼
customer-service
```

El método principal será:

```java
public CustomerDTO findCustomer(Long customerId)
```

La llamada HTTP se realiza con `RestClient`.

```java
@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(RestClient customerRestClient) {
        this.restClient = customerRestClient;
    }

    public CustomerDTO findCustomer(Long customerId) {
        return restClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .body(CustomerDTO.class);
    }
}
```

DTO:

```java
public record CustomerDTO(
        Long id,
        String name,
        String email,
        CustomerStatus status
) {
}
```

---

# 2. ¿Qué problema resuelve cada patrón?

| Patrón              | Pregunta que responde                                 |
| ------------------- | ----------------------------------------------------- |
| **Timeout**         | ¿Cuánto tiempo estoy dispuesto a esperar?             |
| **Retry**           | ¿Conviene volver a intentarlo?                        |
| **Circuit Breaker** | ¿Debo dejar de llamar temporalmente?                  |
| **Bulkhead**        | ¿Cuánta concurrencia puede consumir esta integración? |
| **Rate Limiter**    | ¿Cuántas solicitudes permito por período?             |

No son equivalentes ni intercambiables.

```text
Timeout
    limita duración

Retry
    repite fallos transitorios

Circuit Breaker
    evita llamadas cuando el destino está degradado

Bulkhead
    aísla recursos y concurrencia

Rate Limiter
    limita frecuencia o volumen
```

---

# 3. Dependencias Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

El starter de Spring Boot proporciona aspectos AOP y permite configurar las instancias mediante `application.yml`. ([resilience4j][1])

Conviene administrar la versión con un BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-bom</artifactId>
            <version>${resilience4j.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

# 4. Timeout

## 4.1 Concepto

Un timeout define cuánto tiempo estamos dispuestos a esperar por una operación.

Sin timeout:

```text
Request
   │
   ▼
Servicio remoto lento
   │
   ▼
Thread bloqueado
   │
   ▼
Más requests
   │
   ▼
Más threads bloqueados
   │
   ▼
Agotamiento de recursos
```

Con timeout:

```text
Request
   │
   ▼
Servicio remoto tarda demasiado
   │
   ▼
Timeout
   │
   ▼
Error controlado o fallback
```

---

## 4.2 Tipos principales

### Connect timeout

Tiempo máximo para establecer la conexión TCP.

```text
Aplicación ───── conexión ───── Servicio remoto
```

Si el host no responde o la conexión no puede establecerse, se genera un error.

### Read timeout

Tiempo máximo esperando datos luego de establecer la conexión.

```text
Conexión establecida
       │
       ▼
Esperando respuesta
       │
       ▼
Read timeout
```

### Timeout total de operación

Límite global para toda la operación lógica.

Puede incluir:

* adquisición de conexión;
* conexión;
* envío;
* lectura;
* retries;
* procesamiento.

---

## 4.3 Timeout con `RestClient`

Para clientes síncronos como `RestClient`, es recomendable configurar los timeouts en el cliente HTTP subyacente. La documentación de Spring recomienda apoyarse en ese nivel porque proporciona mayor control sobre la operación de red. ([Home][2])

```java
@Configuration
public class CustomerClientConfiguration {

    @Bean
    RestClient customerRestClient(
            RestClient.Builder builder
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                Duration.ofSeconds(3)
        );

        return builder
                .baseUrl("https://customer-service")
                .requestFactory(requestFactory)
                .build();
    }
}
```

Aquí:

```text
connect timeout = 2 segundos
read timeout    = 3 segundos
```

---

## 4.4 Manejar el timeout

```java
public CustomerDTO findCustomer(Long customerId) {
    try {
        return restClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .body(CustomerDTO.class);

    } catch (ResourceAccessException ex) {
        throw new CustomerIntegrationException(
                "Timeout or connection error calling customer-service",
                ex
        );
    }
}
```

`ResourceAccessException` puede representar diferentes problemas de I/O, no solamente timeout. Por eso conviene conservar la causa original.

---

## 4.5 `TimeLimiter` de Resilience4j

Resilience4j también ofrece `TimeLimiter`, pero está orientado especialmente a operaciones asíncronas, como `CompletableFuture`, `CompletionStage`, `Mono` o `Flux`. ([resilience4j][1])

Ejemplo:

```java
@TimeLimiter(
        name = "customerService",
        fallbackMethod = "customerTimeoutFallback"
)
public CompletableFuture<CustomerDTO> findCustomerAsync(
        Long customerId
) {
    return CompletableFuture.supplyAsync(
            () -> customerClient.findCustomer(customerId)
    );
}
```

Configuración:

```yaml
resilience4j:
  timelimiter:
    instances:
      customerService:
        timeout-duration: 3s
        cancel-running-future: true
```

Fallback:

```java
private CompletableFuture<CustomerDTO> customerTimeoutFallback(
        Long customerId,
        TimeoutException exception
) {
    return CompletableFuture.failedFuture(
            new CustomerIntegrationException(
                    "Customer service exceeded the allowed time",
                    exception
            )
    );
}
```

Para `RestClient` síncrono, normalmente es más directo configurar correctamente los timeouts del cliente HTTP subyacente.

---

# 5. Retry

## 5.1 Concepto

Retry vuelve a ejecutar una operación después de un fallo.

```text
Intento 1
   │
   ├── 503
   ▼
Espera
   │
Intento 2
   │
   ├── timeout
   ▼
Espera
   │
Intento 3
   │
   └── 200 OK
```

Es útil cuando el fallo es probablemente transitorio.

---

## 5.2 Errores reintentables

Normalmente pueden considerarse:

* timeout;
* conexión rechazada temporalmente;
* `502 Bad Gateway`;
* `503 Service Unavailable`;
* `504 Gateway Timeout`;
* eventualmente `429 Too Many Requests`.

No suelen reintentarse:

* `400 Bad Request`;
* `401 Unauthorized`;
* `403 Forbidden`;
* `404 Not Found`;
* validaciones de negocio;
* datos inválidos;
* conflictos permanentes.

---

## 5.3 Configuración

```yaml
resilience4j:
  retry:
    instances:
      customerService:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException$BadGateway
          - org.springframework.web.client.HttpServerErrorException$ServiceUnavailable
          - org.springframework.web.client.HttpServerErrorException$GatewayTimeout
        ignore-exceptions:
          - com.example.customer.CustomerNotFoundException
```

Secuencia aproximada:

```text
Intento 1
   │
   └── falla
       espera 500 ms

Intento 2
   │
   └── falla
       espera 1000 ms

Intento 3
   │
   └── resultado final
```

---

## 5.4 Anotación

```java
@Retry(
        name = "customerService",
        fallbackMethod = "customerRetryFallback"
)
public CustomerDTO findCustomer(Long customerId) {

    return restClient.get()
            .uri("/api/customers/{id}", customerId)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Fallback:

```java
private CustomerDTO customerRetryFallback(
        Long customerId,
        Throwable exception
) {
    throw new CustomerIntegrationException(
            "Customer service failed after all retry attempts",
            exception
    );
}
```

La firma del fallback debe contener los argumentos originales y, como último argumento, una excepción compatible.

---

## 5.5 Retry e idempotencia

Retry es relativamente seguro para operaciones idempotentes:

```http
GET /customers/100
PUT /customers/100
DELETE /customers/100
```

Pero puede ser peligroso para:

```http
POST /payments
POST /transfers
POST /orders
```

Supongamos:

```text
POST /payments
       │
       ▼
Servidor crea el pago
       │
       ▼
La respuesta se pierde
       │
       ▼
Cliente recibe timeout
       │
       ▼
Retry
       │
       ▼
Segundo pago
```

Para proteger una operación no idempotente:

```java
String idempotencyKey = UUID.randomUUID().toString();

return restClient.post()
        .uri("/api/payments")
        .header("Idempotency-Key", idempotencyKey)
        .body(request)
        .retrieve()
        .body(PaymentDTO.class);
```

La misma clave debe conservarse en todos los intentos de la misma operación lógica.

---

## 5.6 Evitar el retry storm

Supongamos:

```text
1000 requests
×
3 intentos
=
3000 llamadas
```

Si el servicio remoto ya está saturado, los retries pueden empeorar el incidente.

Por eso conviene utilizar:

* pocos intentos;
* backoff;
* jitter;
* circuit breaker;
* métricas;
* retries solamente para errores seleccionados.

---

# 6. Circuit Breaker

## 6.1 Concepto

Circuit Breaker observa el resultado de las llamadas.

Cuando la tasa de fallos supera un umbral, deja de llamar al servicio durante un período.

```text
CLOSED
   │
   ├── llamadas permitidas
   │
   └── demasiados errores
            │
            ▼
           OPEN
            │
            ├── llamadas rechazadas inmediatamente
            │
            └── transcurre el tiempo de espera
                         │
                         ▼
                     HALF_OPEN
                         │
                  llamadas de prueba
                    │           │
                 éxito        fallo
                    │           │
                    ▼           ▼
                 CLOSED        OPEN
```

---

## 6.2 Estados

### CLOSED

Las llamadas pasan normalmente.

El circuit breaker recopila estadísticas:

* cantidad de llamadas;
* fallos;
* llamadas lentas;
* tasa de fallos.

### OPEN

Las llamadas no llegan al servicio remoto.

Fallan rápidamente con:

```java
CallNotPermittedException
```

### HALF_OPEN

Se permiten algunas llamadas de prueba.

Si funcionan, vuelve a `CLOSED`. Si fallan, regresa a `OPEN`.

---

## 6.3 Configuración

```yaml
resilience4j:
  circuitbreaker:
    instances:
      customerService:
        register-health-indicator: true
        sliding-window-type: count_based
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
```

Interpretación:

```text
sliding-window-size: 20
```

Evalúa las últimas 20 llamadas.

```text
minimum-number-of-calls: 10
```

No calcula la tasa hasta tener al menos 10 muestras.

```text
failure-rate-threshold: 50
```

Se abre si falla al menos el 50 %.

```text
wait-duration-in-open-state: 10s
```

Permanece abierto durante 10 segundos.

```text
permitted-number-of-calls-in-half-open-state: 3
```

Permite tres llamadas de prueba.

---

## 6.4 Código

```java
@CircuitBreaker(
        name = "customerService",
        fallbackMethod = "customerCircuitBreakerFallback"
)
public CustomerDTO findCustomer(Long customerId) {

    return restClient.get()
            .uri("/api/customers/{id}", customerId)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Fallback:

```java
private CustomerDTO customerCircuitBreakerFallback(
        Long customerId,
        Throwable exception
) {
    if (exception instanceof CallNotPermittedException) {
        throw new CustomerIntegrationException(
                "Customer service circuit breaker is open",
                exception
        );
    }

    throw new CustomerIntegrationException(
            "Customer service call failed",
            exception
    );
}
```

---

## 6.5 Qué errores deben contar

No todos los errores deberían considerarse fallos del servicio remoto.

Por ejemplo:

```text
404 Customer not found
```

puede ser una respuesta funcional válida.

Configuración:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      customerService:
        record-exceptions:
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException
        ignore-exceptions:
          - com.example.customer.CustomerNotFoundException
```

Así, un cliente inexistente no abre el circuito.

---

## 6.6 Fallback: degradación controlada

Un fallback puede:

* devolver datos de cache;
* devolver información parcial;
* utilizar un proveedor alternativo;
* publicar un mensaje para procesamiento posterior;
* devolver un error controlado.

Ejemplo con cache:

```java
private CustomerDTO customerFallback(
        Long customerId,
        Throwable exception
) {
    return customerCache.findById(customerId)
            .orElseThrow(() ->
                    new CustomerIntegrationException(
                            "Customer service unavailable and no cached value exists",
                            exception
                    )
            );
}
```

No siempre es correcto devolver un DTO inventado:

```java
return new CustomerDTO(
        customerId,
        "Unknown",
        null,
        CustomerStatus.ACTIVE
);
```

En sistemas bancarios o de seguros, esa respuesta podría producir una decisión de negocio incorrecta.

---

# 7. Bulkhead

## 7.1 Concepto

Bulkhead significa compartimiento estanco.

El nombre proviene de los barcos: si un compartimiento se inunda, el resto del barco continúa operativo.

En software, limita cuántos recursos puede consumir una integración.

Sin Bulkhead:

```text
customer-service lento
       │
       ▼
Todas las requests ocupan threads
       │
       ▼
Se agota el pool
       │
       ▼
claims-service deja de responder
       │
       ▼
Incluso endpoints no relacionados fallan
```

Con Bulkhead:

```text
customer-service
       │
       ▼
Máximo 20 llamadas concurrentes
       │
       ├── llamadas 1..20 pasan
       │
       └── llamada 21 se rechaza o espera
```

---

## 7.2 Bulkhead por semáforo

Limita la cantidad de ejecuciones concurrentes.

```yaml
resilience4j:
  bulkhead:
    instances:
      customerService:
        max-concurrent-calls: 20
        max-wait-duration: 100ms
```

Código:

```java
@Bulkhead(
        name = "customerService",
        type = Bulkhead.Type.SEMAPHORE,
        fallbackMethod = "customerBulkheadFallback"
)
public CustomerDTO findCustomer(Long customerId) {

    return restClient.get()
            .uri("/api/customers/{id}", customerId)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Fallback:

```java
private CustomerDTO customerBulkheadFallback(
        Long customerId,
        BulkheadFullException exception
) {
    throw new CustomerIntegrationException(
            "Customer integration concurrency limit reached",
            exception
    );
}
```

Resilience4j admite Bulkhead basado en semáforo y Thread Pool Bulkhead; el tipo por defecto en la anotación es el basado en semáforo. ([resilience4j][1])

---

## 7.3 Thread Pool Bulkhead

Aísla la operación en un pool de threads independiente.

```yaml
resilience4j:
  thread-pool-bulkhead:
    instances:
      customerService:
        core-thread-pool-size: 10
        max-thread-pool-size: 20
        queue-capacity: 30
        keep-alive-duration: 20s
```

Conceptualmente:

```text
Requests de la aplicación
          │
          ▼
Pool exclusivo customer-service
          │
          ├── 20 threads máximo
          └── cola de 30 solicitudes
```

Generalmente requiere un retorno asíncrono:

```java
@Bulkhead(
        name = "customerService",
        type = Bulkhead.Type.THREADPOOL
)
public CompletableFuture<CustomerDTO> findCustomerAsync(
        Long customerId
) {
    return CompletableFuture.supplyAsync(
            () -> customerClient.findCustomer(customerId)
    );
}
```

---

## 7.4 Semáforo frente a Thread Pool

| Variante             | Característica                            |
| -------------------- | ----------------------------------------- |
| Semaphore Bulkhead   | Limita concurrencia en el thread llamador |
| Thread Pool Bulkhead | Ejecuta en un pool separado               |
| Semaphore            | Menor complejidad y overhead              |
| Thread Pool          | Mayor aislamiento de threads              |
| Semaphore            | Adecuado para métodos síncronos           |
| Thread Pool          | Adecuado para flujos asíncronos           |

Para un `RestClient` síncrono, el Bulkhead por semáforo suele ser más sencillo.

---

## 7.5 Bulkhead y Java 21

Los virtual threads reducen el costo de tener muchas operaciones bloqueantes, pero no eliminan la necesidad de un Bulkhead.

Sin límite, todavía puede agotarse:

* el pool de conexiones HTTP;
* la capacidad del servicio remoto;
* memoria;
* sockets;
* CPU;
* conexiones a base de datos;
* cuotas externas.

```text
Virtual threads
    reducen costo de threads bloqueados

Bulkhead
    limita consumo total de la dependencia
```

Son mecanismos complementarios.

---

# 8. Rate Limiter

## 8.1 Concepto

Rate Limiter controla cuántas llamadas pueden realizarse dentro de un período.

Ejemplo:

```text
100 llamadas por segundo
```

```text
Segundo 1: 100 permitidas
Segundo 1: llamada 101 rechazada o espera
Segundo 2: se renuevan los permisos
```

Resilience4j proporciona un registro en memoria para administrar instancias de Rate Limiter y permite configurar el período, la cantidad de permisos y cuánto esperar para obtener uno. ([resilience4j][3])

---

## 8.2 Diferencia con Bulkhead

| Bulkhead                          | Rate Limiter                        |
| --------------------------------- | ----------------------------------- |
| Limita concurrencia simultánea    | Limita frecuencia por período       |
| Ejemplo: 20 llamadas en ejecución | Ejemplo: 100 llamadas por segundo   |
| Protege recursos concurrentes     | Protege cuotas y capacidad temporal |

Ejemplo:

```text
Rate Limiter:
máximo 100 llamadas/segundo

Bulkhead:
máximo 20 llamadas simultáneas
```

Ambos pueden utilizarse juntos.

---

## 8.3 Configuración

```yaml
resilience4j:
  ratelimiter:
    instances:
      customerService:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 50ms
```

Significa:

```text
100 permisos
cada 1 segundo
espera máxima por permiso: 50 ms
```

---

## 8.4 Código

```java
@RateLimiter(
        name = "customerService",
        fallbackMethod = "customerRateLimitFallback"
)
public CustomerDTO findCustomer(Long customerId) {

    return restClient.get()
            .uri("/api/customers/{id}", customerId)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Fallback:

```java
private CustomerDTO customerRateLimitFallback(
        Long customerId,
        RequestNotPermitted exception
) {
    throw new CustomerIntegrationException(
            "Customer service rate limit exceeded",
            exception
    );
}
```

---

## 8.5 Rate Limiter de entrada y salida

### Entrada

Protege nuestro microservicio:

```text
Clientes
   │
   ▼
Rate Limiter
   │
   ▼
Nuestro endpoint
```

### Salida

Protege una dependencia o respeta su cuota:

```text
Nuestro microservicio
   │
   ▼
Rate Limiter
   │
   ▼
API externa
```

El Rate Limiter de Resilience4j aplicado al método cliente es principalmente un límite de salida local a la instancia.

En Kubernetes, si existen cinco réplicas y cada una permite 100 solicitudes por segundo:

```text
5 instancias × 100 = hasta 500 solicitudes/segundo
```

Por eso un Rate Limiter local no representa necesariamente un límite distribuido global. Para un límite global pueden necesitarse:

* API Gateway;
* Redis;
* Bucket4j distribuido;
* Envoy;
* NGINX;
* políticas del proveedor cloud.

---

# 9. Combinar los patrones

Aquí surge una pregunta importante:

> ¿En qué orden deben aplicarse?

Una secuencia conceptual razonable es:

```text
Request
   │
   ▼
Rate Limiter
   │
   ▼
Bulkhead
   │
   ▼
Circuit Breaker
   │
   ▼
Retry
   │
   ▼
Timeout / llamada HTTP
   │
   ▼
Servicio remoto
```

Pero el orden exacto depende de los objetivos del sistema.

---

## 9.1 Ejemplo con anotaciones

```java
@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(RestClient customerRestClient) {
        this.restClient = customerRestClient;
    }

    @RateLimiter(
            name = "customerService",
            fallbackMethod = "fallback"
    )
    @Bulkhead(
            name = "customerService",
            type = Bulkhead.Type.SEMAPHORE,
            fallbackMethod = "fallback"
    )
    @CircuitBreaker(
            name = "customerService",
            fallbackMethod = "fallback"
    )
    @Retry(
            name = "customerService",
            fallbackMethod = "fallback"
    )
    public CustomerDTO findCustomer(Long customerId) {

        return restClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new CustomerNotFoundException(
                                    customerId
                            );
                        }
                )
                .body(CustomerDTO.class);
    }

    private CustomerDTO fallback(
            Long customerId,
            Throwable exception
    ) {
        throw new CustomerIntegrationException(
                "Customer integration failed for id "
                        + customerId,
                exception
        );
    }
}
```

Resilience4j permite aplicar estas anotaciones tanto a tipos síncronos como a tipos asíncronos y reactivos, siempre que se incluyan los módulos correspondientes. ([resilience4j][1])

---

# 10. Orden de los aspectos

Con anotaciones, Resilience4j establece un orden de aspectos. También puede configurarse la prioridad.

Sin embargo, antes de modificar el orden conviene entender el efecto.

## Retry por fuera del Circuit Breaker

```text
Retry
  └── Circuit Breaker
          └── llamada
```

Cada intento atraviesa el Circuit Breaker.

El breaker puede registrar cada intento individual.

## Circuit Breaker por fuera del Retry

```text
Circuit Breaker
  └── Retry
          └── llamada
```

El Circuit Breaker puede observar el resultado final de toda la operación con retries.

Esto cambia las métricas y la velocidad con que se abre el circuito.

---

## 10.1 Configuración explícita de orden

```yaml
resilience4j:
  retry:
    retry-aspect-order: 4

  circuitbreaker:
    circuit-breaker-aspect-order: 3

  ratelimiter:
    rate-limiter-aspect-order: 2

  bulkhead:
    bulkhead-aspect-order: 1
```

Los valores exactos no deben copiarse sin analizar su efecto. Es preferible validar el orden con pruebas de integración y métricas.

---

# 11. Configuración completa

```yaml
resilience4j:

  retry:
    instances:
      customerService:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException$BadGateway
          - org.springframework.web.client.HttpServerErrorException$ServiceUnavailable
          - org.springframework.web.client.HttpServerErrorException$GatewayTimeout
        ignore-exceptions:
          - com.example.customer.CustomerNotFoundException

  circuitbreaker:
    instances:
      customerService:
        register-health-indicator: true
        sliding-window-type: count_based
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException
        ignore-exceptions:
          - com.example.customer.CustomerNotFoundException

  bulkhead:
    instances:
      customerService:
        max-concurrent-calls: 20
        max-wait-duration: 100ms

  ratelimiter:
    instances:
      customerService:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 50ms

  timelimiter:
    instances:
      customerService:
        timeout-duration: 3s
        cancel-running-future: true
```

Resilience4j permite definir instancias independientes para cada dependencia remota mediante configuración de Spring Boot. ([resilience4j][1])

---

# 12. No reutilizar una sola configuración para todo

No es recomendable crear una única configuración:

```text
defaultResiliencePolicy
```

y aplicarla sin análisis a todos los servicios.

Ejemplo:

| Dependencia        | Características                      |
| ------------------ | ------------------------------------ |
| `customer-service` | Rápido, interno, crítico             |
| `ai-service`       | Lento, fallback permitido            |
| `payment-provider` | Externo, operaciones no idempotentes |
| `country-catalog`  | Muy cacheable                        |
| `document-service` | Archivos grandes                     |

Cada una necesita parámetros distintos.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      customerService:
        failure-rate-threshold: 40

      aiService:
        failure-rate-threshold: 60

      paymentService:
        failure-rate-threshold: 20
```

---

# 13. Flujo completo ante un fallo

Supongamos:

```text
customer-service responde lentamente
```

El flujo podría ser:

```text
1. Rate Limiter
   Hay permiso disponible.

2. Bulkhead
   Hay capacidad concurrente.

3. Circuit Breaker
   Está CLOSED, permite la llamada.

4. Retry - intento 1
   Read timeout.

5. Retry espera 500 ms.

6. Retry - intento 2
   HTTP 503.

7. Retry espera 1000 ms.

8. Retry - intento 3
   Read timeout.

9. Circuit Breaker registra fallo.

10. Se ejecuta fallback.

11. Se devuelve error controlado o cache.

12. Micrometer registra métricas.
```

Después de suficientes fallos:

```text
Circuit Breaker = OPEN
```

Las próximas solicitudes:

```text
Rate Limiter
   │
Bulkhead
   │
Circuit Breaker OPEN
   │
CallNotPermittedException
   │
Fallback inmediato
```

No llegan al servicio remoto.

---

# 14. Observabilidad con Actuator

Configuración:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - metrics
          - prometheus

  endpoint:
    health:
      show-details: always

  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```

Resilience4j integra métricas y health indicators con Spring Boot Actuator. ([resilience4j][1])

Endpoints:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

---

## 14.1 Métricas importantes

### Circuit Breaker

```text
resilience4j.circuitbreaker.calls
resilience4j.circuitbreaker.state
resilience4j.circuitbreaker.failure.rate
resilience4j.circuitbreaker.slow.call.rate
```

### Retry

```text
resilience4j.retry.calls
```

Puede distinguir:

* successful without retry;
* successful with retry;
* failed without retry;
* failed with retry.

### Bulkhead

```text
resilience4j.bulkhead.available.concurrent.calls
resilience4j.bulkhead.max.allowed.concurrent.calls
```

### Rate Limiter

```text
resilience4j.ratelimiter.available.permissions
resilience4j.ratelimiter.waiting.threads
```

### Time Limiter

```text
resilience4j.timelimiter.calls
```

---

# 15. Errores especiales de cada patrón

| Patrón                  | Excepción habitual              |
| ----------------------- | ------------------------------- |
| Timeout HTTP            | `ResourceAccessException`       |
| TimeLimiter             | `TimeoutException`              |
| Circuit Breaker abierto | `CallNotPermittedException`     |
| Bulkhead lleno          | `BulkheadFullException`         |
| Rate Limit excedido     | `RequestNotPermitted`           |
| Retry agotado           | Normalmente la última excepción |

Puede crearse un traductor central:

```java
public RuntimeException translate(
        Throwable exception
) {
    return switch (exception) {

        case CallNotPermittedException ex ->
                new CustomerIntegrationException(
                        "Circuit breaker is open",
                        ex
                );

        case BulkheadFullException ex ->
                new CustomerIntegrationException(
                        "Customer integration is saturated",
                        ex
                );

        case RequestNotPermitted ex ->
                new CustomerIntegrationException(
                        "Customer integration rate exceeded",
                        ex
                );

        case ResourceAccessException ex ->
                new CustomerIntegrationException(
                        "Customer integration timeout",
                        ex
                );

        default ->
                new CustomerIntegrationException(
                        "Unexpected integration error",
                        exception
                );
    };
}
```

El `switch` con pattern matching está disponible en Java moderno y permite expresar claramente este tipo de clasificación.

---

# 16. Responder correctamente desde la API

Un `@RestControllerAdvice` puede traducir excepciones internas.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            CustomerNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Customer not found");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    @ExceptionHandler(CustomerIntegrationException.class)
    public ResponseEntity<ProblemDetail> handleIntegration(
            CustomerIntegrationException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        exception.getMessage()
                );

        problem.setTitle(
                "Dependent service unavailable"
        );

        return ResponseEntity.status(
                        HttpStatus.SERVICE_UNAVAILABLE
                )
                .header(HttpHeaders.RETRY_AFTER, "10")
                .body(problem);
    }
}
```

Respuesta:

```http
HTTP/1.1 503 Service Unavailable
Retry-After: 10
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Dependent service unavailable",
  "status": 503,
  "detail": "Customer service circuit breaker is open"
}
```

---

# 17. Cómo elegir valores iniciales

No existe una configuración universal.

Los valores deben surgir de:

* SLA y SLO;
* latencia histórica;
* percentiles `p95`, `p99`;
* capacidad del servicio remoto;
* pool de conexiones;
* carga esperada;
* costo de cada operación;
* criticidad del negocio;
* pruebas de carga;
* métricas de producción.

---

## 17.1 Timeout

Una aproximación inicial:

```text
timeout > latencia normal p99
timeout < tiempo máximo tolerado por el usuario
```

Si el servicio normalmente tarda:

```text
p50 = 100 ms
p95 = 300 ms
p99 = 800 ms
```

un timeout de 1 o 1,5 segundos podría ser un punto inicial razonable, dependiendo del flujo completo.

No debería asignarse un timeout de 30 segundos únicamente “para evitar errores”; eso suele transformar errores rápidos en saturación lenta.

---

## 17.2 Retry

Punto inicial frecuente:

```text
2 o 3 intentos totales
```

No:

```text
10 intentos sin espera
```

Debe considerarse el presupuesto total:

```text
3 intentos × timeout de 3 s
+ esperas de backoff
=
más de 10 segundos
```

El timeout de una llamada y el timeout total del caso de uso no son lo mismo.

---

## 17.3 Circuit Breaker

Un punto inicial podría ser:

```text
minimum-number-of-calls: 10 o 20
failure-rate-threshold: 40–60 %
wait-duration-in-open-state: 10–30 s
```

Pero debe ajustarse con mediciones.

Un `minimum-number-of-calls` demasiado bajo puede abrir el circuito por dos fallos aislados.

Uno demasiado alto puede tardar demasiado en reaccionar.

---

## 17.4 Bulkhead

Puede calcularse considerando:

```text
capacidad de conexiones
capacidad del servicio remoto
latencia promedio
tráfico máximo
```

Ejemplo:

```text
Pool HTTP: 50 conexiones
customer-service no debería usar más de 20
```

Entonces:

```yaml
max-concurrent-calls: 20
```

El resto queda disponible para otras integraciones.

---

## 17.5 Rate Limiter

Debe basarse en:

* cuota contractual;
* capacidad del proveedor;
* límite del API Gateway;
* tráfico legítimo esperado.

Si el proveedor permite:

```text
6000 requests/minuto
```

puede establecerse:

```yaml
limit-for-period: 100
limit-refresh-period: 1s
```

dejando un margen operativo cuando sea necesario.

---

# 18. Anti-patrones

## Retry para cualquier excepción

```yaml
retry-exceptions:
  - java.lang.Exception
```

Puede reintentar errores permanentes y duplicar operaciones.

---

## Circuit Breaker como reemplazo del timeout

Un circuit breaker no interrumpe automáticamente una llamada lenta individual.

```text
Circuit Breaker ≠ Timeout
```

Se necesitan ambos.

---

## Bulkhead con cola infinita

Una cola enorme no resuelve la saturación; solamente la oculta y aumenta la latencia.

---

## Fallback con datos falsos

```java
return CustomerDTO.empty();
```

Puede ocultar una falla crítica.

---

## Rate Limiter local interpretado como global

Con varias réplicas, cada instancia mantiene su propio límite local.

---

## Retry sin idempotencia

Especialmente peligroso en:

* pagos;
* transferencias;
* alta de pólizas;
* creación de siniestros;
* emisión de documentos;
* movimientos contables.

---

## Configurar y olvidar

Los patrones deben observarse y ajustarse. Una configuración sin métricas puede empeorar el comportamiento del sistema.

---

# 19. Diseño recomendado por dependencia

```text
CustomerClient
    │
    ├── HTTP timeouts
    ├── Retry selectivo
    ├── Circuit Breaker
    ├── Bulkhead propio
    ├── Rate Limiter propio
    ├── métricas
    └── fallback específico

PaymentClient
    │
    ├── HTTP timeouts más estrictos
    ├── Retry solo con Idempotency-Key
    ├── Circuit Breaker sensible
    ├── Bulkhead independiente
    └── sin fallback que invente pagos

AiClient
    │
    ├── timeout más amplio
    ├── Circuit Breaker
    ├── Bulkhead pequeño
    ├── fallback permitido
    └── cache de resultados
```

El aislamiento debe ser por dependencia:

```text
customerService
paymentService
aiService
documentService
```

No una única instancia de Circuit Breaker o Bulkhead para todas.

---

# 20. Resumen final

```text
Petición
   │
   ▼
Rate Limiter
   │
   ├── controla solicitudes por período
   ▼
Bulkhead
   │
   ├── limita concurrencia
   ▼
Circuit Breaker
   │
   ├── evita llamar a servicios degradados
   ▼
Retry
   │
   ├── repite errores transitorios
   ▼
Timeout
   │
   ├── limita cuánto esperar
   ▼
Servicio remoto
```

| Patrón          | Protege contra                     | No resuelve                        |
| --------------- | ---------------------------------- | ---------------------------------- |
| Timeout         | Esperas excesivas                  | Saturación global                  |
| Retry           | Fallos transitorios                | Fallos permanentes                 |
| Circuit Breaker | Fallos repetidos y cascadas        | Duración de una llamada individual |
| Bulkhead        | Agotamiento compartido de recursos | Cuota temporal                     |
| Rate Limiter    | Exceso de frecuencia               | Llamadas simultáneas lentas        |

La combinación correcta no consiste en colocar todas las anotaciones con valores arbitrarios. Consiste en definir una **política de resiliencia específica para cada dependencia**, basada en latencia, capacidad, idempotencia, criticidad del negocio y métricas reales.

[1]: https://resilience4j.readme.io/v2.0.0/docs/getting-started-3?utm_source=chatgpt.com "Getting Started"
[2]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com "REST Clients :: Spring Framework"
[3]: https://resilience4j.readme.io/docs/ratelimiter?utm_source=chatgpt.com "RateLimiter"
