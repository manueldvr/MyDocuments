# Clientes RESTful II





¡Perfecto! Te armo un **diagrama visual** para entender cómo se encadenan `doOnError`, `onErrorResume` y `retryWhen` en un flujo reactivo con `WebClient`.



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



---


¿Querés que ahora te haga un **mapa mental resumido** de los operadores de error 
(`doOnError`, `onErrorResume`, `onErrorReturn`, `retryWhen`) 
para tenerlo de referencia rápida?










<br>

<br>

<br>








Qué sería que devuelve un usuario de fallback? como traduces fallback?













¡Perfecto! 🚀
Te armo un **ejemplo completo y práctico** combinando:

* `WebClient` (Spring Boot 3 + Java 21)
* Retry automático (`retryWhen`)
* Fallback (valor por defecto o cache)
* Logging estructurado (`doOnError`, `doOnNext`)
* DTO para los datos

---

## 🔹 1. DTO de Usuario

```java
package com.example.demo.dto;

public class UserDTO {
    private Long id;
    private String name;
    private String email;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

---

## 🔹 2. Configuración de WebClient

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

---

## 🔹 3. Servicio con Retry + Fallback + Logging

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

---

## 🔹 5. Flujo de ejecución explicado

1. El cliente hace `GET /api/users/{id}`.
2. `WebClient` intenta obtener el usuario del servicio externo.
3. Si hay error → `doOnError` loguea el problema.
4. Se aplican hasta **3 reintentos** con `retryWhen`.
5. Si aún falla → `onErrorResume` devuelve un **usuario de fallback**.
6. `doOnSuccess` loguea que se obtuvo un resultado (real o fallback).
7. El cliente siempre recibe un `Mono<UserDTO>` JSON.

---

### 🔹 Ejemplo de respuesta final (si falla el API externo):

```json
{
  "id": 123,
  "name": "Usuario Desconocido",
  "email": "unknown@example.com"
}
```

---

✅ **Ventajas de este enfoque:**

* Resiliencia ante fallos externos
* Logging claro de errores y éxitos
* Respuesta garantizada al cliente
* Mantiene el flujo **reactivo** (no bloqueante)


<br>

<br>

<br>


---

Si querés, puedo armar **una versión extendida que combine: 
fallback + cache + múltiples endpoints externos
** para simular un escenario **real de microservicios resilientes**. 
Esto es muy útil en producción.



¡Perfecto! 🚀
Vamos a armar un **ejemplo extendido y realista** de **WebClient + fallback + cache + múltiples endpoints externos**, simulando un escenario típico de **microservicios resilientes**.

---

## 🔹 Escenario

Supongamos que tenemos un servicio que obtiene información de un usuario, pero los datos vienen de **dos APIs externas**:

1. `/users/{id}` → datos básicos del usuario (nombre, email).
2. `/user-profile/{id}` → perfil extendido (edad, país, rol).

Queremos que:

* Si alguna API falla, usamos **fallback**.
* Guardamos los resultados en **cache** temporal para no sobrecargar las APIs.
* Se aplican **reintentos** y logs estructurados.
* Se mantiene **reactividad completa** (`Mono`/`Flux`).

---

## 🔹 1. DTOs

```java
package com.example.demo.dto;

public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String country;
    private String role;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
```

---

## 🔹 2. Servicio con múltiples endpoints y cache

```java
package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExternalApiService {

    private final WebClient webClient;

    // Cache simple en memoria
    private final Map<Long, UserDTO> cache = new ConcurrentHashMap<>();

    public ExternalApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<UserDTO> getFullUser(Long id) {
        // Retornamos cache si existe
        if (cache.containsKey(id)) {
            return Mono.just(cache.get(id));
        }

        // Llamada a API de datos básicos
        Mono<UserDTO> basicMono = webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .doOnError(err -> System.err.println("❌ Error API básica: " + err.getMessage()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .onErrorResume(err -> {
                    System.out.println("⚠️ Usando fallback básico para usuario " + id);
                    UserDTO fallback = new UserDTO();
                    fallback.setId(id);
                    fallback.setName("Desconocido");
                    fallback.setEmail("unknown@example.com");
                    return Mono.just(fallback);
                });

        // Llamada a API de perfil extendido
        Mono<UserDTO> profileMono = webClient.get()
                .uri("/user-profile/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .doOnError(err -> System.err.println("❌ Error API perfil: " + err.getMessage()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .onErrorResume(err -> {
                    System.out.println("⚠️ Usando fallback perfil para usuario " + id);
                    UserDTO fallback = new UserDTO();
                    fallback.setAge(0);
                    fallback.setCountry("Desconocido");
                    fallback.setRole("Desconocido");
                    return Mono.just(fallback);
                });

        // Combinamos ambos resultados
        return Mono.zip(basicMono, profileMono)
                .map(tuple -> {
                    UserDTO basic = tuple.getT1();
                    UserDTO profile = tuple.getT2();
                    basic.setAge(profile.getAge());
                    basic.setCountry(profile.getCountry());
                    basic.setRole(profile.getRole());

                    // Guardamos en cache
                    cache.put(id, basic);

                    System.out.println("✅ Usuario combinado obtenido: " + basic.getName());
                    return basic;
                });
    }
}
```

---

## 🔹 3. Controlador reactivo

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

    @GetMapping("/api/users/full/{id}")
    public Mono<UserDTO> getFullUser(@PathVariable Long id) {
        return apiService.getFullUser(id);
    }
}
```

---

## 🔹 4. Flujo de ejecución

1. **Cliente** solicita `/api/users/full/{id}`.
2. Se verifica **cache interna**.
3. Se llama a **API básica** y **API de perfil extendido** simultáneamente.
4. Cada API tiene:

   * Logging (`doOnError`)
   * Reintentos (`retryWhen`)
   * Fallback (`onErrorResume`)
5. Se combinan ambos resultados con `Mono.zip`.
6. Se guarda el resultado combinado en cache.
7. Se devuelve el `Mono<UserDTO>` al cliente.

---

✅ **Ventajas de este enfoque:**

* Resiliencia total ante fallos de API externas
* Uso de **reintentos con backoff**
* Valores de **fallback** garantizan respuesta siempre
* Cache simple para **reducir llamadas externas**
* Mantiene **flujo reactivo no bloqueante**



<br>

<br>

<br>



---

<br>


Si querés, puedo hacer una **versión aún más avanzada** integrando 
**circuit breaker + cache con TTL + métricas con Micrometer** para 
que sea casi lista para producción.





¡Excelente! 🚀 Vamos a armar una **versión avanzada lista para producción**, integrando:

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

---

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

---

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

---

Esto es un **patrón muy usado en microservicios reactivos**: resiliencia + rendimiento + monitoreo.



<br>

<br>

<br>

---

<br>




Si querés, puedo armar **un diagrama visual final** de **todo este flujo con 
cache, retry, circuit breaker y fallback**, 
para que quede clarísimo cómo interactúan todos los componentes.




¡Genial! 🚀
Vamos a hacer un **diagrama extendido de microservicio reactivo** que combina **múltiples APIs externas** y muestra cómo se integran los datos usando `Mono.zip` o `Flux.merge`, manteniendo **cache, retry, circuit breaker y fallback**.

---

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
 │ TTL 10 min      │              │ a APIs externas        │
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

---

Si querés, puedo hacer **un ejemplo de código completo para este escenario 
extendido**, con **Mono.zip, circuit breaker, retry y fallback** integrado, listo para copiar y pegar en Spring Boot.

¿Querés que haga eso?

