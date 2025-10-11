# Hikari

<br>  

Es un pool de conexiones predeterminado en Spring Boot, y su configuración se realiza mediante la clase **HikariConfig**.   

Esta clase ofrece numerosos parámetros para ajustar el comportamiento y rendimiento del pool según las necesidades de tu aplicación. 

A continuación, se presenta una explicación detallada de los principales parámetros modificables, junto con ejemplos y señalando algunos posibles conflictos entre ellos.

<br>
<br>



## 1. Parámetros Básicos de Conexión

- **jdbcUrl**  
  Es la URL de conexión a la base de datos. Define el protocolo, el host, el puerto y la base de datos a la que te conectas.  
  *Ejemplo:*  
  ```java
  config.setJdbcUrl("jdbc:mysql://localhost:3306/mi_basededatos");
  ```

- **username y password**  
  Credenciales para autenticarse en la base de datos.  
  *Ejemplo:*  
  ```java
  config.setUsername("usuario");
  config.setPassword("contraseña");
  ```

- **driverClassName**  
  Especifica la clase del driver JDBC a utilizar. Con muchos drivers modernos (soporte JDBC4 o superior), Spring Boot suele detectar el driver a partir del JDBC URL, por lo que este parámetro no es obligatorio a menos que se requiera explícitamente.  
  *Ejemplo:*  
  ```java
  config.setDriverClassName("com.mysql.cj.jdbc.Driver");
  ```

<br>
<br>

## 2. Parámetros del Pool de Conexiones

Estos parámetros controlan el tamaño, el tiempo de vida y el comportamiento de las conexiones en el pool.

- **maximumPoolSize**  
  Define el número máximo de conexiones que el pool puede mantener (activas e inactivas).  
  *Ejemplo:*  
  ```java
  config.setMaximumPoolSize(10);
  ```

- **minimumIdle**  
  Es el número mínimo de conexiones inactivas que se intentará mantener en el pool.  
  *Ejemplo:*  
  ```java
  config.setMinimumIdle(5);
  ```  
  **Conflicto potencial:**  
  El valor de `minimumIdle` **no debe ser mayor** que el de `maximumPoolSize`, ya que esto puede llevar a comportamientos inesperados o que ciertos parámetros sean ignorados.

- **connectionTimeout**  
  Tiempo máximo (en milisegundos) que se esperará para obtener una conexión del pool antes de lanzar una excepción.  
  *Ejemplo:*  
  ```java
  config.setConnectionTimeout(30000); // 30 segundos
  ```  
  **Nota:** Un tiempo de espera muy bajo puede provocar errores de conexión en cargas elevadas, mientras que uno muy alto puede hacer que la aplicación parezca responder lentamente ante problemas reales.

- **idleTimeout**  
  Tiempo máximo (en milisegundos) que una conexión puede estar inactiva en el pool antes de ser elegida para su eliminación.  
  *Ejemplo:*  
  ```java
  config.setIdleTimeout(600000); // 10 minutos
  ```  
  **Consideración:** Un `idleTimeout` muy corto puede hacer que se cierren conexiones que se podrían reutilizar, aumentando la sobrecarga de establecer nuevas conexiones, mientras que uno demasiado largo puede retener conexiones innecesarias.

- **maxLifetime**  
  Tiempo máximo (en milisegundos) que una conexión puede vivir en el pool, independientemente de su actividad.  
  *Ejemplo:*  
  ```java
  config.setMaxLifetime(1800000); // 30 minutos
  ```  
  **Recomendación:** Generalmente se configura un poco menor que el tiempo de expiración de la conexión en la base de datos (si existe tal límite) para evitar cerrar conexiones que ya han sido marcadas como inválidas.


<br>
<br>

## 3. Parámetros de Validación y Testeo

- **connectionTestQuery**  
  Consulta SQL que se usa para validar que una conexión es válida. Se utiliza en lugar del método `Connection.isValid()` de JDBC.  
  *Ejemplo:*  
  ```java
  config.setConnectionTestQuery("SELECT 1");
  ```  
  **Nota:** Este parámetro suele ser innecesario si el driver soporta JDBC4, pero es útil para drivers antiguos.

- **validationTimeout**  
  Tiempo máximo (en milisegundos) que se esperará a que una consulta de validación confirme la validez de una conexión.  
  *Ejemplo:*  
  ```java
  config.setValidationTimeout(5000); // 5 segundos
  ```

- **leakDetectionThreshold**  
  Umbral en milisegundos para identificar fugas de conexión. Si una conexión se ha prestado y no se ha devuelto en este tiempo, se marcará con un posible "leak" (fuga).  
  *Ejemplo:*  
  ```java
  config.setLeakDetectionThreshold(2000); // 2 segundos
  ```  
  **Consideración:** Establecer un valor demasiado bajo puede generar falsos positivos; un valor muy alto puede retrasar la detección de fugas.

<br>
<br>

## 4. Parámetros de Comportamiento de las Conexiones

- **autoCommit**  
  Define si las conexiones en el pool deben iniciar en modo autocommit.  
  *Ejemplo:*  
  ```java
  config.setAutoCommit(true);
  ```  
  **Consejo:** Generalmente se recomienda dejarlo en `true` a menos que la aplicación maneje transacciones de forma manual.

- **readOnly**  
  Si se establece en `true`, las conexiones se configuran en modo solo lectura, lo cual puede optimizar ciertas operaciones de solo lectura.  
  *Ejemplo:*  
  ```java
  config.setReadOnly(false);
  ```

- **catalog**  
  Permite definir un catálogo de base de datos predeterminado para las conexiones.  
  *Ejemplo:*  
  ```java
  config.setCatalog("catalogo_predeterminado");
  ```

- **transactionIsolation**  
  Define el nivel de aislamiento de transacciones para las conexiones. Se puede especificar mediante constantes de `java.sql.Connection`, como `Connection.TRANSACTION_READ_COMMITTED`.  
  *Ejemplo:*  
  ```java
  config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
  ```

<br>
<br>

## 5. Parámetros Avanzados y Opcionales

- **poolName**  
  Permite asignar un nombre al pool, lo cual es útil para identificarlo en registros o herramientas de monitoreo.  
  *Ejemplo:*  
  ```java
  config.setPoolName("MiPoolConexiones");
  ```

- **initializationFailTimeout**  
  Tiempo (en milisegundos) que la inicialización del pool esperará para conseguir una conexión inicial antes de fallar.  
  *Ejemplo:*  
  ```java
  config.setInitializationFailTimeout(1); // 1 milisegundo para fallo inmediato, o 0 para no fallar
  ```  
  **Atención:** Un valor de 0 puede significar que la aplicación no notifique inmediatamente problemas en la obtención de conexiones al inicio.

- **allowPoolSuspension**  
  Si se establece en `true`, permite suspender y reanudar el pool de conexiones en tiempo de ejecución (útil en ciertos escenarios de mantenimiento).  
  *Ejemplo:*  
  ```java
  config.setAllowPoolSuspension(false);
  ```

- **dataSourceClassName y dataSourceProperties**  
  En lugar de proporcionar directamente el `jdbcUrl`, se puede optar por especificar una clase de DataSource y propiedades adicionales.  
  *Ejemplo:*  
  ```java
  config.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
  Properties dsProps = new Properties();
  dsProps.setProperty("serverName", "localhost");
  dsProps.setProperty("port", "3306");
  dsProps.setProperty("databaseName", "mi_basededatos");
  config.setDataSourceProperties(dsProps);
  ```  
  **Importante:** No es recomendable usar **jdbcUrl** y **dataSourceClassName** al mismo tiempo, ya que pueden entrar en conflicto. Dependiendo de la configuración, uno puede sobreescribir al otro.

- **schema**  
  Define el esquema por defecto a utilizar en las conexiones.  
  *Ejemplo:*  
  ```java
  config.setSchema("mi_esquema");
  ```

- **metricsTrackerFactory y healthCheckRegistry**  
  Permiten integrar HikariCP con sistemas de métricas y chequeos de salud, muy útiles para monitorización en producción.  
  *Ejemplo:*  
  ```java
  config.setMetricsTrackerFactory(miFactoryDeMetricas);
  // Y para health check:
  config.setHealthCheckRegistry(miHealthCheckRegistry);
  ```

- **isolateInternalQueries**  
  Permite ejecutar internamente las queries de HikariCP en conexiones aisladas de la transacción actual.  
  *Ejemplo:*  
  ```java
  config.setIsolateInternalQueries(true);
  ```

<br>
<br>



## 6. Ejemplo de Configuración Completa en Código

Aquí tienes un ejemplo práctico de configuración en una aplicación Spring Boot utilizando HikariConfig:

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourceConfig {
    public HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mi_basededatos");
        config.setUsername("usuario");
        config.setPassword("contraseña");
        // Parámetros de tamaño del pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        // Parámetros de tiempo
        config.setConnectionTimeout(30000); // 30 segundos
        config.setIdleTimeout(600000);        // 10 minutos
        config.setMaxLifetime(1800000);         // 30 minutos
        // Parámetros de validación
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);      // 5 segundos
        config.setLeakDetectionThreshold(2000); // 2 segundos (verificar con cuidado)
        // Configuración adicional
        config.setAutoCommit(true);
        config.setPoolName("MiPoolConexiones");
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        // En caso de querer utilizar DataSourceClassName en vez de jdbcUrl, coméntalo:
        // config.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        // Properties adicionales en caso de usar dataSourceClassName:
        // Properties dsProps = new Properties();
        // dsProps.setProperty("serverName", "localhost");
        // dsProps.setProperty("port", "3306");
        // dsProps.setProperty("databaseName", "mi_basededatos");
        // config.setDataSourceProperties(dsProps);
        
        return new HikariDataSource(config);
    }
}
```

  

<br>
<br>



## 7. Consideraciones sobre Conflictos de Configuración

- **Valores inconsistentes en tamaño del pool:**  
  - **minimumIdle > maximumPoolSize:** Esto no tiene sentido, ya que se quiere mantener un mínimo de conexiones inactivas pero sin superar el máximo total.  
  - **Ejemplo conflictivo:**  
    ```java
    config.setMinimumIdle(12);
    config.setMaximumPoolSize(10); // Error potencial
    ```

- **Uso simultáneo de jdbcUrl y dataSourceClassName:**  
  Si se especifican ambos, puede ocurrir que uno sobreescriba al otro o que se generen confusiones en la configuración interna. Se recomienda usar uno u otro, según el caso de uso.
  
- **Incompatibilidad entre tiempos:**  
  - Un `connectionTimeout` demasiado bajo combinado con un `leakDetectionThreshold` muy alto puede evitar la detección de fugas, ya que las conexiones no estarán en uso el tiempo suficiente para activar la detección.  
  - Asimismo, establecer un `maxLifetime` inferior al `idleTimeout` puede llevar a que conexiones sean eliminadas antes de cumplir con el tiempo inactivo esperado.

- **Configuración de transacciones:**  
  Si se desactiva el auto-commit y la aplicación no administra transacciones correctamente, puede generar bloqueos o inconsistencias. Es importante que la estrategia de transacción de la aplicación sea coherente con este parámetro.

<br>


## Conclusión

HikariConfig ofrece una amplia gama de parámetros que permiten afinar desde la conexión básica hasta comportamientos muy específicos del pool. La clave está en conocer el entorno y las necesidades de la aplicación:
- **Asegurarse coherencia entre los valores relacionados con el tamaño y tiempos (minimumIdle, maximumPoolSize, connectionTimeout, idleTimeout y maxLifetime) sean coherentes entre sí.**
- **Evitar la  configuración redundante de parámetros que puedan entrar en conflicto, como el uso simultáneo de jdbcUrl y dataSourceClassName.**
- **Ajustar la detección de fugas y la validación de conexiones en función del comportamiento real de la base de datos y la carga esperada.**

<br>


La configuración  permite optimizar el rendimiento y la estabilidad de la conexión a la BD en aplicaciones Spring Boot.

<br>

---

<br>
<br>


# Estado Actual

estado de la conn para OL

```
=======================
DATA SOURCE RAPIPAGO OL 
 
ol.datasource.driverClassName=oracle.jdbc.OracleDriver
ol.datasource.jdbcUrl=${DB_HOST}
ol.datasource.username=${DB_USERNAME}
ol.datasource.password=${DB_PASSWORD}					⊢ parametros Hikari   definicion ⊣
ol.datasource.maxIdleTime=60        # ✖¿ es idleTimeout?		█ idleTimeout		█ Tiempo máximo (en milisegundos) que una conexión puede estar inactiva en el pool antes de ser elegida para su eliminación.
ol.datasource.minPoolSize=3         # ✖ NO EXISTE			█ minimumIdle		█ Es el número mínimo de conexiones inactivas que se intentará mantener en el pool.
ol.datasource.maximumPoolSize=5     # ✔  OK  ✔				█ maximumPoolSize	█ Define el número máximo de conexiones que el pool puede mantener (activas e inactivas).
ol.datasource.maxStatements=0       # ✖ ???				█					█
ol.datasource.idleConnectionTestPeriod=3000     # ✖¿ es validationTimeout ?	█ validationTimeout █ Tiempo máximo (en milisegundos) que se esperará a que una consulta de validación confirme la validez de una conexión.
ol.datasource.loginTimeout=300                  # ✖ es connectionTimeout ?	█ connectionTimeout █ Tiempo máximo (en milisegundos) que se esperará para obtener una conexión del pool antes de lanzar una excepción.
ol.datasource.showSql=true			# ✖ NO EXISTE		█					█
```

**Otros** 

`readOnly` 

Si se establece en true, las conexiones se configuran en modo solo lectura, 
lo cual puede optimizar ciertas operaciones de solo lectura.

<br>
<br>
<br>
<br>

---

<br>

# Propuesta

<br>


Esta configuración debe servir como punto de partida para lograr un pool optimizado para operaciones de lectura en Oracle dentro de un B2F desarrollado con Spring Boot. 

Se deben ajustar cada parámetro según el entorno y las necesidades reales, evaluando el rendimiento en escenarios de carga y ajustando el pool para obtener la máxima eficiencia.



```
bk.datasource.driverClassName=oracle.jdbc.OracleDriver
bk.datasource.jdbcUrl=${DB_BK_HOST}
bk.datasource.username=${DB_BK_USERNAME}
bk.datasource.password=${DB_BK_PASSWORD}

bk.datasource.maximumPoolSize=20
bk.datasource.minimum-idle=10
bk.datasource.connection-timeout=30000
bk.datasource.idle-timeout=600000
bk.datasource.max-lifetime=1800000

# nuevos
bk.datasource.validationTimeout=5000
bk.datasource.read-only=true
bk.datasource.autoCommit=true
bk.datasource.connection-test-query=SELECT 1 FROM DUAL
```

## Explicación







## Configuración



1. **Optimización para Lecturas**  
   - **setReadOnly(true):** Marca las conexiones como de solo lectura, lo que ayuda a evitar operaciones de escritura y puede permitir optimizaciones en el lado del driver o incluso de la base de datos.  
   - **setAutoCommit(true):** En un escenario de sólo lectura, mantener el autoCommit facilita la gestión de transacciones y reduce la sobrecarga.

2. **Configuración del Pool**  
   - **MaximumPoolSize y MinimumIdle:** Se han propuesto valores que permiten atender un alto volumen de peticiones de lectura. Estos valores se deben ajustar en función del tráfico real de la aplicación.  
   - **connectionTimeout, idleTimeout y maxLifetime:** Estos tiempos se configuran para equilibrar la rapidez en la asignación de conexión sin retener conexiones que puedan volverse obsoletas o ser ineficientes, lo cual es especialmente importante en entornos de solo lectura donde se realizan muchas conexiones de corta duración.

3. **Validación de Conexiones**  
   - **connectionTestQuery:** Usar `SELECT 1 FROM DUAL` es estándar en Oracle para comprobar que la conexión sigue activa y operativa.  
   - **validationTimeout:** Permite tener un límite al probar la validez de la conexión.

4. **Otros Ajustes posibles**  
   - **poolName:** Facilita la identificación del pool en logs o en herramientas de monitoreo.  
   - **leakDetectionThreshold:** Ayuda a detectar conexiones que no han sido cerradas correctamente; en entornos de lecturas masivas es recomendable ajustar este valor para evitar falsos positivos.



.  
.  

---

.  
.  



## Consideraciones Adicionales

- **Carga y Concurrencia:**  
  Ajusta `maximumPoolSize` y `minimumIdle` según la cantidad de peticiones concurrentes 
  que se esperan. En un escenario de alta demanda, estos valores se pueden incrementar, 
  aunque siempre es importante evitar saturar la base de datos.

- **Incompatibilidades:**  
  Asegúrate de **no** mezclar parámetros que puedan entrar en conflicto, por ejemplo, 
  no se deben usar simultáneamente `jdbcUrl` y `dataSourceClassName` para la misma configuración.  
  En este ejemplo se utiliza únicamente `jdbcUrl`.


.  
.  



##### Ejemplo de Configuración en Código

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class OracleReadOnlyDataSourceConfig {

    public HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Parámetros de conexión a la base de datos Oracle
        config.setJdbcUrl("jdbc:oracle:thin:@//hostname:1521/ORCLPDB1");
        config.setUsername("usuario");
        config.setPassword("contraseña");
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        // --- Optimización para operaciones de solo lectura ---
        // Marcar las conexiones como readOnly para evitar modificaciones accidentales
        config.setReadOnly(true);
        // En operaciones de solo lectura suele convenir mantener el autoCommit activado.
        config.setAutoCommit(true);

        // --- Configuración del pool ---
        // Tamaño máximo del pool según la carga esperada. Para lecturas simultáneas se recomienda
        config.setMaximumPoolSize(20);
        // Número mínimo de conexiones que se mantendrán disponibles de forma inactiva.
        config.setMinimumIdle(10);

        // Tiempo máximo para esperar que se asigne una conexión (en milisegundos)
        config.setConnectionTimeout(30000); // 30 segundos
        // Tiempo máximo que una conexión puede estar inactiva antes de ser retirada
        config.setIdleTimeout(600000);       // 10 minutos
        // Vida máxima de una conexión en el pool
        config.setMaxLifetime(1800000);      // 30 minutos

        // --- Parámetros de validación ---
        // Oracle requiere una query simple para testear la validez de la conexión. DUAL es la tabla especial de Oracle.
        config.setConnectionTestQuery("SELECT 1 FROM DUAL");
        // Opcional: Se puede ajustar el timeout para la validación de conexión (si se necesita mayor tolerancia a latencia).
        config.setValidationTimeout(5000);   // 5 segundos

        // --- Otros ajustes útiles ---
        // Nombrar el pool para facilitar su identificación en logs y herramientas de monitoreo.
        config.setPoolName("OracleReadOnlyPool");
        // Para entornos donde se requiera detectar fugas en conexiones, se puede establecer un threshold.
        config.setLeakDetectionThreshold(3000); // 3 segundos (ajustar según la carga y características)

        return new HikariDataSource(config);
    }
}
```




