# JSON - Jackson




Las anotaciones de Jackson en Spring Boot con Java 21 permiten personalizar la serialización y 
deserialización de objetos Java a JSON y viceversa. Jackson es la biblioteca predeterminada en Spring Boot 
para manejar JSON, y sus anotaciones ofrecen control detallado sobre cómo se mapean los objetos. 

A continuación, te detallo las principales funcionalidades y ejemplos prácticos con las anotaciones más comunes, usando Java 21 y Spring Boot.

### **Funcionalidades principales de las anotaciones de Jackson**
1. **Renombrar propiedades**: Cambiar el nombre de un campo en el JSON sin modificar el nombre en la clase Java.
2. **Ignorar propiedades**: Excluir campos o métodos durante la serialización/deserialización.
3. **Controlar el formato de datos**: Personalizar el formato de fechas, números, etc.
4. **Manejar relaciones bidireccionales**: Evitar problemas como recursión infinita.
5. **Incluir/excluir campos condicionalmente**: Por ejemplo, omitir valores nulos o vacíos.
6. **Soporte para polimorfismo**: Manejar herencia y subtipos en JSON.
7. **Personalizar serialización/deserialización**: Usar serializadores/deserializadores personalizados.
8. **Ordenar propiedades**: Definir el orden de los campos en el JSON.

### **Ejemplos prácticos**
A continuación, se presentan ejemplos con las anotaciones más comunes, implementados en un proyecto Spring Boot con Java 21. Supondré que tienes un proyecto con las dependencias `spring-boot-starter-web` (que incluye Jackson) y `lombok` para simplificar el código.

#### **1. Renombrar propiedades con `@JsonProperty`**
Permite mapear un campo Java a un nombre diferente en el JSON.

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Student {
    @JsonProperty("student_id")
    private Long id;

    @JsonProperty("full_name")
    private String name;
}
```

**Uso en un controlador Spring Boot**:
```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController {
    @GetMapping("/student")
    public Student getStudent() {
        Student student = new Student();
        student.setId(1L);
        student.setName("Juan Pérez");
        return student;
    }
}
```

**Salida JSON**:
```json
{
  "student_id": 1,
  "full_name": "Juan Pérez"
}
```

**Explicación**: Los nombres de las propiedades en el JSON (`student_id`, `full_name`) son diferentes a los nombres de los campos en la clase (`id`, `name`).[](https://www.tutorialspoint.com/jackson_annotations/jackson_annotations_jsonproperty.htm)

#### **2. Ignorar propiedades con `@JsonIgnore`**
Excluye un campo de la serialización y deserialización.

```java
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class User {
    private String username;
    @JsonIgnore
    private String password; // No se incluye en el JSON
}
```

**Uso**:
```java
@RestController
public class UserController {
    @GetMapping("/user")
    public User getUser() {
        User user = new User();
        user.setUsername("juan123");
        user.setPassword("secreto");
        return user;
    }
}
```

**Salida JSON**:
```json
{
  "username": "juan123"
}
```

**Explicación**: El campo `password` se omite en el JSON gracias a `@JsonIgnore`.[](https://www.appsdeveloperblog.com/jsonignore-annotation-in-java/)

#### **3. Formatear fechas con `@JsonFormat`**
Personaliza el formato de fechas en el JSON.

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class Event {
    private String name;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate date;
}
```

**Uso**:
```java
@RestController
public class EventController {
    @GetMapping("/event")
    public Event getEvent() {
        Event event = new Event();
        event.setName("Concierto");
        event.setDate(LocalDate.of(2025, 5, 27));
        return event;
    }
}
```

**Salida JSON**:
```json
{
  "name": "Concierto",
  "date": "27-05-2025"
}
```

**Explicación**: La anotación `@JsonFormat` formatea la fecha según el patrón especificado. Por defecto, Spring Boot desactiva la serialización de fechas como timestamps (`spring.jackson.serialization.write-dates-as-timestamps=false`).[](https://codingnconcepts.com/spring-boot/jackson-json-request-response-mapping/)

#### **4. Manejar relaciones bidireccionales con `@JsonManagedReference` y `@JsonBackReference`**
Evita recursión infinita en relaciones entre objetos.

```java
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

@Data
public class Department {
    private String name;

    @JsonManagedReference
    private Employee manager;
}

@Data
public class Employee {
    private String name;

    @JsonBackReference
    private Department department;
}
```

**Uso**:
```java
@RestController
public class DepartmentController {
    @GetMapping("/department")
    public Department getDepartment() {
        Department dept = new Department();
        dept.setName("IT");
        Employee emp = new Employee();
        emp.setName("Ana Gómez");
        emp.setDepartment(dept);
        dept.setManager(emp);
        return dept;
    }
}
```

**Salida JSON**:
```json
{
  "name": "IT",
  "manager": {
    "name": "Ana Gómez"
  }
}
```

**Explicación**: `@JsonManagedReference` serializa el lado "principal" de la relación, mientras que `@JsonBackReference` evita que el lado inverso cause un bucle infinito.[](https://www.javaguides.net/p/java-jackson-json-tutorial-with-examples.html)

#### **5. Incluir solo propiedades no nulas con `@JsonInclude`**
Omite campos con valores nulos en el JSON.

```java
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Product {
    private String name;
    private Double price;
    private String description; // Será null y no aparecerá
}
```

**Uso**:
```java
@RestController
public class ProductController {
    @GetMapping("/product")
    public Product getProduct() {
        Product product = new Product();
        product.setName("Laptop");
        product.setPrice(999.99);
        // description es null
        return product;
    }
}
```

**Salida JSON**:
```json
{
  "name": "Laptop",
  "price": 999.99
}
```

**Explicación**: `@JsonInclude(JsonInclude.Include.NON_NULL)` asegura que los campos con valores nulos no se incluyan en el JSON. También se puede configurar globalmente en `application.yml`:
```yaml
spring:
  jackson:
    default-property-inclusion: non_null
```
[](https://www.baeldung.com/spring-boot-customize-jackson-objectmapper)

#### **6. Personalizar serialización con `@JsonSerialize`**
Usa un serializador personalizado para un campo o clase.

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.IOException;
import java.time.LocalDate;

class CustomDateSerializer extends JsonSerializer<LocalDate> {
    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getYear() + "-" + value.getMonthValue());
    }
}

@Data
public class Project {
    private String name;

    @JsonSerialize(using = CustomDateSerializer.class)
    private LocalDate startDate;
}
```

**Uso**:
```java
@RestController
public class ProjectController {
    @GetMapping("/project")
    public Project getProject() {
        Project project = new Project();
        project.setName("App Desarrollo");
        project.setStartDate(LocalDate.of(2025, 5, 27));
        return project;
    }
}
```

**Salida JSON**:
```json
{
  "name": "App Desarrollo",
  "startDate": "2025-5"
}
```

**Explicación**: El serializador personalizado transforma `LocalDate` en un formato específico (año-mes).[](http://www.masterspringboot.com/web/rest-services/how-to-manage-json-data-in-spring-boot-with-jackson-project/)

#### **7. Ordenar propiedades con `@JsonPropertyOrder`**
Controla el orden de las propiedades en el JSON.

```java
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonPropertyOrder({"id", "name", "email"})
@Data
public class Customer {
    private String email;
    private Long id;
    private String name;
}
```

**Uso**:
```java
@RestController
public class CustomerController {
    @GetMapping("/customer")
    public Customer getCustomer() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("María");
        customer.setEmail("maria@example.com");
        return customer;
    }
}
```

**Salida JSON**:
```json
{
  "id": 1,
  "name": "María",
  "email": "maria@example.com"
}
```

**Explicación**: `@JsonPropertyOrder` asegura que las propiedades se serialicen en el orden especificado.[](https://www.tutorialspoint.com/jackson_annotations/jackson_annotations_quick_guide.htm)

#### **8. Soporte para polimorfismo con `@JsonTypeInfo` y `@JsonSubTypes`**
Maneja herencia y subtipos en JSON.

```java
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Dog.class, name = "dog"),
    @JsonSubTypes.Type(value = Cat.class, name = "cat")
})
@Data
public abstract class Animal {
    private String name;
}

@Data
public class Dog extends Animal {
    private String breed;
}

@Data
public class Cat extends Animal {
    private boolean indoor;
}
```

**Uso**:
```java
@RestController
public class AnimalController {
    @GetMapping("/animal")
    public Animal getAnimal() {
        Dog dog = new Dog();
        dog.setName("Rex");
        dog.setBreed("Labrador");
        return dog;
    }
}
```

**Salida JSON**:
```json
{
  "type": "dog",
  "name": "Rex",
  "breed": "Labrador"
}
```

**Explicación**: `@JsonTypeInfo` agrega un campo (`type`) para indicar el subtipo, y `@JsonSubTypes` mapea los nombres de los subtipos. Esto permite deserializar correctamente el JSON a la clase adecuada.[](https://www.javaguides.net/p/java-jackson-json-tutorial-with-examples.html)

### **Configuración global en Spring Boot**
Además de las anotaciones, puedes configurar Jackson globalmente en `application.yml` o mediante un bean `ObjectMapper`. Por ejemplo:

```yaml
spring:
  jackson:
    serialization:
      indent-output: true # Formato legible
      write-dates-as-timestamps: false # Fechas en formato legible
    default-property-inclusion: non_null # Excluir valores nulos
```

O con un bean:
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
```

**Explicación**: Esto configura Jackson para toda la aplicación, asegurando soporte para tipos de Java 8/21 (como `LocalDate`) y otras personalizaciones.[](https://www.baeldung.com/spring-boot-customize-jackson-objectmapper)

### **Consideraciones con Java 21**
- **Records**: Java 21 soporta records, que son ideales para DTOs. Puedes usar anotaciones de Jackson directamente en los records:
```java
import com.fasterxml.jackson.annotation.JsonProperty;

public record Person(@JsonProperty("full_name") String name, int age) {}
```
- **Módulo JavaTime**: Asegúrate de registrar `JavaTimeModule` para manejar tipos como `LocalDate` y `LocalDateTime` correctamente.
- **Dependencias**: Spring Boot 3.x (compatible con Java 21) incluye Jackson por defecto (`jackson-databind`, `jackson-annotations`, `jackson-core`). No necesitas agregar dependencias adicionales a menos que uses módulos específicos.

### **Conclusión**
Las anotaciones de Jackson en Spring Boot con Java 21 ofrecen un control granular sobre la serialización/deserialización de JSON. Desde renombrar propiedades hasta manejar polimorfismo o personalizar formatos, estas herramientas son esenciales para APIs RESTful. Los ejemplos anteriores cubren los casos más comunes, y puedes combinarlos según tus necesidades. Si necesitas ejemplos más específicos o tienes un caso particular, no dudes en preguntar.[](https://www.baeldung.com/jackson-annotations)[](https://springframework.guru/jackson-annotations-json/)[](https://medium.com/trabe/customize-your-serialization-using-jackson-annotations-d6b81c4785a6)