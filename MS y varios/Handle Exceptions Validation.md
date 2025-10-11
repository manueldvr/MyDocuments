# Validaciones

# GlobalException



Para manejar la validación de que la fecha `P_FECHA_DESDE` debe estar en el pasado en tu controlador, puedes usar la anotación `@Past` de Jakarta Bean Validation, que verifica que una fecha sea anterior a la fecha actual.  

Esto se integra perfectamente con tu configuración existente (`@Validated` y el `GlobalExceptionHandler`).

## Implementación

#### 1. Agregar `@Past` al parámetro `P_FECHA_DESDE`
Modifica tu método en el controlador para incluir la anotación `@Past`. Además, como estás recibiendo la fecha como `String` a través de `@RequestParam`, necesitas asegurarte de que Spring pueda convertir ese `String` a un tipo de fecha (como `LocalDate`) para que `@Past` funcione. Aquí hay un ejemplo ajustado:

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rapipago")
@Validated
public class OperacionesRepipagoController {

    private final OpearcionesRapipagoMensualService service;

    public OperacionesRepipagoController(OpearcionesRapipagoMensualService service) {
        this.service = service;
    }

    @GetMapping("/iv_operaciones_rapipago_total_diario")
    public ResponseEntity<?> obtenerOperacionesRapipagoTotalDiario(
            @RequestParam(value = "p_cod_empresa_central", required = false) String codEmpresaCentral,
            @RequestParam("P_FECHA_DESDE")
            @NotBlank(message = "La fecha desde debe estar presente")
            @DateTimeFormat(pattern = "dd/MM/yyyy") // Formato de la fecha en la URL
            @Past(message = "La fecha desde debe estar en el pasado")
            LocalDate fechaDesde,
            @RequestParam("P_FECHA_HASTA")
            @NotBlank(message = "La fecha hasta debe estar presente")
            @DateTimeFormat(pattern = "dd/MM/yyyy")
            LocalDate fechaHasta) {
        // Lógica del servicio
        return ResponseEntity.ok("Procesado: " + fechaDesde + " - " + fechaHasta);
    }
}
```

### Cambios realizados:

- **Cambio de tipo `String` a `LocalDate`**:
  - Cambié el tipo del parámetro `fechaDesde` y `fechaHasta` de `String` a `LocalDate` porque `@Past` funciona con tipos de fecha como `LocalDate`, `LocalDateTime`, `Date`, etc., no con `String` directamente.
- **`@DateTimeFormat`**:
  - Agregué esta anotación para especificar el formato de la fecha que esperas en la URL (por ejemplo, `dd/MM/yyyy`, que coincide con `31/12/2024` de tu ejemplo). Spring usará este formato para convertir el valor del `@RequestParam` a `LocalDate`.
- **`@Past`**:
  - Esta anotación valida que `fechaDesde` sea una fecha anterior a la fecha actual (basada en el reloj del sistema al momento de la solicitud).

### 2. `GlobalExceptionHandler` (sin cambios necesarios)

Tu manejador de excepciones ya está configurado para capturar `ConstraintViolationException`, que se lanzará si `@Past` falla. No necesitas modificarlo, pero para referencia, aquí está cómo se ve:

```java
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice

public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse errors = new ErrorResponse();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString().substring(violation.getPropertyPath().toString().lastIndexOf(".") + 1);
            String message = violation.getMessage();
            errors.put(400, fieldName + ": " + message);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
```

(Asumo que `ErrorResponse` es una clase personalizada con un método `put` para agregar errores).

## 3. Pruebas
- **Fecha en el pasado (válida)**:

  ```
  localhost:8080/api/v1/rapipago/iv_operaciones_rapipago_total_diario?P_FECHA_DESDE=01/01/2024&P_FECHA_HASTA=31/12/2024
  ```

  Respuesta: `200 OK` con `"Procesado: 2024-01-01 - 2024-12-31"`.

- **Fecha en el futuro (inválida)**:

  ```
  localhost:8080/api/v1/rapipago/iv_operaciones_rapipago_total_diario?P_FECHA_DESDE=01/01/2026&P_FECHA_HASTA=31/12/2024

  ```
  
  Respuesta: `400 Bad Request` con algo como:
  
  ```json
  {
    "errors": [
      "P_FECHA_DESDE: La fecha desde debe estar en el pasado"
    ]
  }
  ```

  (El formato exacto depende de cómo esté implementado `ErrorResponse`).

- **Fecha vacía (inválida)**:

  ```
  localhost:8080/api/v1/rapipago/iv_operaciones_rapipago_total_diario?P_FECHA_DESDE=&P_FECHA_HASTA=31/12/2024
  ```
  Respuesta: `400 Bad Request` con:

  ```json
  {
    "errors": [
      "P_FECHA_DESDE: La fecha desde debe estar presente"
    ]
  }
  ```

### Notas importantes
1. **Formato de fecha**:
   - Asegúrate de que el formato especificado en `@DateTimeFormat(pattern = "dd/MM/yyyy")` coincida con cómo envías las fechas en la URL. Si usas otro formato (como `yyyy-MM-dd`), ajusta el `pattern` en consecuencia.
   - Si el formato no coincide, Spring lanzará una excepción de conversión (`TypeMismatchException` o similar), que no será capturada por `ConstraintViolationException`. En ese caso, necesitarías agregar un manejador adicional:

     ```java
     @ExceptionHandler(BindException.class)
     public ResponseEntity<String> handleBindException(BindException ex) {
         return new ResponseEntity<>("Error de formato en los parámetros: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
     }
     ```

2. **Validación adicional**:
   - Si también quieres validar que `P_FECHA_DESDE` sea anterior a `P_FECHA_HASTA`, necesitarías una validación personalizada, ya que `@Past` solo compara con la fecha actual. Esto requeriría una anotación personalizada o lógica en el método.

3. **Dependencias**:
   - Asegúrate de que tienes `spring-boot-starter-web` y `spring-boot-starter-validation` en tu `pom.xml`:
  
     ```xml
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-web</artifactId>
     </dependency>
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-validation</artifactId>
     </dependency>
     ```


<br>
<br>
<br>


## Alternativa: Validación manual

Si no quieres cambiar el tipo a `LocalDate` y prefieres mantener `String`, puedes hacer la validación manualmente:

```java
@GetMapping("/iv_operaciones_rapipago_total_diario")
public ResponseEntity<?> obtenerOperacionesRapipagoTotalDiario(
        @RequestParam(value = "p_cod_empresa_central", required = false) String codEmpresaCentral,
        @RequestParam("P_FECHA_DESDE") @NotBlank(message = "La fecha desde debe estar presente") String fechaDesde,
        @RequestParam("P_FECHA_HASTA") @NotBlank(message = "La fecha hasta debe estar presente") String fechaHasta) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    LocalDate fechaDesdeParsed = LocalDate.parse(fechaDesde, formatter);
    if (fechaDesdeParsed.isAfter(LocalDate.now())) {
        throw new ConstraintViolationException("La fecha desde debe estar en el pasado", Collections.emptySet());
    }
    // Lógica del servicio
    return ResponseEntity.ok("Procesado");
}
```
Esto te da más control, pero pierdes la integración automática con el `GlobalExceptionHandler` para mensajes personalizados por campo.