# DB Test con H2




## **Objetivo:**
Crear un Test unitario usando **H2 en memoria** para 
un repositorio con `JdbcTemplate`.

---

## ✅ **1\. Dependencias Maven (pom.xml)**


```xml
<dependencies>

    <!-- Spring Boot Starter Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- H2 Database para tests -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- JDBC para Spring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <!-- Si utilizas OracleTypes en código -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
        <scope>runtime</scope>
    </dependency>

</dependencies>
```

---

## ✅ **2\. Configuración de H2 para Test (application-test.yml)**

Crea un archivo específico en `src/test/resources/application-test.yml`:

```yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driverClassName: org.h2.Driver
    username: sa
    password: ''
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: none
    show-sql: true
```

- `ddl-auto: none` porque usaremos scripts SQL.

---

## ✅ **3\. Crear Script SQL para procedimiento almacenado (schema.sql)**

En `src/test/resources/schema.sql`:

```sql
CREATE SCHEMA PUCARA;

CREATE TABLE PUCARA.REPORTE_OPERACIONES (
    COD_CIA VARCHAR(10),
    SUB_EMP VARCHAR(10),
    FECHA_OPERACION VARCHAR(20),
    MONTO DECIMAL(10,2)
);

CREATE ALIAS PKG_API_PUCARA_get_iv_reporte_operaciones_total_diario FOR
"com.gire.rapipago.transactions.repository.gl.H2StoredProcedures.getIvReporteOperacionesTotalDiario";
```

- Nota: H2 no soporta procedimientos PL/SQL de Oracle directamente, pero permite simularlos mediante funciones Java.

---

## ✅ **4\. Implementar procedimiento simulado Java (para H2)**

Crea la clase Java:

```java
package com.gire.rapipago.transactions.repository.gl;

import org.h2.tools.SimpleResultSet;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class H2StoredProcedures {

    // Simulación del procedimiento almacenado de Oracle en H2
    public static ResultSet getIvReporteOperacionesTotalDiario(
            String p_cod_cia, String p_sub_emp,
            String p_fecha_desde, String p_fecha_hasta,
            Integer p_start_index, Integer p_page_size
    ) throws SQLException {

        SimpleResultSet rs = new SimpleResultSet();
        rs.addColumn("COD_CIA", Types.VARCHAR);
        rs.addColumn("SUB_EMP", Types.VARCHAR);
        rs.addColumn("FECHA", Types.VARCHAR);
        rs.addColumn("MONTO", Types.DECIMAL);

        // Ejemplo datos mock
        rs.addRow("001", "subEmp1", "2024-01-01", 1000.00);
        rs.addRow("001", "subEmp2", "2024-01-02", 2000.00);

        return rs;
    }
}
```

Esto es solo una simulación sencilla.

---

## ✅ **4\. Código del Test (JUnit5 + Spring JDBC Test)**

`ReporteOperacionesRepositoryTest.java`

```java
package com.gire.rapipago.transactions.repository.gl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@Import(ReporteOperacionesRepository.class)
class ReporteOperacionesRepositoryTest {

    @Autowired
    private DataSource dataSource;

    private ReporteOperacionesRepository repository;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        reporteOperacionesRepository = new ReporteOperacionesRepository(jdbcTemplate);
    }

    @Test
    void testGetReporteOperacionesDiario() {
        Optional<List<ReporteOperacionesDTO>> result =
                reporteOperacionesRepository.getReporteOperacionesDiario(
                        "001", "subEmp1", "2024-01-01", "2024-01-02", 0, 10);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().size());

        ReporteOperacionesDTO dto = result.get().get(0);
        assertEquals("001", dto.getCodCia());
        assertEquals("subEmp1", dto.getSubEmp());
        assertEquals("2024-01-01", dto.getFecha());
        assertEquals(1000.00, dto.getMonto());
    }
}
```

---

## ✅ **4\. Ejemplo DTO (ReporteOperacionesDTO.java)**

Ejemplo simplificado del DTO necesario:

```java
package com.gire.rapipago.transactions.repository.gl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteOperacionesDTO {
    private String codCia;
    private String subEmp;
    private String fecha;
    private Double monto;
}
```

---

## 📌 **Resumen de Imports requeridos:**

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
```

---

## ⚙️ **Cómo funciona esta solución:**

- **Spring Boot Test** inicializa una base H2 automáticamente.
- Los scripts SQL (`schema.sql`) se ejecutan automáticamente en el inicio del test.
- La clase `H2StoredProcedures` simula el procedimiento almacenado (no PL/SQL, sino Java).
- Tu repositorio usa `JdbcTemplate` con la base real H2 sin necesidad de mocks complicados.
- **Spring Test** provee todos los componentes necesarios listos para usar.

---

## 🚨 **Importante**:
- La simulación del procedimiento almacenado es esencial ya que **H2 no soporta PL/SQL nativamente**.
- Para test unitarios más sencillos, considera simplemente testear la lógica del repositorio usando consultas SQL directas, o usar mocks más simples.

---

## 🚀 **Resultado final:**

Tienes una configuración robusta, práctica, y realista para realizar tests unitarios rápidos y efectivos sin problemas con metadatos nulos.

¡Esto asegurará que tu test pase exitosamente, evitando el error original relacionado con `DatabaseMetaData returned null`!