#  Spring Data JPA - Proyeccion Paginada - Controller & Service


En Spring Data JPA qué ejemplos puedes dar con Proyeccion Paginada (DTOs Personalizados) con su respestivos Controller y Service?




Ejemplo completo de cómo trabajar con **Proyecciones Paginadas (DTOs Personalizados)** usando **Spring Data JPA**, incluyendo la configuración del repositorio, servicio y controlador. 

Este ejemplo cubre cómo devolver un DTO específico desde una consulta en un repositorio, y cómo hacerlo de manera paginada.



---

### **1. Supongamos la entidad `Employee`**

```java
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private Double salary;

    // Getters y Setters
}
```

---

### **2. Crear el DTO (Proyección personalizada)**

Creamos un DTO que definirá los campos específicos que queremos devolver.

```java
public class EmployeeDTO {

    private String fullName;
    private String email;

    public EmployeeDTO(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    // Getters y Setters
}
```

---

### **3. Configurar el Repositorio**

En el repositorio, definimos una consulta que proyecta datos en el `EmployeeDTO`. Podemos hacerlo de dos formas:

#### **a. Usando el Constructor del DTO**
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT new com.example.demo.dto.EmployeeDTO(CONCAT(e.firstName, ' ', e.lastName), e.email) " +
           "FROM Employee e")
    Page<EmployeeDTO> findAllProjectedEmployees(Pageable pageable);
}
```

#### **b. Usando una Proyección basada en interfaces**

Si prefieres usar una interfaz como proyección (en lugar de un DTO con constructor), puedes hacer algo como esto:

```java
public interface EmployeeProjection {
    String getFullName();
    String getEmail();
}
```

Y en el repositorio:
```java
@Query("SELECT CONCAT(e.firstName, ' ', e.lastName) AS fullName, e.email AS email FROM Employee e")
Page<EmployeeProjection> findAllProjectedEmployees(Pageable pageable);
```

---

### **4. Implementar el Servicio**

El servicio utiliza el repositorio para realizar la consulta y devolver los datos paginados.

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAllProjectedEmployees(pageable);
    }
}
```

---

### **5. Crear el Controlador**

El controlador expone un endpoint para consumir los datos paginados.

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/employees")
    public Page<EmployeeDTO> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return employeeService.getAllEmployees(PageRequest.of(page, size));
    }
}
```

---

### **6. Probar el Endpoint**

Haz una llamada GET al endpoint `/employees` para obtener los datos paginados. Por ejemplo:

- **URL**: `http://localhost:8080/employees?page=0&size=5`
- **Respuesta**:
```json
{
    "content": [
        {
            "fullName": "John Doe",
            "email": "john.doe@example.com"
        },
        {
            "fullName": "Jane Smith",
            "email": "jane.smith@example.com"
        }
    ],
    "pageable": {
        "sort": {
            "sorted": false,
            "unsorted": true,
            "empty": true
        },
        "pageNumber": 0,
        "pageSize": 5,
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 2,
    "last": true,
    "size": 5,
    "number": 0,
    "sort": {
        "sorted": false,
        "unsorted": true,
        "empty": true
    },
    "first": true,
    "numberOfElements": 2,
    "empty": false
}
```

---

### **7. Características adicionales**
- **Filtros y búsquedas**: Puedes agregar filtros a la consulta usando parámetros dinámicos en el repositorio.
  ```java
  @Query("SELECT new com.example.demo.dto.EmployeeDTO(CONCAT(e.firstName, ' ', e.lastName), e.email) " +
         "FROM Employee e WHERE e.salary > :minSalary")
  Page<EmployeeDTO> findEmployeesWithHighSalary(Double minSalary, Pageable pageable);
  ```

- **Ordenamiento**: Aprovecha las capacidades de ordenamiento de Spring Data JPA usando el parámetro `Sort` en `Pageable`.

---

### **8. Ventajas de este enfoque**
1. **Optimización**: Solo seleccionas las columnas que necesitas, en lugar de traer toda la entidad.
2. **Paginación eficiente**: Combinas la proyección con la paginación para obtener grandes conjuntos de datos de manera escalable.
3. **Flexibilidad**: Puedes usar DTOs personalizados o interfaces para adaptarte a tus necesidades.

¿Quieres extender este ejemplo con búsquedas dinámicas, ordenamiento o validaciones adicionales? 😊