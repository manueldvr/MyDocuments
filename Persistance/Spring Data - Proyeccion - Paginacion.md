#  Spring Data JPA - Proyeccion Paginada - Controller & Service


### en Spring Data JPA qué ejemplos puedes dar con Proyeccion Paginada (DTOs Personalizados) con su respestivos Controller y Service?




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

😊



---
---


extención el ejemplo con búsquedas dinámicas, ordenamiento o validaciones adicionales...



# Extensión 



Puntos adicionales, incorporando búsquedas dinámicas, ordenamiento y validaciones para un enfoque más completo. Este ejemplo incluye todas las capacidades mencionadas.

---

### **1. Búsquedas dinámicas (con filtros por parámetros)**

#### **Repositorio con filtros dinámicos**
Agregamos filtros en la consulta del repositorio. Supongamos que queremos buscar empleados por nombre parcial, salario mínimo y máximo.

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT new com.example.demo.dto.EmployeeDTO(CONCAT(e.firstName, ' ', e.lastName), e.email) " +
           "FROM Employee e " +
           "WHERE (:name IS NULL OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:minSalary IS NULL OR e.salary >= :minSalary) " +
           "AND (:maxSalary IS NULL OR e.salary <= :maxSalary)")
    Page<EmployeeDTO> findFilteredEmployees(
            @Param("name") String name,
            @Param("minSalary") Double minSalary,
            @Param("maxSalary") Double maxSalary,
            Pageable pageable);
}
```

#### Explicación:
- Los filtros son opcionales (`:parametro IS NULL`).
- Se utiliza **`LIKE`** para búsquedas parciales en el nombre.
- Se aplican condiciones de salario mínimo y máximo.

---

#### **Servicio con filtros dinámicos**
Añadimos un método en el servicio para aplicar los filtros.

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

    public Page<EmployeeDTO> getFilteredEmployees(String name, Double minSalary, Double maxSalary, Pageable pageable) {
        return employeeRepository.findFilteredEmployees(name, minSalary, maxSalary, pageable);
    }
}
```

---

#### **Controlador para búsquedas dinámicas**
El controlador expone un endpoint que permite pasar filtros opcionales como parámetros de consulta.

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public Page<EmployeeDTO> getEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        return employeeService.getFilteredEmployees(name, minSalary, maxSalary, pageable);
    }

    private Sort.Order parseSort(String sort) {
        String[] sortParams = sort.split(",");
        String property = sortParams[0];
        String direction = sortParams.length > 1 ? sortParams[1] : "asc";
        return new Sort.Order(Sort.Direction.fromString(direction), property);
    }
}
```

#### Ejemplo de uso del endpoint:
- **URL**: `http://localhost:8080/employees?name=John&minSalary=50000&maxSalary=100000&page=0&size=5&sort=salary,desc`
- **Descripción**:
  - Busca empleados cuyo nombre contenga "John".
  - Filtra por salario entre 50,000 y 100,000.
  - Ordena por salario de mayor a menor.
  - Devuelve resultados paginados (primeras 5 filas).

---

### **2. Ordenamiento (dinámico)**

El método `parseSort` en el controlador convierte un parámetro como `fullName,asc` en un objeto `Sort.Order`. Esto permite ordenar dinámicamente por cualquier campo.

#### Ejemplo:
- **`sort=fullName,asc`**: Ordena por nombre completo de A-Z.
- **`sort=salary,desc`**: Ordena por salario de mayor a menor.

En el código:
```java
Sort.Order parseSort(String sort) {
    String[] sortParams = sort.split(",");
    String property = sortParams[0]; // Campo a ordenar
    String direction = sortParams.length > 1 ? sortParams[1] : "asc"; // Dirección (asc/desc)
    return new Sort.Order(Sort.Direction.fromString(direction), property);
}
```

---

### **3. Validaciones adicionales**

#### **Validaciones en los parámetros**
Podemos agregar validaciones para asegurarnos de que los valores proporcionados en los parámetros sean correctos.

```java
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class EmployeeFilter {

    private String name;

    @Min(value = 0, message = "El salario mínimo no puede ser negativo")
    private Double minSalary;

    @Min(value = 0, message = "El salario máximo no puede ser negativo")
    private Double maxSalary;

    // Getters y Setters
}
```

#### Actualizar el controlador para recibir el objeto de filtro:
```java
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;

@RestController
@Validated
public class EmployeeController {

    @GetMapping("/employees")
    public Page<EmployeeDTO> getEmployees(
            @ModelAttribute @Valid EmployeeFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        return employeeService.getFilteredEmployees(filter.getName(), filter.getMinSalary(), filter.getMaxSalary(), pageable);
    }
}
```

---

### **4. Respuesta del Endpoint**

#### URL:
```plaintext
GET http://localhost:8080/employees?name=Smith&minSalary=2000&sort=fullName,desc
```

#### Respuesta:
```json
{
    "content": [
        {
            "fullName": "Smith Johnson",
            "email": "smith.johnson@example.com"
        },
        {
            "fullName": "John Smith",
            "email": "john.smith@example.com"
        }
    ],
    "pageable": {
        "sort": {
            "sorted": true,
            "unsorted": false,
            "empty": false
        },
        "pageNumber": 0,
        "pageSize": 10,
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 2,
    "last": true,
    "size": 10,
    "number": 0,
    "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
    },
    "first": true,
    "numberOfElements": 2,
    "empty": false
}
```

---

### **Resumen**

1. **Filtros dinámicos**: Filtrar por múltiples parámetros (ej. nombre, rango de salario).
2. **Ordenamiento dinámico**: Personalizar la dirección y el campo para ordenar.
3. **Validaciones**: Validar parámetros como números positivos para salarios.
4. **Paginación completa**: Combinar todo con el soporte de paginación de Spring Data JPA.

Este enfoque es modular y fácil de extender. ¿Quieres agregar más funcionalidades, como exportar los resultados a Excel o agregar autenticación? 😊
