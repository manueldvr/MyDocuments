# RABBIT MQ


**introducción completa a RabbitMQ** y luego te muestro **cómo integrarlo con Spring Boot** paso a paso, pensando en desarrollador Java 21.

index:

- Qué es?
- Conceptos
- Arquitectura
- Tipos de Exchange
- instalacion
- uso en Spring Boot
- Declaración de Colas y Exchange
- Resumen
- Ejemplo para Performance
- Ejemplo para Resiliencia
	- configuraciones de reintentos
- Como definir DLX y DLQ
- ejemplo
- estrategia de Reproceso
- ejemplo: Consumer DLQ con reenvío manual

---

## 🔹 1. Qué es RabbitMQ

RabbitMQ es un **broker de mensajería** (message broker) que actúa como intermediario para el envío de mensajes entre aplicaciones.

* Implementa el protocolo **AMQP (Advanced Message Queuing Protocol)**.
* Permite que sistemas **productores (producers)** envíen mensajes que serán **consumidos (consumers)** de forma asíncrona.
* Es ideal para **desacoplar microservicios**, distribuir cargas de trabajo y manejar eventos.

📌 **Características principales:**

* Mensajería asíncrona y confiable.
* Soporta colas, exchange, bindings y routing keys.
* Asegura **persistencia de mensajes** y **alta disponibilidad**.
* Compatible con varios lenguajes y frameworks.

📌 **Casos de uso:**

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

---

## 🔹 5. Instalación rápida de RabbitMQ (local)

Si querés probar en tu máquina:

```bash
docker run -d --hostname rabbitmq --name rabbitmq \
-p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

* Admin Web UI: [http://localhost:15672](http://localhost:15672)
* User/Pass: `guest` / `guest`
* Puerto AMQP: `5672`

---

## 🔹 6. Uso en Spring Boot (Spring AMQP)

Spring Boot tiene **Spring AMQP** que simplifica todo con anotaciones.

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

### 📌 Producer (Envío de mensajes)

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

---

### 📌 Probarlo en un Controller

```java
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final ProducerService producerService;

    public MessageController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    public String send(@RequestParam String msg) {
        producerService.sendMessage(msg);
        return "Mensaje enviado: " + msg;
    }
}
```

Llamá:

```
POST http://localhost:8080/messages?msg=HolaRabbit
```

Vas a ver en consola:

```
Mensaje enviado: HolaRabbit
Mensaje recibido: HolaRabbit
```

---

## 🔹 7. Resumen

* RabbitMQ permite **comunicación asíncrona** y **desacoplamiento**.
* Usar Spring AMQP hace que no necesites código de bajo nivel.
* Con `@RabbitListener` podés consumir mensajes fácil.
* Con `RabbitTemplate` enviás mensajes.

---

¿Querés que te haga también un **diagrama simple** de cómo fluye el mensaje Producer → Exchange → Queue → Consumer?


            message
Producer ----------------> EXCHANGE----Queue---->Consumer


<br>

<br>

<br>

---

<br>

<br>


dado el ejemplo que propusiste, cómo lo mejorarías con un ejemplo para lograr performance y otro para lograr resiliencia


---



¡Excelente pregunta! Vamos a **llevar el ejemplo básico de RabbitMQ con Spring Boot** a un nivel más avanzado:

1. **Optimización para performance** (alto throughput, baja latencia).
2. **Optimización para resiliencia** (alta disponibilidad, tolerancia a fallas).

---

## 🚀 1. Ejemplo para **Performance**

Objetivo: procesar **muchos mensajes rápidamente**.
Claves: **conexiones y canales reutilizados, listeners concurrentes, batch processing, ack manual, prefetch tuning.**

---

### 🔹 Configuración optimizada

```java
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitPerformanceConfig {

    @Bean
    public CachingConnectionFactory cachingConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL); 
        factory.setChannelCacheSize(50); // Cache de canales
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(CachingConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(5);  // Threads iniciales
        factory.setMaxConcurrentConsumers(10); // Escalabilidad
        factory.setPrefetchCount(20); // Cantidad de mensajes por request
        factory.setBatchListener(true); 
        factory.setBatchSize(10); // Procesar mensajes en batch
        return factory;
    }
}
```

---

### 🔹 Consumer Batch

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FastConsumerService {

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME, containerFactory = "rabbitListenerContainerFactory")
    public void receiveBatch(List<String> messages) {
        System.out.println("Procesando batch: " + messages.size() + " mensajes");
        // Procesar en paralelo si es necesario
        messages.parallelStream().forEach(msg -> System.out.println("Mensaje: " + msg));
    }
}
```

🔑 **Qué logramos:**

* Cache de **canales** → menos overhead de conexiones.
* **Consumidores concurrentes** → varios threads procesando mensajes.
* **Prefetch ajustado** → optimiza cuántos mensajes toma cada consumidor.
* **Batch processing** → reduce overhead de confirmaciones.

---

## 🛡️ 2. Ejemplo para **Resiliencia**

Objetivo: que el sistema **no pierda mensajes y se recupere ante fallas**.
Claves: **mensajes persistentes, colas duraderas, reintentos, dead-letter queues (DLQ).**

---

### 🔹 Declaración de cola con DLQ

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitResilienceConfig {

    public static final String MAIN_QUEUE = "resilient.queue";
    public static final String DLQ = "resilient.dlq";
    public static final String DLX = "resilient.dlx";

    @Bean
    public Queue resilientQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("resilient.exchange");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(resilientQueue()).to(directExchange()).with(MAIN_QUEUE);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }
}
```

---

### 🔹 Consumer con reintentos

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ResilientConsumerService {

    @RabbitListener(queues = RabbitResilienceConfig.MAIN_QUEUE)
    public void receiveMessage(String message) throws Exception {
        try {
            System.out.println("Procesando mensaje: " + message);
            // Simulación de error
            if (message.contains("error")) throw new RuntimeException("Error procesando");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw e; // Se reenvía a la DLQ tras reintentos
        }
    }
}
```

---

### 🔹 Configuración de reintentos

```java
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.listener.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory retryRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, null);
        factory.setDefaultRequeueRejected(false); // Evita loops infinitos
        return factory;
    }
}
```

🔑 **Qué logramos:**

* **Durabilidad**: mensajes y colas persistentes.
* **DLQ**: mensajes que fallan van a una cola separada para revisión.
* **Reintentos controlados**: sin loops infinitos.
* **Escalabilidad segura**: consumers pueden reiniciarse sin perder mensajes.

---

## 🔥 Resumen comparativo

| Estrategia        | Performance                            | Resiliencia                       |
| ----------------- | -------------------------------------- | --------------------------------- |
| Conexiones        | Cache de canales                       | Conexiones persistentes           |
| Consumidores      | Concurrentes, paralelos                | Escalables                        |
| Prefetch          | Ajustado alto                          | Ajustado bajo (procesos críticos) |
| Batch             | Sí, reduce overhead                    | No siempre necesario              |
| Mensajes          | No siempre persistentes                | Siempre persistentes              |
| Dead Letter Queue | Opcional                               | Obligatoria                       |
| Reintentos        | Mejorar velocidad, skip errores rápido | Retries con DLQ y logging         |

---

¿Querés que te haga **un diagrama con DLQ** (cola de mensajes fallidos) para que quede más claro cómo se logra resiliencia?

                                 DLX
								  |
Producer -----> Exchange -----> Queue
								  |
								  DLQ


<br>

---

<br>

<br>

como definir DLX y DLQ?


¡Gran pregunta! Vamos a definir bien **DLX** y **DLQ** en RabbitMQ, con ejemplos claros:

---

## 🔹 Definición

| Concepto                       | Significado                                                                                                         | Función                                                                            |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| **DLX (Dead Letter Exchange)** | Es un **exchange especial** al que RabbitMQ redirige mensajes que no se pudieron entregar o procesar correctamente. | Permite **encaminar mensajes fallidos** hacia una cola separada.                   |
| **DLQ (Dead Letter Queue)**    | Es la **cola receptora** de los mensajes que el DLX redirige.                                                       | Permite **almacenar mensajes problemáticos** para analizar o reprocesar más tarde. |

---

## 🔹 Cuándo se envían mensajes al DLX

RabbitMQ manda un mensaje a un DLX si ocurre alguno de estos casos:

1. El consumidor **rechaza el mensaje** (`nack` o `reject`) y `requeue=false`.
2. El mensaje **expira** (TTL, Time To Live).
3. La cola está **llena** (si tiene límite de longitud).
4. Se usa **`basic.reject`** manualmente.

---

## 🔹 Flujo

1. Mensajes entran a una cola normal (`Main Queue`) a través de un `Exchange`.
2. Si un mensaje falla, se reenvía al **DLX**.
3. El DLX entrega el mensaje a la **DLQ**.
4. Operadores o procesos analizan la DLQ para reprocesar o depurar.

📌 Esto permite que **no pierdas mensajes fallidos** y que no afecten el resto del sistema.

---

## 🔹 Ejemplo práctico (Java + Spring Boot)

```java
@Configuration
public class RabbitDLQConfig {

    public static final String MAIN_EXCHANGE = "app.main.exchange";
    public static final String MAIN_QUEUE = "app.main.queue";
    public static final String DLX = "app.dlx";
    public static final String DLQ = "app.dlq";

    // Cola principal con DLX
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    // DLQ
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    // Exchange principal
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE);
    }

    // DLX
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    // Bindings
    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue()).to(mainExchange()).with(MAIN_QUEUE);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }
}
```

---

## 🔹 Ejemplo de Consumer que rechaza mensajes

```java
@Service
public class SampleConsumer {

    @RabbitListener(queues = RabbitDLQConfig.MAIN_QUEUE)
    public void consume(String message) throws Exception {
        System.out.println("Mensaje recibido: " + message);
        if (message.contains("error")) {
            System.err.println("Error detectado, moviendo a DLQ...");
            throw new RuntimeException("Forzando error");
        }
    }
}
```

📌 Resultado:

* Mensajes correctos → procesados normalmente.
* Mensajes con “error” → rechazados, enviados a **DLX** y luego a **DLQ**.


<br>


---

<br>

<br>

¿Querés que te haga **un ejemplo de reproceso automático** desde la DLQ para recuperar mensajes fallidos?


¡Genial! Vamos a armar **un ejemplo de reproceso automático desde la DLQ** en Spring Boot.
La idea es que los mensajes que cayeron en la **Dead Letter Queue (DLQ)** puedan **reintentarse** (con lógica de backoff o manual).

---

## 🔹 Estrategia de Reproceso

1. **Consumidor de la DLQ**: Escucha la DLQ.
2. **Reintento/Reenvío**:

   * Reintenta procesar el mensaje **en caliente** (misma app).
   * O lo **reenvía a la cola principal** para que siga el flujo normal.
3. **Control de intentos**: Usa un header o metadato para limitar reintentos (evitar loops infinitos).

---

## 🔹 Nueva Cola de Retries (Opcional)

Podés usar una cola intermedia para hacer **delay entre reintentos** (con TTL).

* Mensajes fallidos pasan por una cola de `retry.queue`.
* Tras TTL, vuelven a `main.queue`.
  Pero para simplificar, vamos a **reinyectar directo en main.queue**.

---

## 🔹 Código: Consumer DLQ con reenvío manual

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqConsumer {

    private final RabbitTemplate rabbitTemplate;

    public DlqConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitDLQConfig.DLQ)
    public void processDlqMessage(String message) {
        System.out.println("DLQ detectó mensaje fallido: " + message);

        try {
            // Intento reprocesar aquí directamente
            if (reprocess(message)) {
                System.out.println("Mensaje reprocesado OK");
            } else {
                System.out.println("Reinyectando mensaje en cola principal...");
                rabbitTemplate.convertAndSend(
                        RabbitDLQConfig.MAIN_EXCHANGE,
                        RabbitDLQConfig.MAIN_QUEUE,
                        message
                );
            }

        } catch (Exception e) {
            System.err.println("Error reprocesando mensaje DLQ: " + e.getMessage());
        }
    }

    private boolean reprocess(String message) {
        // Simulación de lógica de reproceso
        return !message.contains("retry-fail");
    }
}
```

---

## 🔹 Flujo Completo

1. **Producer** → envía mensajes a `MAIN_QUEUE`.
2. **Consumer normal** procesa mensajes.
3. Si falla → mensaje va a **DLX** → **DLQ**.
4. **DlqConsumer** escucha DLQ:

   * Si el mensaje ahora puede procesarse, se procesa directamente.
   * Si no, se **reinyecta a la cola principal** para otro intento (o se deja ahí para análisis manual).

---

## 🔹 Variante con Retrasos (Backoff Automático)

Podés configurar una cola `retry.queue` con un TTL (ej: 10s).
Cuando falla:

* DLQ → `retry.queue` (espera TTL) → vuelve a `main.queue`.
  Así escalás **reintentos automáticos con delays**.

---

¿Querés que te lo dibuje en **un diagrama con cola de retry** para que veas el flujo visual completo (main → dlq → retry → main)?

							/--->Retry queue
							|			|
							|Retry		|
							|			|
               Fail			|			|
Main Queue-------------> DLQ      		|Reinject
							|			|
							|			|
							|success	|
                            |           ∨
							--->Main Exchange