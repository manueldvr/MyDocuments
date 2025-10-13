
# ¿Cómo consumir servicios RESTful desde aplicaciones Java 21 basadas en Spring Boot?



<br>


Hoy en día lo recomendado es usar **`RestClient`** o **`WebClient`** (reactivo).

<br>


## Opciones:

| Cliente          | Estilo              | Cuándo usarlo                                                        |
| ---------------- | ------------------- | -------------------------------------------------------------------- |
| **RestClient**   | Sincrónico, simple  | Llamadas HTTP clásicas, fácil migración desde `RestTemplate`.        |
| **WebClient**    | Asíncrono, Reactivo | Escenarios con alta concurrencia o donde usas programación reactiva. |
| **RestTemplate** | Legado              | Evitar en nuevos proyectos; solo para compatibilidad.                |



<br>
<br>

## ✅ Ejemplo con **RestClient** (Recomendado)

Spring Boot 3 y Spring 6 introdujeron `RestClient` como reemplazo oficial de `RestTemplate`.
Es ideal si querés llamadas simples, estilo fluido.

### 1. Configuración (Spring Boot 3.x lo trae por defecto)

No hace falta dependencia extra si ya usás Spring Boot Web.

### 2. Crear un `@Bean` de RestClient (opcional)

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().baseUrl("https://api.example.com").build();
    }
}
```




### 3. Consumir un API REST

```java
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ApiService {

    private final RestClient restClient;

    public ApiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String getData() {
        return restClient.get()
                .uri("/data")
                .retrieve()
                .body(String.class);
    }
}
```


<br>

---

<br>
<br>

## ✅ Ejemplo con **WebClient** (para programación reactiva)

Si tu app necesita **escala, no bloqueo, y reactividad**, `WebClient` es mejor.

### Configuración

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://api.example.com")
                .build();
    }
}
```

### Uso

```java
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ApiReactiveService {

    private final WebClient webClient;

    public ApiReactiveService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getData() {
        return webClient.get()
                .uri("/data")
                .retrieve()
                .bodyToMono(String.class);
    }
}
```

<br>
<br>
<br>
<br>

## Manejo de Errores

<br>

Con `RestClient`:

```java
String response = restClient.get()
    .uri("/data")
    .retrieve()
    .onStatus(status -> status.is4xxClientError(),
        (request, responseSpec) -> {
            throw new RuntimeException("Error 4xx");
        })
    .body(String.class);
```

<br>
<br>


## Recomendación práctica

* Si tu arquitectura **no es reactiva** → usa **RestClient** (más simple).
* Si usas **Spring WebFlux y Reactor** → usa **WebClient**.
* Evitá `RestTemplate` en nuevos desarrollos (está en mantenimiento).


<br>
<br>




## Servicio con Retry + Fallback + Logging

```java
package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    public ExternalApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<UserDTO> getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                // Logging de error
                .doOnError(err -> System.err.println("❌ Error obteniendo usuario " + id + ": " + err.getMessage()))
                // Reintentos con backoff exponencial (3 intentos)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                // Fallback en caso de que todos los reintentos fallen
                .onErrorResume(err -> {
                    System.out.println("⚠️ Usando fallback para el usuario " + id);
                    UserDTO fallback = new UserDTO();
                    fallback.setId(id);
                    fallback.setName("Usuario Desconocido");
                    fallback.setEmail("unknown@example.com");
                    return Mono.just(fallback);
                })
                // Logging de éxito
                .doOnSuccess(user -> System.out.println("✅ Usuario obtenido: " + user.getName()));
    }
}
```

1. El cliente hace `GET /api/users/{id}`.
2. `WebClient` intenta obtener el usuario del servicio externo.
3. Si hay error → `doOnError` loguea el problema.
4. Se aplican hasta **3 reintentos** con `retryWhen`.
5. Si aún falla → `onErrorResume` devuelve un **usuario de fallback**.
6. `doOnSuccess` loguea que se obtuvo un resultado (real o fallback).
7. El cliente siempre recibe un `Mono<UserDTO>` JSON.









<br>
<br>
<br>

<br>



# WebClient

<br>


## ¿Qué es WebClient?

`WebClient` es un **cliente HTTP reactivo y no bloqueante** que forma parte de **Spring WebFlux**.
Es el reemplazo moderno de `RestTemplate` en entornos que requieren:

* **Alta concurrencia**
* **Escalabilidad**
* **Bajo consumo de recursos**
* Integración con **programación reactiva** (Reactor, Mono, Flux)


> En vez de usar hilos bloqueados esperando una respuesta (como `RestTemplate`), `WebClient` trabaja con **reactive streams** (Mono y Flux) y aprovecha mejor el hardware.



## 🧩 Contexto de uso

| Escenario                                   | Por qué usar `WebClient`                                                                                 |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| Microservicios con muchas llamadas HTTP     | Maneja miles de peticiones concurrentes con menos threads.                                               |
| Integraciones con APIs externas             | Evita bloquear hilos mientras espera respuestas de otros servicios.                                      |
| Aplicaciones **reactivas** (Spring WebFlux) | `WebClient` es el cliente nativo para programación reactiva con Mono/Flux.                               |
| Servicios de streaming de datos             | Puede manejar respuestas como `Flux` (streams reactivos).                                                |
| Gateway / Backend For Frontend (BFF)        | Ideal cuando un servicio agrega datos de muchos otros endpoints externos.                                |
| Apps con **alto tráfico**                   | Reduce el uso de threads y memoria, ideal para aplicaciones que procesan muchas conexiones concurrentes. |




<br>
<br>


## 🔍 Diferencias con RestClient/RestTemplate

| Feature               | `RestTemplate` (Legacy) | `RestClient` (Nuevo, síncrono) | `WebClient` (Reactivo)   |
| --------------------- | ----------------------- | ------------------------------ | ------------------------ |
| Bloqueante            | ✅ Sí                    | ✅ Sí                           | ❌ No                     |
| Programación reactiva | ❌ No                    | ❌ No                           | ✅ Sí                     |
| Streaming de datos    | ❌ No                    | ❌ No                           | ✅ Sí                     |
| Complejidad           | Baja                    | Baja                           | Media (requiere Reactor) |
| Rendimiento en escala | Medio                   | Medio                          | Alto                     |



<br>
<br>

## ⚙️ Configuración básica de WebClient

Spring Boot 3 ya trae WebFlux, solo agregás la dependencia si no está:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

## 📝 Ejemplo práctico

### 1. Definir el Bean

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://api.example.com")
                .build();
    }
}
```

---

### 2. Servicio que consume API

```java
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    public ExternalApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> getData() {
        return webClient.get()
                .uri("/data")
                .retrieve()
                .bodyToMono(String.class);
    }
}
```

---

### 3. Controlador

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ApiController {

    private final ExternalApiService externalApiService;

    public ApiController(ExternalApiService externalApiService) {
        this.externalApiService = externalApiService;
    }

    @GetMapping("/consume")
    public Mono<String> consume() {
        return externalApiService.getData();
    }
}
```

---

## 🔐 Manejo de errores y reintentos

```java
import reactor.util.retry.Retry;
import java.time.Duration;

public Mono<String> getDataWithRetry() {
    return webClient.get()
            .uri("/data")
            .retrieve()
            .onStatus(status -> status.is4xxClientError(),
                      response -> Mono.error(new RuntimeException("Error cliente")))
            .onStatus(status -> status.is5xxServerError(),
                      response -> Mono.error(new RuntimeException("Error servidor")))
            .bodyToMono(String.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
}
```

---


<br>
<br>

## 🚀 Ventajas de WebClient

1. **No bloqueante** → Mejor uso de hilos, escalabilidad en microservicios.
2. **Soporte nativo para reactividad** (Mono/Flux).
3. **Streaming**: Consume y procesa datos a medida que llegan.
4. **Configuración flexible** (timeout, interceptores, OAuth2).
5. **Preparado para aplicaciones modernas**: Java 21 + Spring Boot 3.

---

💡 **Regla práctica**:

* Si tu aplicación es **tradicional, bloqueante**, usa `RestClient`.
* Si tu aplicación **ya es reactiva (WebFlux)** o necesitas **escalabilidad extrema**, usa `WebClient`.



---




### Por qué en la complejidad de WebClient es media y requiere Reactor?


¡Excelente observación! 🔍
Te explico **por qué puse que WebClient tiene una complejidad “media” y depende de Reactor**:


<br>
<br>

### 🔑 1. Porque **WebClient es totalmente reactivo**

* `WebClient` no devuelve valores directamente (como un `String` o un objeto).
* En lugar de eso devuelve **`Mono<T>` o `Flux<T>`**, que son tipos de datos reactivos de **Project Reactor**.
* Esto significa que hay que **aprender programación reactiva** (o al menos conceptos básicos):

  * `Mono<T>` = 0 o 1 resultado asíncrono.
  * `Flux<T>` = 0, 1 o muchos resultados (stream).

* Para trabajar con esos tipos, usás **operadores** (map, flatMap, filter, etc.) y pipelines, en vez de lógica secuencial.

Ejemplo:

```java
Mono<String> response = webClient.get()
        .uri("/data")
        .retrieve()
        .bodyToMono(String.class);

// No podés hacer directamente System.out.println(response);
response.subscribe(System.out::println);
```

Ese patrón reactivo **no es inmediato para programadores acostumbrados a código bloqueante**.

---

### 🔑 2. Porque depende de **Project Reactor**

* Spring WebFlux usa **Project Reactor** como base (implementa Reactive Streams).
* Para manejar datos asíncronos, tenés que conocer:

  * Backpressure
  * Operadores (`map`, `flatMap`, `zip`, `onErrorResume`, etc.)
  * Cómo combinar flujos reactivos
* Esto aumenta la **curva de aprendizaje**, sobre todo si venís de programación imperativa.

---

### 🔑 3. Porque cambia la forma de pensar

Con `RestClient`  escribís:

```java
String result = restClient.get()
    .uri("/data")
    .retrieve()
    .body(String.class);
System.out.println(result);
```

Con `WebClient`:

```java
Mono<String> result = webClient.get()
    .uri("/data")
    .retrieve()
    .bodyToMono(String.class);

result.subscribe(System.out::println);  // asíncrono
```

Tenés que manejar:

* **Suscripción manual** (`subscribe`) o integración con controladores reactivos.
* **Encadenar flujos** en vez de esperar resultados inmediatamente.
* **Excepciones y reintentos** con operadores, no con `try-catch` tradicional.

---

### 🔑 4. Cuándo esa complejidad vale la pena

WebClient es más complejo **solo al principio**. Vale la pena en:

* Apps con **miles de conexiones concurrentes**.
* **Microservicios reactivos**.
* Procesos de **streaming de datos** en tiempo real.
* Si querés **máxima eficiencia** en I/O no bloqueante.

Para casos simples o donde no usás reactividad, **RestClient es más directo** y menos “cerebral”.

---

📌 **Resumen en una frase**:

> La complejidad “media” viene porque **WebClient no entrega datos directamente**, sino a través de tipos reactivos (`Mono`/`Flux`), y eso te obliga a **pensar en programación reactiva con Project Reactor**, lo cual tiene una curva de aprendizaje mayor.




<br>
<br>

¿Querés que te haga un **mapa mental de Mono y Flux** para que veas rápido cómo se usan?


Te armo un **Mapa Mental de `Mono` y `Flux` (Project Reactor)** para entender WebClient:

---

```
                 ┌────────────────────────────┐
                 │       Project Reactor      │
                 └─────────────┬──────────────┘
                               │
           ┌───────────────────┴───────────────────┐
           │                                       │
      ┌────▼─────┐                           ┌─────▼─────┐
      │   Mono   │                           │   Flux    │
      └────┬─────┘                           └─────┬─────┘
           │                                       │
   "0 o 1 elemento"                         "0 a N elementos"
   Asíncrono                                Flujo reactivo
   Ej: Respuesta HTTP única                 Ej: Stream de datos

           │                                       │
   ┌───────▼────────┐                       ┌───────▼────────┐
   │  Métodos clave │                       │  Métodos clave │
   └───────┬────────┘                       └───────┬────────┘
           │                                       │
  .just(T)        → Crear Mono             .just(T...)      → Crear Flux
  .empty()        → Mono vacío             .fromIterable()  → Flux desde lista
  .subscribe()    → Ejecutar               .subscribe()     → Ejecutar flujo
  .block()        → Bloquear y obtener     .take(n)         → Tomar primeros n
                      valor (no recomendado)
           │                                       │
 ┌─────────▼──────────┐                 ┌──────────▼─────────┐
 │   Operadores       │                 │    Operadores      │
 └─────────┬──────────┘                 └──────────┬─────────┘
           │                                       │
    .map()         → Transformar dato        .map()         → Transformar cada dato
    .flatMap()     → Encadenar Monos         .flatMap()     → Encadenar flujos
    .filter()      → Filtrar dato            .filter()      → Filtrar elementos
    .onErrorResume()→ Manejar errores        .onErrorContinue()→ Manejar errores parciales

           │                                       │
 ┌─────────▼─────────────┐                ┌────────▼─────────────┐
 │     Contexto          │                │      Contexto        │
 └─────────┬─────────────┘                └─────────┬────────────┘
           │                                       │
Ideal para respuestas únicas               Ideal para streams:
- Llamada HTTP                             - WebSocket
- Query único a DB                         - Streaming de logs
- Cálculos simples                         - API con listas grandes

```




🔍 **Interpretación rápida:**

* **Mono:** Representa **un único valor o vacío** → perfecto para **respuestas HTTP simples**.
* **Flux:** Representa **un flujo de múltiples valores** → perfecto para **streams, listas o datos continuos**.
* Ambos son **asíncronos y no bloqueantes**.
* El patrón es:
  `Publisher (Mono/Flux)` → `Operadores (map, filter, flatMap...)` → `Subscriber (subscribe())`.

---

💡 **Tip**: Para integrarte con controladores en Spring WebFlux, no llamás `.subscribe()`.
Spring lo hace por vos y serializa el `Mono` o `Flux` como respuesta HTTP.

---




<br>
<br>
<br>
<br>









# Ejemplo


**ejemplo comparativo** entre cómo consumir una API con `Mono` (simple) y `Flux` (stream)?  
y en el caso Mono que sea un DTO ?


ver `Mono` (con DTO) y `Flux` (con lista/stream) usando **WebClient** en Spring Boot 3.x + Java 21.



## 🔹 1. Definimos el DTO (para el caso `Mono`)

```java
package com.example.demo.dto;

public class UserDTO {
    private Long id;
    private String name;
    private String email;

    // Getters y setters
    public Long getId() { return id; }
    ...
}
```



## 🔹 2. Configuración de `WebClient`

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build();
    }
}
```



## 3. Servicio: Ejemplo Mono (DTO) y Flux (lista)

```java
package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    public ExternalApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    // 🔹 MONO: obtener un único usuario como DTO
    public Mono<UserDTO> getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class);
    }

    // 🔹 FLUX: obtener todos los usuarios (lista)
    public Flux<UserDTO> getAllUsers() {
        return webClient.get()
                .uri("/users")
                .retrieve()
                .bodyToFlux(UserDTO.class);
    }
}
```



## 4. Controlador para exponer endpoints

```java
package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.ExternalApiService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ExternalApiService apiService;

    public UserController(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    // 🔹 Mono: devuelve un usuario (único DTO)
    @GetMapping("/{id}")
    public Mono<UserDTO> getUser(@PathVariable Long id) {
        return apiService.getUserById(id);
    }

    // 🔹 Flux: devuelve todos los usuarios (stream o lista JSON)
    @GetMapping
    public Flux<UserDTO> getAllUsers() {
        return apiService.getAllUsers();
    }
}
```




## Cómo funciona

| Endpoint           | Retorno         | Descripción                                  |
| ------------------ | --------------- | -------------------------------------------- |
| `GET /api/users/1` | `Mono<UserDTO>` | Retorna **un usuario** en JSON.              |
| `GET /api/users`   | `Flux<UserDTO>` | Retorna **todos los usuarios** (lista JSON). |

---

### 🔑 Diferencia práctica

* `Mono<UserDTO>` → El cliente recibe **un único objeto JSON**:

```json
{
  "id": 1,
  "name": "Leanne Graham",
  "email": "Sincere@april.biz"
}
```

* `Flux<UserDTO>` → El cliente recibe **un array JSON**:

```json
[
  { "id": 1, "name": "Leanne Graham", "email": "Sincere@april.biz" },
  { "id": 2, "name": "Ervin Howell", "email": "Shanna@melissa.tv" }
]
```

<br>

🔍 **Notas importantes**:

* `Mono<T>` y `Flux<T>` los maneja automáticamente Spring WebFlux → No necesitas `.subscribe()` en el controlador.
* Si el cliente HTTP soporta streaming (por ejemplo, `curl` o un frontend reactivo), el `Flux` se transmitirá **por partes** sin esperar todo el resultado.











---

¿Querés que el próximo ejemplo tenga **manejo de errores (4xx/5xx) y reintentos** usando `Mono` y `Flux`?  
si , y agrega como sería el manejo de resultados?



¡Genial! Vamos a **completar el ejemplo** agregando:

1. 🔒 **Manejo de errores HTTP** (4xx, 5xx) en `Mono` y `Flux`.
2. 🔁 **Reintentos automáticos**.
3. ✅ **Manejo de resultados** (cómo consumir los datos en el lado cliente o dentro del servicio).

---

## 🔹 1. Servicio con Manejo de Errores y Reintentos

```java
package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    public ExternalApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    // 🔹 MONO: obtener un único usuario
    public Mono<UserDTO> getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        response -> Mono.error(new RuntimeException("Error 4xx: Usuario no encontrado")))
                .onStatus(status -> status.is5xxServerError(),
                        response -> Mono.error(new RuntimeException("Error 5xx: Problema en el servidor externo")))
                .bodyToMono(UserDTO.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .filter(ex -> !(ex instanceof IllegalArgumentException))
                )
                .doOnSuccess(user -> System.out.println("✅ Usuario encontrado: " + user.getName()))
                .doOnError(err -> System.err.println("❌ Error obteniendo usuario: " + err.getMessage()));
    }

    // 🔹 FLUX: obtener todos los usuarios
    public Flux<UserDTO> getAllUsers() {
        return webClient.get()
                .uri("/users")
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        response -> Mono.error(new RuntimeException("Error 4xx: No autorizado o recurso inválido")))
                .onStatus(status -> status.is5xxServerError(),
                        response -> Mono.error(new RuntimeException("Error 5xx: API caída")))
                .bodyToFlux(UserDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnNext(user -> System.out.println("📦 Recibido usuario: " + user.getId()))
                .doOnError(err -> System.err.println("❌ Error obteniendo usuarios: " + err.getMessage()));
    }
}
```

---

### 🔍 Explicación clave:

* `onStatus(...)`: Intercepta códigos HTTP de error y devuelve una excepción personalizada.
* `retryWhen(...)`: Reintenta la petición (máximo 3 veces) con **backoff exponencial** (espera creciente).
* `doOnSuccess`, `doOnNext`, `doOnError`: Hooks para **loggear resultados** sin romper el flujo.

---

## 🔹 2. Controlador igual que antes

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ExternalApiService apiService;

    public UserController(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/{id}")
    public Mono<UserDTO> getUser(@PathVariable Long id) {
        return apiService.getUserById(id);
    }

    @GetMapping
    public Flux<UserDTO> getAllUsers() {
        return apiService.getAllUsers();
    }
}
```

---

## 🔹 3. Manejo de Resultados (lado del consumidor)

### 🔸 a) Desde el Navegador o cURL:

```bash
curl http://localhost:8080/api/users/1
```

📜 Resultado (Mono):

```json
{
  "id": 1,
  "name": "Leanne Graham",
  "email": "Sincere@april.biz"
}
```

```bash
curl http://localhost:8080/api/users
```

📜 Resultado (Flux):

```json
[
  {"id":1,"name":"Leanne Graham","email":"Sincere@april.biz"},
  {"id":2,"name":"Ervin Howell","email":"Shanna@melissa.tv"}
]
```

---

### 🔸 b) Dentro de la aplicación (imperativo con `.block()` ⚠️)

Si necesitás **bloquear** y obtener el resultado (no recomendado en WebFlux):

```java
UserDTO user = apiService.getUserById(1L).block();
System.out.println("Usuario obtenido: " + user.getName());
```

Para `Flux`:

```java
List<UserDTO> users = apiService.getAllUsers()
                                .collectList()
                                .block();
users.forEach(u -> System.out.println(u.getName()));
```

> ⚠️ `.block()` rompe la naturaleza reactiva. Úsalo solo en casos muy puntuales.

---

### 🔸 c) Manejo Reactivo Correcto

En controladores reactivos, simplemente retornás `Mono`/`Flux`.
Spring se encarga de suscribirse, serializar, y enviar la respuesta HTTP.

En **servicios internos** podés hacer:

```java
apiService.getAllUsers()
    .map(user -> user.getName().toUpperCase())
    .subscribe(name -> System.out.println("Usuario: " + name));
```

---

## 🔑 Resumen

* **Mono**: Representa una respuesta única → ideal para buscar por ID.
* **Flux**: Representa múltiples resultados → ideal para listas o streams.
* **Manejo de errores**: `onStatus`, `doOnError`.
* **Reintentos**: `retryWhen`.
* **Manejo de resultados**:

  * Reactivo → `map`, `flatMap`, `subscribe`.
  * Imperativo (solo si es necesario) → `.block()`.


---



¿Querés que en el **próximo paso** lo haga **más enterprise** con:
✅ DTOs de error personalizados
✅ `ResponseEntity` reactivo
✅ Logs estructurados para debugging?



en el get() para qué sirve doOnError?







¡Muy buena pregunta! 🔍
En **Reactor** (y por extensión en `WebClient`), el operador `doOnError` sirve para **ejecutar una acción secundaria cuando ocurre un error en el flujo reactivo**, sin modificarlo.

---

### 🔑 Concepto

* `doOnError` es un **operador de "side-effect"** (efecto colateral).
* Se ejecuta **solo si ocurre un error** en el pipeline (`Mono` o `Flux`).
* **No maneja el error** ni lo transforma: simplemente **observa** el error y ejecuta algo (ej. loguear, métricas, notificaciones).
* El flujo sigue fallando a menos que uses operadores como `onErrorResume` o `onErrorReturn` para recuperarte.

---

### 🔍 Ejemplo práctico

```java
public Mono<UserDTO> getUserById(Long id) {
    return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(UserDTO.class)
            .doOnError(error -> {
                System.err.println("❌ Error al obtener usuario " + id + ": " + error.getMessage());
            });
}
```

📌 Si la API devuelve error (404, 500, etc.), `doOnError` imprime el log, pero **el `Mono` sigue emitiendo error**.
Otro operador (como `onErrorResume`) tendría que interceptarlo si querés devolver algo alternativo.

---

### 🔎 Comparación rápida de operadores de error

| Operador          | Qué hace                                              |
| ----------------- | ----------------------------------------------------- |
| `doOnError()`     | Solo **observa** el error → logs, métricas, etc.      |
| `onErrorResume()` | **Recupera** el flujo devolviendo otro `Mono`/`Flux`. |
| `onErrorReturn()` | **Devuelve un valor fijo** cuando hay error.          |
| `retryWhen()`     | **Reintenta** la suscripción al flujo cuando falla.   |

---

### 🔗 Ejemplo completo con `doOnError` + `onErrorResume`

```java
public Mono<UserDTO> getUserById(Long id) {
    return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(UserDTO.class)
            .doOnError(err -> System.err.println("Error: " + err.getMessage()))
            .onErrorResume(err -> {
                // Si falla, devolvemos un usuario por defecto
                return Mono.just(new UserDTO());
            });
}
```

---

✅ **En resumen:**
`doOnError` se usa para **actuar cuando hay error** (logging, métricas, alertas) sin cambiar el resultado del flujo.
Si querés **recuperarte del error**, combinás `doOnError` con `onErrorResume`, `onErrorReturn` o `retryWhen`.








<br>
<br>
<br>
<br>




---


<br>
<br>


#### ¿Querés que te haga un **diagrama visual** de cómo fluyen `doOnError`, `onErrorResume` y `retryWhen` en una llamada con `WebClient`?



**diagrama visual** para entender cómo se encadenan `doOnError`, `onErrorResume` y `retryWhen` en un flujo reactivo con `WebClient`.

---

### 🔍 Diagrama de flujo de errores en Reactor (`Mono`/`Flux`)

```
          ┌─────────────────────────┐
          │   Suscripción al flujo  │
          │ (WebClient.get()...)    │
          └───────────┬────────────┘
                      │
             ┌────────▼────────┐
             │ ¿Flujo exitoso? │───────────► Emitir datos (onNext/onSuccess)
             └───────┬─────────┘
                     │ No
                     ▼
           ┌─────────────────────┐
           │     Ocurre Error    │
           └─────────┬──────────┘
                     │
        ┌────────────▼────────────┐
        │ doOnError(error)        │
        │ (Solo observa, loguea)  │
        └────────────┬────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │ ¿Hay retryWhen?        │
        └─────────┬──────────────┘
                  │ Sí
                  ▼
          ┌────────────────────┐
          │ Reintentar flujo   │
          │ (según estrategia) │
          └─────────┬──────────┘
                    │ No más reintentos
                    ▼
        ┌────────────────────────┐
        │ ¿Hay onErrorResume?    │
        └─────────┬──────────────┘
                  │ Sí
                  ▼
     ┌────────────────────────────┐
     │ Reemplazar error con otro  │
     │ Mono/Flux                  │
     └───────────┬────────────────┘
                 │ No
                 ▼
        ┌────────────────────────┐
        │ ¿Hay onErrorReturn?    │
        └─────────┬──────────────┘
                  │ Sí
                  ▼
     ┌────────────────────────────┐
     │ Emitir valor por defecto   │
     └───────────┬────────────────┘
                 │ No
                 ▼
          ┌──────────────────┐
          │ Emitir el error  │
          │ al suscriptor    │
          └──────────────────┘
```

---

### 🔑 Interpretación rápida:

1. **`doOnError()`**

   * Solo **observa** el error.
   * Ideal para **logs, métricas, alertas**.
   * No interrumpe ni cambia el flujo.

2. **`retryWhen()`**

   * Si hay error, vuelve a intentar la petición según la estrategia (ej. 3 veces con backoff).
   * Si se agotan reintentos, pasa al siguiente paso.

3. **`onErrorResume()`**

   * Intercepta el error y **devuelve un flujo alternativo** (otro Mono/Flux).
   * Útil para fallback (ej. datos cacheados).

4. **`onErrorReturn()`**

   * Devuelve **un valor fijo** si hay error.
   * Útil para casos simples.

5. Si nada lo maneja → El error **se propaga** al suscriptor final.

---

🔍 **Ejemplo realista**:

```java
public Mono<UserDTO> getUserById(Long id) {
    return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(UserDTO.class)
            .doOnError(err -> System.err.println("📛 Log error: " + err.getMessage()))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .onErrorResume(err -> {
                System.out.println("⚠️ Usando fallback");
                UserDTO fallback = new UserDTO();
                fallback.setId(id);
                fallback.setName("Usuario Desconocido");
                fallback.setEmail("unknown@example.com");
                return Mono.just(fallback);
            });
}
```

---

💡 Con este pipeline:

* `doOnError` **siempre loguea**.
* `retryWhen` intenta 3 veces.
* Si sigue fallando, `onErrorResume` devuelve un **usuario de fallback**.






















<br>




<br>



<br>
<br>
<br>


<br>



## Versión aún más avanzada

<br>


Si querés, puedo hacer una **versión aún más avanzada** integrando 
**circuit breaker + cache con TTL + métricas con Micrometer** para 
que sea casi lista para producción.


Esto es un **patrón muy usado en microservicios reactivos**: resiliencia + rendimiento + monitoreo.



<br>


* **WebClient reactivo**
* **Fallback**
* **Cache con TTL (time-to-live)**
* **Circuit Breaker** con Resilience4j
* **Retry automático con backoff**
* **Logging estructurado**
* **Métricas con Micrometer / Spring Boot Actuator**

---

## 🔹 1. Dependencias Maven necesarias

```xml
<dependencies>
    <!-- Spring WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Resilience4j for Circuit Breaker -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
        <version>2.0.2</version>
    </dependency>

    <!-- Caffeine Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <version>3.1.8</version>
    </dependency>

    <!-- Spring Boot Actuator + Micrometer -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```


## 🔹 2. Configuración de WebClient y Cache

```java
package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<Long, Object> userCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES) // TTL 10 minutos
                .maximumSize(1000)
                .build();
    }
}
```

---

## 🔹 3. Servicio con Circuit Breaker, Retry y Fallback

```java
package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import com.github.benmanes.caffeine.cache.Cache;

import java.time.Duration;

@Service
public class ExternalApiService {

    private final WebClient webClient;
    private final Cache<Long, UserDTO> userCache;

    public ExternalApiService(WebClient webClient, Cache<Long, UserDTO> userCache) {
        this.webClient = webClient;
        this.userCache = userCache;
    }

    @CircuitBreaker(name = "userServiceCircuit", fallbackMethod = "fallbackUser")
    public Mono<UserDTO> getUserById(Long id) {
        // Retornamos cache si existe
        UserDTO cached = userCache.getIfPresent(id);
        if (cached != null) {
            return Mono.just(cached);
        }

        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .doOnError(err -> System.err.println("❌ Error obteniendo usuario " + id + ": " + err.getMessage()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnSuccess(user -> {
                    if (user != null) {
                        userCache.put(id, user); // Guardamos en cache
                        System.out.println("✅ Usuario cacheado: " + user.getName());
                    }
                });
    }

    // Método fallback para circuit breaker
    public Mono<UserDTO> fallbackUser(Long id, Throwable t) {
        System.out.println("⚠️ Fallback activado para usuario " + id + ": " + t.getMessage());
        UserDTO fallback = new UserDTO();
        fallback.setId(id);
        fallback.setName("Usuario Desconocido");
        fallback.setEmail("unknown@example.com");
        return Mono.just(fallback);
    }
}
```

---

## 🔹 4. Controlador reactivo

```java
package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.ExternalApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UserController {

    private final ExternalApiService apiService;

    public UserController(ExternalApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/api/users/{id}")
    public Mono<UserDTO> getUser(@PathVariable Long id) {
        return apiService.getUserById(id);
    }
}
```




## 🔹 5. Qué logramos con esta arquitectura

| Componente                         | Función                                                               |
| ---------------------------------- | --------------------------------------------------------------------- |
| **WebClient**                      | Consumir API externa de forma reactiva.                               |
| **Cache Caffeine**                 | Guardar datos temporalmente (TTL) para reducir llamadas externas.     |
| **Circuit Breaker (Resilience4j)** | Detecta fallos frecuentes y corta llamadas para proteger el servicio. |
| **Retry**                          | Reintenta fallos transitorios automáticamente.                        |
| **Fallback**                       | Devuelve un valor alternativo si la API sigue fallando.               |
| **Logging estructurado**           | Monitoriza errores y éxitos.                                          |
| **Micrometer / Actuator**          | Métricas y monitorización de uso y resiliencia.                       |

---

### 🔹 6. Flujo completo

1. Cliente solicita `/api/users/{id}`
2. Se revisa **cache** → si existe, retorna directamente
3. Llamada a WebClient → si falla:

   * Reintentos automáticos (`retryWhen`)
   * Circuit Breaker corta solicitudes repetidas
   * Si persiste el fallo → `fallbackUser` devuelve valor de respaldo
4. Éxito → respuesta cacheada y enviada al cliente
5. Métricas y logs quedan disponibles vía **Actuator**










### 🔹 Escenario

El microservicio obtiene información completa de un usuario desde **3 APIs externas**:

1. `/users/{id}` → datos básicos (nombre, email)
2. `/user-profile/{id}` → perfil extendido (edad, país, rol)
3. `/user-history/{id}` → historial de actividad

El objetivo:

* Integrar los datos en un solo DTO (`UserDTO`)
* Aplicar **retry** y **circuit breaker** a cada llamada
* Usar **fallback** si alguna API falla
* Cachear el resultado final
* Flujo totalmente **reactivo**

---

### 🔹 Diagrama extendido: múltiples APIs + fallback + cache

```
          ┌─────────────────────────────┐
          │       Cliente / Frontend     │
          └─────────────┬──────────────┘
                        │
                        ▼
          ┌─────────────────────────────┐
          │   Servicio Spring Boot API   │
          │  (WebFlux + ExternalApiSvc) │
          └─────────────┬──────────────┘
                        │
          ┌─────────────┴───────────────┐
          │                              │
          ▼                              ▼
 ┌─────────────────┐              ┌─────────────────────────┐
 │ Cache (Caffeine)│              │ Llamadas WebClient      │
 │ TTL 10 min      │              │ a APIs externas         │
 └───────┬─────────┘              └─────────┬───────────────┘
         │ Cache hit?                      │
         │ Yes                             │ No
         ▼                                 ▼
   ┌───────────────┐             ┌───────────────────────────┐
   │ Retorna valor │             │ Mono.zip / Flux.merge     │
   │ cacheado      │             │ Combina:                  │
   └───────────────┘             │ 1. Basic info             │
                                 │ 2. Profile info           │
                                 │ 3. History info           │
                                 └─────────┬─────────────────┘
                                           │
                                ┌──────────▼────────────┐
                                │ RetryWhen + Backoff    │
                                │ (cada API)             │
                                └──────────┬────────────┘
                                           │
                                ┌──────────▼────────────┐
                                │ Circuit Breaker        │
                                │ (Resilience4j)         │
                                └──────────┬────────────┘
                                           │
                                ┌──────────▼────────────┐
                                │ Fallbacks              │
                                │ (valor por defecto /   │
                                │ cache previo)          │
                                └──────────┬────────────┘
                                           │
                                ┌──────────▼────────────┐
                                │ Combinar datos finales │
                                │ UserDTO completo       │
                                └──────────┬────────────┘
                                           │
                                ┌──────────▼────────────┐
                                │ Guardar en Cache       │
                                │ TTL 10 min             │
                                └──────────┬────────────┘
                                           │
                                ┌──────────▼────────────┐
                                │ Retornar al Cliente    │
                                │ (Mono<UserDTO>)        │
                                └───────────────────────┘
```

---

### 🔹 Flujo explicado

1. **Cache**: si el usuario ya fue consultado, retornamos inmediatamente.
2. **WebClient** llama a las 3 APIs externas de forma paralela usando `Mono.zip` o `Flux.merge`.
3. Cada API tiene:

   * `retryWhen` → reintentos automáticos
   * `CircuitBreaker` → corta llamadas si la API falla repetidamente
   * `Fallback` → valor por defecto o datos cacheados
4. Los resultados de todas las APIs se combinan en un **`UserDTO` completo**.
5. Se guarda el resultado combinado en cache para futuras consultas.
6. Se retorna un **Mono reactivo** al cliente, garantizando respuesta incluso si alguna API falla.

---

### 🔹 Beneficios de esta arquitectura

* ✅ Integración de múltiples fuentes de datos
* ✅ Resiliencia completa ante fallos externos
* ✅ Cache reduce latencia y carga en APIs externas
* ✅ Reintentos automáticos y circuit breaker evitan saturación
* ✅ Fallback garantiza respuesta siempre
* ✅ Flujo totalmente reactivo, sin bloquear hilos
* ✅ Métricas y logging claros para monitorización


