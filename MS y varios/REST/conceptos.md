# RESTful con Java 21 y Spring Boot
## Guía de conceptos fundamentales

<br>

---

# 1. ¿Qué es REST?

REST (**Representational State Transfer**) es un **estilo arquitectónico** para diseñar aplicaciones distribuidas sobre HTTP.

REST **no es una tecnología**, sino un conjunto de principios.

Una API REST permite que dos aplicaciones se comuniquen mediante recursos utilizando HTTP.

```
Cliente
    |
 HTTP
    |
Spring Boot API
    |
 JSON
```

Por ejemplo:

```
GET /users/15
```

representa el recurso **Usuario 15**.

---

# 2. Conceptos fundamentales

## Recurso (Resource)

Todo gira alrededor de recursos.

Ejemplos

```
Usuario
Pedido
Producto
Factura
Pago
```

Los recursos se identifican mediante URLs.

```
/users
/users/25

/orders
/orders/1001
```

Nunca mediante verbos.

Incorrecto

```
/getUser
/createUser
/deleteUser
```

Correcto

```
GET /users/15

POST /users

DELETE /users/15
```

---

## Representación

REST no envía objetos Java.

Envía una representación del recurso.

Normalmente JSON.

```
{
   "id":15,
   "name":"Manuel",
   "email":"manuel@gmail.com"
}
```

---

## Stateless

Es probablemente el concepto más importante.

Cada request debe contener toda la información necesaria.

No existe estado de conversación.

Incorrecto

```
Request 1
→ "guardar usuario"

Request 2
→ "continuar operación"

(el servidor recuerda el estado)
```

Correcto

Cada request es independiente.

```
GET /users/15

Authorization: Bearer xxxx
```

Toda la información viaja nuevamente.

---

## Uniform Interface

Todas las APIs deberían comportarse igual.

Por eso existen:

GET

POST

PUT

PATCH

DELETE

---

# 3. Métodos HTTP

| Método | Acción | Idempotente |
|----------|------------|------------|
| GET | Leer | Sí |
| POST | Crear | No |
| PUT | Reemplazar | Sí |
| PATCH | Actualizar parcialmente | No (depende del diseño) |
| DELETE | Eliminar | Sí |

Ejemplo

```
GET /users/20
```

obtiene

```
{
 ...
}
```

---

# 4. Códigos HTTP

Una API REST no sólo devuelve datos.

También devuelve el estado de la operación.

## 2xx

Todo salió bien.

```
200 OK

201 Created

204 No Content
```

---

## 4xx

Error del cliente.

```
400 Bad Request

401 Unauthorized

403 Forbidden

404 Not Found

409 Conflict
```

---

## 5xx

Error del servidor.

```
500 Internal Server Error

502 Bad Gateway

503 Service Unavailable

504 Gateway Timeout
```

---

# 5. Spring Boot como servidor REST

Normalmente usamos

```
@RestController
```

```
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id){
        ...
    }

}
```

---

# 6. DTOs

Nunca devolver entidades JPA.

```
@Entity
class User
```

↓

```
UserDTO
```

¿Por qué?

- desacoplamiento

- seguridad

- versionado

- rendimiento

---

# 7. Consumo de APIs REST

Aquí aparecen los clientes HTTP.

En Spring existen tres generaciones.

```
RestTemplate
      ↓
RestClient
      ↓
WebClient
```

---

## RestTemplate

Sincrónico.

Actualmente en mantenimiento.

```
UserDTO dto =
restTemplate.getForObject(...)
```

---

## RestClient

Nuevo.

Sincrónico.

Muy simple.

Ideal para aplicaciones MVC tradicionales.

```
UserDTO dto =
restClient.get()
          ...
```

---

## WebClient

Reactivo.

No bloqueante.

Devuelve

```
Mono<UserDTO>

Flux<UserDTO>
```

Ideal para

- microservicios

- APIs concurrentes

- gateways

- BFF

---

# 8. Mono y Flux

## Mono

Representa

```
0..1 elemento
```

```
Mono<UserDTO>
```

Equivale conceptualmente a

```
Future<UserDTO>
```

pero reactivo.

---

## Flux

Representa

```
0..N elementos
```

Ejemplo

```
Flux<OrderDTO>
```

---

# 9. Pipeline reactivo

Con WebClient prácticamente todo consiste en construir un pipeline.

```
webClient.get()

       ↓

retrieve()

       ↓

bodyToMono()

       ↓

map()

       ↓

flatMap()

       ↓

retry()

       ↓

onErrorResume()

       ↓

subscribe()
```

Es una cadena de operadores.

---

# 10. Operadores Reactor más importantes

## map

Transforma datos.

```
Mono<UserDTO>

↓

Mono<UserResponse>
```

---

## flatMap

Transforma utilizando otra operación asíncrona.

Muy utilizado.

```
Usuario

↓

Consultar Perfil

↓

Consultar Historial
```

---

## zip

Ejecuta varias operaciones en paralelo.

```
Mono<User>

Mono<Profile>

Mono<History>

↓

Mono.zip(...)
```

Resultado

```
Tuple3
```

---

## merge

Une varios Flux.

```
Flux A

Flux B

↓

Flux.merge()
```

---

# 11. Manejo de errores

No se usa

```
try {

}
catch{

}
```

sino operadores.

```
doOnError()

onErrorResume()

onErrorReturn()

retryWhen()
```

---

## doOnError

Solo observa.

Ideal para

- logs

- métricas

- auditoría

No modifica el flujo.

---

## onErrorResume

Recupera el flujo.

```
Error

↓

Usuario Cacheado
```

---

## onErrorReturn

Devuelve un valor fijo.

```
Error

↓

"UNKNOWN"
```

---

## retryWhen

Reintenta automáticamente.

```
Intento 1

↓

Intento 2

↓

Intento 3
```

---

# 12. Fallback

Una de las ideas más importantes.

Cuando el servicio principal falla...

```
API Principal

↓

ERROR

↓

Fallback
```

El fallback puede ser:

- datos cacheados

- otro microservicio

- información parcial

- valores por defecto

---

# 13. Cache

Evita llamadas repetidas.

```
Cliente

↓

Cache

↓

¿Existe?

Sí

↓

Responder

No

↓

API Externa
```

Muy usado:

- Caffeine

- Redis

---

# 14. Circuit Breaker

Protege nuestro sistema.

Sin Circuit Breaker

```
Cliente

↓

API caída

↓

API caída

↓

API caída

↓

Miles de llamadas
```

Con Circuit Breaker

```
Cliente

↓

API caída

↓

Circuit OPEN

↓

No llamar

↓

Fallback
```

---

# 15. Retry

No todos los errores son permanentes.

```
Timeout

↓

Retry

↓

Éxito
```

Normalmente

```
Retry.backoff(...)
```

---

# 16. Observabilidad

En producción necesitamos saber qué ocurre.

Aquí entra

## Micrometer

Micrometer es la API estándar de métricas de Spring Boot.

No muestra gráficos.

Solo recolecta métricas.

Ejemplos:

- cantidad de requests
- tiempo promedio
- percentiles (P95, P99)
- memoria
- CPU
- GC
- conexiones HTTP
- consultas SQL
- cache hits/misses
- circuit breakers abiertos
- retries

---

## Actuator

Expone las métricas.

```
/actuator

/actuator/health

/actuator/metrics

/actuator/prometheus
```

---

## Prometheus

Hace scraping de las métricas expuestas por Actuator.

---

## Grafana

Visualiza dashboards.

```
Micrometer

↓

Actuator

↓

Prometheus

↓

Grafana
```

Esta combinación es el estándar de facto en aplicaciones Spring Boot desplegadas en Kubernetes u OpenShift.

---

# 17. Flujo completo de un microservicio moderno

```text
                Cliente
                   │
                   ▼
          Spring Boot Controller
                   │
                   ▼
              Service Layer
                   │
         ┌─────────┴─────────┐
         │                   │
         ▼                   ▼
      Cache             WebClient
                             │
               ┌─────────────┼─────────────┐
               ▼             ▼             ▼
           Users API    Profile API   History API
               │             │             │
         Retry/Fallback Retry/Fallback Retry/Fallback
               └──────┬──────┴──────┬──────┘
                      ▼
                  Mono.zip()
                      ▼
             Construcción del DTO
                      ▼
                 Guardar Cache
                      ▼
               Devolver Mono<UserDTO>
                      ▼
                  Cliente
```

---

# 18. ¿Qué debería dominar un desarrollador Senior?

## REST

- HTTP
- REST
- JSON
- DTO
- Versionado
- Idempotencia
- Códigos HTTP

## Spring MVC

- `@RestController`
- `ResponseEntity`
- Validaciones
- Exception Handling

## Consumo de APIs

- `RestClient`
- `WebClient`
- Serialización JSON
- Timeouts

## Reactor

- `Mono`
- `Flux`
- `map`
- `flatMap`
- `zip`
- `merge`
- `filter`
- `collectList`

## Resiliencia

- Retry
- Timeout
- Circuit Breaker
- Fallback
- Bulkhead (aislamiento de recursos)
- Rate Limiter (limitación de tráfico)

## Observabilidad

- Micrometer
- Actuator
- Prometheus
- Grafana
- Trazas (OpenTelemetry)
- Correlation IDs
- Logging estructurado

---

## Mi recomendación para tu roadmap

Por el tipo de preguntas que has venido haciendo (Java 21, Spring Boot, concurrencia, Kafka, JMS, JPA y arquitectura de microservicios), el siguiente orden de aprendizaje te dará una comprensión muy sólida:

1. REST y HTTP en profundidad (idempotencia, caché HTTP, negociación de contenido, códigos de estado).
2. `RestClient` para dominar el consumo de APIs de forma síncrona.
3. `WebClient` y Project Reactor (`Mono`, `Flux`, operadores reactivos).
4. Patrones de resiliencia: Retry, Timeout, Circuit Breaker, Bulkhead y Rate Limiter.
5. Observabilidad: Micrometer, Actuator, Prometheus, Grafana y OpenTelemetry.
6. Patrones de integración entre microservicios (API Gateway, BFF, Service Discovery, mensajería con Kafka/RabbitMQ).

Con ese recorrido, estarás cubriendo prácticamente todo el stack que hoy se espera de un desarrollador **Java/Spring Senior** en entornos de microservicios modernos.
