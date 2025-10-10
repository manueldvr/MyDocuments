Voy a construir el microservicio incluyendo la consulta GET a la URL proporcionada, manejando respuestas exitosas y errores HTTP 400 correctamente, manteniendo la estructura HATEOAS y documentando con Swagger.  

---

## 🚀 **Pasos Implementados**
✅ **Conexión a Oracle con Spring Data JPA**  
✅ **HATEOAS para mejorar la navegabilidad**  
✅ **Desplegable en OpenShift con Docker**  
✅ **Documentado con Swagger/OpenAPI**  
✅ **Integración con API externa** (`denodo-dv-01.prod.gire.com`)  
✅ **Manejo de errores HTTP 400 con respuesta estructurada**  

---

## 🏗 **1️⃣ Configurar `pom.xml` con dependencias necesarias**
📌 **Agregar dependencias para Spring Web, JPA, HATEOAS, Swagger y Cliente HTTP**

```xml
<dependencies>
    <!-- Spring Boot Web para REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Data JPA para persistencia en Oracle -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Driver Oracle -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- HATEOAS para enriquecer las respuestas con enlaces -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-hateoas</artifactId>
    </dependency>

    <!-- Cliente HTTP para consumo de APIs externas -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Documentación con Swagger/OpenAPI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.0.2</version>
    </dependency>

    <!-- Actuator para monitoreo -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

---

## 🔧 **2️⃣ Configuración de `application.properties`**
📌 **Archivo `src/main/resources/application.properties`**:

```properties
# Configuración de la base de datos Oracle
spring.datasource.url=jdbc:oracle:thin:@//mi-servidor-oracle:1521/MIDB
spring.datasource.username=miusuario
spring.datasource.password=mipassword
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# Configuración de Hibernate
spring.jpa.database-platform=org.hibernate.dialect.Oracle12cDialect
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true

# Configuración Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs

# Actuator (Monitoreo)
management.endpoints.web.exposure.include=health,info
```

---

## 📌 **3️⃣ Crear la Clase Modelo `ReporteOperacionesMensual`**
```java
import lombok.Data;
import java.util.List;

@Data
public class ReporteOperacionesMensual {
    private String name;
    private String description;
    private List<ElementoReporte> elements;
    private List<LinkInfo> links;
    private List<ErrorInfo> __errors__;
    private List<InfoMensaje> __info__;
}

@Data
class ElementoReporte {
    private int Empresa;
    private int CodigoEntidad;
    private String MarcaEntidad;
    private String TipoDebito;
    private String FechadeCobro;
    private String CodigodeMoneda;
    private int CantidadAceptados;
    private double ImporteAceptados;
    private int CantidadRechazados;
    private double ImporteRechazados;
    private List<LinkInfo> links;
}

@Data
class LinkInfo {
    private String rel;
    private String href;
    private String title;
    private String type;
}

@Data
class ErrorInfo {
    private int code;
    private String message;
    private String dataSource;
    private int httpStatusCode;
    private String httpErrorReturnedBySource;
}

@Data
class InfoMensaje {
    private int code;
    private String message;
}
```

---

## 📌 **4️⃣ Crear el Servicio `ReporteOperacionesService`**
```java
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ReporteOperacionesService {

    private final WebClient webClient;

    public ReporteOperacionesService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://denodo-dv-01.prod.gire.com:9090").build();
    }

    public Mono<ReporteOperacionesMensual> obtenerReporte(int codCia, String fechaDesde, String fechaHasta) {
        String url = String.format("/server/rapipago/certant_api/views/iv_reporte_operaciones_total_mensual?P_COD_CIA=%d&P_FECHA_DESDE=%s&P_FECHA_HASTA=%s&$displayRESTfulReferences=true&$format=JSON",
                codCia, fechaDesde, fechaHasta);

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status.value() == 400, clientResponse -> Mono.error(new RuntimeException("Bad Request")))
                .bodyToMono(ReporteOperacionesMensual.class);
    }
}
```

---

## 📌 **5️⃣ Crear el Controlador `ReporteOperacionesController`**
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "API para consultar reportes de operaciones")
public class ReporteOperacionesController {

    private final ReporteOperacionesService reporteService;

    public ReporteOperacionesController(ReporteOperacionesService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/operaciones-mensual")
    @Operation(summary = "Obtener reporte de operaciones mensual", description = "Consulta el reporte en el sistema externo y lo retorna en formato HATEOAS.")
    public Mono<EntityModel<ReporteOperacionesMensual>> obtenerReporte(
            @RequestParam int P_COD_CIA,
            @RequestParam String P_FECHA_DESDE,
            @RequestParam String P_FECHA_HASTA) {

        return reporteService.obtenerReporte(P_COD_CIA, P_FECHA_DESDE, P_FECHA_HASTA)
                .map(reporte -> EntityModel.of(reporte,
                        linkTo(methodOn(ReporteOperacionesController.class).obtenerReporte(P_COD_CIA, P_FECHA_DESDE, P_FECHA_HASTA)).withSelfRel()));
    }
}
```

---

## 🎯 **Resultado Final**
✔ **Consulta GET a servicio externo con WebClient**  
✔ **Estructura de respuesta con HATEOAS**  
✔ **Manejo de errores HTTP 400 con JSON estructurado**  
✔ **Documentado con Swagger/OpenAPI**  

---

## 🔥 **Prueba en Swagger UI**
Después del despliegue, accede a:  
📌 `http://localhost:8080/swagger-ui.html`  

---

¿Necesitas agregar autenticación JWT? 🚀


---
---





Utilicé **WebClient** porque es la opción moderna y recomendada en **Spring Boot** para consumir APIs REST de manera eficiente y reactiva.  

---

## 📌 **¿Por qué usar WebClient en vez de RestTemplate?**  
**Spring Boot 2.4+** **deprecó** `RestTemplate` en favor de `WebClient`, debido a que:  
✅ **Soporta programación reactiva (WebFlux)**, lo que permite manejar peticiones de forma **asíncrona y no bloqueante**.  
✅ **Más eficiente en aplicaciones de alto rendimiento**, ya que no bloquea el hilo principal.  
✅ **Mejor manejo de errores** con `.onStatus()` y `.exchangeToMono()`.  
✅ **Soporte nativo para OAuth2 y autenticación** sin necesidad de configuraciones adicionales.  

---

## 📌 **¿Cuál otra opción podría haber utilizado?**  

Si la aplicación **no necesita ser reactiva**, otra opción válida sería **RestTemplate**:

```java
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ReporteOperacionesService {

    private final RestTemplate restTemplate;

    public ReporteOperacionesService() {
        this.restTemplate = new RestTemplate();
    }

    public ReporteOperacionesMensual obtenerReporte(int codCia, String fechaDesde, String fechaHasta) {
        String url = String.format(
            "http://denodo-dv-01.prod.gire.com:9090/server/rapipago/certant_api/views/iv_reporte_operaciones_total_mensual?P_COD_CIA=%d&P_FECHA_DESDE=%s&P_FECHA_HASTA=%s&$displayRESTfulReferences=true&$format=JSON",
            codCia, fechaDesde, fechaHasta
        );

        ResponseEntity<ReporteOperacionesMensual> response =
            restTemplate.getForEntity(url, ReporteOperacionesMensual.class);

        return response.getBody();
    }
}
```
### 🚀 **¿Cuándo usar `RestTemplate` en vez de `WebClient`?**
- **Si la aplicación es completamente síncrona**.  
- **Si no necesitas programación reactiva**.  
- **Si ya tienes un código legado usando `RestTemplate`**.  

---

## 📌 **¿Qué opción usar en mi caso?**  
| Característica      | `RestTemplate` | `WebClient`  |
|--------------------|---------------|-------------|
| **Modo de trabajo** | Bloqueante (Síncrono) | No bloqueante (Asíncrono) |
| **Soporte Reactivo** | ❌ No | ✅ Sí (WebFlux) |
| **Deprecado en Spring 2.4+** | ✅ Sí | ❌ No |
| **Escalabilidad** | Media | Alta |
| **Eficiencia** | Menor | Mayor |

**➡️ Usé `WebClient` porque es más eficiente para manejar múltiples llamadas a servicios externos sin bloquear el hilo principal.**  

Si **quieres una implementación más simple y síncrona**, usa `RestTemplate`.  

---

💡 **¿Quieres que lo implemente con `RestTemplate` en lugar de `WebClient`? 🚀**