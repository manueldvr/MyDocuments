# Refactorizacion de LoginRepository


```java
package com.gire.rapipago.transactions.login.repository;

import com.gire.rapipago.transactions.login.entities.*;
import oracle.jdbc.OracleTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LoginRepository {
    private static final Log log = LogFactory.getLog(LoginRepository.class);
    
    private static final String SCHEMA_NAME = "GIRE_CORP";
    private static final String PACKAGE_NAME = "PKG_API_GIRE_CORP";

    private final JdbcTemplate jdbcTemplate;

    public LoginRepository(@Qualifier("glJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Enum para configurar cada procedimiento
    private enum ProcedureConfig {
        EMPRESAS_CLIENTE("get_iv_empresas_cliente", new LoginEmpresasClientesRowMapper()),
        CLIENTE_CONTABLE("get_iv_cliente_contable", new LoginClienteContableRowMapper()),
        EMPRESAS_CRISOL("GET_IV_EMPRESAS_CRISOL", new LoginEmpresasCrisolRowMapper()),
        ENTIDADES_TRANSMISION("GET_IV_ENTIDADES_TRANSMISION", new LoginEntidadesTransmisionRowMapper());

        private final String procedureName;
        private final Object rowMapper;

        ProcedureConfig(String procedureName, Object rowMapper) {
            this.procedureName = procedureName;
            this.rowMapper = rowMapper;
        }
    }

    // Métodos públicos que usan el método genérico
    public Optional<List<LoginEmpresasClientes>> getEmpresasCliente(String cuit, Integer count, Integer startIndex) {
        return executeProcedure(ProcedureConfig.EMPRESAS_CLIENTE, cuit, count, startIndex);
    }

    public Optional<List<LoginClienteContable>> getClienteContable(String cuit, Integer count, Integer startIndex) {
        return executeProcedure(ProcedureConfig.CLIENTE_CONTABLE, cuit, count, startIndex);
    }

    public Optional<List<LoginEmpresasCrisol>> getEmpresasCrisol(String cuit, Integer count, Integer startIndex) {
        return executeProcedure(ProcedureConfig.EMPRESAS_CRISOL, cuit, count, startIndex);
    }

    public Optional<List<LoginEntidadesTransmision>> getEntidadesTransmision(String cuit, Integer count, Integer startIndex) {
        return executeProcedure(ProcedureConfig.ENTIDADES_TRANSMISION, cuit, count, startIndex);
    }

    // Método genérico para ejecutar procedimientos
    private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, String cuit, Integer count, Integer startIndex) {
        log.info("Repository starting...");
        
        SimpleJdbcCall simpleJdbcCall = createJdbcCall(config);
        Map<String, Object> params = createParameterMap(cuit, count, startIndex);
        
        try {
            log.info("Call execution");
            Map<String, Object> result = simpleJdbcCall.execute(params);
            log.info("Repository call executed");
            return checkResult(result);
        } catch (BadSqlGrammarException e) {
            throw new BadSqlGrammarException(
                "Error en la ejecución del procedimiento por error en la sintaxis SQL",
                config.procedureName,
                new SQLException(e)
            );
        } catch (InvalidDataAccessApiUsageException e) {
            throw new InvalidDataAccessApiUsageException(
                "Error en la ejecución del procedimiento por parámetros inválidos",
                e
            );
        } catch (RuntimeException e) {
            throw new RuntimeException("Error inesperado en la ejecución del procedimiento", e);
        }
    }

    // Configuración común de SimpleJdbcCall
    private SimpleJdbcCall createJdbcCall(ProcedureConfig config) {
        return new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName(config.procedureName)
            .withCatalogName(PACKAGE_NAME)
            .withSchemaName(SCHEMA_NAME)
            .declareParameters(
                new SqlParameter("p_cuit", OracleTypes.VARCHAR),
                new SqlParameter("p_page_size", OracleTypes.NUMBER),
                new SqlParameter("p_start_index", OracleTypes.NUMBER),
                new SqlOutParameter("p_result", OracleTypes.CURSOR, config.rowMapper)
            );
    }

    // Creación de parámetros
    private Map<String, Object> createParameterMap(String cuit, Integer count, Integer startIndex) {
        Map<String, Object> params = new HashMap<>();
        params.put("p_cuit", cuit);
        params.put("p_page_size", count);
        params.put("p_start_index", startIndex);
        return params;
    }

    // Verificación genérica de resultados
    @SuppressWarnings("unchecked")
    private <T> Optional<List<T>> checkResult(Map<String, Object> result) {
        if (result == null) {
            throw new RuntimeException("Error en la ejecución del procedimiento, result es nulo");
        }
        if (result.isEmpty() || result.get("p_result") == null) {
            throw new RuntimeException("Error en la ejecución del procedimiento, result es vacío o p_result es nulo");
        }
        
        log.info("Repository results obtained");
        return Optional.of((List<T>) result.get("p_result"));
    }
}
```

### Principales mejoras implementadas:

1. **DRY (No repetir código)**:
   - Se creó un método genérico `executeProcedure` que maneja la lógica común de todos los métodos.
   - Se usa un `enum ProcedureConfig` para configurar cada procedimiento específico.

2. **Constantes**:
   - Se extrajeron `SCHEMA_NAME` y `PACKAGE_NAME` como constantes estáticas.

3. **Métodos auxiliares**:
   - `createJdbcCall`: Configura el `SimpleJdbcCall` de manera consistente.
   - `createParameterMap`: Centraliza la creación de parámetros.
   - `checkResult`: Versión simplificada de `checkRapipagoResultGeneric`.

4. **Manejo de excepciones**:
   - Mensajes de error más consistentes y con causa raíz incluida.
   - Estructura de captura de excepciones unificada.

5. **Legibilidad**:
   - Código más conciso y fácil de leer.
   - Separación clara de responsabilidades.
   - Nombres de métodos y variables más consistentes.

6. **Mantenibilidad**:
   - Para agregar un nuevo procedimiento, solo se necesita agregar una entrada al `enum ProcedureConfig` y un método público.
   - Los cambios en la lógica común solo requieren modificar `executeProcedure`.

7. **Validaciones**:
   - Se mantienen las anotaciones de validación implícitas en los parámetros (aunque no estaban completas en todos los métodos originales).

### Consideraciones adicionales:
- Podrías agregar validaciones explícitas para `count` y `startIndex` usando `@Min(0)` consistentemente en todos los métodos públicos.
- Si necesitas logs más específicos por procedimiento, podrías agregar un parámetro al log en `executeProcedure`.
- Considera usar un `record` o clase para los parámetros en lugar de pasarlos individualmente si el número de parámetros crece.

Este diseño es más escalable y reduce significativamente la duplicación de código mientras mantiene la misma funcionalidad. ¿Hay algún aspecto específico que te gustaría que profundice o modifique?



.  
.  
.  

---

# Con sobrecarga del método *executeProcedure*


Si necesitas que `executeProcedure` soporte diferentes combinaciones de parámetros (es decir, distintas cantidades y tipos de parámetros según el caso), puedes abordar esto de varias maneras. La clave es mantener la flexibilidad sin comprometer la claridad ni la seguridad del tipo. Dado que cada procedimiento podría requerir un subconjunto diferente de parámetros, te presento una solución basada en **sobrecarga de métodos** y un enfoque más dinámico con un objeto de parámetros flexible.

Voy a asumir que algunos procedimientos podrían usar solo `cuit`, otros `cuit` y `count`, y otros todos los parámetros (`cuit`, `count`, `startIndex`, `tipoFiltro`, `estado`). Aquí te muestro cómo refactorizarlo:

---

### Solución: Combinación de Sobrecarga y Mapa Flexible

#### Código refactorizado:
```java
package com.gire.rapipago.transactions.login.repository;

import com.gire.rapipago.transactions.login.entities.*;
import oracle.jdbc.OracleTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LoginRepository {
    private static final Log log = LogFactory.getLog(LoginRepository.class);
    private static final String SCHEMA_NAME = "GIRE_CORP";
    private static final String PACKAGE_NAME = "PKG_API_GIRE_CORP";

    private final JdbcTemplate jdbcTemplate;

    public LoginRepository(@Qualifier("glJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private enum ProcedureConfig {
        EMPRESAS_CLIENTE("get_iv_empresas_cliente", new LoginEmpresasClientesRowMapper()),
        CLIENTE_CONTABLE("get_iv_cliente_contable", new LoginClienteContableRowMapper()),
        EMPRESAS_CRISOL("GET_IV_EMPRESAS_CRISOL", new LoginEmpresasCrisolRowMapper()),
        ENTIDADES_TRANSMISION("GET_IV_ENTIDADES_TRANSMISION", new LoginEntidadesTransmisionRowMapper());

        private final String procedureName;
        private final Object rowMapper;

        ProcedureConfig(String procedureName, Object rowMapper) {
            this.procedureName = procedureName;
            this.rowMapper = rowMapper;
        }
    }

    // Métodos públicos con diferentes combinaciones de parámetros
    public Optional<List<LoginEmpresasClientes>> getEmpresasCliente(String cuit) {
        return executeProcedure(ProcedureConfig.EMPRESAS_CLIENTE, Map.of("p_cuit", cuit));
    }

    public Optional<List<LoginClienteContable>> getClienteContable(String cuit, Integer count) {
        return executeProcedure(ProcedureConfig.CLIENTE_CONTABLE, 
            Map.of("p_cuit", cuit, "p_page_size", count));
    }

    public Optional<List<LoginEmpresasCrisol>> getEmpresasCrisol(String cuit, Integer count, Integer startIndex) {
        return executeProcedure(ProcedureConfig.EMPRESAS_CRISOL, 
            Map.of("p_cuit", cuit, "p_page_size", count, "p_start_index", startIndex));
    }

    public Optional<List<LoginEntidadesTransmision>> getEntidadesTransmision(String cuit, Integer count, Integer startIndex, String tipoFiltro, Integer estado) {
        return executeProcedure(ProcedureConfig.ENTIDADES_TRANSMISION, 
            Map.of(
                "p_cuit", cuit,
                "p_page_size", count,
                "p_start_index", startIndex,
                "p_tipo_filtro", tipoFiltro,
                "p_estado", estado
            ));
    }

    // Método genérico que acepta un mapa de parámetros
    private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, Map<String, Object> params) {
        log.info("Repository starting...");

        SimpleJdbcCall simpleJdbcCall = createJdbcCall(config, params.keySet());
        
        try {
            log.info("Call execution");
            Map<String, Object> result = simpleJdbcCall.execute(params);
            log.info("Repository call executed");
            return checkResult(result);
        } catch (BadSqlGrammarException e) {
            throw new BadSqlGrammarException(
                "Error en la ejecución del procedimiento por error en la sintaxis SQL",
                config.procedureName,
                new SQLException(e)
            );
        } catch (InvalidDataAccessApiUsageException e) {
            throw new InvalidDataAccessApiUsageException(
                "Error en la ejecución del procedimiento por parámetros inválidos",
                e
            );
        } catch (RuntimeException e) {
            throw new RuntimeException("Error inesperado en la ejecución del procedimiento", e);
        }
    }

    // Configuración dinámica de SimpleJdbcCall basada en los parámetros proporcionados
    private SimpleJdbcCall createJdbcCall(ProcedureConfig config, Set<String> paramNames) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName(config.procedureName)
            .withCatalogName(PACKAGE_NAME)
            .withSchemaName(SCHEMA_NAME)
            .declareParameters(
                new SqlOutParameter("p_result", OracleTypes.CURSOR, config.rowMapper)
            );

        // Agregar parámetros de entrada dinámicamente
        for (String paramName : paramNames) {
            int sqlType = switch (paramName) {
                case "p_cuit", "p_tipo_filtro" -> OracleTypes.VARCHAR;
                case "p_page_size", "p_start_index", "p_estado" -> OracleTypes.NUMBER;
                default -> throw new IllegalArgumentException("Parámetro desconocido: " + paramName);
            };
            call.addDeclaredParameter(new SqlParameter(paramName, sqlType));
        }
        
        return call;
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<List<T>> checkResult(Map<String, Object> result) {
        if (result == null) {
            throw new RuntimeException("Error en la ejecución del procedimiento, result es nulo");
        }
        if (result.isEmpty() || result.get("p_result") == null) {
            throw new RuntimeException("Error en la ejecución del procedimiento, result es vacío o p_result es nulo");
        }
        
        log.info("Repository results obtained");
        return Optional.of((List<T>) result.get("p_result"));
    }
}
```

---

### Cambios clave:

1. **Mapa de parámetros flexible**:
   - El método `executeProcedure` ahora acepta un `Map<String, Object>` en lugar de parámetros fijos. Esto permite pasar cualquier combinación de parámetros.
   - Los métodos públicos usan `Map.of()` para construir el mapa de manera concisa.

2. **Configuración dinámica de `SimpleJdbcCall`**:
   - El método `createJdbcCall` ahora toma los nombres de los parámetros del mapa y los declara dinámicamente usando un `switch` para mapearlos a los tipos SQL correctos (`VARCHAR`, `NUMBER`, etc.).
   - Esto asegura que solo se declaren los parámetros que se pasan, evitando errores por parámetros faltantes o inesperados en el procedimiento.

3. **Sobrecarga implícita**:
   - En lugar de definir múltiples versiones de `executeProcedure` con diferentes firmas, usamos un único método genérico y dejamos que los métodos públicos construyan el mapa de parámetros según sea necesario.

4. **Validación**:
   - Si se pasa un parámetro desconocido, se lanza una excepción en `createJdbcCall`. Puedes ajustar esto para ignorar parámetros no soportados si es necesario.

---

### Ejemplo de uso:
- Solo `cuit`:
  ```java
  getEmpresasCliente("123456789");
  ```
- `cuit` y `count`:
  ```java
  getClienteContable("123456789", 10);
  ```
- Todos los parámetros:
  ```java
  getEntidadesTransmision("123456789", 10, 0, "ACTIVO", 1);
  ```

---

### Alternativa: Sobrecarga explícita
Si prefieres evitar el mapa y mantener firmas específicas, podrías sobrecargar `executeProcedure` así:

```java
private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, String cuit) {
    return executeProcedure(config, Map.of("p_cuit", cuit));
}

private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, String cuit, Integer count) {
    return executeProcedure(config, Map.of("p_cuit", cuit, "p_page_size", count));
}

private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, String cuit, Integer count, Integer startIndex) {
    return executeProcedure(config, Map.of("p_cuit", cuit, "p_page_size", count, "p_start_index", startIndex));
}

private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, String cuit, Integer count, Integer startIndex, String tipoFiltro, Integer estado) {
    return executeProcedure(config, Map.of(
        "p_cuit", cuit,
        "p_page_size", count,
        "p_start_index", startIndex,
        "p_tipo_filtro", tipoFiltro,
        "p_estado", estado
    ));
}

// Método base con el mapa
private <T> Optional<List<T>> executeProcedure(ProcedureConfig config, Map<String, Object> params) {
    // Lógica existente...
}
```

#### Desventaja:
- Tendrías que escribir una versión de `executeProcedure` por cada combinación de parámetros que necesites, lo que puede volverse inmanejable si hay muchas variantes.

---

### Recomendación
La solución con el **mapa flexible** (primera opción) es la más práctica porque:
- Es infinitamente escalable: puedes agregar nuevos parámetros sin cambiar la firma de `executeProcedure`.
- Reduce la duplicación de código.
- Mantiene la lógica en un solo lugar.

Si algunos procedimientos no usan ciertos parámetros, asegúrate de que la base de datos los acepte como opcionales (por ejemplo, con valores por defecto en el procedimiento almacenado). Si necesitas validaciones específicas por procedimiento, podrías agregar una capa de validación en los métodos públicos antes de construir el mapa.

¿Te gustaría que ajuste algo más o que explore otra alternativa? Por ejemplo, podría agregar un mecanismo para especificar qué parámetros son obligatorios por procedimiento en el `ProcedureConfig`.

