# CASO: COBRANZAS__Debito directo/automatico

Este script SQL realiza un reporte financiero consolidado sobre transacciones almacenadas en varias tablas (`RESUMEN_FINANCIERAS`, `DETALLE_ESTADO`, `ENTIDADES`, y `DETALLE_TIPO_DEBITO`). A continuación, te explico detalladamente qué hace:

### **Secciones del script:**

1. **Selección de columnas y métricas**:
    - `RF.COD_CIA`: Identifica la empresa.
    - `RF.COD_ENTIDAD`: Representa el código de la entidad asociada.
    - `MAX(E.DESCRIPCION)`: Obtiene la descripción (o "marca") de la entidad asociada.
    - `MAX(TD.DESCRIPCION)`: Obtiene el tipo de débito relacionado con la transacción.
    - `TO_CHAR(trunc(RF.FH_INICIO_CARGA), 'dd/mm/yyyy')`: Muestra la fecha de cobro truncada al día (sin hora) en formato `dd/mm/yyyy`.
    - `RF.COD_MONEDA`: Identifica la moneda de las transacciones.
    - `SUM(RF.CANTIDAD_ACEPTADOS)`: Suma la cantidad de transacciones aceptadas.
    - `SUM(RF.IMPORTE_ACEPTADOS)`: Suma los importes correspondientes a las transacciones aceptadas.
    - `SUM(RF.CANTIDAD_RECHAZADOS)`: Suma la cantidad de transacciones rechazadas.
    - `SUM(RF.IMPORTE_RECHAZADOS)`: Suma los importes correspondientes a las transacciones rechazadas.

2. **Tablas involucradas**:
    - `PUCARA.RESUMEN_FINANCIERAS (RF)`: Tabla principal donde se encuentran los datos financieros resumidos.
    - `PUCARA.DETALLE_ESTADO (DE)`: Contiene detalles de los estados.
    - `GIRE.ENTIDADES (E)`: Proporciona las descripciones de las entidades.
    - `PUCARA.DETALLE_TIPO_DEBITO (TD)`: Contiene las descripciones de los tipos de débito.

3. **Condiciones del `WHERE`**:
    - `RF.COD_CIA IN (:COD_CIA)`: Filtra las transacciones según la empresa proporcionada en el parámetro `:COD_CIA`.
    - `( ( :SUB_EMP is null and RF.COD_ENTIDAD = RF.COD_ENTIDAD ) or (:SUB_EMP is not null and RF.COD_ENTIDAD in (:SUB_EMP)) )`:
        - Si `:SUB_EMP` es `NULL`, no se filtran entidades específicas.
        - Si `:SUB_EMP` no es `NULL`, solo se incluyen las entidades especificadas.
    - `RF.cod_trx in (1)`: Solo considera transacciones con código `1`.
    - `RF.ESTADO = DE.ESTADO`: Asocia el estado de las transacciones con su descripción en `DETALLE_ESTADO`.
    - `RF.COD_ENTIDAD = E.COD_ENTIDAD`: Relaciona las transacciones con las entidades respectivas.
    - `RF.ID_TIPO_DEBITO = TD.ID_TIPO_DEBITO`: Relaciona las transacciones con sus tipos de débito.
    - `TRUNC(RF.FH_INICIO_CARGA) BETWEEN :FECHA_DESDE AND :FECHA_HASTA`: Filtra las transacciones según un rango de fechas.
    - `RF.ESTADO in ('AC', 'PC', 'ES')`: Solo incluye estados válidos (por ejemplo: "Aceptado", "Pendiente de Cobro", "Enviado").

4. **Agrupamiento (`GROUP BY`)**:
    Agrupa los datos por los siguientes campos para consolidar la información:
    - `RF.COD_CIA`, `RF.COD_TRX`, `RF.COD_ENTIDAD`, `RF.ID_TIPO_DEBITO`, `RF.COD_MONEDA`, y la fecha truncada.

5. **Ordenamiento (`ORDER BY`)**:
    Ordena los resultados:
    - Descendentemente por fecha de cobro (`trunc(RF.FH_INICIO_CARGA)`).
    - Ascendentemente por código de entidad (`RF.COD_ENTIDAD`).
    - Ascendentemente por código de moneda (`RF.COD_MONEDA`).

---

### **Resultado esperado**:
El script genera un reporte agrupado con:
- Los datos de transacciones por empresa, entidad, tipo de débito, moneda, y fecha de cobro.
- Cantidades e importes totales de transacciones aceptadas y rechazadas.
- Ordenado por fecha de cobro (más reciente primero) y códigos.

Este reporte permite analizar las operaciones financieras consolidadas y realizar un seguimiento de aceptaciones y rechazos por entidad, tipo de débito y moneda.


---



Para convertir este script SQL en una consulta que pueda ejecutarse a través de un servicio usando **Spring Data JPA**, necesitas crear una entidad, un repositorio y un servicio que manejen la lógica. En este caso, se utiliza una consulta nativa con `@Query`, ya que la consulta tiene agrupamientos, funciones agregadas y lógica que puede ser más difícil de traducir completamente a JPQL. Implementación:

---

### **1. Crear una Entidad para los resultados**
Aunque no estás almacenando datos nuevos, necesitas una clase para mapear los resultados de la consulta. 

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

@Entity
public class ResumenFinancieroDto {

    @Id // Solo para cumplir con JPA, no necesitas un valor único real
    private Long id;

    @Column(name = "Empresa")
    private String empresa;

    @Column(name = "Codigo_Entidad")
    private String codigoEntidad;

    @Column(name = "Marca_Entidad")
    private String marcaEntidad;

    @Column(name = "Tipo_Debito")
    private String tipoDebito;

    @Column(name = "Fecha_de_Cobro")
    private String fechaCobro;

    @Column(name = "Codigo_de_Moneda")
    private String codigoMoneda;

    @Column(name = "Cantidad_Aceptados")
    private Long cantidadAceptados;

    @Column(name = "Importe_Aceptados")
    private Double importeAceptados;

    @Column(name = "Cantidad_Rechazados")
    private Long cantidadRechazados;

    @Column(name = "Importe_Rechazados")
    private Double importeRechazados;

    // Getters y Setters
}
```

---

### **2. Crear un Repositorio con la consulta nativa**
Define un repositorio que use una consulta SQL nativa con el mismo contenido que tu script.

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResumenFinancieroRepository extends Repository<ResumenFinancieroDto, Long> {

    @Query(
        value = """
        SELECT
            RF.COD_CIA AS Empresa,
            RF.COD_ENTIDAD AS Codigo_Entidad,
            MAX(E.DESCRIPCION) AS Marca_Entidad,
            MAX(TD.DESCRIPCION) AS Tipo_Debito,
            TO_CHAR(TRUNC(RF.FH_INICIO_CARGA), 'dd/mm/yyyy') AS Fecha_de_Cobro,
            RF.COD_MONEDA AS Codigo_de_Moneda,
            SUM(RF.CANTIDAD_ACEPTADOS) AS Cantidad_Aceptados,
            SUM(RF.IMPORTE_ACEPTADOS) AS Importe_Aceptados,
            SUM(RF.CANTIDAD_RECHAZADOS) AS Cantidad_Rechazados,
            SUM(RF.IMPORTE_RECHAZADOS) AS Importe_Rechazados
        FROM PUCARA.RESUMEN_FINANCIERAS RF
        JOIN GIRE.ENTIDADES E ON RF.COD_ENTIDAD = E.COD_ENTIDAD
        JOIN PUCARA.DETALLE_ESTADO DE ON RF.ESTADO = DE.ESTADO
        JOIN PUCARA.DETALLE_TIPO_DEBITO TD ON RF.ID_TIPO_DEBITO = TD.ID_TIPO_DEBITO
        WHERE RF.COD_CIA IN (:codCia)
          AND ((:subEmp IS NULL AND RF.COD_ENTIDAD = RF.COD_ENTIDAD) OR (:subEmp IS NOT NULL AND RF.COD_ENTIDAD IN (:subEmp)))
          AND RF.COD_TRX IN (1)
          AND TRUNC(RF.FH_INICIO_CARGA) BETWEEN :fechaDesde AND :fechaHasta
          AND RF.ESTADO IN ('AC', 'PC', 'ES')
        GROUP BY RF.COD_CIA, RF.COD_TRX, RF.COD_ENTIDAD, RF.ID_TIPO_DEBITO, RF.COD_MONEDA, TRUNC(RF.FH_INICIO_CARGA)
        ORDER BY TRUNC(RF.FH_INICIO_CARGA) DESC, RF.COD_ENTIDAD ASC, RF.COD_MONEDA ASC
        """,
        nativeQuery = true
    )
    List<ResumenFinancieroDto> obtenerResumenFinanciero(
        @Param("codCia") List<String> codCia,
        @Param("subEmp") List<String> subEmp,
        @Param("fechaDesde") String fechaDesde,
        @Param("fechaHasta") String fechaHasta
    );
}
```

---

### **3. Crear el Servicio**
Crea una clase de servicio para llamar al repositorio.

```java
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResumenFinancieroService {

    private final ResumenFinancieroRepository resumenFinancieroRepository;

    public ResumenFinancieroService(ResumenFinancieroRepository resumenFinancieroRepository) {
        this.resumenFinancieroRepository = resumenFinancieroRepository;
    }

    public List<ResumenFinancieroDto> obtenerResumenFinanciero(
            List<String> codCia,
            List<String> subEmp,
            String fechaDesde,
            String fechaHasta
    ) {
        return resumenFinancieroRepository.obtenerResumenFinanciero(codCia, subEmp, fechaDesde, fechaHasta);
    }
}
```

---

### **4. Crear un Controlador**
Finalmente, expón la funcionalidad a través de un controlador REST.

```java
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resumen-financiero")
public class ResumenFinancieroController {

    private final ResumenFinancieroService resumenFinancieroService;

    public ResumenFinancieroController(ResumenFinancieroService resumenFinancieroService) {
        this.resumenFinancieroService = resumenFinancieroService;
    }

    @GetMapping
    public List<ResumenFinancieroDto> obtenerResumenFinanciero(
            @RequestParam List<String> codCia,
            @RequestParam(required = false) List<String> subEmp,
            @RequestParam String fechaDesde,
            @RequestParam String fechaHasta
    ) {
        return resumenFinancieroService.obtenerResumenFinanciero(codCia, subEmp, fechaDesde, fechaHasta);
    }
}
```

---

### **Consideraciones**:
1. **Parámetros**: Los parámetros como `:codCia`, `:subEmp`, `:fechaDesde` y `:fechaHasta` se pasan desde el cliente a través del endpoint REST.
2. **Optimización**: La consulta es ejecutada directamente en SQL, por lo que dependerá del rendimiento de tu base de datos.
3. **Manejo de resultados**: La clase `ResumenFinancieroDto` se utiliza únicamente para mapear los resultados. Asegúrate de que los nombres de las columnas coincidan exactamente con los alias definidos en el query SQL.

Con esta implementación, el reporte se obtiene de forma dinámica a través de un servicio REST con Spring Data JPA.




---

---
## solución con Lombok en el DTO

Al usar **Lombok**, la implementación se simplifica bastante ya que puedes evitar escribir manualmente los métodos `getters`, `setters`, `constructores`, y otros. Aquí está la misma solución con **Lombok** aplicado en la entidad `ResumenFinancieroDto`.

---

### **1. Crear la Entidad con Lombok**

anotaciones de **Lombok**: 
- `@Data`: Genera automáticamente `getters`, `setters`, `toString`, `equals`, y `hashCode`.
- `@NoArgsConstructor`: Crea un constructor sin argumentos.
- `@AllArgsConstructor`: Crea un constructor con todos los argumentos.
- `@Builder` (opcional): Facilita la creación de objetos con un patrón de diseño tipo **Builder**.

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenFinancieroDto {

    @Id // Solo para cumplir con JPA, aunque este campo no será clave primaria real
    private Long id;

    @Column(name = "Empresa")
    private String empresa;

    @Column(name = "Codigo_Entidad")
    private String codigoEntidad;

    @Column(name = "Marca_Entidad")
    private String marcaEntidad;

    @Column(name = "Tipo_Debito")
    private String tipoDebito;

    @Column(name = "Fecha_de_Cobro")
    private String fechaCobro;

    @Column(name = "Codigo_de_Moneda")
    private String codigoMoneda;

    @Column(name = "Cantidad_Aceptados")
    private Long cantidadAceptados;

    @Column(name = "Importe_Aceptados")
    private Double importeAceptados;

    @Column(name = "Cantidad_Rechazados")
    private Long cantidadRechazados;

    @Column(name = "Importe_Rechazados")
    private Double importeRechazados;
}
```

---

### **2. Crear el Repositorio con la Consulta Nativa**

El repositorio no cambia. Puedes reutilizar el mismo código:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResumenFinancieroRepository extends Repository<ResumenFinancieroDto, Long> {

    @Query(
        value = """
        SELECT
            RF.COD_CIA AS Empresa,
            RF.COD_ENTIDAD AS Codigo_Entidad,
            MAX(E.DESCRIPCION) AS Marca_Entidad,
            MAX(TD.DESCRIPCION) AS Tipo_Debito,
            TO_CHAR(TRUNC(RF.FH_INICIO_CARGA), 'dd/mm/yyyy') AS Fecha_de_Cobro,
            RF.COD_MONEDA AS Codigo_de_Moneda,
            SUM(RF.CANTIDAD_ACEPTADOS) AS Cantidad_Aceptados,
            SUM(RF.IMPORTE_ACEPTADOS) AS Importe_Aceptados,
            SUM(RF.CANTIDAD_RECHAZADOS) AS Cantidad_Rechazados,
            SUM(RF.IMPORTE_RECHAZADOS) AS Importe_Rechazados
        FROM PUCARA.RESUMEN_FINANCIERAS RF
        JOIN GIRE.ENTIDADES E ON RF.COD_ENTIDAD = E.COD_ENTIDAD
        JOIN PUCARA.DETALLE_ESTADO DE ON RF.ESTADO = DE.ESTADO
        JOIN PUCARA.DETALLE_TIPO_DEBITO TD ON RF.ID_TIPO_DEBITO = TD.ID_TIPO_DEBITO
        WHERE RF.COD_CIA IN (:codCia)
          AND ((:subEmp IS NULL AND RF.COD_ENTIDAD = RF.COD_ENTIDAD) OR (:subEmp IS NOT NULL AND RF.COD_ENTIDAD IN (:subEmp)))
          AND RF.COD_TRX IN (1)
          AND TRUNC(RF.FH_INICIO_CARGA) BETWEEN :fechaDesde AND :fechaHasta
          AND RF.ESTADO IN ('AC', 'PC', 'ES')
        GROUP BY RF.COD_CIA, RF.COD_TRX, RF.COD_ENTIDAD, RF.ID_TIPO_DEBITO, RF.COD_MONEDA, TRUNC(RF.FH_INICIO_CARGA)
        ORDER BY TRUNC(RF.FH_INICIO_CARGA) DESC, RF.COD_ENTIDAD ASC, RF.COD_MONEDA ASC
        """,
        nativeQuery = true
    )
    List<ResumenFinancieroDto> obtenerResumenFinanciero(
        @Param("codCia") List<String> codCia,
        @Param("subEmp") List<String> subEmp,
        @Param("fechaDesde") String fechaDesde,
        @Param("fechaHasta") String fechaHasta
    );
}
```

---

### **3. Crear el Servicio**

El servicio tampoco cambia. El código queda igual al anterior:

```java
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResumenFinancieroService {

    private final ResumenFinancieroRepository resumenFinancieroRepository;

    public ResumenFinancieroService(ResumenFinancieroRepository resumenFinancieroRepository) {
        this.resumenFinancieroRepository = resumenFinancieroRepository;
    }

    public List<ResumenFinancieroDto> obtenerResumenFinanciero(
            List<String> codCia,
            List<String> subEmp,
            String fechaDesde,
            String fechaHasta
    ) {
        return resumenFinancieroRepository.obtenerResumenFinanciero(codCia, subEmp, fechaDesde, fechaHasta);
    }
}
```

---

### **4. Crear el Controlador**

El controlador tampoco sufre cambios:

```java
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resumen-financiero")
public class ResumenFinancieroController {

    private final ResumenFinancieroService resumenFinancieroService;

    public ResumenFinancieroController(ResumenFinancieroService resumenFinancieroService) {
        this.resumenFinancieroService = resumenFinancieroService;
    }

    @GetMapping
    public List<ResumenFinancieroDto> obtenerResumenFinanciero(
            @RequestParam List<String> codCia,
            @RequestParam(required = false) List<String> subEmp,
            @RequestParam String fechaDesde,
            @RequestParam String fechaHasta
    ) {
        return resumenFinancieroService.obtenerResumenFinanciero(codCia, subEmp, fechaDesde, fechaHasta);
    }
}
```

---

### **Ventajas al Usar Lombok**
1. **Menos código repetitivo**: No necesitas escribir manualmente `getters`, `setters`, constructores o métodos como `toString` y `equals`.
2. **Fácil de mantener**: Si agregas nuevos campos a la entidad, Lombok los maneja automáticamente.
3. **Mayor legibilidad**: El enfoque es más claro y conciso.

Esta versión funciona exactamente igual, pero con menos código gracias a **Lombok**. Asegúrate de tener **Lombok configurado correctamente** en tu proyecto para que el código se compile sin problemas.


---
---

## Global Exceptions & Validaciones

Para agregar **manejo global de excepciones** y **validaciones de parámetros** en el controlador, se pueden usar las siguientes estrategias en **Spring Boot**:

---

### **1. Validaciones de Parámetros**

Usa las anotaciones de validación de **Bean Validation** (`javax.validation` o `jakarta.validation`) junto con `@Valid` en los métodos del controlador.

- **Dependencia**: Asegúrate de tener la dependencia de `spring-boot-starter-validation` en tu archivo `pom.xml`.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Validaciones en el Controlador:
Modifica el controlador para agregar validaciones a los parámetros de entrada. Por ejemplo, puedes usar anotaciones como `@NotNull`, `@NotEmpty`, y `@Size`.

```java
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumen-financiero")
public class ResumenFinancieroController {

    private final ResumenFinancieroService resumenFinancieroService;

    public ResumenFinancieroController(ResumenFinancieroService resumenFinancieroService) {
        this.resumenFinancieroService = resumenFinancieroService;
    }

    @GetMapping
    public List<ResumenFinancieroDto> obtenerResumenFinanciero(
            @RequestParam @NotEmpty(message = "El parámetro 'codCia' no puede estar vacío") List<String> codCia,
            @RequestParam(required = false) List<String> subEmp,
            @RequestParam @NotNull(message = "El parámetro 'fechaDesde' es obligatorio") String fechaDesde,
            @RequestParam @NotNull(message = "El parámetro 'fechaHasta' es obligatorio") String fechaHasta
    ) {
        return resumenFinancieroService.obtenerResumenFinanciero(codCia, subEmp, fechaDesde, fechaHasta);
    }
}
```

- **Anotaciones usadas**:
  - `@NotEmpty`: Verifica que una lista no esté vacía.
  - `@NotNull`: Valida que un parámetro no sea nulo.
  - **Opcional**: Puedes usar `@Pattern` para validar formatos específicos (como fechas).

---

### **2. Manejo Global de Excepciones**

Spring Boot permite manejar excepciones globalmente con una clase anotada con `@ControllerAdvice`.

#### Crear Clase Global para Manejo de Excepciones:

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Manejo de excepciones de validación.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Manejo de excepciones generales.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + ex.getMessage());
    }
}
```

---

### **3. Agregar Validaciones Personalizadas**

Si necesitas validaciones específicas (por ejemplo, verificar que la fecha tenga un formato correcto), puedes crear un **validador personalizado**.

#### Crear una Anotación de Validación Personalizada:

```java
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FechaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FechaValida {
    String message() default "El formato de la fecha es inválido, debe ser 'dd/MM/yyyy'";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

#### Crear la Lógica del Validador:

```java
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class FechaValidator implements ConstraintValidator<FechaValida, String> {

    private static final String FORMATO_FECHA = "dd/MM/yyyy";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(FORMATO_FECHA);
        sdf.setLenient(false);
        try {
            sdf.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
```

#### Usar el Validador Personalizado:

Aplica la anotación en el controlador para validar fechas.

```java
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumen-financiero")
public class ResumenFinancieroController {

    private final ResumenFinancieroService resumenFinancieroService;

    public ResumenFinancieroController(ResumenFinancieroService resumenFinancieroService) {
        this.resumenFinancieroService = resumenFinancieroService;
    }

    @GetMapping
    public List<ResumenFinancieroDto> obtenerResumenFinanciero(
            @RequestParam @NotEmpty(message = "El parámetro 'codCia' no puede estar vacío") List<String> codCia,
            @RequestParam(required = false) List<String> subEmp,
            @RequestParam @FechaValida String fechaDesde,
            @RequestParam @FechaValida String fechaHasta
    ) {
        return resumenFinancieroService.obtenerResumenFinanciero(codCia, subEmp, fechaDesde, fechaHasta);
    }
}
```

---

### **4. Resultado**

- Si algún parámetro no cumple las validaciones, el usuario obtendrá una respuesta con un error estructurado en formato JSON, por ejemplo:

```json
{
  "codCia": "El parámetro 'codCia' no puede estar vacío",
  "fechaDesde": "El formato de la fecha es inválido, debe ser 'dd/MM/yyyy'"
}
```

- Si ocurre una excepción no controlada, el usuario recibirá un mensaje como:

```json
{
  "message": "Error interno: <detalle del error>"
}
```

Este enfoque garantiza que los parámetros estén correctamente validados y proporciona un manejo centralizado de errores.


---
## Referencias

Spring Boot Global Exception Handler.
https://medium.com/@aedemirsen/spring-boot-global-exception-handler-842d7143cf2a

Spring Microservice Global Exception Handling and Field validations.
https://medium.com/@kousikpaul/microservice-global-exception-handling-and-field-validations-3fc955a80692
