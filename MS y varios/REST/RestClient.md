# Tutorial de `RestClient` con Java 21 y Spring Boot

`RestClient` es el cliente HTTP **síncrono e imperativo** moderno de Spring. Permite consumir APIs REST con una API fluida, convertir JSON a objetos Java, enviar headers y cuerpos, y manejar códigos de estado sin utilizar el estilo más verboso de `RestTemplate`. Fue incorporado en Spring Framework 6.1; Spring Boot proporciona además un `RestClient.Builder` preconfigurado para integrarlo con la aplicación. ([Home][1])

Su modelo de ejecución es bloqueante:

```text
Thread de la aplicación
        │
        ├── envía request HTTP
        │
        ├── espera la respuesta
        │
        ├── recibe y deserializa el body
        │
        └── continúa ejecutándose
```

Esto lo hace especialmente adecuado para aplicaciones Spring MVC tradicionales, procesos batch, integraciones empresariales y microservicios cuya concurrencia no justifica adoptar programación reactiva.

---

## 1. `RestClient` frente a otras alternativas

| Cliente        | Modelo                   | Uso principal                           |
| -------------- | ------------------------ | --------------------------------------- |
| `RestTemplate` | Síncrono                 | Aplicaciones existentes y código legacy |
| `RestClient`   | Síncrono                 | Nuevo código imperativo                 |
| `WebClient`    | Reactivo y no bloqueante | Alta concurrencia, streaming y WebFlux  |

La diferencia fundamental entre `RestClient` y `WebClient` no es solamente la sintaxis.

```java
// RestClient: devuelve el resultado directamente
CustomerDTO customer = restClient.get()
        .uri("/customers/{id}", id)
        .retrieve()
        .body(CustomerDTO.class);
```

```java
// WebClient: devuelve una representación reactiva
Mono<CustomerDTO> customer = webClient.get()
        .uri("/customers/{id}", id)
        .retrieve()
        .bodyToMono(CustomerDTO.class);
```

Con `RestClient`, el thread queda esperando hasta recibir la respuesta. Con `WebClient`, la operación puede continuar sin bloquear el thread mientras llega la respuesta.

---

# 2. Proyecto base

Supongamos que nuestra aplicación consume esta API:

```text
GET    /api/customers/{id}
GET    /api/customers
POST   /api/customers
PUT    /api/customers/{id}
PATCH  /api/customers/{id}
DELETE /api/customers/{id}
```

## Dependencia Maven

Para una aplicación Spring MVC:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

No es necesario agregar una dependencia específica llamada `rest-client`. `RestClient` forma parte de Spring Web.

Para validaciones:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

# 3. DTOs del ejemplo

Con Java 21 podemos utilizar `record` para representar DTOs inmutables.

```java
package com.example.customer.client.dto;

public record CustomerDTO(
        Long id,
        String name,
        String email,
        CustomerStatus status
) {
}
```

```java
package com.example.customer.client.dto;

public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED
}
```

DTO para crear un cliente:

```java
package com.example.customer.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email
) {
}
```

DTO para actualizar parcialmente:

```java
package com.example.customer.client.dto;

public record UpdateCustomerRequest(
        String name,
        String email,
        CustomerStatus status
) {
}
```

---

# 4. Formas de crear un `RestClient`

Spring ofrece varias formas.

## Creación directa

```java
RestClient restClient = RestClient.create();
```

## Con URL base

```java
RestClient restClient =
        RestClient.create("https://api.example.com");
```

## Mediante builder

```java
RestClient restClient = RestClient.builder()
        .baseUrl("https://api.example.com")
        .build();
```

La documentación también permite construirlo a partir de la configuración de un `RestTemplate`, algo útil en migraciones progresivas. ([Home][2])

En Spring Boot conviene inyectar el `RestClient.Builder` que el framework preconfigura, en lugar de llamar directamente a `RestClient.create()`. ([Home][3])

---

# 5. Configuración recomendada

## `application.yml`

```yaml
clients:
  customer:
    base-url: https://customer-api.example.com
    api-key: ${CUSTOMER_API_KEY}
    connect-timeout: 2s
    read-timeout: 5s
```

## Propiedades tipadas

```java
package com.example.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "clients.customer")
public record CustomerClientProperties(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
```

Activamos la clase:

```java
package com.example.customer;

import com.example.customer.config.CustomerClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CustomerClientProperties.class)
public class CustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}
```

---

# 6. Configurar timeouts

Un cliente HTTP sin timeouts puede dejar threads esperando indefinidamente o durante demasiado tiempo.

Con el cliente HTTP del JDK:

```java
package com.example.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerClientProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-Key", properties.apiKey())
                .build();
    }
}
```

La configuración distingue dos límites:

| Timeout         | Qué controla                                  |
| --------------- | --------------------------------------------- |
| Connect timeout | Tiempo máximo para establecer la conexión     |
| Read timeout    | Tiempo máximo esperando datos de la respuesta |

Spring abstrae distintas implementaciones HTTP subyacentes, como el cliente HTTP del JDK o Apache HttpComponents. ([Home][2])

---

# 7. Anatomía de una llamada

Una operación típica tiene este flujo:

```java
CustomerDTO customer = restClient
        .get()                             // 1. Método HTTP
        .uri("/api/customers/{id}", id)   // 2. URI
        .header("X-Correlation-Id", id)   // 3. Headers
        .retrieve()                       // 4. Ejecutar y procesar
        .body(CustomerDTO.class);         // 5. Deserializar body
```

Conceptualmente:

```text
RestClient
   │
   ├── método HTTP
   ├── URI y parámetros
   ├── headers
   ├── body opcional
   ├── ejecución
   ├── validación del status
   └── conversión de respuesta
```

---

# 8. Consumir un `GET`

## Obtener un recurso

```java
public CustomerDTO findById(Long id) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Request generado:

```http
GET /api/customers/100 HTTP/1.1
Host: customer-api.example.com
Accept: application/json
X-API-Key: ...
```

Respuesta esperada:

```json
{
  "id": 100,
  "name": "Laura Gómez",
  "email": "laura@example.com",
  "status": "ACTIVE"
}
```

Jackson convierte automáticamente el JSON a `CustomerDTO`.

---

# 9. Query parameters

Supongamos:

```http
GET /api/customers?status=ACTIVE&page=0&size=20
```

Puede construirse así:

```java
public CustomerPageDTO findAll(
        CustomerStatus status,
        int page,
        int size
) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/api/customers")
                    .queryParam("status", status)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build())
            .retrieve()
            .body(CustomerPageDTO.class);
}
```

DTO paginado:

```java
public record CustomerPageDTO(
        List<CustomerDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
```

El `uriBuilder` es preferible a concatenar strings:

```java
// Evitar
String uri = "/api/customers?status=" + status
        + "&page=" + page
        + "&size=" + size;
```

El builder se ocupa correctamente del escape y codificación de los parámetros.

---

# 10. Obtener una lista

Este código parece natural:

```java
List<CustomerDTO> customers = restClient.get()
        .uri("/api/customers")
        .retrieve()
        .body(List.class);
```

Pero pierde la información genérica. En la práctica produciría un `List<LinkedHashMap>`.

Debe utilizarse `ParameterizedTypeReference`:

```java
import org.springframework.core.ParameterizedTypeReference;

public List<CustomerDTO> findAll() {

    return restClient.get()
            .uri("/api/customers")
            .retrieve()
            .body(new ParameterizedTypeReference<List<CustomerDTO>>() {
            });
}
```

También puede escribirse con clase anónima y operador diamante:

```java
.body(new ParameterizedTypeReference<>() {});
```

El tipo del método permite inferir `List<CustomerDTO>`.

---

# 11. Crear recursos con `POST`

```java
public CustomerDTO create(CreateCustomerRequest request) {

    return restClient.post()
            .uri("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Request:

```http
POST /api/customers HTTP/1.1
Content-Type: application/json
Accept: application/json

{
  "name": "Laura Gómez",
  "email": "laura@example.com"
}
```

Respuesta:

```http
HTTP/1.1 201 Created
Location: /api/customers/100
Content-Type: application/json
```

```json
{
  "id": 100,
  "name": "Laura Gómez",
  "email": "laura@example.com",
  "status": "ACTIVE"
}
```

---

# 12. Obtener headers y status de la respuesta

Cuando no alcanza con extraer el body, puede obtenerse un `ResponseEntity`.

```java
public ResponseEntity<CustomerDTO> create(
        CreateCustomerRequest request
) {
    return restClient.post()
            .uri("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(CustomerDTO.class);
}
```

Después:

```java
ResponseEntity<CustomerDTO> response = client.create(request);

HttpStatusCode status = response.getStatusCode();
HttpHeaders headers = response.getHeaders();
CustomerDTO customer = response.getBody();

URI location = headers.getLocation();
```

Esto es útil cuando importan:

* `Location`
* `ETag`
* `Cache-Control`
* `Retry-After`
* headers de paginación
* código HTTP exacto

---

# 13. Actualización completa con `PUT`

```java
public CustomerDTO replace(
        Long id,
        CreateCustomerRequest request
) {
    return restClient.put()
            .uri("/api/customers/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(CustomerDTO.class);
}
```

`PUT` suele representar el reemplazo completo del recurso y es idempotente.

---

# 14. Actualización parcial con `PATCH`

```java
public CustomerDTO update(
        Long id,
        UpdateCustomerRequest request
) {
    return restClient.patch()
            .uri("/api/customers/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Ejemplo:

```java
UpdateCustomerRequest request =
        new UpdateCustomerRequest(
                null,
                "new-email@example.com",
                CustomerStatus.ACTIVE
        );
```

El servidor debe definir con claridad qué significa un atributo `null`: omitir el cambio, borrar el valor o rechazarlo.

---

# 15. Eliminar con `DELETE`

Cuando la respuesta no contiene body:

```java
public void delete(Long id) {

    restClient.delete()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .toBodilessEntity();
}
```

Respuesta típica:

```http
HTTP/1.1 204 No Content
```

También puede devolverse el `ResponseEntity<Void>`:

```java
public ResponseEntity<Void> delete(Long id) {

    return restClient.delete()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .toBodilessEntity();
}
```

---

# 16. Headers por request

## Bearer token

```java
public CustomerDTO findById(Long id, String token) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .headers(headers ->
                    headers.setBearerAuth(token))
            .retrieve()
            .body(CustomerDTO.class);
}
```

## Correlation ID

```java
public CustomerDTO findById(
        Long id,
        String correlationId
) {
    return restClient.get()
            .uri("/api/customers/{id}", id)
            .header("X-Correlation-Id", correlationId)
            .retrieve()
            .body(CustomerDTO.class);
}
```

## Varios headers

```java
return restClient.get()
        .uri("/api/customers/{id}", id)
        .headers(headers -> {
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-Correlation-Id", correlationId);
            headers.set("X-Channel", "BACKOFFICE");
        })
        .retrieve()
        .body(CustomerDTO.class);
```

---

# 17. Headers globales

Los headers que deben enviarse en todas las llamadas pueden configurarse en el builder:

```java
RestClient client = builder
        .baseUrl(properties.baseUrl())
        .defaultHeader(
                HttpHeaders.ACCEPT,
                MediaType.APPLICATION_JSON_VALUE
        )
        .defaultHeader("X-Application-Name", "claims-service")
        .build();
```

Un header por request puede reemplazar o complementar los valores globales según cómo se configure. `RestClient.Builder` admite headers, cookies y personalización global de cada request. ([Home][4])

---

# 18. Manejo de errores por defecto

Con `retrieve()`, una respuesta HTTP de error no se convierte silenciosamente en un DTO.

Por defecto, los códigos `4xx` y `5xx` producen excepciones derivadas de `RestClientResponseException`. ([Home][5])

Por ejemplo:

```java
try {
    CustomerDTO customer = restClient.get()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .body(CustomerDTO.class);

} catch (HttpClientErrorException.NotFound ex) {
    // HTTP 404

} catch (HttpClientErrorException ex) {
    // Otros errores 4xx

} catch (HttpServerErrorException ex) {
    // Errores 5xx

} catch (ResourceAccessException ex) {
    // Errores de red, conexión o timeout

} catch (RestClientException ex) {
    // Error general del cliente
}
```

Jerarquía simplificada:

```text
RestClientException
│
├── ResourceAccessException
│      └── conexión, timeout, I/O
│
└── RestClientResponseException
       │
       ├── HttpClientErrorException
       │      ├── BadRequest
       │      ├── Unauthorized
       │      ├── Forbidden
       │      ├── NotFound
       │      └── Conflict
       │
       └── HttpServerErrorException
              ├── InternalServerError
              ├── BadGateway
              ├── ServiceUnavailable
              └── GatewayTimeout
```

---

# 19. Manejo de errores con `onStatus`

Es preferible traducir errores HTTP externos a excepciones propias del dominio.

## Excepciones personalizadas

```java
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Customer not found: " + id);
    }
}
```

```java
public class CustomerServiceException extends RuntimeException {

    public CustomerServiceException(String message) {
        super(message);
    }

    public CustomerServiceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
```

## Tratamiento en la llamada

```java
public CustomerDTO findById(Long id) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .onStatus(
                    status -> status.value() == 404,
                    (request, response) -> {
                        throw new CustomerNotFoundException(id);
                    }
            )
            .onStatus(
                    HttpStatusCode::is5xxServerError,
                    (request, response) -> {
                        throw new CustomerServiceException(
                                "Customer API returned "
                                        + response.getStatusCode()
                        );
                    }
            )
            .body(CustomerDTO.class);
}
```

El orden importa: los handlers más específicos deben declararse antes que los generales.

---

# 20. Leer el body de error

Una API externa puede devolver:

```json
{
  "code": "CUSTOMER_NOT_FOUND",
  "message": "Customer 100 does not exist",
  "traceId": "abc-123"
}
```

DTO:

```java
public record ExternalApiError(
        String code,
        String message,
        String traceId
) {
}
```

Manejo:

```java
public CustomerDTO findById(Long id) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .onStatus(
                    HttpStatusCode::isError,
                    (request, response) -> {

                        ExternalApiError error =
                                response.bodyTo(
                                        ExternalApiError.class
                                );

                        throw new CustomerServiceException(
                                "External error [%s]: %s, traceId=%s"
                                        .formatted(
                                                error.code(),
                                                error.message(),
                                                error.traceId()
                                        )
                        );
                    }
            )
            .body(CustomerDTO.class);
}
```

Debe considerarse que el body puede estar vacío o no respetar el formato esperado. Una implementación productiva debería proteger también esa deserialización.

---

# 21. Handlers globales de status

Para no repetir `onStatus()` en cada operación:

```java
@Bean
RestClient customerRestClient(
        RestClient.Builder builder,
        CustomerClientProperties properties
) {
    return builder
            .baseUrl(properties.baseUrl())
            .defaultStatusHandler(
                    status -> status.value() == 401,
                    (request, response) -> {
                        throw new CustomerServiceException(
                                "Customer API authentication failed"
                        );
                    }
            )
            .defaultStatusHandler(
                    HttpStatusCode::is5xxServerError,
                    (request, response) -> {
                        throw new CustomerServiceException(
                                "Customer API unavailable: "
                                        + response.getStatusCode()
                        );
                    }
            )
            .build();
}
```

`RestClient.Builder` proporciona `defaultStatusHandler` para establecer políticas comunes de error. ([Home][4])

Una llamada concreta aún puede agregar un tratamiento más específico:

```java
.retrieve()
.onStatus(
        status -> status.value() == 404,
        (request, response) -> {
            throw new CustomerNotFoundException(id);
        }
)
.body(CustomerDTO.class);
```

---

# 22. `retrieve()` frente a `exchange()`

## `retrieve()`

Es la opción habitual:

```java
CustomerDTO dto = restClient.get()
        .uri("/api/customers/{id}", id)
        .retrieve()
        .body(CustomerDTO.class);
```

Está orientado a:

* extraer el body;
* convertirlo a un tipo;
* obtener un `ResponseEntity`;
* manejar status con `onStatus`.

## `exchange()`

Ofrece control completo sobre request y response.

```java
public Optional<CustomerDTO> findOptional(Long id) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .exchange((request, response) -> {

                if (response.getStatusCode().value() == 404) {
                    return Optional.empty();
                }

                if (response.getStatusCode().isError()) {
                    throw new CustomerServiceException(
                            "Unexpected status: "
                                    + response.getStatusCode()
                    );
                }

                CustomerDTO customer =
                        response.bodyTo(CustomerDTO.class);

                return Optional.ofNullable(customer);
            });
}
```

Utilice `exchange()` cuando la interpretación de la respuesta dependa fuertemente del código, los headers o distintos formatos de body.

No conviene utilizarlo indiscriminadamente: `retrieve()` resulta más legible para la mayoría de los casos.

---

# 23. Respuestas sin body y `null`

Este código puede devolver `null` si la respuesta no tiene body:

```java
CustomerDTO result = restClient.get()
        .uri("/api/customers/{id}", id)
        .retrieve()
        .body(CustomerDTO.class);
```

No debe suponerse que `body()` siempre devuelve un objeto.

Puede validarse:

```java
CustomerDTO result = restClient.get()
        .uri("/api/customers/{id}", id)
        .retrieve()
        .body(CustomerDTO.class);

if (result == null) {
    throw new CustomerServiceException(
            "Customer API returned an empty body"
    );
}

return result;
```

O:

```java
return Optional.ofNullable(
        restClient.get()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .body(CustomerDTO.class)
).orElseThrow(() ->
        new CustomerServiceException(
                "Customer API returned an empty body"
        )
);
```

---

# 24. Autenticación Basic

```java
return restClient.get()
        .uri("/api/customers/{id}", id)
        .headers(headers ->
                headers.setBasicAuth(username, password))
        .retrieve()
        .body(CustomerDTO.class);
```

Para credenciales estáticas, pueden configurarse globalmente:

```java
RestClient client = builder
        .baseUrl(properties.baseUrl())
        .defaultHeaders(headers ->
                headers.setBasicAuth(username, password))
        .build();
```

No conviene guardar credenciales directamente en el código o en un `application.yml` versionado. Deben provenir de secrets o variables protegidas.

---

# 25. Bearer token dinámico

En OAuth2, el token normalmente cambia con el tiempo.

```java
@Component
public class AccessTokenProvider {

    public String getToken() {
        // Obtener desde OAuth2AuthorizedClientManager,
        // un servicio de identidad o un cache seguro.
        return "...";
    }
}
```

Cliente:

```java
@Component
public class CustomerClient {

    private final RestClient restClient;
    private final AccessTokenProvider tokenProvider;

    public CustomerClient(
            RestClient restClient,
            AccessTokenProvider tokenProvider
    ) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
    }

    public CustomerDTO findById(Long id) {

        return restClient.get()
                .uri("/api/customers/{id}", id)
                .headers(headers ->
                        headers.setBearerAuth(
                                tokenProvider.getToken()
                        ))
                .retrieve()
                .body(CustomerDTO.class);
    }
}
```

Para un sistema real, Spring Security OAuth2 Client puede automatizar la obtención y renovación de tokens.

---

# 26. Interceptores

Un interceptor puede modificar todas las requests o examinar las responses.

Casos habituales:

* correlation ID;
* logging;
* autenticación;
* métricas;
* headers corporativos;
* trazabilidad.

Spring define `ClientHttpRequestInterceptor`, que puede registrarse tanto en `RestClient` como en `RestTemplate`. ([Home][6])

## Interceptor de correlation ID

```java
package com.example.customer.client;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CorrelationIdInterceptor
        implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        if (!request.getHeaders()
                .containsHeader("X-Correlation-Id")) {

            request.getHeaders().set(
                    "X-Correlation-Id",
                    UUID.randomUUID().toString()
            );
        }

        return execution.execute(request, body);
    }
}
```

Registro:

```java
@Bean
RestClient customerRestClient(
        RestClient.Builder builder,
        CustomerClientProperties properties,
        CorrelationIdInterceptor correlationInterceptor
) {
    return builder
            .baseUrl(properties.baseUrl())
            .requestInterceptor(correlationInterceptor)
            .build();
}
```

---

# 27. Interceptor de logging

```java
@Component
public class ClientLoggingInterceptor
        implements ClientHttpRequestInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ClientLoggingInterceptor.class
            );

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        long start = System.nanoTime();

        try {
            ClientHttpResponse response =
                    execution.execute(request, body);

            long durationMs =
                    Duration.ofNanos(
                            System.nanoTime() - start
                    ).toMillis();

            log.info(
                    "HTTP client method={} uri={} status={} durationMs={}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode(),
                    durationMs
            );

            return response;

        } catch (IOException ex) {

            long durationMs =
                    Duration.ofNanos(
                            System.nanoTime() - start
                    ).toMillis();

            log.error(
                    "HTTP client failed method={} uri={} durationMs={}",
                    request.getMethod(),
                    request.getURI(),
                    durationMs,
                    ex
            );

            throw ex;
        }
    }
}
```

No se deben registrar indiscriminadamente:

* tokens;
* passwords;
* documentos;
* tarjetas;
* datos personales;
* bodies financieros o médicos.

---

# 28. Caché HTTP condicional con ETag

Primera petición:

```java
ResponseEntity<CustomerDTO> response = restClient.get()
        .uri("/api/customers/{id}", id)
        .retrieve()
        .toEntity(CustomerDTO.class);

String etag = response.getHeaders().getETag();
```

Petición posterior:

```java
ResponseEntity<CustomerDTO> response = restClient.get()
        .uri("/api/customers/{id}", id)
        .header(HttpHeaders.IF_NONE_MATCH, etag)
        .retrieve()
        .toEntity(CustomerDTO.class);
```

Si el recurso no cambió, el servidor puede responder:

```http
HTTP/1.1 304 Not Modified
```

En ese caso debe utilizarse la copia almacenada localmente.

---

# 29. Control de concurrencia con `If-Match`

Supongamos que el cliente recuperó:

```http
ETag: "customer-100-v5"
```

Para actualizar:

```java
public CustomerDTO update(
        Long id,
        UpdateCustomerRequest request,
        String etag
) {
    return restClient.patch()
            .uri("/api/customers/{id}", id)
            .header(HttpHeaders.IF_MATCH, etag)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .onStatus(
                    status -> status.value() == 412,
                    (httpRequest, response) -> {
                        throw new ConcurrentModificationException(
                                "Customer was modified by another process"
                        );
                    }
            )
            .body(CustomerDTO.class);
}
```

Si otro proceso modificó el recurso, el servidor puede responder:

```http
HTTP/1.1 412 Precondition Failed
```

Esto evita sobrescribir cambios ajenos silenciosamente.

---

# 30. Retry: no reintentar todo

`RestClient` es un cliente HTTP; la política de retry suele implementarse alrededor del método mediante Spring Retry o Resilience4j.

El retry debe utilizarse principalmente para errores transitorios:

* timeout;
* conexión rechazada temporalmente;
* `502 Bad Gateway`;
* `503 Service Unavailable`;
* `504 Gateway Timeout`;
* posiblemente `429 Too Many Requests`, respetando `Retry-After`.

No debería reintentarse automáticamente:

* `400 Bad Request`;
* `401 Unauthorized`;
* `403 Forbidden`;
* `404 Not Found`;
* una validación de negocio;
* un `POST` no idempotente sin protección.

## Ejemplo con Spring Retry

Dependencia:

```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

Activación:

```java
@EnableRetry
@SpringBootApplication
public class CustomerApplication {
}
```

Cliente:

```java
@Retryable(
        retryFor = {
                ResourceAccessException.class,
                HttpServerErrorException.ServiceUnavailable.class,
                HttpServerErrorException.BadGateway.class,
                HttpServerErrorException.GatewayTimeout.class
        },
        maxAttempts = 3,
        backoff = @Backoff(
                delay = 500,
                multiplier = 2.0
        )
)
public CustomerDTO findById(Long id) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Fallback final:

```java
@Recover
public CustomerDTO recover(
        RestClientException exception,
        Long id
) {
    throw new CustomerServiceException(
            "Customer API unavailable after retries",
            exception
    );
}
```

---

# 31. Idempotencia y retry

Considere:

```java
restClient.post()
        .uri("/api/payments")
        .body(paymentRequest)
        .retrieve()
        .body(PaymentDTO.class);
```

Si ocurre un timeout, no sabemos con certeza si el servidor creó el pago.

Reintentar podría duplicarlo.

Una solución habitual:

```java
String idempotencyKey = UUID.randomUUID().toString();

restClient.post()
        .uri("/api/payments")
        .header("Idempotency-Key", idempotencyKey)
        .body(paymentRequest)
        .retrieve()
        .body(PaymentDTO.class);
```

El servidor debe almacenar temporalmente la clave y devolver el resultado original ante repeticiones.

El mismo `Idempotency-Key` debe reutilizarse en todos los reintentos de la misma operación lógica. Generar una clave nueva en cada intento anula la protección.

---

# 32. Circuit Breaker

Un retry intenta superar una falla temporal. Un circuit breaker evita seguir llamando a un servicio que está claramente degradado.

```text
CLOSED
  │
  ├── llamadas normales
  │
  └── demasiados errores
          │
          ▼
         OPEN
          │
          ├── falla rápidamente
          └── no llama al servicio
                  │
                  ▼
              HALF_OPEN
                  │
          prueba recuperación
```

Ejemplo conceptual con Resilience4j:

```java
@CircuitBreaker(
        name = "customerApi",
        fallbackMethod = "customerFallback"
)
public CustomerDTO findById(Long id) {

    return restClient.get()
            .uri("/api/customers/{id}", id)
            .retrieve()
            .body(CustomerDTO.class);
}

private CustomerDTO customerFallback(
        Long id,
        Throwable exception
) {
    throw new CustomerServiceException(
            "Customer API temporarily unavailable",
            exception
    );
}
```

Un fallback no siempre debe inventar un objeto vacío. En muchos dominios financieros es más seguro devolver un error controlado que mostrar información incompleta como si fuese válida.

---

# 33. Diseño de una clase cliente

Es preferible encapsular `RestClient` en una clase específica.

```java
package com.example.customer.client;

import com.example.customer.client.dto.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(RestClient customerRestClient) {
        this.restClient = customerRestClient;
    }

    public CustomerDTO findById(Long id) {

        CustomerDTO result = restClient.get()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new CustomerNotFoundException(id);
                        }
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        (request, response) -> {
                            throw new CustomerServiceException(
                                    "Customer API unavailable: "
                                            + response.getStatusCode()
                            );
                        }
                )
                .body(CustomerDTO.class);

        return Objects.requireNonNull(
                result,
                "Customer API returned an empty body"
        );
    }

    public List<CustomerDTO> findAll(
            CustomerStatus status
    ) {
        List<CustomerDTO> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/customers")
                        .queryParam("status", status)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return result != null ? result : List.of();
    }

    public CustomerDTO create(
            CreateCustomerRequest request
    ) {
        CustomerDTO result = restClient.post()
                .uri("/api/customers")
                .body(request)
                .retrieve()
                .body(CustomerDTO.class);

        return Objects.requireNonNull(
                result,
                "Customer API returned an empty body"
        );
    }

    public CustomerDTO update(
            Long id,
            UpdateCustomerRequest request
    ) {
        CustomerDTO result = restClient.patch()
                .uri("/api/customers/{id}", id)
                .body(request)
                .retrieve()
                .body(CustomerDTO.class);

        return Objects.requireNonNull(
                result,
                "Customer API returned an empty body"
        );
    }

    public void delete(Long id) {
        restClient.delete()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
```

El service de negocio utiliza el cliente sin conocer detalles HTTP:

```java
@Service
public class CustomerService {

    private final CustomerClient customerClient;

    public CustomerService(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    public CustomerDTO getActiveCustomer(Long id) {

        CustomerDTO customer =
                customerClient.findById(id);

        if (customer.status() != CustomerStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Customer is not active"
            );
        }

        return customer;
    }
}
```

Separación de responsabilidades:

```text
Controller
   │
   ▼
Service
   │
   ├── lógica de negocio
   │
   ▼
CustomerClient
   │
   ├── URI
   ├── headers
   ├── serialización
   ├── status HTTP
   ├── timeout
   └── errores de integración
        │
        ▼
API externa
```

---

# 34. No capturar excepciones demasiado pronto

Este enfoque suele ser incorrecto:

```java
public CustomerDTO findById(Long id) {
    try {
        return restClient.get()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .body(CustomerDTO.class);

    } catch (Exception ex) {
        return null;
    }
}
```

Problemas:

* oculta el motivo real;
* transforma timeout, 404 y 500 en lo mismo;
* produce `NullPointerException` posteriormente;
* dificulta métricas y troubleshooting;
* podría presentar un resultado incorrecto.

Es preferible traducir las excepciones:

```java
catch (ResourceAccessException ex) {
    throw new CustomerServiceException(
            "Timeout or connection error",
            ex
    );
}
```

Y dejar que errores no recuperables asciendan con contexto suficiente.

---

# 35. Pruebas con `MockRestServiceServer`

Spring proporciona soporte de testing para clientes HTTP.

```java
@SpringBootTest
class CustomerClientTest {

    private MockRestServiceServer server;
    private CustomerClient customerClient;

    @BeforeEach
    void setUp() {

        RestClient.Builder builder =
                RestClient.builder();

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        RestClient restClient = builder
                .baseUrl("https://customer-api.test")
                .build();

        customerClient =
                new CustomerClient(restClient);
    }
}
```

## Caso exitoso

```java
@Test
void shouldFindCustomer() {

    server.expect(
            requestTo(
                    "https://customer-api.test/api/customers/100"
            )
    )
    .andExpect(method(HttpMethod.GET))
    .andRespond(
            withSuccess(
                    """
                    {
                      "id": 100,
                      "name": "Laura Gómez",
                      "email": "laura@example.com",
                      "status": "ACTIVE"
                    }
                    """,
                    MediaType.APPLICATION_JSON
            )
    );

    CustomerDTO result =
            customerClient.findById(100L);

    assertEquals(100L, result.id());
    assertEquals("Laura Gómez", result.name());
    assertEquals(CustomerStatus.ACTIVE, result.status());

    server.verify();
}
```

Imports estáticos habituales:

```java
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
```

## Caso 404

```java
@Test
void shouldThrowWhenCustomerDoesNotExist() {

    server.expect(
            requestTo(
                    "https://customer-api.test/api/customers/999"
            )
    )
    .andExpect(method(HttpMethod.GET))
    .andRespond(
            withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                        {
                          "code": "CUSTOMER_NOT_FOUND",
                          "message": "Customer does not exist"
                        }
                        """)
    );

    assertThrows(
            CustomerNotFoundException.class,
            () -> customerClient.findById(999L)
    );

    server.verify();
}
```

## Verificar POST

```java
@Test
void shouldCreateCustomer() {

    server.expect(
            requestTo(
                    "https://customer-api.test/api/customers"
            )
    )
    .andExpect(method(HttpMethod.POST))
    .andExpect(
            content().contentType(
                    MediaType.APPLICATION_JSON
            )
    )
    .andExpect(
            jsonPath("$.name")
                    .value("Laura Gómez")
    )
    .andRespond(
            withStatus(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                        {
                          "id": 100,
                          "name": "Laura Gómez",
                          "email": "laura@example.com",
                          "status": "ACTIVE"
                        }
                        """)
    );

    CreateCustomerRequest request =
            new CreateCustomerRequest(
                    "Laura Gómez",
                    "laura@example.com"
            );

    CustomerDTO created =
            customerClient.create(request);

    assertEquals(100L, created.id());

    server.verify();
}
```

---

# 36. Observabilidad

Una llamada externa debe poder medirse y rastrearse.

Métricas recomendadas:

| Métrica              | Utilidad                               |
| -------------------- | -------------------------------------- |
| Latencia             | Detectar lentitud del servicio externo |
| Cantidad de requests | Volumen de integración                 |
| Errores por status   | Diferenciar 4xx y 5xx                  |
| Timeouts             | Detectar problemas de red o saturación |
| Retries              | Detectar degradación temporal          |
| Circuit breaker      | Conocer aperturas y recuperaciones     |

Ejemplo simple con Micrometer:

```java
@Component
public class CustomerClient {

    private final RestClient restClient;
    private final Timer requestTimer;

    public CustomerClient(
            RestClient customerRestClient,
            MeterRegistry meterRegistry
    ) {
        this.restClient = customerRestClient;

        this.requestTimer = Timer.builder(
                        "customer.api.requests"
                )
                .description(
                        "Latency of Customer API calls"
                )
                .register(meterRegistry);
    }

    public CustomerDTO findById(Long id) {

        return requestTimer.record(() -> {

            CustomerDTO result = restClient.get()
                    .uri("/api/customers/{id}", id)
                    .retrieve()
                    .body(CustomerDTO.class);

            return Objects.requireNonNull(result);
        });
    }
}
```

Aquí `Timer.record()` sí mide correctamente toda la operación porque `RestClient` es síncrono: la lambda no termina hasta que llega la respuesta.

---

# 37. RestClient y threads virtuales de Java 21

Al ser síncrono, `RestClient` bloquea el thread mientras espera.

Con threads tradicionales, miles de operaciones simultáneas pueden consumir muchos recursos. Java 21 permite utilizar virtual threads para que el modelo bloqueante resulte más escalable en cargas dominadas por I/O.

En Spring Boot puede habilitarse, según la versión y configuración usada:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Conceptualmente:

```text
RestClient + platform threads
        │
        └── cada espera ocupa un thread del SO

RestClient + virtual threads
        │
        └── cada espera ocupa un thread virtual liviano
```

Los virtual threads mejoran la escalabilidad del modelo bloqueante, pero no corrigen:

* timeouts mal definidos;
* conexiones insuficientes;
* servicios externos lentos;
* queries lentas;
* ausencia de circuit breaker;
* límites incorrectos del pool HTTP.

---

# 38. Cuándo elegir `RestClient`

## Es una buena elección cuando

* la aplicación utiliza Spring MVC;
* el flujo de negocio es imperativo;
* se quiere una sustitución moderna de `RestTemplate`;
* el equipo no necesita Reactor;
* el volumen de concurrencia es moderado;
* se utilizan virtual threads;
* la integración debe ser simple y legible;
* se desarrollan procesos batch o jobs.

## Conviene evaluar `WebClient` cuando

* toda la aplicación es reactiva;
* se usan `Mono` y `Flux`;
* hay streaming;
* existe alta concurrencia sostenida;
* se necesita backpressure;
* se combinan muchas llamadas no bloqueantes;
* se trabaja con Server-Sent Events.

No conviene usar `RestClient` dentro de un pipeline WebFlux pensando que se vuelve reactivo automáticamente:

```java
// Bloquea el thread que ejecuta este map
mono.map(value -> restClient.get()
        .uri("/external")
        .retrieve()
        .body(CustomerDTO.class));
```

---

# 39. Errores frecuentes

## Crear un cliente en cada llamada

Incorrecto:

```java
public CustomerDTO findById(Long id) {

    RestClient client =
            RestClient.create("https://api.example.com");

    return client.get()
            .uri("/customers/{id}", id)
            .retrieve()
            .body(CustomerDTO.class);
}
```

Conviene crear un bean reutilizable.

---

## No configurar timeouts

```java
RestClient.create(baseUrl);
```

Puede funcionar en desarrollo, pero es insuficiente como configuración productiva.

---

## Usar `List.class`

```java
.body(List.class);
```

Pierde el tipo de los elementos.

Utilice:

```java
.body(new ParameterizedTypeReference<List<CustomerDTO>>() {});
```

---

## Reintentar indiscriminadamente

```java
@Retryable(retryFor = Exception.class)
```

Puede reintentar:

* errores de validación;
* credenciales inválidas;
* recursos inexistentes;
* operaciones no idempotentes.

---

## Devolver `null` ante cualquier error

```java
catch (Exception ex) {
    return null;
}
```

Oculta la causa y propaga errores posteriores.

---

## Registrar tokens

```java
log.info("Authorization: {}", token);
```

Nunca debe hacerse.

---

## Mezclar lógica de negocio y HTTP

Incorrecto:

```java
public CustomerDTO findById(Long id) {

    CustomerDTO customer = restClient...

    if (customer.status() == BLOCKED) {
        calculatePremium();
        updatePolicy();
        sendEmail();
    }

    return customer;
}
```

El cliente debería encargarse de la integración HTTP. La lógica de negocio pertenece al service.

---

# 40. Implementación final recomendada

```java
@Configuration
@EnableConfigurationProperties(CustomerClientProperties.class)
public class CustomerClientConfiguration {

    @Bean
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerClientProperties properties,
            CorrelationIdInterceptor correlationInterceptor
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        "X-Application-Name",
                        "claims-service"
                )
                .requestInterceptor(
                        correlationInterceptor
                )
                .defaultStatusHandler(
                        HttpStatusCode::is5xxServerError,
                        (request, response) -> {
                            throw new CustomerServiceException(
                                    "Customer API returned "
                                            + response.getStatusCode()
                            );
                        }
                )
                .build();
    }
}
```

```java
@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(
            RestClient customerRestClient
    ) {
        this.restClient = customerRestClient;
    }

    public CustomerDTO findById(Long id) {

        CustomerDTO customer = restClient.get()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new CustomerNotFoundException(id);
                        }
                )
                .body(CustomerDTO.class);

        if (customer == null) {
            throw new CustomerServiceException(
                    "Customer API returned an empty body"
            );
        }

        return customer;
    }

    public List<CustomerDTO> findByStatus(
            CustomerStatus status
    ) {
        List<CustomerDTO> customers =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/customers")
                                .queryParam(
                                        "status",
                                        status
                                )
                                .build())
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<>() {
                                }
                        );

        return customers != null
                ? List.copyOf(customers)
                : List.of();
    }

    public CustomerDTO create(
            CreateCustomerRequest request
    ) {
        CustomerDTO customer = restClient.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        "Idempotency-Key",
                        UUID.randomUUID().toString()
                )
                .body(request)
                .retrieve()
                .body(CustomerDTO.class);

        if (customer == null) {
            throw new CustomerServiceException(
                    "Customer API returned an empty body"
            );
        }

        return customer;
    }

    public void delete(Long id) {
        restClient.delete()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
```

---

# Mapa mental final

```text
RestClient
│
├── Configuración
│   ├── baseUrl
│   ├── requestFactory
│   ├── connect timeout
│   ├── read timeout
│   ├── default headers
│   └── interceptors
│
├── Requests
│   ├── GET
│   ├── POST
│   ├── PUT
│   ├── PATCH
│   └── DELETE
│
├── URI
│   ├── path variables
│   ├── query parameters
│   └── URI builder
│
├── Request body
│   ├── DTO
│   ├── JSON
│   └── Content-Type
│
├── Response
│   ├── body(Class)
│   ├── ParameterizedTypeReference
│   ├── ResponseEntity
│   └── bodiless response
│
├── Errores
│   ├── onStatus
│   ├── defaultStatusHandler
│   ├── 4xx
│   ├── 5xx
│   ├── timeout
│   └── excepciones de dominio
│
├── Resiliencia
│   ├── retry selectivo
│   ├── idempotencia
│   ├── circuit breaker
│   └── fallback controlado
│
├── Seguridad
│   ├── Basic Auth
│   ├── Bearer token
│   ├── API key
│   └── protección de secretos
│
└── Producción
    ├── logging seguro
    ├── correlation ID
    ├── métricas
    ├── testing
    └── virtual threads
```

La idea principal es que `RestClient` no debe verse solamente como una forma abreviada de ejecutar un `GET`. En una integración productiva, el cliente debe concentrar **configuración HTTP, timeouts, serialización, autenticación, trazabilidad y traducción de errores**, mientras el service conserva exclusivamente la lógica de negocio.

[1]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html?utm_source=chatgpt.com "REST Clients :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClient.html?utm_source=chatgpt.com "RestClient (Spring Framework 7.0.8 API)"
[3]: https://docs.spring.io/spring-boot/reference/io/rest-client.html?utm_source=chatgpt.com "Calling REST Services :: Spring Boot"
[4]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClient.Builder.html?utm_source=chatgpt.com "RestClient.Builder (Spring Framework 7.0.8 API)"
[5]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClient.ResponseSpec.html?utm_source=chatgpt.com "RestClient.ResponseSpec (Spring Framework 7.0.8 API)"
[6]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/client/ClientHttpRequestInterceptor.html?utm_source=chatgpt.com "ClientHttpRequestInterceptor (Spring Framework 7.0.8 API)"
