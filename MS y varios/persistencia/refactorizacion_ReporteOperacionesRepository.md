# Refactoring 2




En Spring Boot con Java 21.
Teniendo la siguiente clase anotada como Repository: ReporteOperacionesRepository








Para refactorizar la clase `ReporteOperacionesRepository` en Spring Boot con Java 21 y mejorar la llamada a la base de datos, podemos aplicar patrones de diseño y principios como **DRY (Don't Repeat Yourself)**, **Template Method**, o una combinación de **Factory** y **Strategy**. El código actual tiene duplicación significativa entre `getReporteOperacionesDiario` y `getReporteOperacionesMensual`, y la configuración de `SimpleJdbcCall` podría abstraerse para ser más reusable y mantenible.

A continuación, te propongo una refactorización paso a paso, aplicando el patrón **Template Method** para encapsular la lógica común de la llamada a la base de datos y permitir la personalización de las partes específicas (como el nombre del procedimiento y el mapeo de resultados). También usaré características modernas de Java 21 para mantener el código limpio y eficiente.

---

### Problemas Identificados
1. **Duplicación de Código**:
   - Ambos métodos (`getReporteOperacionesDiario` y `getReporteOperacionesMensual`) repiten la configuración de `SimpleJdbcCall`, el manejo de parámetros, la ejecución y el manejo de excepciones.
2. **Acoplamiento**:
   - La lógica de ejecución está fuertemente acoplada al nombre del procedimiento y al `RowMapper` específico.
3. **Mantenibilidad**:
   - Agregar un nuevo reporte requiere copiar y pegar código, lo que aumenta el riesgo de errores.
4. **Legibilidad**:
   - La configuración extensa de `SimpleJdbcCall` hace que el código sea verbosity y difícil de seguir.

---

### Solución Propuesta: Patrón Template Method
El patrón **Template Method** define un esqueleto de algoritmo en una clase base, delegando pasos específicos (como el nombre del procedimiento) a subclases o configuraciones. Aquí, crearemos una clase base o un método genérico que maneje la lógica común de la llamada a procedimientos almacenados, y permitiremos especificar el procedimiento y el mapeo como parámetros o configuraciones.

#### Pasos
1. **Abstraer la Lógica Común**: Crear un método genérico para configurar y ejecutar `SimpleJdbcCall`.
2. **Parametrizar Diferencias**: Pasar el nombre del procedimiento y el `RowMapper` como parámetros.
3. **Centralizar Excepciones**: Manejar excepciones en un solo lugar.
4. **Usar Records**: Aprovechar Java 21 para encapsular parámetros con un `record`.

---

### Código Refactorizado
```java
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import oracle.jdbc.OracleTypes;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReporteOperacionesRepository {

    private static final Log log = LogFactory.getLog(ReporteOperacionesRepository.class);
    private static final String SCHEMA_NAME = "PUCARA";
    private static final String CATALOG_NAME = "PKG_API_PUCARA";

    private final JdbcTemplate jdbcTemplate;

    public ReporteOperacionesRepository(@Qualifier("glJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Record para encapsular parámetros comunes
    public record ReporteParams(
            String codCia,
            String subEmp,
            String fechaDesde,
            String fechaHasta,
            Integer startIndex,
            Integer count) {
    }

    // Método genérico para ejecutar procedimientos almacenados
    private <T> Optional<List<T>> executeStoredProcedure(String procedureName, ReporteParams params, RowMapper<T> rowMapper) {
        log.info("Repository starting for procedure: " + procedureName);

        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName(procedureName)
                .withCatalogName(CATALOG_NAME)
                .withSchemaName(SCHEMA_NAME)
                .declareParameters(
                        new SqlParameter("p_cod_cia", OracleTypes.VARCHAR),
                        new SqlParameter("p_sub_emp", OracleTypes.VARCHAR),
                        new SqlParameter("p_fecha_desde", OracleTypes.VARCHAR),
                        new SqlParameter("p_fecha_hasta", OracleTypes.VARCHAR),
                        new SqlParameter("p_start_index", OracleTypes.NUMBER),
                        new SqlParameter("p_page_size", OracleTypes.NUMBER),
                        new SqlOutParameter("p_result", OracleTypes.CURSOR, rowMapper)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("p_cod_cia", params.codCia());
        inParams.put("p_sub_emp", params.subEmp());
        inParams.put("p_fecha_desde", params.fechaDesde());
        inParams.put("p_fecha_hasta", params.fechaHasta());
        inParams.put("p_start_index", params.startIndex());
        inParams.put("p_page_size", params.count());

        Map<String, Object> result;
        try {
            log.info("Executing stored procedure: " + procedureName);
            result = simpleJdbcCall.execute(inParams);
        } catch (BadSqlGrammarException e) {
            throw new BadSqlGrammarException(
                    "Error en la ejecución del procedimiento por error en la sintaxis SQL",
                    procedureName,
                    new SQLException(e)
            );
        } catch (InvalidResultSetAccessException e) {
            throw new InvalidResultSetAccessException(
                    "Error en la ejecución del procedimiento por parámetros inválidos",
                    procedureName,
                    e
            );
        } catch (RuntimeException e) {
            throw new RuntimeException("Error inesperado al ejecutar " + procedureName, e);
        }

        log.info("Repository ends for procedure: " + procedureName);
        return Util.checkResultGeneric(result);
    }

    // Método público para reporte diario
    public Optional<List<ReporteOperacionesDTO>> getReporteOperacionesDiario(
            String codCia, String pSubEmp, String fechaDesde, String fechaHasta, Integer startIndex, Integer count) {
        ReporteParams params = new ReporteParams(codCia, pSubEmp, fechaDesde, fechaHasta, startIndex, count);
        return executeStoredProcedure(
                "get_iv_reporte_operaciones_total_diario",
                params,
                new ReporteOperacionesRowMapper()
        );
    }

    // Método público para reporte mensual
    public Optional<List<ReporteOperacionesDTO>> getReporteOperacionesMensual(
            String codCia, String pSubEmp, String fechaDesde, String fechaHasta, Integer startIndex, Integer count) {
        ReporteParams params = new ReporteParams(codCia, pSubEmp, fechaDesde, fechaHasta, startIndex, count);
        return executeStoredProcedure(
                "get_iv_reporte_operaciones_total_mensual",
                params,
                new ReporteOperacionesRowMapper()
        );
    }
}

// Clase utilitaria simulada (deberías tenerla definida en tu proyecto)
class Util {
    @SuppressWarnings("unchecked")
    public static <T> Optional<List<T>> checkResultGeneric(Map<String, Object> result) {
        if (result == null || result.isEmpty() || !result.containsKey("p_result")) {
            return Optional.empty();
        }
        return Optional.of((List<T>) result.get("p_result"));
    }
}

// RowMapper simulado (deberías tenerlo implementado)
class ReporteOperacionesRowMapper implements RowMapper<ReporteOperacionesDTO> {
    @Override
    public ReporteOperacionesDTO mapRow(java.sql.ResultSet rs, int rowNum) throws SQLException {
        // Implementación del mapeo
        return new ReporteOperacionesDTO(); // Placeholder
    }
}

// DTO simulado
class ReporteOperacionesDTO {
    // Campos y métodos del DTO
}
```

---

### Explicación de la Refactorización
1. **Patrón Template Method**:
   - El método `executeStoredProcedure` actúa como la plantilla que define el flujo común:
     - Configuración de `SimpleJdbcCall`.
     - Preparación de parámetros.
     - Ejecución y manejo de excepciones.
     - Retorno del resultado.
   - Los métodos públicos (`getReporteOperacionesDiario` y `getReporteOperacionesMensual`) solo especifican el procedimiento y el `RowMapper`.

2. **Uso de `record`**:
   - `ReporteParams` encapsula los parámetros comunes en una estructura inmutable, aprovechando Java 21 para mantener el código limpio y evitar pasar múltiples argumentos sueltos.

3. **Constantes**:
   - `SCHEMA_NAME` y `CATALOG_NAME` se extraen como constantes estáticas para evitar duplicación y facilitar cambios futuros.

4. **Genéricos**:
   - El método `executeStoredProcedure` usa un tipo genérico `<T>` para permitir diferentes tipos de DTOs en el futuro, haciéndolo más flexible (por ejemplo, podrías usar `ReporteVentasDTO` con otro `RowMapper`).

5. **Manejo de Excepciones**:
   - Las excepciones se manejan en un solo lugar, con mensajes específicos que incluyen el nombre del procedimiento para mejor trazabilidad.

6. **DRY**:
   - Se elimina la duplicación al centralizar la lógica de la llamada a la base de datos.

---

### Beneficios
- **Reusabilidad**: Puedes agregar nuevos métodos para otros procedimientos (por ejemplo, `getReporteOperacionesAnual`) simplemente llamando a `executeStoredProcedure` con el nombre del procedimiento y el `RowMapper` adecuado.
- **Mantenibilidad**: Cambiar la configuración de `SimpleJdbcCall` (como el esquema o catálogo) se hace en un solo lugar.
- **Legibilidad**: Los métodos públicos son más cortos y enfocados en su propósito específico.
- **Escalabilidad**: El enfoque genérico permite soportar diferentes DTOs sin modificar la lógica base.

---

### Alternativa: Patrón Strategy
Si quisieras aún más flexibilidad (por ejemplo, diferentes configuraciones de parámetros o excepciones por procedimiento), podrías usar el patrón **Strategy** definiendo una interfaz para las configuraciones:

```java
interface StoredProcedureStrategy<T> {
    String getProcedureName();
    RowMapper<T> getRowMapper();
    Map<String, Object> buildParams(ReporteParams params);
}

public <T> Optional<List<T>> executeStoredProcedure(StoredProcedureStrategy<T> strategy, ReporteParams params) {
    // Lógica similar a executeStoredProcedure anterior
}
```

Sin embargo, para este caso, el **Template Method** es suficiente y más simple.

---

### Ejemplo de Uso
```java
ReporteOperacionesRepository repo = new ReporteOperacionesRepository(jdbcTemplate);
Optional<List<ReporteOperacionesDTO>> diario = repo.getReporteOperacionesDiario("3", "SUB1", "01/01/2023", "31/12/2023", 0, 10);
Optional<List<ReporteOperacionesDTO>> mensual = repo.getReporteOperacionesMensual("3", "SUB1", "01/01/2023", "31/12/2023", 0, 10);
```

---

### Conclusión
Esta refactorización aplica el patrón **Template Method** para eliminar duplicación y mejorar la estructura del código, aprovechando `records` de Java 21 para encapsular parámetros. Si necesitas generalizar aún más (por ejemplo, para diferentes firmas de procedimientos), puedo ajustar la solución. ¿Qué te parece? ¿Hay algo más que quieras optimizar?