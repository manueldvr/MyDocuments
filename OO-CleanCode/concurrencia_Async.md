# Tutorial Simple: Usando @Async en Spring Boot


¡Hola! En este tutorial te explicaré de manera sencilla qué es la anotación `@Async` en Spring Boot, cómo configurarla y cómo usarla con ejemplos prácticos. `@Async` es una funcionalidad poderosa para ejecutar métodos de forma asíncrona, lo que mejora el rendimiento de tu aplicación al no bloquear el hilo principal (por ejemplo, en operaciones I/O como llamadas a APIs externas o tareas pesadas).

Este tutorial asume que tienes conocimientos básicos de Java y Spring Boot. Usaremos Maven para la gestión de dependencias y Spring Boot 3.x (la versión más reciente al momento de este tutorial).

<br>
<br>

## 1. Introducción a @Async

- **¿Qué es?**: `@Async` marca un método para que se ejecute en un hilo separado, en lugar de en el hilo actual. Esto es útil para tareas que no necesitan bloquear la respuesta inmediata.
- **Beneficios**: Mejora la escalabilidad, reduce tiempos de respuesta y permite manejar concurrencia.
- **Limitaciones**:
  - El método debe ser `public` y estar en un bean gestionado por Spring (como un `@Service` o `@Component`).
  - No puedes usar `@Async` en métodos del mismo bean que lo llama (debes inyectar el bean en sí mismo o usar AOP).
  - Por defecto, usa un `SimpleAsyncTaskExecutor`, pero es recomendable configurar un `ThreadPoolTaskExecutor`.

<br>

## 2. Requisitos y Configuración Inicial

### Dependencias
Crea un proyecto Spring Boot con Maven. Agrega esta dependencia en tu `pom.xml` (viene incluida en `spring-boot-starter`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

### Estructura del Proyecto
Crea una estructura básica:
```
src/
└── main/
    ├── java/
    │   └── com/example/async/
    │       ├── AsyncApplication.java
    │       ├── service/
    │       │   └── EmailService.java
    │       └── config/
    │           └── AsyncConfig.java
    └── resources/
        └── application.properties
```

<br>


## 3. Habilitando @Async

Para activar el soporte asíncrono, agrega la anotación `@EnableAsync` en tu clase principal.

**AsyncApplication.java**:
```java
package com.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // ¡Esto habilita @Async!
public class AsyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(AsyncApplication.class, args);
    }
}
```

## 4. Ejemplo Básico: Tarea Síncrona vs. Asíncrona

Imaginemos un servicio que envía emails. Sin `@Async`, el envío bloquearía el hilo principal. Con `@Async`, se ejecuta en background.

### Servicio de Email
**EmailService.java** (en `service/`):
```java
package com.example.async.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // Método síncrono (bloquea el hilo)
    public void sendEmailSync(String to, String message) {
        System.out.println("Enviando email síncrono a " + to + "...");
        try {
            Thread.sleep(3000);  // Simula un envío lento (3 segundos)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Email síncrono enviado a " + to + "!");
    }

    // Método asíncrono (no bloquea)
    @Async
    public void sendEmailAsync(String to, String message) {
        System.out.println("Enviando email asíncrono a " + to + " en hilo: " + Thread.currentThread().getName());
        try {
            Thread.sleep(3000);  // Simula el envío
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Email asíncrono enviado a " + to + "!");
    }
}
```

### Controlador para Probar
Crea un controlador REST para invocar estos métodos.

**EmailController.java** (agrega en `controller/`):
```java
package com.example.async.controller;

import com.example.async.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/send-sync")
    public String sendSync(@RequestParam String to) {
        long start = System.currentTimeMillis();
        emailService.sendEmailSync(to, "Hola!");
        long end = System.currentTimeMillis();
        return "Email síncrono enviado en " + (end - start) + " ms";
    }

    @GetMapping("/send-async")
    public String sendAsync(@RequestParam String to) {
        long start = System.currentTimeMillis();
        emailService.sendEmailAsync(to, "Hola asíncrono!");
        long end = System.currentTimeMillis();
        return "Email asíncrono iniciado en " + (end - start) + " ms (se ejecuta en background)";
    }
}
```

### Prueba
- Ejecuta la app: `mvn spring-boot:run`.
- Llama a `/send-sync?to=usuario@example.com`: Tardará ~3 segundos.
- Llama a `/send-async?to=usuario@example.com`: Responderá inmediatamente (~0 ms), y verás el log del envío después.

**Salida esperada para async**:
```
Enviando email asíncrono a usuario@example.com en hilo: pool-1-thread-1
Email asíncrono enviado a usuario@example.com!
```

<br>

## 5. Ejemplo Práctico: Retorno de Valor con CompletableFuture

Si necesitas el resultado de la tarea asíncrona, usa `CompletableFuture` (incluido en Java 8+).

Modifica el servicio:

**EmailService.java** (agrega este método):
```java
import java.util.concurrent.CompletableFuture;

@Async
public CompletableFuture<String> sendEmailWithResult(String to, String message) {
    System.out.println("Enviando email con resultado a " + to + " en hilo: " + Thread.currentThread().getName());
    try {
        Thread.sleep(3000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    String result = "Email enviado exitosamente a " + to;
    System.out.println(result);
    return CompletableFuture.completedFuture(result);
}
```

**EmailController.java** (agrega):
```java
@GetMapping("/send-async-result")
public String sendAsyncWithResult(@RequestParam String to) {
    long start = System.currentTimeMillis();
    CompletableFuture<String> future = emailService.sendEmailWithResult(to, "Hola con resultado!");
    String result = future.get();  // Bloquea hasta que termine (usa .join() para no manejar excepciones)
    long end = System.currentTimeMillis();
    return "Resultado: " + result + " en " + (end - start) + " ms";
}
```

- Prueba `/send-async-result?to=usuario@example.com`: Tardará ~3 segundos, pero obtienes el resultado.
- Para no bloquear, usa `future.thenAccept(System.out::println);` en lugar de `get()`.

## 6. Configuración Avanzada: ThreadPool Personalizado

Por defecto, Spring crea hilos ilimitados, lo que puede saturar el sistema. Configura un pool fijo.

**AsyncConfig.java** (en `config/`):
```java
package com.example.async.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // Hilos mínimos
        executor.setMaxPoolSize(5);       // Hilos máximos
        executor.setQueueCapacity(100);   // Cola para tareas pendientes
        executor.setThreadNamePrefix("AsyncThread-");
        executor.initialize();
        return executor;
    }
}
```

- En `@Async`, especifica el pool: `@Async("taskExecutor")`.
- Actualiza los métodos en `EmailService` con `@Async("taskExecutor")`.

Esto limita los hilos a 5, con una cola para más tareas.

## 7. Consejos y Mejores Prácticas

- **Manejo de Excepciones**: Usa `@Async` con `AsyncUncaughtExceptionHandler` para capturar errores en hilos asíncronos.
- **Testing**: Usa `@SpringBootTest` y `Mockito` para probar servicios asíncronos (verifica con `CompletableFuture`).
- **Monitoreo**: Integra con Actuator para métricas de threads.
- **Cuándo usarlo**: Para tareas no críticas como logging, notificaciones o procesamiento batch. No para transacciones ACID.

## Conclusión

¡Listo! Con este tutorial básico, ya puedes implementar `@Async` en tu app Spring Boot. Prueba los ejemplos en tu IDE (como IntelliJ o Eclipse) y experimenta variando los tiempos de sleep para ver la diferencia. Si tienes dudas o quieres expandir a temas como `@Cacheable` con async, ¡pregúntame!











<br>

<br>
<br>
<br>

---

<br>
<br>



















# Monitoreo de @Async en Spring Boot: Opciones Avanzadas

El monitoreo de tareas asíncronas es clave para detectar cuellos de botella, como hilos saturados o tareas fallidas.  
En el tutorial anterior mencioné **Spring Boot Actuator** como punto de partida, pero veamos cómo implementarlo brevemente y luego exploremos alternativas. Usaré ejemplos prácticos para que puedas probarlos rápidamente.

<br>

## 1. Monitoreo con Spring Boot Actuator (Recordatorio Rápido)

Actuator expone endpoints para métricas del JVM, incluyendo threads. Es nativo de Spring Boot y se integra con Micrometer para métricas detalladas.

### Configuración Básica
Agrega la dependencia en `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

En `application.properties`:
```properties
management.endpoints.web.exposure.include=health,metrics,threads
management.endpoint.health.show-details=always
```

### Ejemplo: Monitoreo de Threads Asíncronos
- Ejecuta tu app y accede a:
  - `http://localhost:8080/actuator/metrics/jvm.threads.live`: Muestra threads vivos (incluyendo los de tu pool asíncrono).
  - `http://localhost:8080/actuator/threaddump`: Dump completo de threads para ver qué hace cada hilo (busca "AsyncThread-" si usas el pool personalizado).

**Salida de ejemplo (en JSON)**:
```json
{
  "name": "jvm.threads.live",
  "description": "The current number of live threads including both daemon and non-daemon threads",
  "baseUnit": "threads",
  "measurements": [{"statistic": "VALUE", "value": 12.0}]
}
```

Para métricas específicas de @Async, usa Micrometer en tu `AsyncConfig`:
```java
// En AsyncConfig.java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // ... configuración anterior
    executor.setRejectedExecutionHandler((r, e) -> {
        // Registra rechazo como métrica
        MeterRegistry registry = ...; // Inyecta MeterRegistry
        Counter.builder("async.rejected.tasks").register(registry).increment();
        throw new RejectedExecutionException("Tarea rechazada");
    });
    return executor;
}
```

Esto te da contadores personalizados en `/actuator/metrics/async.rejected.tasks`.

<br>

## 2. Opciones de Monitoreo Adicionales

Además de Actuator, hay herramientas que van desde gratuitas y open-source hasta comerciales. Elegí las más relevantes para @Async (enfocadas en threads, latencia y throughput). Usaré una tabla para compararlas:

| Opción                  | Descripción                                                                 | Pros                                                                 | Contras                                                              | Integración con Spring Boot                          | Ejemplo Práctico |
|-------------------------|-----------------------------------------------------------------------------|----------------------------------------------------------------------|----------------------------------------------------------------------|-----------------------------------------------------|------------------|
| **Micrometer + Prometheus/Grafana** | Micrometer (base de Actuator) exporta métricas a Prometheus, que Grafana visualiza en dashboards. Ideal para métricas de pools de threads (e.g., active/idle threads). | Gratuito, escalable, visualizaciones ricas (gráficos de tiempo real). | Curva de aprendizaje para setup; requiere contenedores (Docker).     | Agrega `micrometer-registry-prometheus` y expón `/actuator/prometheus`. | Instala Prometheus/Grafana via Docker. Query: `spring_task_executor_pool_size{name="taskExecutor"}`. Dashboard: Gráfico de threads activos. |
| **Zipkin o Jaeger (Distributed Tracing)** | Rastrea llamadas asíncronas a través de servicios (útil si tu @Async invoca APIs). Registra spans para latencia por hilo. | Excelente para debugging de flujos asíncronos; soporta sampling.     | Enfocado en tracing, no en métricas puras; overhead en logs.         | Usa `spring-cloud-sleuth-zipkin` o OpenTelemetry. Anota métodos @Async con `@NewSpan`. | Dependencia: `spring-cloud-starter-zipkin`. Accede a `http://localhost:9411` para ver traces de `sendEmailAsync`. |
| **ELK Stack (Elasticsearch + Logstash + Kibana)** | Monitorea logs de tareas @Async (e.g., tiempos de ejecución) y los indexa para búsquedas/visualizaciones. | Potente para logs estructurados; alertas en tiempo real.              | Pesado para setups locales; no nativo para métricas numéricas.       | Configura Logback con Logstash appender. Loggea en @Async: `log.info("Tarea completada en {} ms", duration);`. | Docker-compose para ELK. Query en Kibana: "AsyncThread-* AND duration > 2000ms" para alertar tareas lentas. |
| **JMX (Java Management Extensions)** | Expone MBeans para monitorear pools de threads via herramientas como JConsole o VisualVM. | Nativo de Java, sin dependencias extras; bajo overhead.               | Interfaz gráfica básica; no escalable para producción.               | Habilita JMX en `application.properties`: `spring.jmx.enabled=true`. Monitorea `java.lang:type=Threading`. | Abre JConsole, conecta a tu app y ve "Threads" > "Thread Count" para hilos asíncronos. |
| **Datadog o New Relic (APM Comerciales)** | Plataformas APM que monitorean threads, latencia y errores en @Async con dashboards automáticos y alertas. | Fácil setup, IA para anomalías; integra con Kubernetes.               | Costo por host/ingesta; vendor lock-in.                              | Agrega agente (e.g., `dd-java-agent` para Datadog). Auto-detecta métricas de Spring. | En Datadog: Instala agente, ve dashboard "APM > Traces" para spans de @Async. Alerta: "Thread pool > 80%". |




### Recomendación
- **Para principiantes**: Empieza con **Micrometer + Prometheus/Grafana** (gratuito y visual).
- **Para producción**: Combina Actuator con tracing (Zipkin) si tu app es distribuida.
- **Setup Rápido para Prometheus**:
  1. Agrega dependencia: `spring-boot-starter-actuator` + `micrometer-registry-prometheus`.
  2. En `application.properties`: `management.metrics.export.prometheus.enabled=true`.
  3. Docker: `docker run -p 9090:9090 prom/prometheus` con config para scrapear `/actuator/prometheus`.
  4. En Grafana (puerto 3000), agrega Prometheus como data source y crea un panel: "Rate de tareas completadas" con query `rate(spring_task_executor_completed_task_count[5m])`.

<br>

## 3. Ejemplo Práctico: Integrando Micrometer para Métricas de @Async

Modifica tu `EmailService` para registrar métricas:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class EmailService {
    private final MeterRegistry meterRegistry;

    public EmailService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Async("taskExecutor")
    public void sendEmailAsync(String to, String message) {
        Timer.Sample sample = Timer.start(meterRegistry);  // Inicia timer
        // ... lógica de envío
        sample.stop(Timer.builder("async.email.duration")
                .tag("recipient", to)
                .register(meterRegistry));
    }
}
```



- Accede a `/actuator/metrics/async.email.duration`: Verás percentiles de duración (e.g., 95% < 3s).
- En Grafana: Gráfico de latencia por tag.

¡Con esto, tu monitoreo de @Async estará sólido! Si quieres un ejemplo completo de setup para una herramienta específica (e.g., Grafana dashboard JSON), dime. ¿Qué opción te interesa probar primero? 😊
