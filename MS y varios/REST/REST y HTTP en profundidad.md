# REST y HTTP en profundidad




Este es uno de los temas más importantes para un desarrollador Backend Senior, porque **REST no es simplemente "hacer un GET y devolver JSON"**. Detrás de una API REST bien diseñada hay una serie de conceptos de HTTP que impactan directamente en rendimiento, escalabilidad, resiliencia, interoperabilidad y experiencia del cliente.

En Java 21 y Spring Boot, muchas de estas funcionalidades ya están soportadas por el framework, pero es importante comprender **qué problema resuelven** y **cuándo utilizarlas**.

---

# Tutorial: REST y HTTP en profundidad

## Java 21 + Spring Boot

# Índice

1. ¿Qué es realmente REST?
2. HTTP como protocolo de aplicación
3. Recursos
4. URI
5. Métodos HTTP
6. Seguridad (Safe Methods)
7. Idempotencia
8. Cache HTTP
9. Negociación de Contenido (Content Negotiation)
10. Códigos de Estado HTTP
11. Headers HTTP más importantes
12. Versionado de APIs
13. Paginación
14. Errores REST
15. Buenas prácticas
16. Ejemplo completo en Spring Boot

---

# 1. ¿Qué es realmente REST?

REST significa

**Representational State Transfer**

Fue definido por Roy Fielding en el año 2000.

REST **no es un protocolo**.

REST **es un estilo arquitectónico**.

Define restricciones para construir servicios distribuidos.

---

## Principios REST

### Cliente - Servidor

```text
Cliente
      │
HTTP
      │
Servidor
```

Ambos evolucionan independientemente.

---

### Stateless

Cada request debe contener toda la información necesaria.

Incorrecto

```text
Request 1

↓

Servidor guarda estado

↓

Request 2 depende del estado anterior
```

Correcto

```text
GET /customers/100
Authorization: Bearer ...
```

Toda la información viaja en la petición.

---

### Cacheable

Las respuestas pueden cachearse.

```text
Cliente

↓

GET

↓

Servidor

↓

Cache

↓

Cliente
```

Esto reduce:

* tráfico
* CPU
* consultas a BD

---

### Uniform Interface

Todos los recursos siguen las mismas reglas.

Ejemplo

```text
GET /customers

GET /customers/100

POST /customers

PUT /customers/100

DELETE /customers/100
```

---

# 2. HTTP

HTTP es el protocolo que utiliza REST.

Una petición HTTP tiene:

```text
Request Line

Headers

Body
```

Ejemplo

```http
GET /customers/100 HTTP/1.1

Host: api.company.com

Accept: application/json

Authorization: Bearer xxxx
```

---

La respuesta

```http
HTTP/1.1 200 OK

Content-Type: application/json

{
   ...
}
```

---

# 3. Recursos

En REST todo es un recurso.

Incorrecto

```text
/getCustomer

/createCustomer

/deleteCustomer
```

Correcto

```text
/customers

/customers/100

/customers/100/orders
```

Los verbos los aporta HTTP.

---

# 4. Métodos HTTP

## GET

Obtiene recursos.

Nunca debería modificar información.

```http
GET /customers/10
```

---

## POST

Crea un recurso.

```http
POST /customers
```

---

## PUT

Reemplaza completamente un recurso.

```http
PUT /customers/10
```

---

## PATCH

Actualiza parcialmente.

```http
PATCH /customers/10
```

Body

```json
{
   "phone":"555-1234"
}
```

---

## DELETE

Elimina.

```http
DELETE /customers/10
```

---

# 5. Métodos Safe

HTTP define métodos "seguros".

Son aquellos que **no modifican el servidor**.

Safe

* GET
* HEAD
* OPTIONS

No Safe

* POST
* PUT
* PATCH
* DELETE

---

Ejemplo

```text
GET /customers
```

Puede ejecutarse 1000 veces.

Nunca debería insertar registros.

---

# 6. Idempotencia

Este concepto suele generar confusión.

## Definición

Una operación es idempotente si ejecutarla una vez o varias veces produce el mismo estado final.

---

## GET

```text
GET /customers/100
```

100 veces.

El estado no cambia.

Es idempotente.

---

## PUT

Supongamos

```text
PUT /customers/100
```

Body

```json
{
  "name":"Juan"
}
```

La primera llamada

```text
Nombre = Juan
```

La segunda llamada

```text
Nombre = Juan
```

La tercera

```text
Nombre = Juan
```

El estado final es el mismo.

Es idempotente.

---

## DELETE

```text
DELETE /customers/100
```

Primera llamada

Cliente eliminado.

Segunda llamada

Ya no existe.

Pero el estado final sigue siendo

```text
Cliente inexistente
```

Por eso DELETE también es idempotente.

---

## POST

No es idempotente.

```text
POST /payments
```

Llamada 1

```text
Pago 500
```

Llamada 2

```text
Otro pago 500
```

Tenemos dos pagos.

---

## ¿Por qué importa?

Supongamos un timeout.

```text
Cliente

↓

POST

↓

Timeout
```

El cliente no sabe si el servidor recibió la petición.

Entonces reintenta.

Si POST no está diseñado para evitar duplicados...

Puede generar:

* pagos duplicados
* transferencias duplicadas
* compras duplicadas

---

## Idempotency-Key

Muchos sistemas modernos solucionan esto con un encabezado específico.

```http
POST /payments

Idempotency-Key:
4f7b8c9d
```

Si llega nuevamente la misma clave

El servidor responde con el resultado original, en lugar de crear un nuevo recurso.

Este patrón es muy utilizado en APIs de pagos.

---

# 7. Cache HTTP

Una de las ventajas menos aprovechadas de HTTP.

Supongamos

```text
GET /countries
```

Los países cambian muy poco.

No tiene sentido consultar Oracle cada vez.

---

Servidor

```http
Cache-Control:
max-age=86400
```

El navegador guarda la respuesta por 24 horas.

---

## Cache-Control

Muy utilizado.

Ejemplo

```http
Cache-Control:
public,
max-age=300
```

Significa

Todos pueden cachearlo durante cinco minutos.

---

## no-cache

```http
Cache-Control:
no-cache
```

No significa "no guardar".

Significa:

"Antes de usar el contenido almacenado, debes validarlo con el servidor".

---

## no-store

```http
Cache-Control:
no-store
```

No debe guardarse nunca.

Ideal para:

* datos bancarios
* información médica
* autenticación

---

# 8. ETag

Muy importante.

Supongamos

```text
GET /customers/100
```

Respuesta

```http
ETag:
"ab123"
```

Luego

```http
GET /customers/100

If-None-Match:
"ab123"
```

Si nada cambió

Servidor responde

```http
304 Not Modified
```

Sin enviar el JSON nuevamente.

Reduce muchísimo el tráfico.

---

# 9. Negociación de Contenido

El cliente decide qué formato desea.

Header

```http
Accept:
application/json
```

o

```http
Accept:
application/xml
```

Spring automáticamente puede devolver ambos formatos si están configurados los convertidores adecuados.

```java
@GetMapping(
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
    }
)
```

---

## Content-Type

Indica el formato enviado por el cliente.

```http
Content-Type:
application/json
```

No debe confundirse con `Accept`.

| Header       | ¿Quién lo envía?   | ¿Qué indica?                   |
| ------------ | ------------------ | ------------------------------ |
| Accept       | Cliente            | Formato que desea recibir      |
| Content-Type | Cliente o Servidor | Formato del cuerpo del mensaje |

---

# 10. Códigos de Estado HTTP

Los códigos de estado forman parte del contrato de la API.

## 2xx – Éxito

| Código         | Significado                 | Cuándo usarlo                      |
| -------------- | --------------------------- | ---------------------------------- |
| 200 OK         | Operación exitosa           | GET, PUT, PATCH                    |
| 201 Created    | Recurso creado              | POST exitoso                       |
| 202 Accepted   | Procesamiento asíncrono     | Colas, Kafka, JMS                  |
| 204 No Content | Sin contenido para devolver | DELETE exitoso o PUT sin respuesta |

---

## 3xx – Redirección y caché

| Código | Significado                  |
| ------ | ---------------------------- |
| 301    | Redirección permanente       |
| 302    | Redirección temporal         |
| 304    | Recurso no modificado (ETag) |

---

## 4xx – Error del cliente

| Código | Significado                               | Ejemplo                               |
| ------ | ----------------------------------------- | ------------------------------------- |
| 400    | Solicitud inválida                        | Validación fallida                    |
| 401    | No autenticado                            | Token ausente o inválido              |
| 403    | Prohibido                                 | Sin permisos                          |
| 404    | Recurso inexistente                       | Cliente no encontrado                 |
| 405    | Método no permitido                       | POST sobre un recurso solo de lectura |
| 409    | Conflicto                                 | Duplicado, conflicto de versiones     |
| 415    | Tipo de contenido no soportado            | XML cuando solo se acepta JSON        |
| 422    | Entidad procesable con errores semánticos | Reglas de negocio incumplidas         |

---

## 5xx – Error del servidor

| Código | Significado                            |
| ------ | -------------------------------------- |
| 500    | Error interno inesperado               |
| 502    | Gateway recibió una respuesta inválida |
| 503    | Servicio temporalmente no disponible   |
| 504    | Timeout entre servicios                |

---

# 11. Headers HTTP más utilizados

| Header           | Uso                                         |
| ---------------- | ------------------------------------------- |
| Authorization    | Autenticación (JWT, OAuth2)                 |
| Accept           | Formato esperado por el cliente             |
| Content-Type     | Formato del cuerpo                          |
| Cache-Control    | Directivas de caché                         |
| ETag             | Identificador de versión del recurso        |
| If-None-Match    | Validación condicional usando ETag          |
| Location         | URI del recurso recién creado (201 Created) |
| Retry-After      | Indica cuándo reintentar tras un 503 o 429  |
| X-Correlation-Id | Trazabilidad entre microservicios           |

---

# 12. Versionado de APIs

Una API evoluciona con el tiempo y es importante mantener la compatibilidad con los consumidores existentes.

Las estrategias más comunes son:

### En la URI (la más utilizada)

```text
/api/v1/customers

/api/v2/customers
```

### En un Header

```http
Accept:
application/vnd.company.v2+json
```

### Como parámetro (menos recomendable)

```text
/customers?version=2
```

En Spring Boot suele utilizarse el versionado en la URI por su simplicidad y claridad.

---

# 13. Paginación

En lugar de devolver miles de registros en una sola respuesta:

```http
GET /customers?page=0&size=20
```

o, como en algunos de tus proyectos:

```http
GET /customers?$start_index=0&$count=20
```

La respuesta puede incluir enlaces HATEOAS (`self`, `next`, `prev`) y metadatos de paginación.

---

# 14. Manejo de errores REST

Una buena API no devuelve simplemente:

```http
500 Internal Server Error
```

Es preferible un formato consistente:

```json
{
  "timestamp": "2026-07-18T15:00:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "customerId must be positive",
  "path": "/customers/-1"
}
```

En Spring Boot esto suele implementarse con `@RestControllerAdvice` y manejadores específicos para excepciones.

---

# 15. Buenas prácticas

* Diseñar URIs orientadas a recursos, no a acciones.
* Utilizar el método HTTP adecuado para cada operación.
* Respetar la idempotencia de `GET`, `PUT` y `DELETE`.
* Aprovechar la caché HTTP cuando los datos sean relativamente estables.
* Implementar `ETag` para reducir tráfico innecesario.
* Devolver códigos de estado HTTP precisos.
* Mantener un formato uniforme para los errores.
* Versionar la API antes de introducir cambios incompatibles.
* Documentar la API con OpenAPI/Swagger.

---

# 16. Ejemplo completo con Spring Boot

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {

        CustomerDTO dto = service.findById(id);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .eTag("\"customer-" + dto.id() + "-" + dto.version() + "\"")
                .body(dto);
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> create(
            @RequestBody @Valid CustomerDTO dto) {

        CustomerDTO created = service.create(dto);

        URI location = URI.create("/api/v1/customers/" + created.id());

        return ResponseEntity.created(location)
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}
```

## Resumen conceptual

```text
                 Cliente HTTP
                      │
                      ▼
               HTTP Request
                      │
      ┌──────────────────────────────────┐
      │ URI + Método + Headers + Body    │
      └──────────────────────────────────┘
                      │
                      ▼
          Spring Boot Controller
                      │
                      ▼
                Lógica de negocio
                      │
          ┌───────────┴───────────┐
          │                       │
          ▼                       ▼
      Base de datos          Otros servicios
          │                       │
          └───────────┬───────────┘
                      ▼
              HTTP Response
                      │
      ┌──────────────────────────────────┐
      │ Status Code                      │
      │ Headers (Cache, ETag, etc.)      │
      │ Content-Type                     │
      │ Body (JSON/XML)                  │
      └──────────────────────────────────┘
                      │
                      ▼
                  Cliente
```

Este conjunto de conceptos constituye la base para diseñar APIs REST robustas. A partir de aquí, el siguiente nivel consiste en profundizar en **HATEOAS**, **Conditional Requests (`If-Modified-Since`, `If-Match`, `If-None-Match`)**, **control de concurrencia optimista mediante ETag**, **compresión HTTP (Gzip/Brotli)** y **HTTP/2 y HTTP/3**, temas muy relevantes para microservicios modernos desarrollados con Java 21 y Spring Boot.
