# BFF API para grandes volumenes de datos


Cuando un servicio en una API BFF con Spring Boot necesita traer una cantidad considerable de registros desde la base de datos, esto puede generar problemas de rendimiento, como tiempos de respuesta largos o consumo excesivo de memoria. Para resolverlo, hay varias estrategias que puedes aplicar, dependiendo del caso de uso y las necesidades del frontend. A continuación, te detallo cómo abordarlo de manera eficiente:

---

### 1. Implementar paginación
La paginación es la solución más común y práctica para manejar grandes volúmenes de datos. Devuelves los registros en "páginas" más pequeñas y el frontend los solicita según sea necesario.

#### En el backend
Usa `Pageable` de Spring Data para paginar los resultados de la base de datos.

##### Ejemplo en el servicio
```java
package com.example.bff.service;

import com.example.bff.dto.FrontendResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FrontendService {

    private final OrderRepository orderRepository; // Suponiendo un repositorio JPA

    public FrontendService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Page<FrontendResponseDTO> getUserOrders(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderRepository.findByUserId(userId, pageable);

        return ordersPage.map(order -> {
            FrontendResponseDTO dto = new FrontendResponseDTO();
            dto.setOrderId(order.getId());
            dto.setAmount(order.getAmount());
            return dto;
        });
    }
}

// DTO para la respuesta
@Data
public class FrontendResponseDTO {
    private Long orderId;
    private double amount;
}

// Repositorio JPA
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(String userId, Pageable pageable);
}
```

##### En el controlador
```java
package com.example.bff.controller;

import com.example.bff.dto.FrontendResponseDTO;
import com.example.bff.service.FrontendService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frontend")
public class FrontendController {

    private final FrontendService frontendService;

    public FrontendController(FrontendService frontendService) {
        this.frontendService = frontendService;
    }

    @GetMapping("/user-orders/{userId}")
    public Page<FrontendResponseDTO> getUserOrders(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return frontendService.getUserOrders(userId, page, size);
    }
}
```

#### Respuesta al frontend
El frontend recibirá algo como:
```json
{
  "content": [
    {"orderId": 1, "amount": 50.0},
    {"orderId": 2, "amount": 75.5}
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0
  },
  "totalPages": 5,
  "totalElements": 50,
  "number": 0,
  "size": 10
}
```
El frontend puede usar `pageNumber` y `totalPages` para navegar entre las páginas.

#### Ventajas
- Reduce la carga en la base de datos y la red.
- Escala bien con grandes volúmenes de datos.
- Compatible con interfaces de usuario que muestran listas largas (ej. tablas o scrolls infinitos).

---

### 2. Usar filtros o búsqueda específica
Si el frontend no necesita todos los registros, permite filtrarlos desde el inicio para disminuir la cantidad de datos devueltos.

#### Ejemplo en el controlador
```java
@GetMapping("/user-orders/{userId}")
public Page<FrontendResponseDTO> getUserOrders(
        @PathVariable String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status) { // Filtro opcional
    return frontendService.getUserOrders(userId, page, size, status);
}
```

#### En el servicio
```java
public Page<FrontendResponseDTO> getUserOrders(String userId, int page, int size, String status) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Order> ordersPage;
    if (status != null && !status.isEmpty()) {
        ordersPage = orderRepository.findByUserIdAndStatus(userId, status, pageable);
    } else {
        ordersPage = orderRepository.findByUserId(userId, pageable);
    }
    return ordersPage.map(this::mapToDTO);
}
```

#### Ventajas
- El frontend tiene control granular sobre qué datos necesita.
- Menor carga en el backend al evitar procesar datos innecesarios.

---

### 3. Procesamiento por lotes (batch processing)
Si el frontend necesita procesar todos los registros (por ejemplo, para generar un informe), pero no mostrarlos de golpe, puedes usar un enfoque de procesamiento por lotes con streams o iteradores.

#### Ejemplo con `Stream`
```java
public List<FrontendResponseDTO> getAllUserOrdersInBatch(String userId) {
    Stream<Order> orderStream = orderRepository.findByUserIdStream(userId);
    return orderStream.map(this::mapToDTO)
                     .collect(Collectors.toList());
}

// En el repositorio
@Query("SELECT o FROM Order o WHERE o.userId = :userId")
Stream<Order> findByUserIdStream(@Param("userId") String userId);
```

#### Nota
- Usa `@Transactional` para mantener la sesión de Hibernate abierta durante el procesamiento del stream.
- Este enfoque es útil para tareas en segundo plano o exportaciones, no para respuestas inmediatas al frontend.

---

### 4. Cachear resultados
Si los datos no cambian con frecuencia, usa una caché (como Spring Cache con Redis o Caffeine) para evitar consultas repetitivas a la base de datos.

#### Ejemplo con `@Cacheable`
```java
@Cacheable(value = "userOrders", key = "#userId + '-' + #page + '-' + #size")
public Page<FrontendResponseDTO> getUserOrders(String userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return orderRepository.findByUserId(userId, pageable).map(this::mapToDTO);
}
```

#### Configuración de caché
Agrega la dependencia `spring-boot-starter-cache` y configura un proveedor como Redis:
```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
```

#### Ventajas
- Reduce la carga en la base de datos para consultas frecuentes.
- Mejora el tiempo de respuesta al frontend.

---

### 5. Respuesta asíncrona o en streaming
Para casos donde el frontend puede manejar datos en tiempo real o parciales, usa WebSockets o Server-Sent Events (SSE) para enviar los registros progresivamente.

#### Ejemplo con SSE
```java
@GetMapping(value = "/user-orders/{userId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<FrontendResponseDTO> streamUserOrders(@PathVariable String userId) {
    return Flux.fromStream(orderRepository.findByUserIdStream(userId))
               .map(this::mapToDTO);
}
```

#### Dependencia
Agrega `spring-boot-starter-webflux` para usar `Flux`.

#### Respuesta al frontend
El frontend recibirá los datos como un flujo continuo:
```
data: {"orderId": 1, "amount": 50.0}
data: {"orderId": 2, "amount": 75.5}
```

#### Ventajas
- Ideal para grandes datasets que el frontend procesa en tiempo real.
- Reduce la memoria usada en el servidor al no cargar todo de golpe.

---

### 6. Optimización en la base de datos
- **Índices**: Asegúrate de que las columnas usadas en las consultas (como `userId`) tengan índices.
- **Proyecciones**: Usa consultas que solo traigan los campos necesarios en lugar de toda la entidad:
  ```java
  @Query("SELECT o.id, o.amount FROM Order o WHERE o.userId = :userId")
  Page<Object[]> findOrderProjectionsByUserId(@Param("userId") String userId, Pageable pageable);
  ```
- **Límite superior**: Define un tamaño máximo razonable para evitar sobrecargar el sistema.

---

### Recomendación según el caso
- **Listas en pantalla**: Usa paginación (opción 1) con filtros (opción 2).
- **Informes o exportaciones**: Usa batch processing (opción 3) o streaming (opción 5).
- **Datos estáticos**: Añade caché (opción 4).
- **Tiempo real**: Implementa SSE o WebSockets (opción 5).



---

.  
.  

.  

# Fetch Size


Sí, configurar el tamaño de recuperación (`fetch size`) con `JdbcTemplate` al ejecutar un procedimiento almacenado (stored procedure) es una excelente opción para manejar grandes cantidades de registros de manera eficiente. Esto permite controlar cuántas filas se traen de la base de datos a la memoria en cada iteración, reduciendo el consumo de recursos y mejorando el rendimiento. A continuación, te explico cómo implementarlo en el contexto de una API BFF con Spring Boot:

---

### ¿Qué es el `fetch size`?
El `fetch size` determina cuántas filas se recuperan de la base de datos en cada ronda de comunicación entre el driver JDBC y el servidor. Por defecto, este valor puede ser pequeño (como 10), lo que genera muchas idas y venidas a la base de datos si hay miles de registros. Ajustarlo a un valor mayor optimiza el proceso, pero debe equilibrarse para no saturar la memoria.

---

### 1. Configurar el proyecto
Asegúrate de tener las dependencias necesarias:
- `spring-boot-starter-jdbc` para usar `JdbcTemplate`.
- El driver JDBC correspondiente a tu base de datos (por ejemplo, `mysql-connector-java` para MySQL).

#### Ejemplo en `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

### 2. Crear el procedimiento almacenado
Supongamos que tienes un procedimiento almacenado en la base de datos que devuelve una gran cantidad de pedidos para un usuario. Ejemplo en MySQL:
```sql
DELIMITER //
CREATE PROCEDURE GetUserOrders(IN userId VARCHAR(50))
BEGIN
    SELECT id, amount FROM orders WHERE user_id = userId;
END //
DELIMITER ;
```

---

### 3. Configurar `JdbcTemplate` con `fetch size`
Spring Boot configura `JdbcTemplate` como un bean automáticamente si usas `spring-boot-starter-jdbc`. Para establecer el `fetch size`, necesitas personalizar cómo se ejecuta la consulta al procedimiento almacenado.

#### En el servicio
```java
package com.example.bff.service;

import com.example.bff.dto.FrontendResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class FrontendService {

    private final JdbcTemplate jdbcTemplate;

    public FrontendService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FrontendResponseDTO> getUserOrders(String userId) {
        // Configurar el fetch size
        jdbcTemplate.setFetchSize(1000); // Ajusta según tus necesidades (ej. 1000 filas por ronda)

        // Llamar al procedimiento almacenado
        return jdbcTemplate.query(
            "{call GetUserOrders(?)}",
            preparedStatement -> preparedStatement.setString(1, userId),
            this::mapRowToDTO
        );
    }

    private FrontendResponseDTO mapRowToDTO(ResultSet rs, int rowNum) throws SQLException {
        FrontendResponseDTO dto = new FrontendResponseDTO();
        dto.setOrderId(rs.getLong("id"));
        dto.setAmount(rs.getDouble("amount"));
        return dto;
    }
}
```

#### DTO
```java
package com.example.bff.dto;

import lombok.Data;

@Data
public class FrontendResponseDTO {
    private Long orderId;
    private double amount;
}
```

#### En el controlador
```java
package com.example.bff.controller;

import com.example.bff.dto.FrontendResponseDTO;
import com.example.bff.service.FrontendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/frontend")
public class FrontendController {

    private final FrontendService frontendService;

    public FrontendController(FrontendService frontendService) {
        this.frontendService = frontendService;
    }

    @GetMapping("/user-orders/{userId}")
    public List<FrontendResponseDTO> getUserOrders(@PathVariable String userId) {
        return frontendService.getUserOrders(userId);
    }
}
```

---

### 4. Procesamiento por lotes con `fetch size`
Si el número de registros es muy grande y no quieres cargarlos todos en memoria, puedes usar un enfoque de procesamiento por lotes con un `ResultSet` explícito.

#### Ejemplo con `ResultSet`
```java
public void processUserOrdersInBatches(String userId, Consumer<FrontendResponseDTO> processor) {
    jdbcTemplate.setFetchSize(1000); // Tamaño del lote

    jdbcTemplate.query(
        con -> {
            CallableStatement cs = con.prepareCall("{call GetUserOrders(?)}");
            cs.setString(1, userId);
            return cs;
        },
        rs -> {
            while (rs.next()) {
                FrontendResponseDTO dto = mapRowToDTO(rs, rs.getRow());
                processor.accept(dto); // Procesa cada registro individualmente
            }
        }
    );
}
```

#### Uso en el controlador
```java
@GetMapping(value = "/user-orders/{userId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<FrontendResponseDTO> streamUserOrders(@PathVariable String userId) {
    return Flux.create(sink -> {
        frontendService.processUserOrdersInBatches(userId, dto -> sink.next(dto));
        sink.complete();
    });
}
```
Esto usa `Flux` de Spring WebFlux para enviar los datos como un flujo al frontend.

---

### 5. Ajustar el `fetch size`
- **Valor pequeño (ej. 10-100)**: Más idas a la base de datos, menos memoria utilizada. Útil para datasets pequeños o cuando la memoria es crítica.
- **Valor grande (ej. 500-1000)**: Menos idas a la base de datos, mejor rendimiento en datasets grandes, pero más consumo de memoria.
- **Pruebas**: Ajusta el valor según el tamaño promedio de tus registros y el hardware disponible. Por ejemplo, empieza con 1000 y haz pruebas de estrés.

---

### 6. Consideraciones
- **Base de datos**: El `fetch size` debe ser soportado por el driver JDBC y la base de datos. Por ejemplo, en MySQL, funciona bien con el conector estándar.
- **Transacciones**: Si usas transacciones largas, asegúrate de gestionar el tiempo de vida de la conexión.
- **Escalabilidad**: Para datasets extremadamente grandes, combina esto con paginación manual (pasando parámetros al procedimiento almacenado, como `OFFSET` y `LIMIT`).

#### Ejemplo con paginación en el procedimiento
```sql
DELIMITER //
CREATE PROCEDURE GetUserOrdersPaged(IN userId VARCHAR(50), IN startRow INT, IN pageSize INT)
BEGIN
    SELECT id, amount FROM orders WHERE user_id = userId LIMIT startRow, pageSize;
END //
DELIMITER ;
```

```java
public List<FrontendResponseDTO> getUserOrdersPaged(String userId, int page, int size) {
    jdbcTemplate.setFetchSize(size);
    int startRow = page * size;
    return jdbcTemplate.query(
        "{call GetUserOrdersPaged(?, ?, ?)}",
        ps -> {
            ps.setString(1, userId);
            ps.setInt(2, startRow);
            ps.setInt(3, size);
        },
        this::mapRowToDTO
    );
}
```

---

### Ventajas de usar `fetch size` con `JdbcTemplate`
- Control fino sobre la cantidad de datos traídos por ronda.
- Compatible con procedimientos almacenados existentes.
- Menor overhead que cargar todo en memoria y luego paginar en la aplicación.

### Desventajas
- No ofrece metadatos de paginación como `Page` de Spring Data JPA (total de páginas, etc.).
- Requiere ajustes manuales según el caso.

¿Te gustaría que profundice en cómo integrar esto con un frontend específico o en cómo manejar errores si el procedimiento falla?