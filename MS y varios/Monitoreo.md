# MONITOREO






¡Claro! Vamos a ver cómo un programador puede configurar y usar **Spring Boot Actuator** y **Micrometer** en un proyecto de Spring Boot, con ejemplos prácticos y enfocados en lo que harías desde el código. Explicaré paso a paso, con fragmentos de código relevantes, para que sea fácil de entender y aplicar, especialmente si estás empezando. Usaré un enfoque práctico desde la perspectiva de un desarrollador: configuración, implementación y cómo leer los resultados.

El objetivo es mostrarte cómo usar **Actuator** para exponer métricas y salud de la aplicación, y **Micrometer** para generar métricas personalizadas y conectarlas con herramientas como Prometheus. Todo será simple, con un proyecto mínimo.

---

### 1. **Spring Boot Actuator**
**Actuator** es una herramienta integrada en Spring Boot que expone endpoints HTTP para monitorear la salud, métricas, variables de entorno y más de tu aplicación. Es ideal para principiantes porque requiere poca configuración y te da información útil de inmediato.

#### Paso 1: Configurar Actuator
1. **Añade la dependencia** en tu archivo `pom.xml` (si usas Maven):
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```
   Si usas Gradle, añade:
   ```gradle
   implementation 'org.springframework.boot:spring-boot-starter-actuator'
   ```

2. **Habilita endpoints** en `application.yml` (o `application.properties`):
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,info # Habilita endpoints específicos
     endpoint:
       health:
         show-details: always # Muestra detalles de salud
   ```

3. **Crea un proyecto básico**:
   Supongamos que tienes una aplicación Spring Boot con un controlador simple:
   ```java
   import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RestController;

   @RestController
   public class HelloController {
       @GetMapping("/hello")
       public String hello() {
           return "¡Hola, Spring Boot!";
       }
   }
   ```

4. **Prueba los endpoints de Actuator**:
   - Inicia tu aplicación (usa `mvn spring-boot:run` o tu IDE).
   - Accede a estos endpoints vía HTTP (puedes usar `curl`, Postman o un navegador):
     - **`http://localhost:8080/actuator/health`**: Muestra el estado de la app.
       ```json
       {
         "status": "UP",
         "components": {
           "diskSpace": {"status": "UP", ...},
           "ping": {"status": "UP"}
         }
       }
       ```
     - **`http://localhost:8080/actuator/metrics`**: Lista métricas disponibles, como uso de memoria o hilos.
       ```json
       {
         "names": [
           "jvm.memory.used",
           "http.server.requests",
           ...
         ]
       }
       ```
     - Para ver una métrica específica: `http://localhost:8080/actuator/metrics/http.server.requests`.
       ```json
       {
         "name": "http.server.requests",
         "measurements": [
           {"statistic": "COUNT", "value": 5},
           {"statistic": "TOTAL_TIME", "value": 0.123}
         ],
         ...
       }
       ```

**Desde el punto de vista del programador**:
- **Qué haces**: Añades la dependencia, configuras los endpoints en `application.yml` y pruebas con HTTP.
- **Qué obtienes**: Visibilidad instantánea sobre salud y métricas básicas (CPU, memoria, requests).
- **Truco**: Usa `management.endpoints.web.base-path=/monitor` para cambiar el prefijo de `/actuator` si quieres personalizar.

---

### 2. **Micrometer**
**Micrometer** es una librería de métricas que se integra con Actuator para generar y exportar métricas personalizadas o estándar a sistemas como Prometheus. Es perfecto para medir cosas específicas, como cuántas veces se llama un endpoint o el tiempo de ejecución de un método.

#### Paso 1: Configurar Micrometer
1. **Añade la dependencia de Micrometer** (para Prometheus, por ejemplo):
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```
   Esto incluye Micrometer y el adaptador para Prometheus.

2. **Habilita el endpoint de Prometheus** en `application.yml`:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus # Añade prometheus
     metrics:
       export:
         prometheus:
           enabled: true
   ```

3. **Añade métricas personalizadas**:
   Vamos a crear una métrica para contar cuántas veces se llama el endpoint `/hello`.
   Modifica el controlador:
   ```java
   import io.micrometer.core.instrument.Counter;
   import io.micrometer.core.instrument.MeterRegistry;
   import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RestController;

   @RestController
   public class HelloController {
       private final Counter helloCounter;

       public HelloController(MeterRegistry meterRegistry) {
           // Registra un contador personalizado
           this.helloCounter = Counter.builder("hello.requests.total")
                   .description("Número de veces que se llama al endpoint /hello")
                   .tags("endpoint", "/hello")
                   .register(meterRegistry);
       }

       @GetMapping("/hello")
       public String hello() {
           helloCounter.increment(); // Incrementa el contador
           return "¡Hola, Spring Boot!";
       }
   }
   ```

4. **Prueba las métricas**:
   - Llama varias veces a `http://localhost:8080/hello`.
   - Accede a `http://localhost:8080/actuator/prometheus`. Verás algo como:
     ```prometheus
     # HELP hello_requests_total Número de veces que se llama al endpoint /hello
     # TYPE hello_requests_total counter
     hello_requests_total{endpoint="/hello"} 5.0
     ```
   - Esto muestra que `/hello` fue llamado 5 veces.

5. **Opcional: Conecta con Prometheus y Grafana**:
   - Instala Prometheus con Docker:
     ```bash
     docker run -d -p 9090:9090 prom/prometheus
     ```
   - Configura `prometheus.yml` para scrapear tu app:
     ```yaml
     scrape_configs:
       - job_name: 'spring-app'
         metrics_path: '/actuator/prometheus'
         static_configs:
           - targets: ['host.docker.internal:8080']
     ```
   - Instala Grafana (Docker: `docker run -d -p 3000:3000 grafana/grafana`) y añade Prometheus como fuente de datos.
   - Crea un dashboard en Grafana para visualizar `hello_requests_total`.

**Desde el punto de vista del programador**:
- **Qué haces**: Inyectas `MeterRegistry` en tu código, defines contadores o temporizadores (como `Counter` o `Timer`), y usas el endpoint `/actuator/prometheus`.
- **Qué obtienes**: Métricas personalizadas (ej: número de llamadas a `/hello`) que puedes visualizar en herramientas como Grafana.
- **Truco**: Usa tags en las métricas (`endpoint=/hello`) para filtrar en Prometheus/Grafana.

---

### Ejemplo Completo
**Estructura del proyecto**:
```
src/main/java/com/example/demo/
  - DemoApplication.java
  - HelloController.java
src/main/resources/
  - application.yml
pom.xml
```

**Código completo**:
- `pom.xml`:
  ```xml
  <dependencies>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-web</artifactId>
      </dependency>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-actuator</artifactId>
      </dependency>
      <dependency>
          <groupId>io.micrometer</groupId>
          <artifactId>micrometer-registry-prometheus</artifactId>
      </dependency>
  </dependencies>
  ```
- `application.yml`:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,metrics,prometheus
    endpoint:
      health:
        show-details: always
  ```
- `HelloController.java` (como arriba).

**Cómo probar**:
1. Ejecuta la app (`mvn spring-boot:run`).
2. Llama a `http://localhost:8080/hello` varias veces.
3. Revisa métricas en `http://localhost:8080/actuator/prometheus`.
4. Opcional: Configura Prometheus y Grafana para dashboards.

---

### Perspectiva del Programador
- **Facilidad**: Actuator requiere solo una dependencia y un archivo de configuración. Micrometer es igual de simple si usas contadores básicos.
- **Debugging**: Usa `/actuator/health` para verificar si tu app está viva. Usa `/actuator/metrics` para detectar cuellos de botella (ej: tiempo de respuesta).
- **Escalabilidad**: Las métricas de Micrometer se integran con Prometheus, lo que te prepara para entornos de producción sin cambiar código.
- **Errores comunes**:
  - Olvidar exponer endpoints (`include: *` para todos).
  - No inyectar `MeterRegistry` en el constructor.
  - No configurar Prometheus correctamente (verifica `targets` en `prometheus.yml`).

**Siguientes pasos**:
- Añade un `Timer` para medir el tiempo de ejecución de métodos:
  ```java
  Timer timer = Timer.builder("hello.duration").register(meterRegistry);
  timer.record(() -> /* tu lógica */);
  ```


- Explora otros endpoints de Actuator como `/actuator/info` para metadatos de la app.









<br>
<br>
<br>


---

<br>
<br>
<br>

Desde el punto de vista del programador, ejemplificar Spring Boot Actuator y Micrometer?

<br>

## Spring Boot Actuator y Micrometer


<br>




¡Claro! Vamos a ver cómo un programador puede configurar y usar **Spring Boot Actuator** y **Micrometer** en un proyecto de Spring Boot, con ejemplos prácticos y enfocados en lo que harías desde el código. Explicaré paso a paso, con fragmentos de código relevantes, para que sea fácil de entender y aplicar, especialmente si estás empezando. Usaré un enfoque práctico desde la perspectiva de un desarrollador: configuración, implementación y cómo leer los resultados.

El objetivo es mostrarte cómo usar **Actuator** para exponer métricas y salud de la aplicación, y **Micrometer** para generar métricas personalizadas y conectarlas con herramientas como Prometheus. Todo será simple, con un proyecto mínimo.

---

### 1. **Spring Boot Actuator**
**Actuator** es una herramienta integrada en Spring Boot que expone endpoints HTTP para monitorear la salud, métricas, variables de entorno y más de tu aplicación. Es ideal para principiantes porque requiere poca configuración y te da información útil de inmediato.

#### Paso 1: Configurar Actuator
1. **Añade la dependencia** en tu archivo `pom.xml` (si usas Maven):
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```
   Si usas Gradle, añade:
   ```gradle
   implementation 'org.springframework.boot:spring-boot-starter-actuator'
   ```

2. **Habilita endpoints** en `application.yml` (o `application.properties`):
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,info # Habilita endpoints específicos
     endpoint:
       health:
         show-details: always # Muestra detalles de salud
   ```

3. **Crea un proyecto básico**:
   Supongamos que tienes una aplicación Spring Boot con un controlador simple:
   ```java
   import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RestController;

   @RestController
   public class HelloController {
       @GetMapping("/hello")
       public String hello() {
           return "¡Hola, Spring Boot!";
       }
   }
   ```

4. **Prueba los endpoints de Actuator**:
   - Inicia tu aplicación (usa `mvn spring-boot:run` o tu IDE).
   - Accede a estos endpoints vía HTTP (puedes usar `curl`, Postman o un navegador):
     - **`http://localhost:8080/actuator/health`**: Muestra el estado de la app.
       ```json
       {
         "status": "UP",
         "components": {
           "diskSpace": {"status": "UP", ...},
           "ping": {"status": "UP"}
         }
       }
       ```
     - **`http://localhost:8080/actuator/metrics`**: Lista métricas disponibles, como uso de memoria o hilos.
       ```json
       {
         "names": [
           "jvm.memory.used",
           "http.server.requests",
           ...
         ]
       }
       ```
     - Para ver una métrica específica: `http://localhost:8080/actuator/metrics/http.server.requests`.
       ```json
       {
         "name": "http.server.requests",
         "measurements": [
           {"statistic": "COUNT", "value": 5},
           {"statistic": "TOTAL_TIME", "value": 0.123}
         ],
         ...
       }
       ```

**Desde el punto de vista del programador**:
- **Qué haces**: Añades la dependencia, configuras los endpoints en `application.yml` y pruebas con HTTP.
- **Qué obtienes**: Visibilidad instantánea sobre salud y métricas básicas (CPU, memoria, requests).
- **Truco**: Usa `management.endpoints.web.base-path=/monitor` para cambiar el prefijo de `/actuator` si quieres personalizar.

---

### 2. **Micrometer**
**Micrometer** es una librería de métricas que se integra con Actuator para generar y exportar métricas personalizadas o estándar a sistemas como Prometheus. Es perfecto para medir cosas específicas, como cuántas veces se llama un endpoint o el tiempo de ejecución de un método.

#### Paso 1: Configurar Micrometer
1. **Añade la dependencia de Micrometer** (para Prometheus, por ejemplo):
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```
   Esto incluye Micrometer y el adaptador para Prometheus.

2. **Habilita el endpoint de Prometheus** en `application.yml`:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus # Añade prometheus
     metrics:
       export:
         prometheus:
           enabled: true
   ```

3. **Añade métricas personalizadas**:
   Vamos a crear una métrica para contar cuántas veces se llama el endpoint `/hello`.
   Modifica el controlador:
   ```java
   import io.micrometer.core.instrument.Counter;
   import io.micrometer.core.instrument.MeterRegistry;
   import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RestController;

   @RestController
   public class HelloController {
       private final Counter helloCounter;

       public HelloController(MeterRegistry meterRegistry) {
           // Registra un contador personalizado
           this.helloCounter = Counter.builder("hello.requests.total")
                   .description("Número de veces que se llama al endpoint /hello")
                   .tags("endpoint", "/hello")
                   .register(meterRegistry);
       }

       @GetMapping("/hello")
       public String hello() {
           helloCounter.increment(); // Incrementa el contador
           return "¡Hola, Spring Boot!";
       }
   }
   ```

4. **Prueba las métricas**:
   - Llama varias veces a `http://localhost:8080/hello`.
   - Accede a `http://localhost:8080/actuator/prometheus`. Verás algo como:
     ```prometheus
     # HELP hello_requests_total Número de veces que se llama al endpoint /hello
     # TYPE hello_requests_total counter
     hello_requests_total{endpoint="/hello"} 5.0
     ```
   - Esto muestra que `/hello` fue llamado 5 veces.

5. **Opcional: Conecta con Prometheus y Grafana**:
   - Instala Prometheus con Docker:
     ```bash
     docker run -d -p 9090:9090 prom/prometheus
     ```
   - Configura `prometheus.yml` para scrapear tu app:
     ```yaml
     scrape_configs:
       - job_name: 'spring-app'
         metrics_path: '/actuator/prometheus'
         static_configs:
           - targets: ['host.docker.internal:8080']
     ```
   - Instala Grafana (Docker: `docker run -d -p 3000:3000 grafana/grafana`) y añade Prometheus como fuente de datos.
   - Crea un dashboard en Grafana para visualizar `hello_requests_total`.

**Desde el punto de vista del programador**:
- **Qué haces**: Inyectas `MeterRegistry` en tu código, defines contadores o temporizadores (como `Counter` o `Timer`), y usas el endpoint `/actuator/prometheus`.
- **Qué obtienes**: Métricas personalizadas (ej: número de llamadas a `/hello`) que puedes visualizar en herramientas como Grafana.
- **Truco**: Usa tags en las métricas (`endpoint=/hello`) para filtrar en Prometheus/Grafana.

---

### Ejemplo Completo
**Estructura del proyecto**:
```
src/main/java/com/example/demo/
  - DemoApplication.java
  - HelloController.java
src/main/resources/
  - application.yml
pom.xml
```

**Código completo**:
- `pom.xml`:
  ```xml
  <dependencies>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-web</artifactId>
      </dependency>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-actuator</artifactId>
      </dependency>
      <dependency>
          <groupId>io.micrometer</groupId>
          <artifactId>micrometer-registry-prometheus</artifactId>
      </dependency>
  </dependencies>
  ```
- `application.yml`:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,metrics,prometheus
    endpoint:
      health:
        show-details: always
  ```
- `HelloController.java` (como arriba).

**Cómo probar**:
1. Ejecuta la app (`mvn spring-boot:run`).
2. Llama a `http://localhost:8080/hello` varias veces.
3. Revisa métricas en `http://localhost:8080/actuator/prometheus`.
4. Opcional: Configura Prometheus y Grafana para dashboards.

---

### Perspectiva del Programador
- **Facilidad**: Actuator requiere solo una dependencia y un archivo de configuración. Micrometer es igual de simple si usas contadores básicos.
- **Debugging**: Usa `/actuator/health` para verificar si tu app está viva. Usa `/actuator/metrics` para detectar cuellos de botella (ej: tiempo de respuesta).
- **Escalabilidad**: Las métricas de Micrometer se integran con Prometheus, lo que te prepara para entornos de producción sin cambiar código.
- **Errores comunes**:
  - Olvidar exponer endpoints (`include: *` para todos).
  - No inyectar `MeterRegistry` en el constructor.
  - No configurar Prometheus correctamente (verifica `targets` en `prometheus.yml`).

**Siguientes pasos**:
- Añade un `Timer` para medir el tiempo de ejecución de métodos:
  ```java
  Timer timer = Timer.builder("hello.duration").register(meterRegistry);
  timer.record(() -> /* tu lógica */);
  ```
- Explora otros endpoints de Actuator como `/actuator/info` para metadatos de la app.
