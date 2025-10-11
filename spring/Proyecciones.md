# Tutorial: Cómo utilizar Spring Boot Projections

Spring Boot Projections es una característica poderosa para trabajar con 
consultas específicas en JPA, proporcionando una forma eficiente de obtener 
únicamente los datos necesarios desde la base de datos sin cargar entidades 
completas.  

En este tutorial, exploraremos los conceptos básicos de las proyecciones en Spring Boot, su configuración y ejemplos prácticos.

---

## ¿Qué son las Projections en Spring Boot?

Las proyecciones son interfaces o clases que definimos para extraer un subconjunto de datos desde nuestras entidades. En lugar de recuperar y mapear toda la entidad, podemos seleccionar solo los campos necesarios, mejorando así el rendimiento de las consultas.

Spring Data JPA soporta tres tipos principales de proyecciones:

1. **Proyecciones basadas en interfaces**
2. **Proyecciones basadas en clases** (DTOs)
3. **Proyecciones dinámicas**

---

## Configuración Inicial

Antes de comenzar, asegúrate de tener un proyecto Spring Boot configurado con las dependencias necesarias:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

Configura una entidad de ejemplo:

```java
@Entity
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String department;
    private Double salary;

    // Getters y setters
}
```

---

## Tipos de Proyecciones

### 1. **Proyecciones basadas en interfaces**

Podemos definir una interfaz con los getters de los campos que queremos proyectar:

```java
public interface EmployeeNameProjection {
    String getFirstName();
    String getLastName();
}
```

En el repositorio, definimos un método para usar la proyección:

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<EmployeeNameProjection> findByDepartment(String department);
}
```

Uso:

```java
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/names-by-department")
    public List<EmployeeNameProjection> getNamesByDepartment(@RequestParam String department) {
        return employeeRepository.findByDepartment(department);
    }
}
```

### 2. **Proyecciones basadas en clases (DTOs)**

Creamos una clase para la proyección:

```java
public class EmployeeDTO {
    private final String firstName;
    private final String lastName;

    public EmployeeDTO(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters
}
```

El repositorio:

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query("SELECT new com.example.demo.EmployeeDTO(e.firstName, e.lastName) FROM Employee e WHERE e.department = :department")
    List<EmployeeDTO> findEmployeeDTOByDepartment(String department);
}
```

### 3. **Proyecciones dinámicas**

Spring permite usar proyecciones dinámicas para elegir la proyección en tiempo de ejecución:

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    <T> List<T> findByDepartment(String department, Class<T> type);
}
```

Uso:

```java
List<EmployeeNameProjection> names = employeeRepository.findByDepartment("IT", EmployeeNameProjection.class);
List<EmployeeDTO> dtos = employeeRepository.findByDepartment("IT", EmployeeDTO.class);
```

---

## Ejemplo Completo

Supongamos que queremos una API que devuelva el nombre completo de los empleados en un departamento. Esto podría implementarse usando proyecciones basadas en interfaces.

### Código Completo

#### Entidad

```java
@Entity
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String department;

    // Getters y setters
}
```

#### Proyección

```java
public interface EmployeeNameProjection {
    String getFirstName();
    String getLastName();
}
```

#### Repositorio

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<EmployeeNameProjection> findByDepartment(String department);
}
```

#### Controlador

```java
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/names-by-department")
    public List<EmployeeNameProjection> getEmployeeNamesByDepartment(@RequestParam String department) {
        return employeeRepository.findByDepartment(department);
    }
}
```

---

## Conclusión

Spring Boot Projections es una herramienta flexible y eficiente para optimizar consultas en aplicaciones basadas en Spring Data JPA. Usar proyecciones ayuda a reducir la sobrecarga de datos al recuperar solo la información necesaria desde la base de datos, lo que resulta en aplicaciones más rápidas y ligeras.

¿Listo para implementar Spring Boot Projections en tu proyecto? ¡Prueba estos ejemplos y ajusta las consultas a tus necesidades!

