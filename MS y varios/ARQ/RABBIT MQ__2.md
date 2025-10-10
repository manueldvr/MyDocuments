# RABBIT MQ 2

<br>

<br>

<br>


Podrías darme un ejemplo realista de una implementación basica para comunicación entre microservicios construidos con Spring Boot / Java21? diciendome como sería la configuracion de/los proyectos basados en maven.



### Ejemplo de Implementación Básica de Comunicación entre Microservicios con Spring Boot y RabbitMQ

Para ilustrar una comunicación básica entre microservicios usando Spring Boot (versión 3.3.x, compatible con Java 21) y RabbitMQ como broker de mensajes, crearemos dos microservicios simples:

1. **Productor (Sender Service)**: Un servicio que envía mensajes a una cola en RabbitMQ.
2. **Consumidor (Receiver Service)**: Un servicio que escucha la cola y procesa los mensajes recibidos.

Usaremos comunicación asíncrona basada en mensajes (publish-subscribe o point-to-point), que es común en arquitecturas de microservicios para desacoplar servicios y mejorar la escalabilidad. Asumiremos un patrón simple: el productor envía un mensaje JSON a una cola, y el consumidor lo recibe y lo procesa (por ejemplo, lo imprime en logs).

**Requisitos previos**:
- Java 21 instalado.
- Maven 3.8+.
- RabbitMQ server corriendo localmente (puedes instalarlo vía Docker: `docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:management`).
- Usaremos Spring Boot Starter para AMQP (RabbitMQ).

Crearemos dos proyectos Maven separados: `sender-service` y `receiver-service`.

#### 1. Configuración de Maven (pom.xml) para Ambos Proyectos
Ambos servicios compartirán una estructura similar en su `pom.xml`, pero con dependencias comunes para Spring Boot y RabbitMQ. Usa Java 21 como versión de compilación.

Crea los proyectos con:
```
mvn archetype:generate -DgroupId=com.example -DartifactId=sender-service -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```
(Haz lo mismo para `receiver-service`, cambiando el artifactId).

Luego, edita el `pom.xml` de cada uno para agregar Spring Boot y RabbitMQ:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>sender-service</artifactId>  <!-- Cambia a receiver-service para el otro proyecto -->
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>  <!-- Versión compatible con Java 21 -->
        <relativePath/>
    </parent>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Web para exponer endpoints (opcional para testing) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- Spring Boot AMQP para RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <!-- Para testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Jackson para JSON (incluido por defecto en Spring Boot) -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- **Notas sobre Maven**:
  - El `spring-boot-starter-parent` maneja versiones de dependencias automáticamente.
  - Compila con `mvn clean install` en cada proyecto.
  - Ejecuta con `mvn spring-boot:run` o genera un JAR con `mvn package` y corre `java -jar target/*.jar`.
  - Para Java 21, asegúrate de que tu JDK sea 21 (puedes usar SDKMAN o similar para gestionarlo).

#### 2. Configuración Común en `application.yml` (para Ambos Servicios)
En `src/main/resources/application.yml`, configura la conexión a RabbitMQ (usa valores por defecto para local):

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Nombre de la cola (compartido entre servicios)
app:
  queue: my-queue
```

#### 3. Código para el Productor (Sender Service)
- **Clase Principal**: `SenderApplication.java`
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SenderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SenderApplication.class, args);
    }
}
```

- **Configuración de RabbitMQ**: `RabbitConfig.java` (crea la cola si no existe)
```java
package com.example;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue myQueue() {
        return new Queue("my-queue", true);  // Cola durable
    }
}
```

- **Servicio para Enviar Mensajes**: `MessageSender.java`
```java
package com.example;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageSender {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.queue}")
    private String queueName;

    public MessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(queueName, message);
        System.out.println("Mensaje enviado: " + message);
    }
}
```

- **Controlador para Testing (opcional)**: `SenderController.java` (expone un endpoint REST para enviar mensajes)
```java
package com.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SenderController {
    private final MessageSender messageSender;

    public SenderController(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @PostMapping("/send")
    public String send(@RequestBody String message) {
        messageSender.sendMessage(message);
        return "Mensaje enviado exitosamente";
    }
}
```

#### 4. Código para el Consumidor (Receiver Service)
- **Clase Principal**: `ReceiverApplication.java` (similar al sender)
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReceiverApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReceiverApplication.class, args);
    }
}
```

- **Configuración de RabbitMQ**: `RabbitConfig.java` (igual que en sender, para declarar la cola)

- **Listener para Recibir Mensajes**: `MessageReceiver.java`
```java
package com.example;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageReceiver {
    @RabbitListener(queues = "${app.queue}")
    public void receiveMessage(String message) {
        System.out.println("Mensaje recibido: " + message);
        // Aquí procesa el mensaje, e.g., guarda en DB o llama a otro servicio
    }
}
```

#### 5. Cómo Probarlo
1. Inicia RabbitMQ.
2. Ejecuta el receiver: `mvn spring-boot:run` en `receiver-service`.
3. Ejecuta el sender: `mvn spring-boot:run` en `sender-service`.
4. Envía un mensaje vía curl: `curl -X POST -H "Content-Type: text/plain" -d "Hola desde el productor!" http://localhost:8080/send` (asumiendo puerto 8080 por defecto).
5. Verás el mensaje en los logs del receiver.




Este es un ejemplo básico y realista para empezar. En producción, agrega manejo de errores, reintentos (con `SimpleRabbitListenerContainerFactory`), serialización JSON personalizada, o usa exchanges para enrutamiento avanzado. Si necesitas expandirlo (e.g., con Docker Compose para orquestar servicios), ¡házmelo saber!










<br>

<br>

<br>









Productor y Consumidor podrían estar en  diferentes proyectos SpringBoot?



Sí, el productor y el consumidor pueden (y suelen) estar en proyectos Spring Boot separados, ya que RabbitMQ actúa como un intermediario que permite la comunicación entre aplicaciones independientes. En el ejemplo proporcionado anteriormente, ya se estructuró asumiendo que el **Productor (Sender Service)** y el **Consumidor (Receiver Service)** están en proyectos Maven separados (`sender-service` y `receiver-service`), cada uno con su propio `pom.xml`, configuración y código.

### Aclaraciones sobre la Separación en Proyectos Diferentes
1. **Independencia**: Cada microservicio (productor y consumidor) es un proyecto Spring Boot autónomo con su propio ciclo de vida, lo que permite:
   - Despliegues separados.
   - Escalabilidad independiente.
   - Mantenimiento aislado.
2. **Comunicación vía RabbitMQ**: RabbitMQ actúa como el "pegamento" entre ambos servicios. El productor envía mensajes a una cola (o exchange), y el consumidor los lee, sin necesidad de que los servicios se conozcan directamente.
3. **Configuración Compartida**: Ambos proyectos necesitan conocer la configuración de RabbitMQ (host, puerto, credenciales) y el nombre de la cola (en este caso, `my-queue`). Esto se logra a través del archivo `application.yml` en cada proyecto.

### Detalles Clave del Ejemplo Anterior
- **Proyectos Separados**: En el ejemplo, ya se crearon dos proyectos Maven distintos:
  - `sender-service`: Contiene el código del productor (`SenderApplication`, `MessageSender`, etc.).
  - `receiver-service`: Contiene el código del consumidor (`ReceiverApplication`, `MessageReceiver`, etc.).
- **Estructura de Carpetas**:
  ```
  /sender-service
    ├── src/main/java/com/example/
    │   ├── SenderApplication.java
    │   ├── RabbitConfig.java
    │   ├── MessageSender.java
    │   └── SenderController.java
    ├── src/main/resources/
    │   └── application.yml
    └── pom.xml

  /receiver-service
    ├── src/main/java/com/example/
    │   ├── ReceiverApplication.java
    │   ├── RabbitConfig.java
    │   └── MessageReceiver.java
    ├── src/main/resources/
    │   └── application.yml
    └── pom.xml
  ```
- **Configuración Común**: Ambos proyectos comparten la misma configuración de RabbitMQ en `application.yml` (host: localhost, puerto: 5672, cola: `my-queue`), pero son aplicaciones independientes que se ejecutan en puertos diferentes (por defecto, 8080 para el sender, y otro puerto como 8081 para el receiver si están en la misma máquina).
- **Ejecución**: Cada proyecto se ejecuta por separado con `mvn spring-boot:run` o como JARs independientes.

### Cambios Necesarios para Confirmar Separación
Si quieres asegurarte de que los servicios estén completamente desacoplados:
1. **Puertos Diferentes**: Asegúrate de que cada servicio corre en un puerto diferente. Por ejemplo, en `sender-service/src/main/resources/application.yml`:
   ```yaml
   server:
     port: 8080
   ```
   Y en `receiver-service/src/main/resources/application.yml`:
   ```yaml
   server:
     port: 8081
   ```
2. **Nombres de Paquetes Únicos**: Aunque en el ejemplo usé `com.example` para ambos, en un entorno real podrías usar `com.example.sender` y `com.example.receiver` para mayor claridad.
3. **Cola Declarada en Ambos**: La cola (`my-queue`) se declara en ambos proyectos (`RabbitConfig.java`) para garantizar que exista, aunque RabbitMQ solo la crea una vez. Esto es una práctica común para evitar errores si un servicio se inicia antes que el otro.

### Ejemplo de Código Actualizado (Opcional)
Si necesitas un ajuste específico para enfatizar la separación, aquí está un ejemplo de cómo modificar el `pom.xml` y los paquetes para reflejar mejor la independencia. Por ejemplo, el `pom.xml` del consumidor con un `groupId` diferente:

<xaiArtifact artifact_id="dea02fab-0fa7-4eeb-9867-f9f7aa07ca1b" artifact_version_id="2b7cc4a5-8da2-49a8-b5e9-80066d2d391b" title="pom.xml" contentType="text/xml">
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.receiver</groupId>
    <artifactId>receiver-service</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
</xaiArtifact>

Y el paquete base para las clases Java en `receiver-service` sería `com.example.receiver`. Por ejemplo, `ReceiverApplication.java`:
```java
package com.example.receiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReceiverApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReceiverApplication.class, args);
    }
}
```

El productor tendría un `groupId` como `com.example.sender` y un paquete base `com.example.sender`.

### Cómo Ejecutar y Probar
1. **Inicia RabbitMQ**: Asegúrate de que RabbitMQ esté corriendo (`docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management`).
2. **Inicia el Consumidor**: En el directorio `receiver-service`, ejecuta `mvn spring-boot:run`. Esto inicia el servicio en el puerto 8081 (o el configurado) y comienza a escuchar la cola `my-queue`.
3. **Inicia el Productor**: En el directorio `sender-service`, ejecuta `mvn spring-boot:run`. Esto inicia el servicio en el puerto 8080.
4. **Envía un Mensaje**: Usa curl o Postman para enviar un mensaje al productor:
   ```bash
   curl -X POST -H "Content-Type: text/plain" -d "Mensaje de prueba" http://localhost:8080/send
   ```
5. **Verifica en el Consumidor**: Revisa los logs del `receiver-service` para confirmar que el mensaje fue recibido.

### Notas Adicionales
- **Escalabilidad**: Puedes ejecutar múltiples instancias del consumidor (en diferentes puertos o máquinas) para balancear la carga. RabbitMQ distribuirá los mensajes entre los consumidores activos.
- **Docker Compose**: Para orquestar ambos servicios y RabbitMQ, podrías usar un archivo `docker-compose.yml`. Si quieres, puedo proporcionar un ejemplo.
- **Serialización JSON**: Si los mensajes son objetos complejos, usa Jackson para serializar/deserializar. Por ejemplo, configura `RabbitTemplate` con un `Jackson2JsonMessageConverter`:
  ```java
  @Bean
  public MessageConverter jsonMessageConverter() {
      return new Jackson2JsonMessageConverter();
  }
  ```
  Y úsalo en el `RabbitTemplate`.

Si necesitas más detalles, como agregar un exchange, manejar errores, o un ejemplo con Docker Compose, ¡dímelo!

