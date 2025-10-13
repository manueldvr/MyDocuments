# 4. Java HttpClient (JDK)



**Descripción**: Introducido en **Java 11** y mejorado en **Java 21**, `HttpClient` es una API nativa del JDK para realizar solicitudes HTTP síncronas y asíncronas. No depende de Spring, lo que la hace ideal para aplicaciones que buscan minimizar dependencias externas.


**Características**:
- Soporte para HTTP/1.1 y HTTP/2.
- Operaciones síncronas y asíncronas (con `CompletableFuture`).
- Configuración de timeouts, autenticación y encabezados personalizados.
- Ligero, ya que está incluido en el JDK.


**Casos de uso**:
- Aplicaciones que no usan Spring o que buscan evitar dependencias externas.
- Escenarios simples donde no se necesita la integración con Spring.
- Proyectos que aprovechan características modernas de Java 21 (como `CompletableFuture`).

**Dependencias en `pom.xml`**:

Ninguna, ya que está incluido en el JDK 21.

**Ejemplo**:

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.example.com/users"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        // POST request
        String newUser = "{\"name\": \"Alice\", \"email\": \"alice@example.com\"}";
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.example.com/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(newUser))
                .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(postResponse.body());
    }
}
```

**Ventajas**:
- Parte del JDK, sin dependencias externas.
- Soporte nativo para HTTP/2 y operaciones asíncronas.
- Ideal para aplicaciones minimalistas.

**Limitaciones**:
- Menos integración con Spring (sin soporte para convertidores de mensajes automáticos).
- Requiere más código para manejar JSON u otros formatos complejos.

<br>

<br>
<br>

<br>

# Feign Client


**Descripción**: **OpenFeign** es una biblioteca declarativa para crear clientes HTTP, popular en aplicaciones Spring Cloud. Permite definir interfaces Java que se mapean directamente a endpoints REST, reduciendo el código repetitivo.



**Características**:
- API declarativa basada en anotaciones (`@FeignClient`).
- Integración nativa con Spring Cloud (Eureka, Ribbon, etc.).
- Soporte para codificadores/decodificadores personalizados (Jackson, Gson, etc.).
- Manejo de errores y reintentos configurable.

**Casos de uso**:
- Microservicios en arquitecturas Spring Cloud.
- Escenarios donde se desea una interfaz limpia y declarativa para consumir APIs.
- Proyectos que requieren balanceo de carga o descubrimiento de servicios.

**Dependencias en `pom.xml`**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>4.1.3</version>
</dependency>
```

**Ejemplo**:
```java
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Definir la interfaz del cliente
@FeignClient(name = "user-service", url = "https://api.example.com")
interface UserClient {
    @GetMapping("/users")
    String getUsers();

    @PostMapping("/users")
    String createUser(@RequestBody String user);
}

// Usar el cliente en una clase
import org.springframework.beans.factory.annotation.Autowired;

public class FeignClientExample {
    @Autowired
    private UserClient userClient;

    public void execute() {
        // GET request
        String users = userClient.getUsers();
        System.out.println(users);

        // POST request
        String newUser = "{\"name\": \"Bob\", \"email\": \"bob@example.com\"}";
        String response = userClient.createUser(newUser);
        System.out.println(response);
    }
}
```

**Ventajas**:
- API declarativa que reduce el código boilerplate.
- Excelente integración con Spring Cloud.
- Soporte para balanceo de carga y reintentos.

**Limitaciones**:
- Más pesado que `RestClient` o `WebClient` debido a las dependencias de Spring Cloud.
- Menos flexible para escenarios no declarativos.

---

### Comparación de Alternativas
| Cliente           | Tipo           | Casos de Uso Principales                     | Ventajas                              | Limitaciones                          |
|-------------------|----------------|---------------------------------------------|---------------------------------------|---------------------------------------|
| **RestClient**    | Síncrono       | Reemplazo de `RestTemplate`, APIs síncronas | API fluida, moderna, ligera            | Solo síncrono                        |
| **WebClient**     | Reactivo       | Aplicaciones reactivas, alta concurrencia   | No bloqueante, streaming, flexible    | Curva de aprendizaje reactiva         |
| **TestRestTemplate** | Síncrono (Test) | Pruebas de integración                    | Simplifica autenticación y cookies    | Solo para pruebas                    |
| **HttpClient**    | Síncrono/Asíncrono | Aplicaciones sin Spring, minimalistas    | Nativo, sin dependencias              | Menos integración con Spring          |
| **Feign Client**  | Declarativo    | Microservicios, Spring Cloud                | API limpia, integración con Cloud     | Dependencias pesadas                 |

---

### Conclusión
- **RestClient** es el reemplazo recomendado para `RestTemplate` en aplicaciones síncronas modernas, con una API más limpia y soporte para Java 21.
- **WebClient** es ideal para aplicaciones reactivas o de alta concurrencia, pero requiere aprender programación reactiva.
- **TestRestTemplate** es perfecto para pruebas de integración, especialmente cuando se necesita autenticación o manejo de cookies.
- **Java HttpClient** es una opción ligera para proyectos que no usan Spring o buscan minimizar dependencias.
- **Feign Client** es excelente en entornos de microservicios con Spring Cloud, ofreciendo una API declarativa.

Para un proyecto típico en **Spring Boot 3.x** con **Java 21**, recomendaría usar **RestClient** para la mayoría de los casos síncronos y **WebClient** si planeas adoptar una arquitectura reactiva. Usa **TestRestTemplate** exclusivamente para pruebas de integración. Si trabajas en un entorno de microservicios con Spring Cloud, considera **Feign Client**.


<br>


**Referencias**:
- Spring Framework Documentation:,[](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html)[](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- Baeldung:,[](https://www.baeldung.com/spring-webclient-resttemplate)[](https://www.baeldung.com/spring-boot-testresttemplate)
- HowToDoInJava:,[](https://howtodoinjava.com/spring-boot2/resttemplate/spring-restful-client-resttemplate-example/)[](https://howtodoinjava.com/spring-boot2/resttemplate/resttemplate-httpclient-java-config/)





<br>
<br>
<br>

<br>

<br>







---

<br>


# Solo RestClient y Feign Client.  

<br>



### Considerando tu respuesta, si necesito llamar a esta URL: https://corresponsalias.santander.com.ar/sb/v3/tokens
que retorna un token a ser utilizado por otras llamadas, por ejemplo:  
"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mNvcnJlc3BvbnNhbGlhcy5zYW50YW5kZXIuY29tLmFyIn0.6cMOA93HfXZCREXXc9DL7Us3XbvM-JHIt3nbkkagO0I"  
si quiero utilizar RestClient y Feign Client, podrías darme ejemplos de implementación considerando 3 reintentos?


.  
.  


A continuación, te proporciono ejemplos de implementación para consumir la URL `https://corresponsalias.santander.com.ar/sb/v3/tokens` utilizando **RestClient** y **Feign Client** en un proyecto con **Spring Boot 3.x** y **Java 21**. Ambos ejemplos incluyen un mecanismo de **3 reintentos** en caso de fallos (por ejemplo, errores HTTP 5xx o problemas de conexión). También asumo que la llamada a la URL es un **POST** (como es común para obtener tokens) y que el cuerpo de la solicitud contiene credenciales u otros parámetros necesarios. Si la llamada es diferente (por ejemplo, GET), por favor proporciónalo para ajustar el código.

---

## 1. RestClient con Reintentos
**Descripción**: Usaremos `RestClient` con un mecanismo de reintentos configurado mediante un interceptor personalizado o una biblioteca como `Resilience4j`. En este caso, optaré por `Resilience4j` para manejar los reintentos de forma declarativa, ya que es una solución robusta y ampliamente utilizada en Spring Boot.

**Dependencias en `pom.xml`**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.3.3</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <version>3.3.3</version>
</dependency>
```

**Configuración de Resilience4j** (en `application.yml`):
```yaml
resilience4j.retry:
  instances:
    tokenService:
      maxAttempts: 3
      waitDuration: 500ms
      retryExceptions:
        - org.springframework.web.client.HttpServerErrorException
        - java.net.ConnectException
```

**Ejemplo de Código**:
```x-java-source
package com.example.demo.service;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TokenServiceRestClient {

    private final RestClient restClient;

    public TokenServiceRestClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://corresponsalias.santander.com.ar")
                .build();
    }

    @Retry(name = "tokenService")
    public String getToken(String credentials) {
        return restClient.post()
                .uri("/sb/v3/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .retrieve()
                .body(String.class);
    }
}
```

**Uso del Servicio**:
```java
package com.example.demo;

import com.example.demo.service.TokenServiceRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    @Autowired
    private TokenServiceRestClient tokenService;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String credentials = "{\"clientId\": \"your-client-id\", \"clientSecret\": \"your-client-secret\"}";
        String token = tokenService.getToken(credentials);
        System.out.println("Token: " + token);
    }
}
```

**Explicación**:
- **RestClient**: Configurado con una URL base para simplificar las llamadas.
- **Resilience4j**: La anotación `@Retry` asegura que la solicitud se reintente hasta 3 veces en caso de excepciones como errores HTTP 5xx o problemas de conexión. La espera entre intentos es de 500ms.
- **Cuerpo de la solicitud**: Se asume un JSON con credenciales (ajusta según los requisitos reales de la API).
- **Salida**: La respuesta es el token como una cadena (por ejemplo, `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`).

**Ventajas**:
- API fluida y moderna de `RestClient`.
- Reintentos manejados de forma declarativa con `Resilience4j`.
- Fácil de integrar con Spring Boot.

**Limitaciones**:
- Requiere configurar `Resilience4j` para reintentos avanzados.
- Síncrono, no ideal para alta concurrencia.

.  
.  

---
.  

## 2. Feign Client con Reintentos
**Descripción**: Usaremos **OpenFeign** con Spring Cloud para definir un cliente declarativo. Para los reintentos, configuraremos un `Retryer` personalizado en Feign. También usaremos `Resilience4j` para una integración más limpia con Spring Boot, aunque Feign tiene su propio mecanismo de reintentos.

**Dependencias en `pom.xml`**:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>4.1.3</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <version>3.3.3</version>
</dependency>
```

**Configuración de Resilience4j** (en `application.yml`):
```yaml
resilience4j.retry:
  instances:
    tokenFeignClient:
      maxAttempts: 3
      waitDuration: 500ms
      retryExceptions:
        - feign.RetryableException
        - java.net.ConnectException
```

**Ejemplo de Código**:
```x-java-source
package com.example.demo.client;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "tokenClient", url = "https://corresponsalias.santander.com.ar")
public interface TokenFeignClient {

    @PostMapping(value = "/sb/v3/tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Retry(name = "tokenFeignClient")
    String getToken(@RequestBody String credentials);
}
```

**Uso del Cliente**:

```java
package com.example.demo;

import com.example.demo.client.TokenFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DemoApplication implements CommandLineRunner {

    @Autowired
    private TokenFeignClient tokenClient;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String credentials = "{\"clientId\": \"your-client-id\", \"clientSecret\": \"your-client-secret\"}";
        String token = tokenClient.getToken(credentials);
        System.out.println("Token: " + token);
    }
}
```

**Explicación**:
- **Feign Client**: La interfaz `TokenFeignClient` define el endpoint `/sb/v3/tokens` con una solicitud POST que acepta un cuerpo JSON y retorna una cadena (el token).
- **Resilience4j**: La anotación `@Retry` asegura que la solicitud se reintente hasta 3 veces en caso de excepciones como `RetryableException` (errores de Feign) o problemas de conexión.
- **Configuración**: La anotación `@EnableFeignClients` habilita el soporte para Feign en Spring Boot.
- **Cuerpo de la solicitud**: Igual que en el ejemplo de `RestClient`, se envía un JSON con credenciales.

**Ventajas**:
- API declarativa, muy limpia y fácil de mantener.
- Integración nativa con Spring Cloud y posibilidad de usar balanceo de carga.
- Reintentos manejados de forma transparente con `Resilience4j`.

**Limitaciones**:
- Más dependencias que `RestClient` (Spring Cloud Feign).
- Menos flexible para configuraciones avanzadas de bajo nivel.

<br>


### Notas Adicionales

- **Credenciales**: En ambos ejemplos, el cuerpo de la solicitud (`credentials`) es un ejemplo. Ajusta el JSON según los requisitos de la API (por ejemplo, `clientId`, `clientSecret`, o cualquier otro parámetro).
- **Manejo de Errores**: Puedes agregar manejo de errores más específico en ambos casos usando `.onError()` en `RestClient` o un `ErrorDecoder` en Feign.
- **Seguridad**: Si la API requiere encabezados adicionales (como `Authorization` o `X-API-Key`), agrégalos en la configuración de `RestClient` o en la interfaz de Feign.
- **Timeout**: Configura timeouts en `RestClient` (usando `RestClientBuilder`) o en Feign (usando `Feign.Builder`) si la API tiene tiempos de respuesta variables.
- **Formato de Respuesta**: Asumo que la API retorna el token como una cadena plana. Si retorna un JSON (por ejemplo, `{"token": "eyJhbGci..."}`), ajusta el tipo de retorno a una clase DTO como:

   ```java
  public class TokenResponse {
      private String token;
      // Getters y setters
  }
  ```

**Ejemplo de Configuración de Timeout para RestClient**:

```java
RestClient restClient = RestClient.builder()
        .baseUrl("https://corresponsalias.santander.com.ar")
        .requestFactory(new HttpComponentsClientHttpRequestFactory(
                HttpClientBuilder.create()
                        .setConnectionTimeToLive(5, TimeUnit.SECONDS)
                        .build()))
        .build();
```

**Ejemplo de Configuración de Timeout para Feign**:

```java
@Bean
public Feign.Builder feignBuilder() {
    return Feign.builder()
            .options(new Request.Options(5_000, TimeUnit.MILLISECONDS, 5_000, TimeUnit.MILLISECONDS, true));
}
```

---

### Conclusión
- **RestClient** es ideal si buscas una solución síncrona, ligera y moderna con una API fluida. La integración con `Resilience4j` hace que los reintentos sean fáciles de configurar.
- **Feign Client** es perfecto para arquitecturas de microservicios o si prefieres una API declarativa. También se integra bien con `Resilience4j` para reintentos.

Ambos enfoques cumplen con el requisito de realizar 3 reintentos y son compatibles con **Java 21** y **Spring Boot 3.x**. Si trabajas en un proyecto simple, `RestClient` es más ligero; si estás en un entorno de microservicios con Spring Cloud, `Feign Client` es más adecuado. Ajusta los parámetros de la solicitud (método HTTP, cuerpo, encabezados) según la documentación de la API de Santander.

Si necesitas más detalles (por ejemplo, manejo de errores avanzado, autenticación OAuth, o una implementación específica), por favor proporciónalos.





<br>
<br>
<br>

<br>  




# Test Unitarios y de Integración para Feign Client

<br>

A continuación, extiendo la solución de **Feign Client** proporcionada previamente para incluir **manejo de errores avanzado** y ejemplos de **pruebas unitarias** y **pruebas de integración**. El manejo de errores se implementará utilizando un `ErrorDecoder` personalizado para procesar respuestas HTTP de error (por ejemplo, 4xx, 5xx) y excepciones de red. Las pruebas unitarias usarán **Mockito** para simular el comportamiento del cliente Feign, mientras que las pruebas de integración usarán **TestRestTemplate** y un servidor WireMock para simular la API de Santander.

El contexto sigue siendo la llamada a la URL `https://corresponsalias.santander.com.ar/sb/v3/tokens` para obtener un token, con **3 reintentos** usando `Resilience4j`, en un proyecto con **Spring Boot 3.x** y **Java 21**. Asumo que la solicitud es un **POST** con un cuerpo JSON de credenciales y que la respuesta es una cadena con el token (por ejemplo, `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`).

---

### 1. Manejo de Errores Avanzado con Feign Client
Para manejar errores avanzados, implementaremos un `ErrorDecoder` personalizado que procese diferentes códigos de estado HTTP y excepciones de red, lanzando excepciones específicas según el caso. También registraremos los errores para facilitar la depuración.

**Dependencias en `pom.xml`** (igual que antes, con adiciones para pruebas):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.3</version>
        <relativePath/>
    </parent>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
            <version>4.1.3</version>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.2.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.github.tomakehurst</groupId>
            <artifactId>wiremock-jre8</artifactId>
            <version>2.35.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**Configuración de Resilience4j** 

(en `application.yml`, sin cambios):

```yaml
resilience4j.retry:
  instances:
    tokenFeignClient:
      maxAttempts: 3
      waitDuration: 500ms
      retryExceptions:
        - feign.RetryableException
        - java.net.ConnectException
```

**ErrorDecoder Personalizado**:

```x-java-source
package com.example.demo.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class CustomErrorDecoder implements ErrorDecoder {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String requestUrl = response.request().url();
        int status = response.status();

        // Leer el cuerpo de la respuesta para detalles del error
        String errorMessage = "Unknown error";
        try (var bodyReader = response.body().asReader()) {
            errorMessage = new String(bodyReader.readAllBytes());
        } catch (IOException e) {
            logger.error("Failed to read response body for request: {}", requestUrl, e);
        }

        logger.error("Request to {} failed with status {}: {}", requestUrl, status, errorMessage);

        // Manejo de errores según el código de estado
        return switch (status) {
            case 400 -> new BadRequestException("Bad request: " + errorMessage);
            case 401, 403 -> new AuthenticationException("Authentication failed: " + errorMessage);
            case 404 -> new ResourceNotFoundException("Resource not found: " + requestUrl);
            case 429 -> new RateLimitException("Rate limit exceeded: " + errorMessage);
            case 500, 502, 503, 504 -> new RetryableException("Server error: " + errorMessage);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}

// Excepciones personalizadas
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}

class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}

class RetryableException extends RuntimeException {
    public RetryableException(String message) {
        super(message);
    }
}
```

**Configuración de Feign para Usar el ErrorDecoder**:

```x-java-source
package com.example.demo.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
}
```

**Feign Client (sin cambios)**:

```x-java-source
package com.example.demo.client;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "tokenClient", url = "https://corresponsalias.santander.com.ar")
public interface TokenFeignClient {

    @PostMapping(value = "/sb/v3/tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Retry(name = "tokenFeignClient")
    String getToken(@RequestBody String credentials);
}
```

**Explicación del Manejo de Errores**:

- **CustomErrorDecoder**: Procesa los códigos de estado HTTP y el cuerpo de la respuesta para generar excepciones específicas:
  - **400**: `BadRequestException` para errores de solicitud mal formada.
  - **401/403**: `AuthenticationException` para problemas de autenticación.
  - **404**: `ResourceNotFoundException` si el endpoint no existe.
  - **429**: `RateLimitException` para límites de tasa.
  - **5xx**: `RetryableException` para errores del servidor, que desencadenan reintentos vía `Resilience4j`.
- **Logging**: Se registran los detalles del error (URL, código de estado, mensaje) usando SLF4J.
- **Reintentos**: Los errores 5xx y excepciones de red (`ConnectException`) se reintentan hasta 3 veces, como se configuró en `application.yml`.

<br>
<br>


### 2. Pruebas Unitarias
Las pruebas unitarias simulan el comportamiento del cliente Feign usando **Mockito** para evitar llamadas reales a la API.

**Ejemplo de Prueba Unitaria**:

```x-java-source
package com.example.demo.client;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenFeignClientTest {

    @Mock
    private TokenFeignClient tokenFeignClient;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void testGetTokenSuccess() {
        String credentials = "{\"clientId\": \"test\", \"clientSecret\": \"secret\"}";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        when(tokenFeignClient.getToken(credentials)).thenReturn(expectedToken);

        String actualToken = tokenService.getToken(credentials);

        assertEquals(expectedToken, actualToken);
        verify(tokenFeignClient, times(1)).getToken(credentials);
    }

    @Test
    void testGetTokenFailure() {
        String credentials = "{\"clientId\": \"test\", \"clientSecret\": \"secret\"}";
        when(tokenFeignClient.getToken(credentials)).thenThrow(new FeignException.BadRequest("Bad Request", null, null));

        Exception exception = assertThrows(FeignException.class, () -> tokenService.getToken(credentials));
        assertTrue(exception.getMessage().contains("Bad Request"));
        verify(tokenFeignClient, times(1)).getToken(credentials);
    }
}

// Servicio para pruebas
class TokenService {
    private final TokenFeignClient tokenFeignClient;

    TokenService(TokenFeignClient tokenFeignClient) {
        this.tokenFeignClient = tokenFeignClient;
    }

    String getToken(String credentials) {
        return tokenFeignClient.getToken(credentials);
    }
}
```

**Explicación**:
- **Mockito**: Se usa para simular el comportamiento de `TokenFeignClient`.
- **Casos de prueba**:
  - **Éxito**: Simula una respuesta exitosa con un token.
  - **Fallo**: Simula un error HTTP 400 (`BadRequest`).
- **Verificaciones**: Se asegura que el cliente Feign sea invocado correctamente y que las excepciones se manejen como se espera.

---

### 3. Pruebas de Integración
Las pruebas de integración usan **WireMock** para simular la API de Santander y **TestRestTemplate** para probar la integración completa del cliente Feign con el sistema Spring Boot. WireMock permite configurar respuestas mock para la URL `https://corresponsalias.santander.com.ar/sb/v3/tokens`.

**Configuración de WireMock**:
Asegúrate de que WireMock esté corriendo (puedes iniciarlo como un servidor standalone o embebido en la prueba).

**Ejemplo de Prueba de Integración**:

```x-java-source
package com.example.demo.client;

import com.example.demo.DemoApplication;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
class TokenFeignClientIntegrationTest {

    @Autowired
    private TokenFeignClient tokenFeignClient;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(8089); // Puerto arbitrario
        wireMockServer.start();
        configureFor("localhost", 8089);
    }

    @AfterEach
    void teardown() {
        wireMockServer.stop();
    }

    @Test
    void testGetTokenSuccess() {
        String credentials = "{\"clientId\": \"test\", \"clientSecret\": \"secret\"}";
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        stubFor(post(urlEqualTo("/sb/v3/tokens"))
                .withRequestBody(equalToJson(credentials))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(expectedToken)));

        String actualToken = tokenFeignClient.getToken(credentials);
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void testGetTokenUnauthorized() {
        String credentials = "{\"clientId\": \"test\", \"clientSecret\": \"wrong\"}";

        stubFor(post(urlEqualTo("/sb/v3/tokens"))
                .withRequestBody(equalToJson(credentials))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\": \"Unauthorized\"}")));

        assertThrows(AuthenticationException.class, () -> tokenFeignClient.getToken(credentials));
    }

    @Test
    void testGetTokenServerErrorWithRetry() {
        String credentials = "{\"clientId\": \"test\", \"clientSecret\": \"secret\"}";

        stubFor(post(urlEqualTo("/sb/v3/tokens"))
                .withRequestBody(equalToJson(credentials))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\": \"Service Unavailable\"}")));

        assertThrows(RetryableException.class, () -> tokenFeignClient.getToken(credentials));
        verify(3, postRequestedFor(urlEqualTo("/sb/v3/tokens"))); // Verifica 3 intentos
    }
}
```

**Explicación**:
- **WireMock**: Simula la API de Santander en el puerto 8089, configurando respuestas para diferentes escenarios:
  - **Éxito**: Respuesta 200 con el token.
  - **401**: Error de autenticación.
  - **503**: Error del servidor que desencadena reintentos.
- **TestRestTemplate**: No se usa directamente aquí, ya que probamos el cliente Feign, pero podría usarse para probar un endpoint local de la aplicación.
- **Casos de prueba**:
  - **Éxito**: Verifica que el token se obtenga correctamente.
  - **401**: Verifica que se lance `AuthenticationException`.
  - **503**: Verifica que se realicen 3 intentos antes de fallar con `RetryableException`.

**Configuración Adicional**:
Para que WireMock funcione con la URL real (`https://corresponsalias.santander.com.ar`), debes configurar el cliente Feign para usar el servidor WireMock durante las pruebas. Esto se puede hacer sobrescribiendo la URL en el contexto de prueba:

```java
@FeignClient(name = "tokenClient", url = "${token.api.url:https://corresponsalias.santander.com.ar}")
public interface TokenFeignClient {
    // ...
}
```

Y en `application-test.yml`:

```yaml
token.api.url: http://localhost:8089
```

---

### Conclusión
- **Manejo de Errores Avanzado**: El `CustomErrorDecoder` procesa códigos de estado HTTP y excepciones de red, lanzando excepciones específicas para facilitar el manejo en la aplicación. Los errores 5xx desencadenan reintentos automáticos vía `Resilience4j`.
- **Pruebas Unitarias**: Usan **Mockito** para simular el comportamiento del cliente Feign, probando casos de éxito y fallo sin realizar llamadas reales.
- **Pruebas de Integración**: Usan **WireMock** para simular la API de Santander, probando la integración completa del cliente Feign, incluyendo reintentos y manejo de errores.

**Notas**:
- Ajusta el cuerpo de la solicitud (`credentials`) y el formato de la respuesta según la documentación de la API.
- Si la API requiere encabezados adicionales (por ejemplo, `Authorization`), agrégalos en la interfaz Feign con `@RequestHeader`.
- Para pruebas de integración más realistas, considera usar un entorno de staging o un token de prueba proporcionado por Santander.

