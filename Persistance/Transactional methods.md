# @Transactional


## El uso con readOnly = true


La anotación `@Transactional(readOnly = true)` en Spring Boot se utiliza para indicar que una operación en un método o clase es de **solo lectura** y debe ejecutarse dentro de una transacción gestionada por Spring. 


### **1. ¿Qué es `@Transactional`?**
La anotación `@Transactional` pertenece al módulo de Spring para la gestión de transacciones (`spring-tx`). Se utiliza para definir el ámbito de una transacción en un método o clase, permitiendo que Spring gestione automáticamente la creación, confirmación o reversión de transacciones en función de las reglas definidas.

### **2. ¿Qué significa `readOnly = true`?**
Cuando se especifica `readOnly = true`, se indica que la transacción es de **solo lectura**. Esto tiene varias implicaciones:

- **Optimización del rendimiento**: Al marcar una transacción como de solo lectura, Spring y el proveedor de la base de datos (como Hibernate o JPA) pueden realizar optimizaciones, como evitar el seguimiento de cambios en las entidades (dirty checking) o el uso de conexiones más ligeras.
- **Restricción de escritura**: Las operaciones de escritura (como `INSERT`, `UPDATE`, `DELETE`) no están permitidas en esta transacción. Si se intenta realizar una escritura, puede lanzarse una excepción dependiendo del proveedor de persistencia (por ejemplo, Hibernate puede arrojar una excepción como `TransactionRequiredException` o similar).
- **Uso típico**: Se usa en métodos que solo realizan consultas (`SELECT`) a la base de datos, como búsquedas o reportes, donde no se modifican los datos.

### **3. Contexto en Spring Boot**
En Spring Boot, `@Transactional(readOnly = true)` se usa frecuentemente en aplicaciones que trabajan con bases de datos a través de Spring Data JPA o Hibernate. Por ejemplo, se aplica en métodos de servicios o repositorios que solo leen datos. Spring Boot configura automáticamente el gestor de transacciones (`PlatformTransactionManager`) según el proveedor de persistencia detectado (como JPA, JDBC, etc.).

### **4. Ejemplo de uso**
```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional // Sin readOnly, permite escritura
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
```

- **Método `findUserById`**: Usa `@Transactional(readOnly = true)` porque solo realiza una consulta a la base de datos, sin modificar datos.
- **Método `saveUser`**: No usa `readOnly = true` porque implica una operación de escritura.

### **5. Beneficios de `readOnly = true`**
- **Optimización**: Reduce el overhead en el manejo de transacciones, ya que no se necesita rastrear cambios o preparar operaciones de escritura.
- **Seguridad**: Evita modificaciones accidentales en los datos dentro del método.
- **Claridad semántica**: Indica explícitamente que el método solo lee datos, mejorando la legibilidad del código.

### **6. Consideraciones**
- **Base de datos subyacente**: No todas las bases de datos o proveedores de persistencia aprovechan al máximo `readOnly = true`. Por ejemplo, Hibernate lo usa para desactivar el *dirty checking*, pero algunas bases de datos pueden ignorar esta configuración.
- **Excepciones**: Si un método con `readOnly = true` intenta realizar una escritura, el comportamiento depende del proveedor. Hibernate, por ejemplo, puede lanzar una excepción.
- **Propagación**: Por defecto, `@Transactional` usa la propagación `REQUIRED`. Esto significa que si el método se ejecuta dentro de una transacción existente, heredará sus propiedades, pero `readOnly = true` podría causar conflictos si la transacción padre permite escrituras.
- **Colocación de la anotación**: Puede usarse a nivel de método o clase. Si se coloca a nivel de clase, todos los métodos de la clase heredan la configuración, pero puedes sobrescribirla en métodos individuales.

### **7. Configuración adicional en Spring Boot**
Spring Boot configura automáticamente las transacciones si usas dependencias como `spring-boot-starter-data-jpa`. Asegúrate de tener habilitada la gestión de transacciones con:

```java
@SpringBootApplication
@EnableTransactionManagement // Generalmente no es necesario en Spring Boot, ya que se habilita automáticamente
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### **8. Cuándo usar `readOnly = true`**
- Métodos que solo ejecutan consultas `SELECT`.
- Operaciones de lectura intensiva donde deseas optimizar el rendimiento.
- Cuando deseas garantizar que un método no modifique datos accidentalmente.

### **9. Cuándo NO usar `readOnly = true`**
- Métodos que realizan operaciones de escritura (`INSERT`, `UPDATE`, `DELETE`).
- Si el método podría necesitar escalar a una transacción de escritura en el futuro.
- En transacciones complejas donde la propagación o el contexto transaccional no está claro.

### **10. Ejemplo práctico con Spring Data JPA**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Transactional(readOnly = true)
    List<User> findByLastName(String lastName);
}
```

Aquí, el método `findByLastName` está marcado como `readOnly = true` porque solo recupera datos de la base de datos.

### **Conclusión**
`@Transactional(readOnly = true)` es una herramienta poderosa en Spring Boot para optimizar y proteger operaciones de solo lectura en transacciones gestionadas. Se usa principalmente en métodos que consultan datos sin modificarlos, mejorando el rendimiento y la claridad del código. Sin embargo, debe usarse con cuidado, considerando las limitaciones del proveedor de persistencia y el contexto transaccional.

Si tienes un caso específico o necesitas más detalles, no dudes en preguntar.