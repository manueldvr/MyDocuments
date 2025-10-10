# Clientes RESTful III

<br>

Ejemplo Completo


con **ejemplo completo en Spring Boot 3 / Java 21** para un microservicio **reactivo** que:

* Consume **múltiples APIs externas** (`/users`, `/user-profile`, `/user-history`)
* Integra datos en un **UserDTO** con `Mono.zip`
* Aplica **retry + circuit breaker + fallback**
* Usa **cache con TTL**
* Expone **métricas con Micrometer / Actuator**



<br>

<br>

---


## 🔹 1. Dependencias Maven


dependencias:

- spring-boot-starter-webflux
- resilience4j-spring-boot3
- caffeine
- spring-boot-starter-actuator
- micrometer-registry-prometheus

pom:

```xml
<dependencies>
    <!-- Spring WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Resilience4j (Circuit Breaker) -->
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

    <!-- Actuator + Micrometer -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer Prometheus (opcional) -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
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
                .baseUrl("https://jsonplaceholder.typicode.com") // Simulando API externa
                .build();
    }

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<Long, Object> userCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }
}
```

---

## 🔹 3. DTO de Usuario

```java
package com.example.demo.dto;

public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String country;
    private String role;
    private String lastActivity;

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

    public String getLastActivity() { return lastActivity; }
    public void setLastActivity(String lastActivity) { this.lastActivity = lastActivity; }
}
```

---

## 🔹 4. Servicio con múltiples APIs, cache, fallback y circuit breaker

```java
package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final Timer userRequestTimer;

    public ExternalApiService(WebClient webClient, Cache<Long, UserDTO> userCache, MeterRegistry meterRegistry) {
        this.webClient = webClient;
        this.userCache = userCache;
        this.userRequestTimer = meterRegistry.timer("user.requests.timer");
    }

    @CircuitBreaker(name = "userServiceCircuit", fallbackMethod = "fallbackUser")
    public Mono<UserDTO> getFullUser(Long id) {
        UserDTO cached = (UserDTO) userCache.getIfPresent(id);
        if (cached != null) {
            return Mono.just(cached);
        }

        return userRequestTimer.record(() ->
            Mono.zip(
                    fetchBasicUser(id),
                    fetchProfileUser(id),
                    fetchUserHistory(id)
            ).map(tuple -> {
                UserDTO basic = tuple.getT1();
                UserDTO profile = tuple.getT2();
                UserDTO history = tuple.getT3();

                basic.setAge(profile.getAge());
                basic.setCountry(profile.getCountry());
                basic.setRole(profile.getRole());
                basic.setLastActivity(history.getLastActivity());

                userCache.put(id, basic); // Cache
                return basic;
            })
        );
    }

    private Mono<UserDTO> fetchBasicUser(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .doOnError(err -> System.err.println("❌ Error API básica: " + err.getMessage()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .onErrorResume(err -> {
                    UserDTO fallback = new UserDTO();
                    fallback.setId(id);
                    fallback.setName("Desconocido");
                    fallback.setEmail("unknown@example.com");
                    return Mono.just(fallback);
                });
    }

    private Mono<UserDTO> fetchProfileUser(Long id) {
        return webClient.get()
                .uri("/user-profile/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .doOnError(err -> System.err.println("❌ Error API perfil: " + err.getMessage()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .onErrorResume(err -> {
                    UserDTO fallback = new UserDTO();
                    fallback.setAge(0);
                    fallback.setCountry("Desconocido");
                    fallback.setRole("Desconocido");
                    return Mono.just(fallback);
                });
    }

    private Mono<UserDTO> fetchUserHistory(Long id) {
        return webClient.get()
                .uri("/user-history/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .doOnError(err -> System.err.println("❌ Error API historial: " + err.getMessage()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                .onErrorResume(err -> {
                    UserDTO fallback = new UserDTO();
                    fallback.setLastActivity("No disponible");
                    return Mono.just(fallback);
                });
    }

    // Fallback general si el Circuit Breaker corta
    public Mono<UserDTO> fallbackUser(Long id, Throwable t) {
        System.out.println("⚠️ Fallback activado para usuario " + id + ": " + t.getMessage());
        UserDTO fallback = new UserDTO();
        fallback.setId(id);
        fallback.setName("Usuario Desconocido");
        fallback.setEmail("unknown@example.com");
        fallback.setAge(0);
        fallback.setCountry("Desconocido");
        fallback.setRole("Desconocido");
        fallback.setLastActivity("No disponible");
        return Mono.just(fallback);
    }
}
```

---

## 🔹 5. Controlador reactivo

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

## 🔹 6. Métricas con Micrometer / Actuator

* **Timer**: `user.requests.timer` mide la latencia de la integración de APIs.
* **Actuator endpoint**: `/actuator/metrics` o `/actuator/prometheus` si usas Prometheus.
* Permite medir:

| Métrica                              | Descripción                                      |
| ------------------------------------ | ------------------------------------------------ |
| `user.requests.timer.count`          | Cantidad de peticiones a `/api/users/full/{id}`  |
| `user.requests.timer.mean`           | Latencia promedio de combinación de APIs         |
| `resilience4j.circuitbreaker.calls`  | Llamadas totales al circuito                     |
| `resilience4j.circuitbreaker.failed` | Llamadas fallidas detectadas por circuit breaker |

---

### 🔹 Flujo completo del microservicio

1. Cliente solicita `/api/users/full/{id}`
2. Verifica **cache** → devuelve si existe
3. Llama **3 APIs externas en paralelo** (`Mono.zip`)
4. Cada API aplica:

   * **RetryWhen** → reintentos automáticos
   * **CircuitBreaker** → corta llamadas si falla frecuentemente
   * **Fallback** → valor por defecto si persiste el fallo
5. Se **combina el resultado** en un DTO único
6. Se **guarda en cache**
7. Se mide latencia con **Micrometer Timer**
8. Se retorna el `Mono<UserDTO>` al cliente

---

💡 **Beneficios**:

* Resiliencia total ante fallos externos
* Flujo reactivo no bloqueante
* Métricas observables en producción
* Cache reduce latencia y carga en APIs externas
* Respuesta garantizada incluso si alguna API falla

---

Si querés, puedo hacer un **diagrama visual final** de este **flujo completo con múltiples APIs, cache, retry, circuit breaker, fallback y métricas**. Esto resumiría todo lo que hemos hablado.

¿Querés que haga ese diagrama?



