# Multiple Data Source Uses



Si tienes **varios `DataSource`** configurados en tu aplicación Spring Boot y  
necesitas seleccionar uno específico para ejecutar un **store procedure** o  
una **query nativa**, hay varias formas de hacerlo según la tecnología que  
estés usando (JPA, JdbcTemplate o NamedParameterJdbcTemplate).  

---

## **🔹 Opción 1:**  
## Usar `@PersistenceContext` con el `EntityManager` correcto (JPA)

Si estás usando JPA (`@Query` o `EntityManager`),  
puedes inyectar el **`EntityManager`** correcto utilizando el `EntityManagerFactory`  
asociado al `DataSource` deseado.

.  


**Pasos:**  


### **📌 Paso 1: Definir `EntityManager` para cada `DataSource`**
Ya tienes un `LocalContainerEntityManagerFactoryBean` por cada fuente de datos.  
Ahora, expón el `EntityManager` en la configuración:

```java
@Bean(name = "glEntityManager")
public EntityManager entityManager(
        @Qualifier("glEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory) {
    return factory.getObject().createEntityManager();
}
```

---

### **📌 Paso 2: Usar `EntityManager` en el repositorio**
Ahora, en tu servicio o repositorio, puedes seleccionar el `EntityManager` adecuado:

```java
@Service
public class GlStoreProcedureService {

    @PersistenceContext(unitName = "glEntityManager")
    private EntityManager glEntityManager;

    public Object ejecutarStoreProcedure(String param) {
        StoredProcedureQuery query = glEntityManager.createStoredProcedureQuery("nombre_del_sp");
        query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        query.setParameter(1, param);
        
        query.execute();
        return query.getResultList(); // o query.getSingleResult()
    }
}
```

✅ **Ventaja**: Usa directamente JPA y puedes mapear la respuesta a entidades.  
⛔ **Desventaja**: Puede ser menos flexible si necesitas más control sobre la conexión.

.  

---

## **🔹 Opción 2: Usar `JdbcTemplate` para más control (SQL Nativo o Store Procedures)**
Si prefieres usar `JdbcTemplate`, puedes inyectar el correcto usando `@Qualifier`.

### **📌 Inyección en el Servicio**
```java
@Service
public class GlJdbcService {

    private final JdbcTemplate glJdbcTemplate;

    @Autowired
    public GlJdbcService(@Qualifier("glJdbcTemplate") JdbcTemplate glJdbcTemplate) {
        this.glJdbcTemplate = glJdbcTemplate;
    }

    public List<Map<String, Object>> ejecutarQueryNativa(String param) {
        String sql = "SELECT * FROM tabla WHERE columna = ?";
        return glJdbcTemplate.queryForList(sql, param);
    }

    public void ejecutarStoreProcedure(String param) {
        glJdbcTemplate.execute("CALL nombre_del_sp('" + param + "')");
    }
}
```

✅ **Ventaja**: Más flexible y directo, ideal para consultas nativas y procedimientos almacenados.  
⛔ **Desventaja**: No usa entidades JPA, lo que puede hacer el código menos estructurado.

.  

---

## **🔹 Opción 3: Usar `NamedParameterJdbcTemplate` para Queries más flexibles**
Si quieres mejorar la legibilidad de las consultas y evitar concatenación de parámetros:

### **📌 Inyección en el Servicio**
```java
@Service
public class GlNamedJdbcService {

    private final NamedParameterJdbcTemplate glNamedJdbcTemplate;

    @Autowired
    public GlNamedJdbcService(@Qualifier("glJdbcTemplate") DataSource glDataSource) {
        this.glNamedJdbcTemplate = new NamedParameterJdbcTemplate(glDataSource);
    }

    public List<Map<String, Object>> ejecutarQueryNativa(String param) {
        String sql = "SELECT * FROM tabla WHERE columna = :param";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("param", param);
        
        return glNamedJdbcTemplate.queryForList(sql, params);
    }
}
```

✅ **Ventaja**: Evita inyecciones SQL y mejora la legibilidad.  
⛔ **Desventaja**: No se puede usar para procedimientos almacenados directamente.

---

## **📝 Resumen: ¿Cuál método usar?**
| Método | Uso recomendado |
|--------|---------------|
| `EntityManager` (`@PersistenceContext`) | Si trabajas con JPA y necesitas mapear resultados a entidades. |
| `JdbcTemplate` | Para consultas nativas y procedimientos almacenados con más control. |
| `NamedParameterJdbcTemplate` | Para consultas más legibles y seguras sin procedimientos almacenados. |

Si necesitas ejecutar un **store procedure** y seleccionar el **data source específico**, la mejor opción suele ser **JdbcTemplate con `@Qualifier`**.

.  
.  

## Como sería para @NativeQuery o @Query



Si quieres usar `@Query(nativeQuery = true)` en un **repositorio JPA** (`JpaRepository` o `CrudRepository`), necesitas asegurarte de que el repositorio esté asociado al `EntityManagerFactory` correcto.  

Dado que tienes **múltiples `DataSource`**, debes anotar el repositorio con `@Repository` y asociarlo a la configuración correcta.

---

## **📌 Configuración de un Repositorio con `@Query(nativeQuery = true)`**

Supongamos que tienes una entidad `Operacion` y quieres hacer una consulta nativa en el repositorio:

```java
@Entity
@Table(name = "OPERACION", schema = "umbral")  // El schema debe coincidir con el definido en la config
public class Operacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    
    private Double monto;

    // Getters y Setters
}
```

Ahora creamos el **repositorio asociado al DataSource `glDataSource`**.

```java
@Repository
public interface OperacionRepository extends JpaRepository<Operacion, Long> {

    @Query(value = "SELECT * FROM OPERACION WHERE nombre = :nombre", nativeQuery = true)
    List<Operacion> buscarPorNombre(@Param("nombre") String nombre);
}
```

---

## **📌 Asociar el Repositorio al DataSource Correcto**
Cuando usas **varios DataSource**, debes asegurarte de que el repositorio usa la **configuración correcta**.  

En la configuración `GlDataSourceConfig.java`, ya tienes la anotación:

```java
@EnableJpaRepositories(
        basePackages = "com.lll.rapipago.transactions.repository.gl",  
        entityManagerFactoryRef = "glEntityManagerFactory",
        transactionManagerRef = "glTransactionManager"
)
```

Esto indica que **los repositorios ubicados en `com.lll.rapipago.transactions.repository.gl`** usarán el `glEntityManagerFactory`, el cual ya apunta a `glDataSource`.

Por lo tanto, tu **repositorio `OperacionRepository` debe estar en el paquete correcto**:  
📂 `com.lll.rapipago.transactions.repository.gl`

---

## **📌 Uso en el Servicio**
Ahora puedes inyectar el repositorio y hacer la consulta:

```java
@Service
public class OperacionService {

    private final OperacionRepository operacionRepository;

    @Autowired
    public OperacionService(OperacionRepository operacionRepository) {
        this.operacionRepository = operacionRepository;
    }

    public List<Operacion> obtenerOperacionesPorNombre(String nombre) {
        return operacionRepository.buscarPorNombre(nombre);
    }
}
```

---

## **📝 Resumen**
✅ **`@Query(nativeQuery = true)` es compatible con JPA**, pero debes asegurarte de:  
1. **Definir el esquema correctamente** en la entidad (`@Table(schema = "umbral")`).  
2. **Colocar el repositorio en el paquete correcto** para que use el `EntityManagerFactory` del DataSource correspondiente.  
3. **Confirmar que el esquema de la base de datos coincida** con lo configurado en `GlDataSourceConfig`.  


.  
.  
.  
.  




---
.  


Si quieres llamar a un **Stored Procedure** en un entorno con múltiples `DataSource`, debes asegurarte de que el repositorio esté asociado al **`EntityManagerFactory` correcto** y que el procedimiento almacenado sea compatible con **Spring Data JPA**.  

---

## **📌 Opción 1: Usar `@Procedure` en `@Repository`**  
Spring Data JPA permite llamar a stored procedures usando la anotación `@Procedure`.  

### **1️⃣ Definir una Proyección (DTO) para la Respuesta**  
Si tu procedimiento almacenado devuelve una lista de datos, puedes mapearlo a un DTO:  

```java
public interface ReporteOperacionesDTO {
    
    String getFecha();
    Double getMontoTotal();
    Integer getCantidadOperaciones();
}
```

⚠️ **Nota**:  
- **Usamos una interfaz en lugar de una clase** para aprovechar Spring Projections y evitar la necesidad de un `@Entity`.

---

### **2️⃣ Crear el Repositorio con `@Procedure`**
El repositorio debe estar **en el paquete correcto** para que use el `glEntityManagerFactory`.  

```java
@Repository
public interface ReporteOperacionesRepository extends CrudRepository<ReporteOperacionesDTO, Integer> {

    @Procedure(value = "sp_reporte_operaciones_diario")
    List<ReporteOperacionesDTO> obtenerReporteOperaciones(@Param("fecha") String fecha);
}
```

⚠️ **Importante**:  
- `"sp_reporte_operaciones_diario"` debe ser el **nombre exacto** del procedimiento almacenado en la base de datos.
- **Si tiene un esquema diferente**, usa `@Procedure(schema = "umbral", value = "sp_reporte_operaciones_diario")`.

---

### **3️⃣ Llamar al Stored Procedure desde un Servicio**
Ahora puedes inyectar el repositorio en un **servicio** y ejecutar el procedimiento almacenado:

```java
@Service
public class ReporteOperacionesService {

    private final ReporteOperacionesRepository reporteOperacionesRepository;

    @Autowired
    public ReporteOperacionesService(ReporteOperacionesRepository reporteOperacionesRepository) {
        this.reporteOperacionesRepository = reporteOperacionesRepository;
    }

    public List<ReporteOperacionesDTO> obtenerReporte(String fecha) {
        return reporteOperacionesRepository.obtenerReporteOperaciones(fecha);
    }
}
```

---

## **📌 Opción 2: Usar `EntityManager` para Mayor Control**
Si `@Procedure` no funciona correctamente (por ejemplo, si el SP tiene múltiples parámetros o una lógica compleja), puedes usar `EntityManager` manualmente.

```java
@Repository
public class ReporteOperacionesRepository {

    @PersistenceContext(unitName = "glEntityManagerFactory")
    private EntityManager entityManager;

    public List<Object[]> obtenerReporteOperaciones(String fecha) {
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_reporte_operaciones_diario")
            .registerStoredProcedureParameter("fecha", String.class, ParameterMode.IN)
            .setParameter("fecha", fecha);

        query.execute();
        return query.getResultList();
    }
}
```

Aquí, el resultado de `getResultList()` devuelve una **lista de arrays de objetos (`Object[]`)**, que puedes mapear manualmente a un DTO.

---

## **📝 Resumen**
✅ **Si el procedimiento almacenado es simple**, usa `@Procedure` en un repositorio basado en `CrudRepository`.  
✅ **Si el procedimiento es más complejo**, usa `EntityManager` y `StoredProcedureQuery`.  
✅ **Asegúrate de que el repositorio esté en el paquete correcto** para que use el `EntityManagerFactory` correcto.  

🚀 Con esto puedes llamar **Stored Procedures** en un entorno con múltiples `DataSource` en Spring Boot.


.    
.  


---

.  
.  


# Asignacion  del DATA Source cuando se usa un Store Procedure


En un entorno con **múltiples `DataSource`**, el **repositorio** usará el `EntityManagerFactory` correcto según la configuración de `@EnableJpaRepositories`.  

---

## **📌 Cómo se Asigna el DataSource Correcto**
Cada **repositorio** pertenece a un **`EntityManagerFactory` específico**, lo cual está definido en la configuración de los `DataSourceConfig`.

Veamos cómo funciona:

### **1️⃣ Configuración de Múltiples `DataSource`**
Supongamos que tienes dos configuraciones de `DataSource`, una para **GL** y otra para **otro sistema**.

#### **Configuración del DataSource `glDataSource`**
```java
@Configuration
@EnableJpaRepositories(
        basePackages = "com.lll.rapipago.transactions.repository.gl",  // 📌 Repositorios de GL
        entityManagerFactoryRef = "glEntityManagerFactory",
        transactionManagerRef = "glTransactionManager"
)
public class GlDataSourceConfig {

    @Bean(name = "glDataSource")
    @ConfigurationProperties(prefix = "gl.datasource")
    public DataSource glDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "glEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean glEntityManagerFactory(
            @Qualifier("glDataSource") DataSource glDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(glDataSource);
        em.setPackagesToScan("com.lll.rapipago.transactions.entities.gl");  // 📌 Entidades de GL

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.default_schema", "umbral");  
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "glTransactionManager")
    public PlatformTransactionManager glTransactionManager(
            @Qualifier("glEntityManagerFactory") LocalContainerEntityManagerFactoryBean glEntityManagerFactory) {
        return new JpaTransactionManager(glEntityManagerFactory.getObject());
    }
}
```

#### **Configuración de Otro DataSource (Ejemplo)**
```java
@Configuration
@EnableJpaRepositories(
        basePackages = "com.lll.rapipago.transactions.repository.otro",  // 📌 Repositorios de otro sistema
        entityManagerFactoryRef = "otroEntityManagerFactory",
        transactionManagerRef = "otroTransactionManager"
)
public class OtroDataSourceConfig {

    @Bean(name = "otroDataSource")
    @ConfigurationProperties(prefix = "otro.datasource")
    public DataSource otroDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "otroEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean otroEntityManagerFactory(
            @Qualifier("otroDataSource") DataSource otroDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(otroDataSource);
        em.setPackagesToScan("com.lll.rapipago.transactions.entities.otro");  // 📌 Entidades de otro sistema

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        return em;
    }

    @Bean(name = "otroTransactionManager")
    public PlatformTransactionManager otroTransactionManager(
            @Qualifier("otroEntityManagerFactory") LocalContainerEntityManagerFactoryBean otroEntityManagerFactory) {
        return new JpaTransactionManager(otroEntityManagerFactory.getObject());
    }
}
```

---

### **2️⃣ Cómo se Asigna el DataSource Correcto al Repositorio**
Cuando **Spring Boot** carga los repositorios, revisa los paquetes definidos en `@EnableJpaRepositories`:

- **Los repositorios en `com.lll.rapipago.transactions.repository.gl`** usarán `glEntityManagerFactory` (GL DataSource).
- **Los repositorios en `com.lll.rapipago.transactions.repository.otro`** usarán `otroEntityManagerFactory` (Otro DataSource).

Por lo tanto, para que **`ReporteOperacionesRepository` use `glDataSource`**, asegúrate de que esté en el paquete `com.lll.rapipago.transactions.repository.gl`:

```java
@Repository
public interface ReporteOperacionesRepository extends CrudRepository<ReporteOperacionesDTO, Integer> {

    @Procedure(value = "sp_reporte_operaciones_diario")
    List<ReporteOperacionesDTO> obtenerReporteOperaciones(@Param("fecha") String fecha);
}
```

Al ejecutar este repositorio, **Spring automáticamente usará `glEntityManagerFactory`**, porque está dentro del paquete configurado para GL.

---

### **3️⃣ Llamar al Repositorio desde un Servicio**
Finalmente, en el servicio inyectamos el repositorio y lo usamos sin preocuparnos del `DataSource`:

```java
@Service
public class ReporteOperacionesService {

    private final ReporteOperacionesRepository reporteOperacionesRepository;

    @Autowired
    public ReporteOperacionesService(ReporteOperacionesRepository reporteOperacionesRepository) {
        this.reporteOperacionesRepository = reporteOperacionesRepository;
    }

    public List<ReporteOperacionesDTO> obtenerReporte(String fecha) {
        return reporteOperacionesRepository.obtenerReporteOperaciones(fecha);
    }
}
```

---

## **📝 Resumen**
✅ **Cada `DataSourceConfig` define qué repositorios usan cada conexión.**  
✅ **La anotación `@EnableJpaRepositories(basePackages = "...")` determina qué `EntityManagerFactory` se usa para cada paquete de repositorios.**  
✅ **Los repositorios que llaman stored procedures deben estar en el paquete correcto para asociarse al `DataSource` adecuado.**  

🚀 **Con esta configuración, Spring Boot seleccionará automáticamente el `DataSource` correcto** según el repositorio que estés usando.