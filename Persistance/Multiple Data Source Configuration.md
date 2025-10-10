# Data Sources
.


Esta clase configura un DataSource en un proyecto Spring Boot con Spring Data JPA. Se usa para conectarse a una base de datos específica y manejar transacciones.

.  

```java
@Configuration
@EnableJpaRepositories(
        basePackages = "com.gire.rapipago.transactions.repository.gl",
        entityManagerFactoryRef = "glEntityManagerFactory",
        transactionManagerRef = "glTransactionManager"
)
public class GlDataSourceConfig {

    @Bean(name = "glDataSource")
    @ConfigurationProperties(prefix = "gl.datasource")
    public DataSource glDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "glJdbcTemplate")
    public JdbcTemplate glJdbcTemplate(@Qualifier("glDataSource") DataSource glDataSource) {
        return new JdbcTemplate(glDataSource);
    }

    @Bean(name = "glEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean glEntityManagerFactory(
            @Qualifier("glDataSource") DataSource glDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(glDataSource);
        em.setPackagesToScan("com.gire.rapipago.transactions.entities.gl.denodo");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.default_schema", "umbral");  // aca puse umbral pero quizas no tenga un schema default
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "glTransactionManager")
    public PlatformTransactionManager bkTransactionManager(
            @Qualifier("glEntityManagerFactory") LocalContainerEntityManagerFactoryBean glEntityManagerFactory) {
        return new JpaTransactionManager(glEntityManagerFactory.getObject());
    }
}
```







Sí, esta clase configura un **DataSource** en un proyecto **Spring Boot** con **Spring Data JPA**. Se usa para conectarse a una base de datos específica y manejar transacciones.

---

## **Explicación de la Clase `GlDataSourceConfig`**

### **1. Anotaciones en la clase**
```java
@Configuration
@EnableJpaRepositories(
        basePackages = "com.lll.rapipago.transactions.repository.gl", 
        entityManagerFactoryRef = "glEntityManagerFactory",
        transactionManagerRef = "glTransactionManager"
)
```
- **`@Configuration`**: Indica que esta clase es una configuración de Spring.
- **`@EnableJpaRepositories`**:
  - `basePackages`: Indica en qué paquete se encuentran los repositorios JPA asociados a este **DataSource**.
  - `entityManagerFactoryRef`: Especifica el **EntityManagerFactory** para esta base de datos.
  - `transactionManagerRef`: Indica qué **TransactionManager** se debe usar para gestionar transacciones.

👉 **Propósito:** Configurar repositorios JPA para que usen el `glDataSource`.

---

### **2. Definición del DataSource**
```java
@Bean(name = "glDataSource")
@ConfigurationProperties(prefix = "gl.datasource")
public DataSource glDataSource() {
    return DataSourceBuilder.create().build();
}
```
- **`@Bean(name = "glDataSource")`**: Define un **Bean** llamado `"glDataSource"` (será inyectado en otros lugares del código).
- **`@ConfigurationProperties(prefix = "gl.datasource")`**:  
  - Spring buscará en `application.properties` o `application.yml` propiedades que comiencen con `gl.datasource` (como `url`, `username`, `password`, etc.).
- **`DataSourceBuilder.create().build();`**: Crea un objeto **DataSource** con los parámetros configurados.

👉 **Propósito:** Configurar el **DataSource** que conectará a la base de datos.

---

### **3. Configuración de `JdbcTemplate`**
```java
@Bean(name = "glJdbcTemplate")
public JdbcTemplate glJdbcTemplate(@Qualifier("glDataSource") DataSource glDataSource) {
    return new JdbcTemplate(glDataSource);
}
```
- **`JdbcTemplate`** es una clase de Spring que facilita el acceso a la base de datos usando **JDBC**.
- **`@Qualifier("glDataSource")`**: Especifica que debe usarse el `glDataSource` para este **JdbcTemplate**.

👉 **Propósito:** Permitir consultas SQL directas con `JdbcTemplate` en esta base de datos.

---

### **4. Configuración de `EntityManagerFactory`**
```java
@Bean(name = "glEntityManagerFactory")
public LocalContainerEntityManagerFactoryBean glEntityManagerFactory(
        @Qualifier("glDataSource") DataSource glDataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(glDataSource);
    em.setPackagesToScan("com.gire.rapipago.transactions.entities.gl.denodo");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.default_schema", "umbral");  // aca puse umbral pero quizas no tenga un schema default
    em.setJpaPropertyMap(properties);

    return em;
}
```
- **`@Bean(name = "glEntityManagerFactory")`**: Crea un **EntityManagerFactory** llamado `"glEntityManagerFactory"`.
- **`setDataSource(glDataSource)`**: Usa el **DataSource** configurado anteriormente.
- **`setPackagesToScan("com.gire.rapipago.transactions.entities.gl.denodo")`**:
  - Indica en qué paquete buscar **entidades JPA** (`@Entity`).
- **`HibernateJpaVendorAdapter`**: Especifica que se usará Hibernate como proveedor de JPA.
- **Propiedad `hibernate.default_schema`**:
  - Define el **esquema por defecto** como `"umbral"`.
  - Si la base de datos no usa esquemas, se puede omitir.

👉 **Propósito:** Configurar Hibernate y JPA para manejar entidades relacionadas con esta base de datos.

---

### **5. Configuración del `TransactionManager`**
```java
@Bean(name = "glTransactionManager")
public PlatformTransactionManager bkTransactionManager(
        @Qualifier("glEntityManagerFactory") LocalContainerEntityManagerFactoryBean glEntityManagerFactory) {
    return new JpaTransactionManager(glEntityManagerFactory.getObject());
}
```
- **`@Bean(name = "glTransactionManager")`**: Crea un manejador de transacciones llamado `"glTransactionManager"`.
- **`JpaTransactionManager(glEntityManagerFactory.getObject())`**:
  - Usa el **EntityManagerFactory** configurado anteriormente.
  - Gestiona transacciones para esta base de datos.

👉 **Propósito:** Manejar transacciones JPA en esta base de datos.

---

## **📌 Resumen**
Esta clase configura un **DataSource** adicional en Spring Boot para manejar conexiones con una base de datos específica. Los componentes principales son:
1. **`glDataSource`**: Configura la conexión a la base de datos.
2. **`glJdbcTemplate`**: Permite ejecutar consultas SQL con JDBC.
3. **`glEntityManagerFactory`**: Configura Hibernate y JPA para manejar entidades.
4. **`glTransactionManager`**: Maneja transacciones sobre esta base de datos.

Esta configuración permite que los repositorios JPA de `com.lll.rapipago.transactions.repository.gl` usen este DataSource en lugar del principal.


.  

---
.  

## **📌 ¿Qué es `LocalContainerEntityManagerFactoryBean`?**

`LocalContainerEntityManagerFactoryBean`** es una clase de Spring usada para configurar y crear un **EntityManagerFactory** en aplicaciones que usan **JPA (Java Persistence API)**.


Es una **implementación de `FactoryBean<EntityManagerFactory>`**, que Spring usa para configurar un **EntityManagerFactory** en aplicaciones JPA/Hibernate.



En términos simples, **es un contenedor que configura y proporciona un `EntityManagerFactory`** para manejar la persistencia de datos con JPA.
.  

**Ejemplo en la configuración anterior:**
```java
@Bean(name = "glEntityManagerFactory")
public LocalContainerEntityManagerFactoryBean glEntityManagerFactory(
        @Qualifier("glDataSource") DataSource glDataSource) {
    
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    
    em.setDataSource(glDataSource);  // Usa el DataSource específico
    em.setPackagesToScan("com.gire.rapipago.transactions.entities.gl.denodo"); // Indica dónde buscar entidades JPA

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter); // Indica que Hibernate será el proveedor de JPA

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.default_schema", "umbral");  
    em.setJpaPropertyMap(properties); // Configuración extra de Hibernate

    return em;
}
```
👉 **Propósito:** Configura una **fábrica de `EntityManager`** asociada a un **DataSource**, escanea las entidades y usa Hibernate como proveedor de JPA.

.  
.  

## **¿Para qué sirve?**
- Permite a Spring **gestionar automáticamente `EntityManagerFactory`**, en lugar de crearlo manualmente.
- Facilita la **configuración de JPA y Hibernate** en una aplicación Spring Boot.
- Se puede usar para definir múltiples bases de datos en una misma aplicación (como en tu caso).

.  
.
  
  
## 📌 Componentes Clave en `LocalContainerEntityManagerFactoryBean`
Veamos qué hacen los métodos clave que se usan en su configuración:

### 1. `setDataSource(DataSource dataSource)`**
```java
em.setDataSource(glDataSource);
```
- Asigna un **DataSource** para la conexión con la base de datos.

### 2. `setPackagesToScan(String... packages)`**
```java
em.setPackagesToScan("com.gire.rapipago.transactions.entities.gl.denodo");
```
- Indica en qué paquete buscar **clases `@Entity`**.

### 3. `setJpaVendorAdapter(JpaVendorAdapter jpaVendorAdapter)`**
```java
HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
em.setJpaVendorAdapter(vendorAdapter);
```
- Define el proveedor JPA, en este caso, **Hibernate**.

### 4. `setJpaPropertyMap(Map<String, ?> properties)`**
```java
Map<String, Object> properties = new HashMap<>();
properties.put("hibernate.default_schema", "umbral");  
em.setJpaPropertyMap(properties);
```
- Configura propiedades adicionales de Hibernate, como el **esquema por defecto**.

---

## 📌 Diferencias entre `EntityManagerFactory` y `LocalContainerEntityManagerFactoryBean`**
| Característica | `EntityManagerFactory` | `LocalContainerEntityManagerFactoryBean` |
|--------------|----------------------|-----------------------------|
| ¿Qué es? | Fábrica para crear `EntityManager`. | Bean de Spring que configura y gestiona un `EntityManagerFactory`. |
| ¿Quién lo usa? | Aplicaciones JPA estándar. | Aplicaciones Spring con JPA. |
| Configuración | Se configura manualmente. | Se configura automáticamente con `@Bean`. |
| Integración con Spring | Necesita código extra para integrarse con Spring. | Se integra directamente en Spring Boot. |

.  
.

## Resumen
- `LocalContainerEntityManagerFactoryBean` es un **Bean de Spring** que crea y configura un `EntityManagerFactory` para manejar la persistencia en JPA.
- Permite configurar Hibernate, el `DataSource`, el paquete de entidades y propiedades adicionales.
- Se usa en aplicaciones Spring Boot para integrar **JPA + Hibernate** de manera automática.
- Es esencial cuando trabajas con **múltiples bases de datos** en un mismo proyecto.

💡 **En mi caso:** `glEntityManagerFactory` se usa para conectarse a una base de datos diferente a la principal, permitiendo gestionar repositorios JPA separados.