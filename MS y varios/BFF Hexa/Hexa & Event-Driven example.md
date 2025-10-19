# Hexa & Event-Driven example






**ejemplo completo y moderno** en **Spring Boot (Java 21)** que combine los **patrones Hexagonal + Event-Driven**, como lo usaría un **Senior Developer** en un microservicio real.

---

# Caso de uso: 

#### *Order → Payment → Notification*


## Objetivo

Modelar un microservicio que:

1. **Recibe una orden de compra.**
2. Publica un **evento de dominio** `OrderCreatedEvent`.
3. Un *listener* reacciona al evento y procesa el pago.
4. Cuando el pago se confirma, se dispara un **evento** `PaymentConfirmedEvent` que genera una notificación.

Todo **sin dependencias directas entre capas ni servicios** — el dominio no sabe de infraestructuras externas (DB, correo, etc.).

<br>


## Estructura Hexagonal

```
src/main/java/com/example/order
 ├── application/
 │    └── usecase/
 │         ├── CreateOrderUseCase.java
 │         └── ProcessPaymentUseCase.java
 ├── domain/
 │    ├── model/
 │    │    └── Order.java
 │    ├── event/
 │    │    ├── OrderCreatedEvent.java
 │    │    └── PaymentConfirmedEvent.java
 │    └── port/
 │         ├── in/
 │         │    └── OrderCommand.java
 │         └── out/
 │              └── PaymentProcessor.java
 ├── infrastructure/
 │    ├── adapter/
 │    │    ├── PaymentProcessorKafkaAdapter.java
 │    │    └── NotificationEmailAdapter.java
 │    └── listener/
 │         ├── OrderCreatedListener.java
 │         └── PaymentConfirmedListener.java
 └── OrderApplication.java
```

---

## 1. Dominio

### Entidad

```java
// domain/model/Order.java
package com.example.order.domain.model;

import java.util.UUID;

public record Order(UUID id, String customerEmail, double amount) {}
```

---

###  Eventos de dominio

```java
// domain/event/OrderCreatedEvent.java
package com.example.order.domain.event;

import com.example.order.domain.model.Order;

public record OrderCreatedEvent(Order order) {}
```

```java
// domain/event/PaymentConfirmedEvent.java
package com.example.order.domain.event;

import com.example.order.domain.model.Order;

public record PaymentConfirmedEvent(Order order) {}
```

<br>



###  Puertos de entrada / salida

```java
// domain/port/in/OrderCommand.java
package com.example.order.domain.port.in;

import com.example.order.domain.model.Order;

public interface OrderCommand {
    Order createOrder(String customerEmail, double amount);
}
```

```java
// domain/port/out/PaymentProcessor.java
package com.example.order.domain.port.out;

import com.example.order.domain.model.Order;

public interface PaymentProcessor {
    void processPayment(Order order);
}
```

<br>
<br>

## 2. Capa de aplicación (Casos de uso)

```java
// application/usecase/CreateOrderUseCase.java
package com.example.order.application.usecase;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.in.OrderCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreateOrderUseCase implements OrderCommand {

    private final ApplicationEventPublisher publisher;

    public CreateOrderUseCase(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Order createOrder(String customerEmail, double amount) {
        Order order = new Order(UUID.randomUUID(), customerEmail, amount);
        publisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}
```

<br>
<br>



## 3. Adaptadores de infraestructura (salida)

### 🔹 Procesador de pagos (ejemplo: simula integración con Kafka)

```java
// infrastructure/adapter/PaymentProcessorKafkaAdapter.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorKafkaAdapter implements PaymentProcessor {
    @Override
    public void processPayment(Order order) {
        System.out.println("[Kafka] Procesando pago para orden: " + order.id());
        // lógica simulada...
    }
}
```

<br>

### 🔹 Envío de notificaciones (por email)

```java
// infrastructure/adapter/NotificationEmailAdapter.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailAdapter {
    public void sendPaymentConfirmation(Order order) {
        System.out.println("📧 Email enviado a " + order.customerEmail() +
                           " por pago confirmado de $" + order.amount());
    }
}
```

<br>
<br>

##  4. Listeners (Event-Driven)

###  Escucha `OrderCreatedEvent` y dispara procesamiento de pago

```java
// infrastructure/listener/OrderCreatedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final PaymentProcessor paymentProcessor;
    private final ApplicationEventPublisher publisher;

    public OrderCreatedListener(PaymentProcessor paymentProcessor, ApplicationEventPublisher publisher) {
        this.paymentProcessor = paymentProcessor;
        this.publisher = publisher;
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        paymentProcessor.processPayment(event.order());
        publisher.publishEvent(new PaymentConfirmedEvent(event.order()));
    }
}
```

<br>


###   Escucha `PaymentConfirmedEvent` y envía notificación
 
```java
// infrastructure/listener/PaymentConfirmedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.infrastructure.adapter.NotificationEmailAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmedListener {

    private final NotificationEmailAdapter notifier;

    public PaymentConfirmedListener(NotificationEmailAdapter notifier) {
        this.notifier = notifier;
    }

    @EventListener
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        notifier.sendPaymentConfirmation(event.order());
    }
}
```

<br>
<br>


## 5. Controlador (puerto de entrada HTTP)

```java
// infrastructure/controller/OrderController.java
package com.example.order.infrastructure.controller;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.in.OrderCommand;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderCommand orderCommand;

    public OrderController(OrderCommand orderCommand) {
        this.orderCommand = orderCommand;
    }

    @PostMapping
    public Order create(@RequestParam String email, @RequestParam double amount) {
        return orderCommand.createOrder(email, amount);
    }
}
```

<br>


##  Flujo final

```
POST /orders?email=ana@example.com&amount=200.0
      ↓
CreateOrderUseCase → publica OrderCreatedEvent
      ↓
OrderCreatedListener → procesa pago (Kafka) → publica PaymentConfirmedEvent
      ↓
PaymentConfirmedListener → envía email de confirmación
```

<br>


## Beneficios


| Patrón                  | Beneficio                                                   |
| ----------------------- | ----------------------------------------------------------- |
| **Hexagonal**           | Separa dominio de infraestructura (fácil test, mantenible)  |
| **Event-Driven**        | Desacopla flujo de negocio (no hay llamadas directas)       |
| **DDD-style Use Cases** | Casos de uso explícitos, centrados en el dominio            |
| **Alta extensibilidad** | Puedes añadir más listeners o adaptadores sin tocar el core |




<br>

<br>

<br>
<br>
<br>





# **Extender el ejemplo anterior** agregando:

1.  **Persistencia con Spring Data JPA** (base de datos relacional).
2.  **Test de integración** que verifica el flujo completo con eventos (`OrderCreated → PaymentConfirmed`).

Así pasamos de un ejemplo teórico a uno **completamente funcional**, como en un microservicio real.

<br>


# Contexto

Continuamos con el microservicio **Order → Payment → Notification**, manteniendo la **arquitectura hexagonal + event-driven**.

<br>


## 1. Dominio (con persistencia)

### Entidad `Order`

Agregamos anotaciones JPA, pero **sin romper el aislamiento del dominio**.

```java
// domain/model/Order.java
package com.example.order.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    private String customerEmail;
    private double amount;
    private boolean paid;

    protected Order() {} // constructor JPA

    public Order(String customerEmail, double amount) {
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.paid = false;
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return paid; }

    public void markAsPaid() { this.paid = true; }
}
```

<br>


## 2. Puerto de salida: repositorio

```java
// domain/port/out/OrderRepository.java
package com.example.order.domain.port.out;

import com.example.order.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
}
```

<br>


## 3. Adaptador de persistencia (infraestructura)

Usamos Spring Data JPA como implementación del puerto.

```java
// infrastructure/adapter/OrderJpaRepository.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {}
```

```java
// infrastructure/adapter/OrderRepositoryAdapter.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id);
    }
}
```

<br>


## 4. Caso de uso actualizado: `CreateOrderUseCase`

Ahora guarda la orden en la base de datos antes de publicar el evento.

```java
// application/usecase/CreateOrderUseCase.java
package com.example.order.application.usecase;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.in.OrderCommand;
import com.example.order.domain.port.out.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CreateOrderUseCase implements OrderCommand {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public CreateOrderUseCase(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public Order createOrder(String customerEmail, double amount) {
        Order order = repository.save(new Order(customerEmail, amount));
        publisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}
```

<br>

<br>



## 5. Listener de pago con persistencia

Actualiza la orden a `paid = true` y vuelve a guardarla.

```java
// infrastructure/listener/OrderCreatedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.OrderRepository;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final PaymentProcessor paymentProcessor;
    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderCreatedListener(PaymentProcessor paymentProcessor, OrderRepository repository, ApplicationEventPublisher publisher) {
        this.paymentProcessor = paymentProcessor;
        this.repository = repository;
        this.publisher = publisher;
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        Order order = event.order();
        paymentProcessor.processPayment(order);
        order.markAsPaid();
        repository.save(order);
        publisher.publishEvent(new PaymentConfirmedEvent(order));
    }
}
```

<br>



## 6. Configuración `application.yml`

Ejemplo usando **H2 en memoria** para pruebas e integración:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

<br>


## 7. Test de integración con eventos

Verifica que el flujo *Order → Payment → Notification* se complete correctamente.

```java
// test/java/com/example/order/OrderIntegrationTest.java
package com.example.order;

import com.example.order.domain.model.Order;
import com.example.order.infrastructure.adapter.OrderJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @Test
    void whenOrderCreated_thenPaymentProcessedAndSavedAsPaid() throws Exception {
        // given
        Order order = new Order("ana@example.com", 150.0);
        jpaRepository.save(order);

        // when
        publisher.publishEvent(new com.example.order.domain.event.OrderCreatedEvent(order));
        Thread.sleep(500); // esperar propagación asíncrona de eventos

        // then
        Order updated = jpaRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.isPaid()).isTrue();
    }
}
```

<br>
<br>


## Flujo final con persistencia

```
POST /orders?email=ana@example.com&amount=150
   ↓
CreateOrderUseCase guarda orden (paid=false)
   ↓
Publica OrderCreatedEvent
   ↓
OrderCreatedListener procesa pago + marca paid=true + guarda
   ↓
Publica PaymentConfirmedEvent
   ↓
PaymentConfirmedListener envía notificación
```

<br>

<br>


## Conclusiones

| Patrón                           | Aporta                                                |
| -------------------------------- | ----------------------------------------------------- |
| **Hexagonal (Ports & Adapters)** | Aisla dominio, infraestructura intercambiable         |
| **Event-Driven (Observer)**      | Desacopla flujos internos (sin dependencias directas) |
| **Spring Data Adapter**          | Implementa repositorios sin romper la abstracción     |
| **Testing de Integración**       | Verifica comportamiento real entre capas              |




<br>

<br>
<br>

<br>





Con **transacciones y eventos asíncronos (`@Async` + `@TransactionalEventListener`)** para simular un escenario productivo más realista (por ejemplo, notificación enviada *después de commit*)

<br>





Perfecto — vamos a **convertir el flujo en uno listo para producción** con:

* **Eventos ejecutados *después del commit*** (para evitar efectos secundarios si el `save` falla/retrocede).
* **Listeners asíncronos** (para no bloquear el hilo de request).
* **Transacciones bien definidas** (cuando sea necesario usar `REQUIRES_NEW` para operaciones de compensación/side-effects).

Te muestro el **diseño** + **código** (Java 21, Spring Boot 3.x), y al final un **test de integración** que usa Awaitility para esperar la ejecución asíncrona sin `Thread.sleep`.

<br>

<br>

# 1) Idea clave

* Guardamos la orden dentro de una transacción (`@Transactional`).
* Publicamos `OrderCreatedEvent` (con `ApplicationEventPublisher`) dentro de esa transacción.
* Un listener marcado con `@TransactionalEventListener(phase = AFTER_COMMIT)` **no** ejecutará hasta que la transacción que publicó el evento haya sido *commit*eada.
* Marcamos el listener como `@Async` para que corra en un hilo separado.
* Si el listener necesita actualizar la base (marcar `paid=true`) lo hacemos en una **transacción nueva** (`@Transactional(propagation = Propagation.REQUIRES_NEW)`), así la actualización no queda ligada a fallos posteriores en el listener.

<br>

<br>

# 2) Configuración: habilitar `@Async` y definir Executor

```java
// config/AsyncConfig.java
package com.example.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(20);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("async-exec-");
        exec.initialize();
        return exec;
    }
}
```

> `@EnableAsync` permite usar `@Async`. El `Bean` llamado `taskExecutor` se usará por defecto por Spring para `@Async`. Ajusta los parámetros según carga.

<br>

<br>

# 3) Publicador (caso de uso) — transaccional

```java
// application/usecase/CreateOrderUseCase.java
package com.example.order.application.usecase;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.in.OrderCommand;
import com.example.order.domain.port.out.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOrderUseCase implements OrderCommand {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public CreateOrderUseCase(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public Order createOrder(String customerEmail, double amount) {
        Order order = repository.save(new Order(customerEmail, amount));
        // Publicado dentro de la transacción: listeners con AFTER_COMMIT se ejecutan sólo si la tx hace commit
        publisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}
```

<br>

<br>


# 4) Listener de `OrderCreatedEvent` — AFTER_COMMIT + ASYNC + nueva transacción para marcar `paid`

```java
// infrastructure/listener/OrderCreatedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.OrderRepository;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedListener {

    private final PaymentProcessor paymentProcessor;
    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderCreatedListener(PaymentProcessor paymentProcessor,
                                OrderRepository repository,
                                ApplicationEventPublisher publisher) {
        this.paymentProcessor = paymentProcessor;
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Se ejecutará AFTER_COMMIT (solo si la tx que publicó el evento hizo commit).
     * @Async -> se ejecuta en un hilo del taskExecutor.
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.order();

        // 1) Procesar pago (call externo simulada)
        paymentProcessor.processPayment(order);

        // 2) Guardar marca de pago en una transacción nueva para aislar el commit del update
        markOrderAsPaidInNewTx(order);

        // 3) Publicar evento de pago confirmado
        publisher.publishEvent(new PaymentConfirmedEvent(order));
    }

    /**
     * Guardar el cambio de estado en una transacción separada (REQUIRES_NEW)
     * para asegurar que esta actualización no dependa de la tx original.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderAsPaidInNewTx(Order order) {
        // cargar entidad gestionada (puede re-lectura para obtener ID real)
        repository.findById(order.getId()).ifPresent(o -> {
            o.markAsPaid();
            repository.save(o);
        });
    }
}
```
<br>

<br>

**Notas importantes:**

* `@TransactionalEventListener(phase = AFTER_COMMIT)` garantiza que el `handleOrderCreated` **no** correrá si la transacción que llamó a `publishEvent` hace rollback.
* `@Async` hace que el listener se ejecute en otro hilo. Útil para no bloquear el request HTTP.
* `markOrderAsPaidInNewTx` usa `REQUIRES_NEW` para asegurar que el `save` del `paid=true` se haga en una transacción propia. Así ese commit no depende del hilo original.


<br>

<br>


# 5) Listener de `PaymentConfirmedEvent` — enviar notificación *después del commit* y asíncrono

```java
// infrastructure/listener/PaymentConfirmedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.infrastructure.adapter.NotificationEmailAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentConfirmedListener {

    private final NotificationEmailAdapter notifier;

    public PaymentConfirmedListener(NotificationEmailAdapter notifier) {
        this.notifier = notifier;
    }

    // AFTER_COMMIT para evitar enviar correo si la tx que publica falla.
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        notifier.sendPaymentConfirmation(event.order());
    }
}
```

<br>

<br>

# 6) PaymentProcessor (puede ser un adaptador que llama a un gateway externo)

```java
// infrastructure/adapter/PaymentProcessorImpl.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorImpl implements PaymentProcessor {

    @Override
    public void processPayment(Order order) {
        // Simulación de llamada externa a gateway
        // En producción: HTTP client / SDK / reintentos / idempotency keys
        System.out.println("Procesando pago para orderId=" + order.getId() + ", amount=" + order.getAmount());
        // Si se lanza excepción, el listener fallará — decida si debe reintentar o compensar.
    }
}
```

<br>

<br>

# 7) Notifier (adaptador para enviar email)

```java
// infrastructure/adapter/NotificationEmailAdapter.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailAdapter {
    public void sendPaymentConfirmation(Order order) {
        System.out.println("📧 Enviando email a " + order.getCustomerEmail() + " por pago de $" + order.getAmount());
        // Integrar con JavaMailSender, SES, Sendgrid, etc.
    }
}
```

<br>

<br>

# 8) Test de integración (espera la ejecución asíncrona con Awaitility)

Agrega dependencia Maven para Awaitility:

```xml
<dependency>
  <groupId>org.awaitility</groupId>
  <artifactId>awaitility</artifactId>
  <version>4.2.0</version>
  <scope>test</scope>
</dependency>
```

Test:

```java
// test/java/com/example/order/OrderTransactionalEventTest.java
package com.example.order;

import com.example.order.domain.model.Order;
import com.example.order.infrastructure.adapter.OrderJpaRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OrderTransactionalEventTest {

    @Autowired
    private com.example.order.application.usecase.CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @Test
    void whenCreateOrder_thenPaymentProcessedAndMarkedPaid() {
        // given
        Order order = createOrderUseCase.createOrder("ana@example.com", 120.0);

        // then: busy-wait hasta que el listener asíncrono marque la orden como pagada
        Awaitility.await()
                 .atMost(Duration.ofSeconds(5))
                 .pollInterval(Duration.ofMillis(200))
                 .untilAsserted(() -> {
                     Order updated = jpaRepository.findById(order.getId()).orElseThrow();
                     assertThat(updated.isPaid()).isTrue();
                 });
    }
}
```

> Usamos Awaitility para evitar `Thread.sleep` y esperar a que la tarea asíncrona complete dentro de un timeout razonable.

<br>

<br>

# 9) Buenas prácticas y consideraciones adicionales

1. **Idempotencia**: operaciones que llaman a servicios externos (pago, email) deben ser **idempotentes** o tener keys de idempotencia para evitar efectos duplicados si se reintenta.
2. **Retries y backoff**: usa `Resilience4j` o Spring Retry en adaptadores para llamadas externas.
3. **Dead-letter / fallback**: si la notificación/pago falla, enviar a una cola de fallback o guardar evento para reintentos manuales.
4. **Observabilidad**: instrumentar con trazas (OpenTelemetry) para seguir el flujo entre el request, evento y listeners asíncronos.
5. **Transactional boundaries**: publicar eventos dentro de la transacción garantiza coherencia; usar `AFTER_COMMIT` evita efectos secundarios si la tx falla.
6. **Errores en listeners asíncronos**: captura excepciones, registra, y decide política de reintento; si el `@Async` falla en el thread pool, por defecto pierdes la excepción sin propagación — añade lógica de retry/alerting.

<br>

<br>

¿Querés que adapte este ejemplo para que:

* el procesamiento de pago sea **idempotente** con una `paymentId`,
* o que use **mensajería (Kafka)** en vez de `ApplicationEventPublisher` para listeners distribuidos?
  Dime cuál y lo ajusto con código listo para copiar.






<br>
<br>
<br>

<br>
<br>
<br>







## **Convertir el flujo en uno listo para producción** con:



* **Eventos ejecutados *después del commit*** (para evitar efectos secundarios si el `save` falla/retrocede).
* **Listeners asíncronos** (para no bloquear el hilo de request).
* **Transacciones bien definidas** (cuando sea necesario usar `REQUIRES_NEW` para operaciones de compensación/side-effects).

Te muestro el **diseño** + **código** (Java 21, Spring Boot 3.x), y al final un **test de integración** que usa Awaitility para esperar la ejecución asíncrona sin `Thread.sleep`.

<br>
<br>

# 1) Idea clave

* Guardamos la orden dentro de una transacción (`@Transactional`).
* Publicamos `OrderCreatedEvent` (con `ApplicationEventPublisher`) dentro de esa transacción.
* Un listener marcado con `@TransactionalEventListener(phase = AFTER_COMMIT)` **no** ejecutará hasta que la transacción que publicó el evento haya sido *commit*eada.
* Marcamos el listener como `@Async` para que corra en un hilo separado.
* Si el listener necesita actualizar la base (marcar `paid=true`) lo hacemos en una **transacción nueva** (`@Transactional(propagation = Propagation.REQUIRES_NEW)`), así la actualización no queda ligada a fallos posteriores en el listener.

<br>
<br>

# 2) Configuración: habilitar `@Async` y definir Executor

```java
// config/AsyncConfig.java
package com.example.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(20);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("async-exec-");
        exec.initialize();
        return exec;
    }
}
```

> `@EnableAsync` permite usar `@Async`. El `Bean` llamado `taskExecutor` se usará por defecto por Spring para `@Async`. Ajusta los parámetros según carga.

<br>
<br>

# 3) Publicador (caso de uso) — transaccional

```java
// application/usecase/CreateOrderUseCase.java
package com.example.order.application.usecase;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.in.OrderCommand;
import com.example.order.domain.port.out.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOrderUseCase implements OrderCommand {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public CreateOrderUseCase(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public Order createOrder(String customerEmail, double amount) {
        Order order = repository.save(new Order(customerEmail, amount));
        // Publicado dentro de la transacción: listeners con AFTER_COMMIT se ejecutan sólo si la tx hace commit
        publisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}
```

<br>
<br>

# 4) Listener de `OrderCreatedEvent` — AFTER_COMMIT + ASYNC + nueva transacción para marcar `paid`

```java
// infrastructure/listener/OrderCreatedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.OrderRepository;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedListener {

    private final PaymentProcessor paymentProcessor;
    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderCreatedListener(PaymentProcessor paymentProcessor,
                                OrderRepository repository,
                                ApplicationEventPublisher publisher) {
        this.paymentProcessor = paymentProcessor;
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Se ejecutará AFTER_COMMIT (solo si la tx que publicó el evento hizo commit).
     * @Async -> se ejecuta en un hilo del taskExecutor.
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.order();

        // 1) Procesar pago (call externo simulada)
        paymentProcessor.processPayment(order);

        // 2) Guardar marca de pago en una transacción nueva para aislar el commit del update
        markOrderAsPaidInNewTx(order);

        // 3) Publicar evento de pago confirmado
        publisher.publishEvent(new PaymentConfirmedEvent(order));
    }

    /**
     * Guardar el cambio de estado en una transacción separada (REQUIRES_NEW)
     * para asegurar que esta actualización no dependa de la tx original.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderAsPaidInNewTx(Order order) {
        // cargar entidad gestionada (puede re-lectura para obtener ID real)
        repository.findById(order.getId()).ifPresent(o -> {
            o.markAsPaid();
            repository.save(o);
        });
    }
}
```

<br>
<br>

**Notas importantes:**

* `@TransactionalEventListener(phase = AFTER_COMMIT)` garantiza que el `handleOrderCreated` **no** correrá si la transacción que llamó a `publishEvent` hace rollback.
* `@Async` hace que el listener se ejecute en otro hilo. Útil para no bloquear el request HTTP.
* `markOrderAsPaidInNewTx` usa `REQUIRES_NEW` para asegurar que el `save` del `paid=true` se haga en una transacción propia. Así ese commit no depende del hilo original.

<br>
<br>

# 5) Listener de `PaymentConfirmedEvent` — enviar notificación *después del commit* y asíncrono

```java
// infrastructure/listener/PaymentConfirmedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.infrastructure.adapter.NotificationEmailAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentConfirmedListener {

    private final NotificationEmailAdapter notifier;

    public PaymentConfirmedListener(NotificationEmailAdapter notifier) {
        this.notifier = notifier;
    }

    // AFTER_COMMIT para evitar enviar correo si la tx que publica falla.
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        notifier.sendPaymentConfirmation(event.order());
    }
}
```

<br>
<br>

# 6) PaymentProcessor (puede ser un adaptador que llama a un gateway externo)

```java
// infrastructure/adapter/PaymentProcessorImpl.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorImpl implements PaymentProcessor {

    @Override
    public void processPayment(Order order) {
        // Simulación de llamada externa a gateway
        // En producción: HTTP client / SDK / reintentos / idempotency keys
        System.out.println("Procesando pago para orderId=" + order.getId() + ", amount=" + order.getAmount());
        // Si se lanza excepción, el listener fallará — decida si debe reintentar o compensar.
    }
}
```

<br>
<br>


# 7) Notifier (adaptador para enviar email)

```java
// infrastructure/adapter/NotificationEmailAdapter.java
package com.example.order.infrastructure.adapter;

import com.example.order.domain.model.Order;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailAdapter {
    public void sendPaymentConfirmation(Order order) {
        System.out.println("📧 Enviando email a " + order.getCustomerEmail() + " por pago de $" + order.getAmount());
        // Integrar con JavaMailSender, SES, Sendgrid, etc.
    }
}
```

<br>
<br>

# 8) Test de integración (espera la ejecución asíncrona con Awaitility)

Agrega dependencia Maven para Awaitility:

```xml
<dependency>
  <groupId>org.awaitility</groupId>
  <artifactId>awaitility</artifactId>
  <version>4.2.0</version>
  <scope>test</scope>
</dependency>
```

Test:

```java
// test/java/com/example/order/OrderTransactionalEventTest.java
package com.example.order;

import com.example.order.domain.model.Order;
import com.example.order.infrastructure.adapter.OrderJpaRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OrderTransactionalEventTest {

    @Autowired
    private com.example.order.application.usecase.CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @Test
    void whenCreateOrder_thenPaymentProcessedAndMarkedPaid() {
        // given
        Order order = createOrderUseCase.createOrder("ana@example.com", 120.0);

        // then: busy-wait hasta que el listener asíncrono marque la orden como pagada
        Awaitility.await()
                 .atMost(Duration.ofSeconds(5))
                 .pollInterval(Duration.ofMillis(200))
                 .untilAsserted(() -> {
                     Order updated = jpaRepository.findById(order.getId()).orElseThrow();
                     assertThat(updated.isPaid()).isTrue();
                 });
    }
}
```

> Usamos Awaitility para evitar `Thread.sleep` y esperar a que la tarea asíncrona complete dentro de un timeout razonable.

<br>
<br>

# 9) Buenas prácticas y consideraciones adicionales

1. **Idempotencia**: operaciones que llaman a servicios externos (pago, email) deben ser **idempotentes** o tener keys de idempotencia para evitar efectos duplicados si se reintenta.
2. **Retries y backoff**: usa `Resilience4j` o Spring Retry en adaptadores para llamadas externas.
3. **Dead-letter / fallback**: si la notificación/pago falla, enviar a una cola de fallback o guardar evento para reintentos manuales.
4. **Observabilidad**: instrumentar con trazas (OpenTelemetry) para seguir el flujo entre el request, evento y listeners asíncronos.
5. **Transactional boundaries**: publicar eventos dentro de la transacción garantiza coherencia; usar `AFTER_COMMIT` evita efectos secundarios si la tx falla.
6. **Errores en listeners asíncronos**: captura excepciones, registra, y decide política de reintento; si el `@Async` falla en el thread pool, por defecto pierdes la excepción sin propagación — añade lógica de retry/alerting.

<br>
<br>

¿Querés que adapte este ejemplo para que:

* el procesamiento de pago sea **idempotente** con una `paymentId`,
* o que use **mensajería (Kafka)** en vez de `ApplicationEventPublisher` para listeners distribuidos?
  Dime cuál y lo ajusto con código listo para copiar.







<br>
<br>

<br>
<br>



---


<br>
<br>

# Adaptación
Ejemplo para que:
el procesamiento de pago sea idempotente con una paymentId,
o que use mensajería (Kafka) en vez de ApplicationEventPublisher para listeners distribuidos



*  Arquitectura Hexagonal
*  Transacciones reales
*  Eventos asíncronos (`@Async` + `@TransactionalEventListener`)
*  Test de integración completo

<br>
<br>

#  Escenario

Flujo del pedido completo:

```
Cliente → CreateOrderUseCase
     ↓
Guarda Order en BD (transacción)
     ↓
Publica OrderCreatedEvent después del commit
     ↓
Listener asíncrono procesa pago
     ↓
Marca orden como pagada + persiste cambio
     ↓
Publica PaymentConfirmedEvent (post-commit)
     ↓
Listener asíncrono envía notificación
```

<br>
<br>

##  1. Configuración de asincronía y transacciones

Habilitamos ambos comportamientos:

```java
// Application.java
package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 🧠 2. Entidad con persistencia

```java
// domain/model/Order.java
package com.example.order.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    private String customerEmail;
    private double amount;
    private boolean paid;

    protected Order() {}

    public Order(String customerEmail, double amount) {
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.paid = false;
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return paid; }

    public void markAsPaid() { this.paid = true; }
}
```

<br>
<br>

## 3. Evento y caso de uso transaccional

Publicamos el evento **solo si la transacción se confirma** (`AFTER_COMMIT`).

```java
// domain/event/OrderCreatedEvent.java
package com.example.order.domain.event;

import com.example.order.domain.model.Order;

public record OrderCreatedEvent(Order order) {}
```

```java
// application/usecase/CreateOrderUseCase.java
package com.example.order.application.usecase;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.in.OrderCommand;
import com.example.order.domain.port.out.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOrderUseCase implements OrderCommand {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public CreateOrderUseCase(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public Order createOrder(String customerEmail, double amount) {
        Order order = repository.save(new Order(customerEmail, amount));
        publisher.publishEvent(new OrderCreatedEvent(order)); // publicado post-commit
        return order;
    }
}
```

---

## 💳 4. Listener asíncrono y transaccional

Procesa pago **después del commit** de la orden.

```java
// infrastructure/listener/OrderCreatedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.event.PaymentConfirmedEvent;
import com.example.order.domain.model.Order;
import com.example.order.domain.port.out.OrderRepository;
import com.example.order.domain.port.out.PaymentProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedListener {

    private final PaymentProcessor paymentProcessor;
    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderCreatedListener(PaymentProcessor paymentProcessor, OrderRepository repository, ApplicationEventPublisher publisher) {
        this.paymentProcessor = paymentProcessor;
        this.repository = repository;
        this.publisher = publisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        Order order = event.order();
        paymentProcessor.processPayment(order);
        order.markAsPaid();
        repository.save(order);
        publisher.publishEvent(new PaymentConfirmedEvent(order));
    }
}
```

<br>
<br>

## 📬 5. Segundo listener: envío de notificación

También asíncrono y post-commit.

```java
// infrastructure/listener/PaymentConfirmedListener.java
package com.example.order.infrastructure.listener;

import com.example.order.domain.event.PaymentConfirmedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentConfirmedListener {

    @Async
    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        System.out.printf("📧 Notificación enviada a %s%n",
                event.order().getCustomerEmail());
    }
}
```

---

## 🧪 6. Test de integración completo

Simula el flujo real con commit + eventos asíncronos.

```java
// test/java/com/example/order/OrderAsyncIntegrationTest.java
package com.example.order;

import com.example.order.application.usecase.CreateOrderUseCase;
import com.example.order.domain.model.Order;
import com.example.order.infrastructure.adapter.OrderJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderAsyncIntegrationTest {

    @Autowired
    private CreateOrderUseCase useCase;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @Test
    void whenOrderCreated_thenEventuallyPaidAndNotified() throws Exception {
        Order order = useCase.createOrder("ana@example.com", 250.0);

        // Esperar procesamiento asíncrono
        Thread.sleep(1000);

        Order updated = jpaRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.isPaid()).isTrue();
    }
}
```

<br>
<br>

##  7. Resumen técnico

| Concepto                         | Implementación                                                                   |
| -------------------------------- | -------------------------------------------------------------------------------- |
| **Transacción**                  | `@Transactional` en el caso de uso                                               |
| **Eventos post-commit**          | `@TransactionalEventListener(phase = AFTER_COMMIT)`                              |
| **Ejecución asíncrona**          | `@Async`                                                                         |
| **Aislamiento del dominio**      | `Order`, `OrderRepository`, `OrderCommand` no dependen de Spring                 |
| **Resiliencia y extensibilidad** | Se pueden añadir más listeners (auditoría, métricas, etc.) sin modificar el core |

<br>
<br>

##  Beneficios clave (nivel Senior)

✅ Eventos sólo se disparan si el commit fue exitoso (evita inconsistencias)
✅ Listeners no bloquean el thread principal
✅ Arquitectura Hexagonal mantiene la independencia de infraestructura
✅ Fácil de extender a colas como Kafka o RabbitMQ cambiando sólo el adaptador



¿Querés que te muestre cómo extender este patrón para **publicar los eventos de dominio en Kafka (event sourcing real)** usando Spring Boot 3.3 y Spring Cloud Stream?
