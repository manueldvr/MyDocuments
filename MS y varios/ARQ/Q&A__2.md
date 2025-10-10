# Q&A 2


index

1. @Async y @Transactional
2. Manejando timeouts
3. Reintentos
4. Control avanzado de errores
5. Mejores prácticas de desarrollo
6. Arquitecturas distribuidas y patrones de integración
7. Herramientas de Mensajeria
8. conceptos y Arquitectura
9. Tipos deExchanges
10. ejemplo




## Ciclo de vida completo de los servicios

concepción hasta su puesta en producción y mantenimiento.

**Jira:** Tableros Kanban, Proyectos, Lista.


### @Async y @Transactional
	
- Habilitar @EnableAsync y @EnableTransactionManagement
-  @Async vs @Transactional

Contexto de **transacción** no propagado por defecto.  

Cuando un método anotado con `@Async` es invocado, se ejecuta en un hilo diferente 
gestionado por un **TaskExecutor**. 

Las **transacciones** en Spring están vinculadas al hilo actual a través de un **TransactionManager**. 

Si un método `@Async` llama a otro método transaccional, la transacción del hilo principal no se propaga al hilo asíncrono. Esto puede causar que las operaciones en el método asíncrono no se ejecuten dentro de una transacción, lo que podría generar inconsistencias en la base de datos si no se maneja correctamente.


**Solución:** 
Si necesitas que el método asíncrono se ejecute dentro de una transacción, 
asegúrate de anotarlo explícitamente con `@Transactional`.  
Por ejemplo:

```java
@Async
@Transactional
public void asyncMethod() {
    // Operaciones que requieren transacción
}
```
Esto asegura que el método asíncrono inicie su propia transacción en el nuevo hilo.





### Propagación de transacciones

Si un método `@Async` necesita interactuar con una transacción existente, 
ten en cuenta que la propagación por defecto de `@Transactional` es **Propagation.REQUIRED**.
 
Esto significa que, si no hay una transacción activa en el hilo asíncrono, se creará una nueva. Sin embargo, si el método asíncrono es invocado desde un contexto transaccional, no heredará la transacción del hilo principal a menos que se configure explícitamente.

> Solución: Evalúa si necesitas una propagación específica (como **Propagation.REQUIRES_NEW**) para garantizar que el método asíncrono siempre inicie una nueva transacción:

```java
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void asyncMethod() {
    // Nueva transacción en el hilo asíncrono
}
```

---

### Evitar dependencias entre métodos asíncronos y transaccionales

Si un método transaccional depende de la finalización de un método asíncrono, 
no hay garantía de que el método asíncrono haya completado su ejecución antes 
de que la transacción principal se confirme. Esto puede llevar a inconsistencias 
si el método asíncrono modifica datos que la transacción principal espera.

> Solución: Usa mecanismos como CompletableFuture o callbacks para coordinar la finalización de métodos asíncronos antes de proceder con la lógica transaccional:

```java
@Async
public CompletableFuture<Void> asyncMethod() {
    // Lógica asíncrona
    return CompletableFuture.completedFuture(null);
}

@Transactional
public void transactionalMethod() {
    CompletableFuture<Void> future = asyncMethod();
    future.join(); // Espera a que el método asíncrono termine
    // Continúa con la lógica transaccional
}
```

---

	
### Manejando timeouts
Hay varios niveles donde atacarlo:

| Nivel                     | Contexto                    | Herramienta típica                                         |
| ------------------------- | --------------------------- | ---------------------------------------------------------- |
| **Cliente HTTP**          | Llamadas a APIs REST        | `RestClient`, `RestTemplate`*, `WebClient`, `HttpClient`                  |
| **DataSource (DB)**       | Consultas SQL/JPA/Hibernate | `spring.datasource.hikari.*`                               |
| **Spring @Transactional** | Operaciones transaccionales | `@Transactional(timeout = X)`                              |
| **Métodos / Servicios**   | Código personalizado        | `CompletableFuture`, `ExecutorService`, `TimeoutException` |
| **Circuit Breaker**       | Resiliencia avanzada        | Resilience4j, Spring Cloud Circuit Breaker                 |


Ejemplo:

```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
public class ApiService {
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.ejemplo.com")
            .build();

    public Mono<String> getDatos() {
        return webClient.get()
                .uri("/endpoint")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3)) // <<---Timeout de 3 segundos <<-------
                .onErrorReturn("Timeout alcanzado");
    }
}
```
	
	
para Metodos y Servicios / código personalizado:

```java
import java.util.concurrent.*;

public class TimeoutExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> future = executor.submit(() -> {
            Thread.sleep(4000); // Simula tarea lenta
            return "Resultado";
        });

        try {
            String result = future.get(2, TimeUnit.SECONDS); // Timeout de 2s
            System.out.println(result);
        } catch (TimeoutException e) {
            System.out.println("Tarea tardó demasiado");
            future.cancel(true);
        } finally {
            executor.shutdown();
        }
    }
}
```


---
	

### Reintentos
	Resilience4j
	Spring Retry
	
	
	
### Control avanzado de errores.

agregando trazabilidad, métricas y resiliencia.


1. **Centralización del manejo de excepciones**
   Usar un único punto para capturar y procesar excepciones (con `@ControllerAdvice`)

2. **Normalización de respuestas de error**
   formato consistente:

3. **Clasificación y tipado de errores**
   Definir jerarquías de excepciones propias (`BusinessException`, `ValidationException`, `DatabaseException`, etc.) 

4. **Logging estructurado y trazabilidad**
   Registrar errores con información adicional 
   (ID de correlación, usuario, request, stacktrace) para diagnóstico rápido.

5. **Integración con observabilidad**
   Publicar métricas (con **Micrometer/Prometheus**), alertas o trazas distribuidas 
   (con OpenTelemetry).  Monitoreo y trazabilidad (usa herramientas como **Spring Actuator**)

6. **Resiliencia y fallback**
   Integrar mecanismos de reintentos, circuit breakers (con Resilience4j), y 
   colas (RabbitMQ/Kafka) para procesar errores asincrónicos.

---

## Promoviendo y aplicar las mejores prácticas de desarrollo, 
incluyendo la escritura de código limpio, la creación de pruebas unitarias y de integración, y la revisión de código.

SOLID

Monitoreo y trazabilidad (usa herramientas como **Spring Actuator**, **Prometheus**, o **Grafana**).



## Arquitecturas distribuidas y patrones de integración.

- **Desacoplamiento**: componentes (microservicios) son independientes, con propia lógica y BD (si aplica).
- **Escalabilidad**:  escalar horizontalmente (agregar más instancias) o verticalmente (más recursos por nodo).
- **Resiliencia**: Resilience4j, Spring Retry.
- **Comunicación**: Los componentes intercambian datos mediante APIs (como REST), mensajería (como Kafka o RabbitMQ) u otros protocolos.

#### Ejemplo en Spring Boot
para desarrollar aplicaciones modulares y livianas:
- **Microservicios**: Cada servicio (ej. "Usuarios", "Pedidos") se implementa como una aplicación Spring Boot independiente con su propia base de datos.
- **Spring Cloud**: Proporciona herramientas para gestionar configuraciones distribuidas (Spring Cloud Config), descubrimiento de servicios (Eureka), balanceo de carga (Ribbon), y tolerancia a fallos (Resilience4j).


<br>

<br>

<br>

---

<br>

<br>

## Herramientas de mensajería JMS , RabbitMQ

Coordinando flujos de mensajería (sincronía y asincronía)

### Performance y resiliencia en entornos de alta disponibilidad.

**Casos de uso:**

* Procesamiento de trabajos en background.
* Integración entre microservicios.
* Sistemas de notificaciones.
* Comunicación en tiempo real (con colas duraderas).

---

## 🔹 2. Conceptos clave

| Concepto        | Descripción                                                                  |
| --------------- | ---------------------------------------------------------------------------- |
| **Producer**    | Publica mensajes en RabbitMQ.                                                |
| **Consumer**    | Recibe y procesa mensajes de una cola.                                       |
| **Queue**       | Cola donde se almacenan mensajes hasta que sean consumidos.                  |
| **Exchange**    | Encaminador que recibe mensajes y los envía a colas según reglas (bindings). |
| **Binding**     | Relación entre una cola y un exchange.                                       |
| **Routing Key** | Clave usada por el exchange para decidir a qué cola enviar.                  |
| **VHost**       | Espacio lógico dentro de RabbitMQ para aislar configuraciones.               |

---

## 🔹 3. Arquitectura básica

1. El **Producer** envía mensajes a un **Exchange**.
2. El **Exchange** decide a qué **Queue(s)** enviar el mensaje.
3. Los **Consumers** escuchan las colas y procesan los mensajes.

---

## 🔹 4. Tipos de Exchanges

| Tipo        | Descripción                                                         |
| ----------- | ------------------------------------------------------------------- |
| **Direct**  | Envía mensajes a una cola si la `routing key` coincide exactamente. |
| **Topic**   | Permite patrones de routing key (`user.*`, `order.#`).              |
| **Fanout**  | Envía mensajes a **todas** las colas vinculadas.                    |
| **Headers** | Usa cabeceras en lugar de routing keys.                             |




<br>

---



Uso en Spring Boot (Spring AMQP)

tiene **Spring AMQP** que simplifica todo con anotaciones.

### 📌 Dependencia en `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

### 📌 Configuración en `application.yml`

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

---

### 📌 Declaración de colas y exchange (Bean de Configuración)

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "demo.queue";
    public static final String EXCHANGE_NAME = "demo.exchange";
    public static final String ROUTING_KEY = "demo.key";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
```

---

### 📌 Producer (Envío de mensajes) - RabbitTemplate

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProducerService {

    private final RabbitTemplate rabbitTemplate;

    public ProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.ROUTING_KEY,
                message
        );
        System.out.println("Mensaje enviado: " + message);
    }
}
```

---

### 📌 Consumer (Recepción de mensajes)

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receiveMessage(String message) {
        System.out.println("Mensaje recibido: " + message);
    }
}
```

<br>

---


1. **Optimización para performance** (alto throughput, baja latencia).

* Cache de **canales** → menos overhead de conexiones.
* **Consumidores concurrentes** → varios threads procesando mensajes.
* **Prefetch ajustado** → optimiza cuántos mensajes toma cada consumidor.
* **Batch processing** → reduce overhead de confirmaciones.



2. **Optimización para resiliencia** (alta disponibilidad, tolerancia a fallas).

Objetivo: que el sistema **no pierda mensajes y se recupere ante fallas**.
Claves: **mensajes persistentes, colas duraderas, reintentos, dead-letter queues (DLQ).**