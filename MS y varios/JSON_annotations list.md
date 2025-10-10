# JSON_annotations list


A continuación, te proporciono una lista exhaustiva de las anotaciones más relevantes del paquete `com.fasterxml.jackson.annotation` en el contexto de Jackson con Spring Boot y Java 21, junto con su definición y un ejemplo práctico para cada una. Los ejemplos están diseñados para ser concisos, funcionales y aplicables en un proyecto Spring Boot con Java 21. Asumo que tienes un proyecto con `spring-boot-starter-web` (que incluye Jackson) y, en algunos casos, `lombok` para simplificar el código.

### **Lista de anotaciones de Jackson**


**1. @JsonProperty**  
**2. @JsonIgnore**  
**3. @JsonProperties**  
**4. @JsonInclude**  
**5. @JsonFormat**  
**6. @JsonManagedReference**  
**7. @JsonBackReference**  
**8. @JsonIdentityInfo**
**9. @JsonProperty**  
**10. @JsonTypeInfo y @JsonSubTypes**  
**11. @JsonPropertyOrder**  
**12. @JsonDeserialize**  
**13. @JsonCreator**  
**14. @JsonValue**  
**15. @JsonAnyGetter**  
**16. @JsonAnySetter**  
**17. @JsonGetter**  
**18. @JsonSetter**  
**19. @JsonRawValue**  
**20. @JsonRootName**  
**21. @JsonUnwrapped**  
**22. @JsonView**  


#### **1. @JsonProperty**
**Definición**: Especifica el nombre de una propiedad en el JSON para un campo, getter o setter. Permite renombrar propiedades, definir acceso (lectura/escritura) y asociar nombres personalizados.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Student {
    @JsonProperty("student_id")
    private Long id;

    @JsonProperty(value = "full_name", access = JsonProperty.Access.READ_ONLY)
    private String name;
}
```
**Uso en controlador**:
```java
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
**Explicación**: `student_id` y `full_name` reemplazan los nombres de los campos `id` y `name`. `access = READ_ONLY` hace que `name` solo se serialice, no se deserialice.

---

#### **2. @JsonIgnore**
**Definición**: Indica que un campo, método o parámetro debe ser ignorado durante la serialización y deserialización.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class User {
    private String username;
    @JsonIgnore
    private String password;
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
**Explicación**: El campo `password` no aparece en el JSON.

---

#### **3. @JsonIgnoreProperties**
**Definición**: Especifica propiedades a ignorar a nivel de clase. También permite ignorar propiedades desconocidas durante la deserialización.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties({"internalCode", "ignoreUnknown=true"})
@Data
public class Product {
    private String name;
    private String internalCode;
    private double price;
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
        product.setInternalCode("ABC123");
        product.setPrice(999.99);
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
**Explicación**: `internalCode` se ignora. Si el JSON de entrada tiene propiedades desconocidas, no causarán errores gracias a `ignoreUnknown=true`.

---

#### **4. @JsonInclude**
**Definición**: Controla la inclusión de propiedades en el JSON según su valor (por ejemplo, excluir nulos o vacíos).

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class Book {
    private String title;
    private String author;
    private String isbn; // Será null
}
```
**Uso**:
```java
@RestController
public class BookController {
    @GetMapping("/book")
    public Book getBook() {
        Book book = new Book();
        book.setTitle("Spring Guide");
        book.setAuthor("John Doe");
        return book;
    }
}
```
**Salida JSON**:
```json
{
  "title": "Spring Guide",
  "author": "John Doe"
}
```
**Explicación**: `isbn` no aparece porque es `null`. Otras opciones incluyen `NON_EMPTY`, `NON_DEFAULT`, etc.

---

#### **5. @JsonFormat**
**Definición**: Define el formato de serialización para tipos como fechas, números o enums.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class Event {
    private String name;

    @JsonFormat(pattern = "dd/MM/yyyy")
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
  "date": "27/05/2025"
}
```
**Explicación**: La fecha se formatea como `dd/MM/yyyy` en lugar del formato ISO por defecto.

---

#### **6. @JsonManagedReference**
**Definición**: Marca el lado "principal" de una relación bidireccional para evitar recursión infinita durante la serialización.

**Ejemplo** (usado con `@JsonBackReference`):
```java
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

@Data
public class Department {
    private String name;

    @JsonManagedReference
    private Employee manager;
}
```

---

#### **7. @JsonBackReference**
**Definición**: Marca el lado "inverso" de una relación bidireccional para evitar que se serialice, previniendo recursión infinita.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;

@Data
public class Employee {
    private String name;

    @JsonBackReference
    private Department department;
}
```
**Uso combinado**:
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
**Explicación**: `@JsonManagedReference` serializa `manager`, pero `@JsonBackReference` evita que `department` en `Employee` cause un bucle infinito.

---

#### **8. @JsonIdentityInfo**
**Definición**: Maneja referencias circulares asignando un identificador único a los objetos, evitando recursión infinita sin omitir propiedades.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Data
public class Team {
    private Long id;
    private String name;
    private Team parentTeam;
}
```
**Uso**:
```java
@RestController
public class TeamController {
    @GetMapping("/team")
    public Team getTeam() {
        Team team = new Team();
        team.setId(1L);
        team.setName("Dev Team");
        Team parent = new Team();
        parent.setId(2L);
        parent.setName("Main Team");
        team.setParentTeam(parent);
        parent.setParentTeam(team); // Relación circular
        return team;
    }
}
```
**Salida JSON**:
```json
{
  "id": 1,
  "name": "Dev Team",
  "parentTeam": {
    "id": 2,
    "name": "Main Team",
    "parentTeam": 1
  }
}
```
**Explicación**: `@JsonIdentityInfo` usa el campo `id` como identificador, serializando solo la referencia (`1`) en lugar de repetir el objeto.

---

#### **9. @JsonTypeInfo y @JsonSubTypes**
**Definición**: Maneja polimorfismo, permitiendo serializar/deserializar subtipos de una clase base. `@JsonTypeInfo` define cómo identificar el tipo, y `@JsonSubTypes` lista los subtipos posibles.

**Ejemplo**:
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
**Explicación**: `type` indica el subtipo (`dog`), y Jackson deserializa correctamente a `Dog`.

---

#### **10. @JsonPropertyOrder**
**Definición**: Especifica el orden de las propiedades en el JSON serializado.

**Ejemplo**:
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
**Explicación**: Las propiedades se ordenan según `@JsonPropertyOrder`.

---

#### **11. @JsonSerialize**
**Definición**: Especifica un serializador personalizado para un campo o clase.

**Ejemplo**:
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
**Explicación**: El serializador personalizado formatea `LocalDate` como `año-mes`.

---

#### **12. @JsonDeserialize**
**Definición**: Especifica un deserializador personalizado para un campo o clase.

**Ejemplo**:
```java
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import java.io.IOException;
import java.time.LocalDate;

class CustomDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String[] parts = p.getText().split("-");
        return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
    }
}

@Data
public class Task {
    private String name;

    @JsonDeserialize(using = CustomDateDeserializer.class)
    private LocalDate dueDate;
}
```
**Uso (entrada JSON)**:
```json
{
  "name": "Tarea 1",
  "dueDate": "2025-5"
}
```
**Código del controlador**:
```java
@RestController
public class TaskController {
    @PostMapping("/task")
    public Task createTask(@RequestBody Task task) {
        return task;
    }
}
```
**Explicación**: El deserializador convierte `"2025-5"` en `LocalDate.of(2025, 5, 1)`.

---

#### **13. @JsonAlias**
**Definición**: Permite definir nombres alternativos para una propiedad durante la deserialización, manteniendo el nombre original en la serialización.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class Profile {
    @JsonAlias({"user_name", "nickname"})
    private String username;
}
```
**Uso (entrada JSON)**:
```json
{
  "nickname": "juan123"
}
```
**Código**:
```java
@RestController
public class ProfileController {
    @PostMapping("/profile")
    public Profile createProfile(@RequestBody Profile profile) {
        return profile;
    }
}
```
**Salida JSON**:
```json
{
  "username": "juan123"
}
```
**Explicación**: Acepta `user_name` o `nickname` en la deserialización, pero serializa como `username`.

---

#### **14. @JsonCreator**
**Definición**: Marca un constructor o método de fábrica para la deserialización, útil para clases con lógica específica.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Order {
    private final String orderId;
    private final String customer;

    @JsonCreator
    public Order(@JsonProperty("order_id") String orderId, @JsonProperty("customer") String customer) {
        this.orderId = orderId.toUpperCase(); // Lógica personalizada
        this.customer = customer;
    }
}
```
**Uso (entrada JSON)**:
```json
{
  "order_id": "abc123",
  "customer": "Juan"
}
```
**Código**:
```java
@RestController
public class OrderController {
    @PostMapping("/order")
    public Order createOrder(@RequestBody Order order) {
        return order;
    }
}
```
**Salida JSON**:
```json
{
  "orderId": "ABC123",
  "customer": "Juan"
}
```
**Explicación**: `@JsonCreator` usa el constructor para deserializar, aplicando la lógica de convertir `orderId` a mayúsculas.

---

#### **15. @JsonValue**
**Definición**: Indica que un único método o campo debe usarse para representar el objeto completo en la serialización.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Status {
    private String code;
    private String description;

    @JsonValue
    public String getCode() {
        return code;
    }
}
```
**Uso**:
```java
@RestController
public class StatusController {
    @GetMapping("/status")
    public Status getStatus() {
        return new Status("OK", "Todo bien");
    }
}
```
**Salida JSON**:
```json
"OK"
```
**Explicación**: Solo el valor de `code` se serializa, ignorando otras propiedades.

---

#### **16. @JsonAnyGetter**
**Definición**: Permite serializar un mapa como propiedades dinámicas en el JSON.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class Config {
    private String name;
    private Map<String, Object> properties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
}
```
**Uso**:
```java
@RestController
public class ConfigController {
    @GetMapping("/config")
    public Config getConfig() {
        Config config = new Config();
        config.setName("AppConfig");
        config.getProperties().put("port", 8080);
        config.getProperties().put("enabled", true);
        return config;
    }
}
```
**Salida JSON**:
```json
{
  "name": "AppConfig",
  "port": 8080,
  "enabled": true
}
```
**Explicación**: Las entradas del mapa `properties` se serializan como propiedades de nivel superior.

---

#### **17. @JsonAnySetter**
**Definición**: Permite deserializar propiedades desconocidas en un mapa.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class DynamicData {
    private String name;
    private Map<String, Object> extras = new HashMap<>();

    @JsonAnySetter
    public void setExtra(String key, Object value) {
        extras.put(key, value);
    }
}
```
**Uso (entrada JSON)**:
```json
{
  "name": "Test",
  "customField": "value",
  "anotherField": 42
}
```
**Código**:
```java
@RestController
public class DynamicDataController {
    @PostMapping("/data")
    public DynamicData createData(@RequestBody DynamicData data) {
        return data;
    }
}
```
**Explicación**: `customField` y `anotherField` se almacenan en el mapa `extras`.

---

#### **18. @JsonGetter**
**Definición**: Marca un método como getter para la serialización, asignándole un nombre de propiedad en el JSON.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Data;

@Data
public class Person {
    private String firstName;
    private String lastName;

    @JsonGetter("full_name")
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```
**Uso**:
```java
@RestController
public class PersonController {
    @GetMapping("/person")
    public Person getPerson() {
        Person person = new Person();
        person.setFirstName("Juan");
        person.setLastName("Pérez");
        return person;
    }
}
```
**Salida JSON**:
```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "full_name": "Juan Pérez"
}
```
**Explicación**: El método `getFullName` se serializa como la propiedad `full_name`.

---

#### **19. @JsonSetter**
**Definición**: Marca un método como setter para la deserialización, asociándolo a una propiedad del JSON.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

@Data
public class Item {
    private String name;
    private int quantity;

    @JsonSetter("qty")
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
```
**Uso (entrada JSON)**:
```json
{
  "name": "Pen",
  "qty": 10
}
```
**Código**:
```java
@RestController
public class ItemController {
    @PostMapping("/item")
    public Item createItem(@RequestBody Item item) {
        return item;
    }
}
```
**Explicación**: La propiedad `qty` del JSON se mapea al campo `quantity` mediante el método `setQuantity`.

---

#### **20. @JsonRawValue**
**Definición**: Serializa un campo como JSON crudo, sin escapar comillas, útil para incrustar JSON preformateado.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;

@Data
public class Response {
    private String message;

    @JsonRawValue
    private String jsonData = "{\"key\": \"value\"}";
}
```
**Uso**:
```java
@RestController
public class ResponseController {
    @GetMapping("/response")
    public Response getResponse() {
        Response response = new Response();
        response.setMessage("OK");
        return response;
    }
}
```
**Salida JSON**:
```json
{
  "message": "OK",
  "jsonData": {"key": "value"}
}
```
**Explicación**: `jsonData` se incluye como JSON sin escapar comillas.

---

#### **21. @JsonRootName**
**Definición**: Especifica un nombre de raíz para envolver el JSON, útil cuando se habilita `SerializationFeature.WRAP_ROOT_VALUE`.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;

@JsonRootName("user")
@Data
public class UserProfile {
    private String username;
}
```
**Configuración**:
```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
    return mapper;
}
```
**Uso**:
```java
@RestController
public class UserProfileController {
    @GetMapping("/profile")
    public UserProfile getProfile() {
        UserProfile profile = new UserProfile();
        profile.setUsername("juan123");
        return profile;
    }
}
```
**Salida JSON**:
```json
{
  "user": {
    "username": "juan123"
  }
}
```
**Explicación**: El JSON se envuelve con el nombre `user`.

---

#### **22. @JsonUnwrapped**
**Definición**: Desenrolla las propiedades de un objeto anidado, incluyéndolas como propiedades de nivel superior en el JSON.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;

@Data
public class Address {
    private String street;
    private String city;
}

@Data
public class Customer {
    private String name;
    @JsonUnwrapped
    private Address address;
}
```
**Uso**:
```java
@RestController
public class CustomerController {
    @GetMapping("/customer")
    public Customer getCustomer() {
        Customer customer = new Customer();
        customer.setName("Juan");
        Address address = new Address();
        address.setStreet("Main St");
        address.setCity("Madrid");
        customer.setAddress(address);
        return customer;
    }
}
```
**Salida JSON**:
```json
{
  "name": "Juan",
  "street": "Main St",
  "city": "Madrid"
}
```
**Explicación**: Las propiedades de `Address` se incluyen en el mismo nivel que `name`.

---

#### **23. @JsonView**
**Definición**: Permite definir vistas para controlar qué campos se serializan según el contexto.

**Ejemplo**:
```java
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;

public class Views {
    public static class Public {}
    public static class Internal extends Public {}
}

@Data
public class Item {
    @JsonView(Views.Public.class)
    private String name;

    @JsonView(Views.Internal.class)
    private double price;
}
```
**Uso**:
```java
@RestController
public class ItemController {
    @JsonView(Views.Public.class)
    @GetMapping("/item/public")
    public Item getItemPublic() {
        Item item = new Item();
        item.setName("Pen");
        item.setPrice(1.99);
        return item;
    }

    @JsonView(Views.Internal.class)
    @GetMapping("/item/internal")
    public Item getItemInternal() {
        Item item = new Item();
        item.setName("Pen");
        item.setPrice(1.99);
        return item;
    }
}
```
**Salida JSON**:
- `/item/public`:
```json
{
  "name": "Pen"
}
```
- `/item/internal`:
```json
{
  "name": "Pen",
  "price": 1.99
}
```
**Explicación**: `@JsonView` filtra las propiedades según la vista especificada.

---

### **Notas adicionales**
- **Configuración global**: Puedes personalizar Jackson en Spring Boot mediante `application.yml` o un `ObjectMapper` personalizado, como se mostró en la respuesta anterior.
- **Java 21**: Los ejemplos son compatibles con Java 21, incluyendo soporte para `record`:
```java
import com.fasterxml.jackson.annotation.JsonProperty;

public record User(@JsonProperty("user_name") String username) {}
```
- **Dependencias**: Asegúrate de tener `jackson-databind` y, para tipos de Java 8/21 (como `LocalDate`), registra `JavaTimeModule`:
```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
}
```

### **Conclusión**
Las anotaciones de Jackson cubren una amplia gama de casos, desde renombrar propiedades hasta manejar relaciones complejas y polimorfismo. Los ejemplos son independientes y listos para usar en un proyecto Spring Boot con Java 21. Si necesitas más detalles, un ejemplo combinado o ayuda con un caso específico, házmelo saber.