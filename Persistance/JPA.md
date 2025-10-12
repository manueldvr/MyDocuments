# JPA



Te proporcionaré un ejemplo claro de una relación **1:N** (uno a muchos) usando **Spring Data JPA**. En este caso, usaré un ejemplo donde una entidad `Author` (Autor) tiene varios `Book` (Libros). La relación será **un autor puede tener muchos libros**.

### **Paso 1: Definir las entidades**

#### Entidad `Author` (Uno)

```java
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Book> books = new ArrayList<>();

    // Constructores
    public Author() {}

    public Author(String name) {
        this.name = name;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}
```

#### Entidad `Book` (Muchos)
```java
import jakarta.persistence.*;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    // Constructores
    public Book() {}

    public Book(String title, Author author) {
        this.title = title;
        this.author = author;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
```

**Explicación de las anotaciones**:

- **`@OneToMany`**: En `Author`, indica que un autor puede tener muchos libros. El `mappedBy = "author"` especifica que la relación está gestionada por el campo `author` en la entidad `Book`.
- **`@ManyToOne`**: En `Book`, indica que muchos libros pueden pertenecer a un solo autor.
- **`@JoinColumn`**: Define la columna `author_id` en la tabla `Book` como clave foránea.
- **`fetch = FetchType.LAZY`**: Carga perezosa para optimizar el rendimiento (los datos relacionados no se cargan hasta que se necesiten).
- **`cascade = CascadeType.ALL`**: Operaciones como guardar o eliminar un autor se propagan a los libros asociados.

### **Paso 2: Repositorios**

#### Repositorio para `Author`
```java
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
```

#### Repositorio para `Book`
```java
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
```

### **Paso 3: Controlador REST**
Un controlador para exponer un endpoint que permita leer un autor y sus libros (relación 1:N).

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthorController {

    @Autowired
    private AuthorRepository authorRepository;

    @GetMapping("/authors/{id}")
    public Author getAuthorWithBooks(@PathVariable Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found"));
    }
}
```

### **Paso 4: Configuración básica**
Asegúrate de tener configurado Spring Boot con JPA en tu `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
```

### **Paso 5: Probar la relación 1:N**
Puedes probar la relación insertando datos y consultándolos. Aquí un ejemplo de cómo hacerlo en un método de prueba o en un servicio:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Override
    public void run(String... args) throws Exception {
        // Crear un autor
        Author author = new Author("J.K. Rowling");

        // Crear libros
        Book book1 = new Book("Harry Potter 1", author);
        Book book2 = new Book("Harry Potter 2", author);

        // Asociar libros al autor
        author.getBooks().add(book1);
        author.getBooks().add(book2);

        // Guardar en la base de datos
        authorRepository.save(author);

        // Consultar autor con sus libros
        Author savedAuthor = authorRepository.findById(author.getId()).orElse(null);
        System.out.println("Autor: " + savedAuthor.getName());
        savedAuthor.getBooks().forEach(book ->
            System.out.println("Libro: " + book.getTitle()));
    }
}
```

### **Resultado esperado**
Al ejecutar la aplicación y consultar el endpoint `/api/authors/1` (o el ID correspondiente), obtendrás algo como:

```json
{
    "id": 1,
    "name": "J.K. Rowling",
    "books": [
        {
            "id": 1,
            "title": "Harry Potter 1",
            "author_id": 1
        },
        {
            "id": 2,
            "title": "Harry Potter 2",
            "author_id": 1
        }
    ]
}
```

### **Notas finales**
- **Lazy Loading**: Los libros no se cargan hasta que accedes a la lista `books` del autor, gracias a `FetchType.LAZY`.
- **Cascade**: Si eliminas un autor, los libros asociados también se eliminan automáticamente debido a `CascadeType.ALL`.
- **H2 Database**: Este ejemplo usa H2 como base de datos en memoria para simplicidad, pero puedes usar cualquier base de datos compatible con JPA (MySQL, PostgreSQL, etc.).

Este código muestra cómo implementar y consultar una relación 1:N con Spring JPA de manera práctica y eficiente. Si necesitas más detalles o un caso más complejo, házmelo saber.


Te mostraré cómo realizar un **insert** de un `Author` con dos `Book` asociados en una relación **1:N** usando **Spring Data JPA**. El ejemplo se basa en las entidades definidas anteriormente (`Author` y `Book`) y se enfoca en insertar un autor con dos libros en la base de datos.



### **Código para insertar un Author con dos Books**

A continuación, te proporciono un ejemplo completo que incluye un controlador REST con un endpoint para realizar el insert de un `Author` con dos `Book`. El código asume que las entidades `Author` y `Book`, así como los repositorios `AuthorRepository` y `BookRepository`, ya están definidos como en el ejemplo anterior.


```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthorController {

    @Autowired
    private AuthorRepository authorRepository;

    @PostMapping("/authors")
    public Author createAuthorWithBooks(@RequestBody AuthorRequest authorRequest) {
        // Crear un nuevo autor
        Author author = new Author(authorRequest.getName());

        // Crear los dos libros y asociarlos al autor
        Book book1 = new Book(authorRequest.getBook1Title(), author);
        Book book2 = new Book(authorRequest.getBook2Title(), author);

        // Agregar los libros a la lista del autor
        author.getBooks().add(book1);
        author.getBooks().add(book2);

        // Guardar el autor (los libros se guardan automáticamente por el cascade)
        return authorRepository.save(author);
    }
}

// Clase DTO para recibir los datos del request
class AuthorRequest {
    private String name;
    private String book1Title;
    private String book2Title;

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBook1Title() {
        return book1Title;
    }

    public void setBook1Title(String book1Title) {
        this.book1Title = book1Title;
    }

    public String getBook2Title() {
        return book2Title;
    }

    public void setBook2Title(String book2Title) {
        this.book2Title = book2Title;
    }
}
```

### **Explicación del código**

1. **Controlador REST**:
   - El método `createAuthorWithBooks` recibe un JSON con el nombre del autor y los títulos de los dos libros a través de un DTO (`AuthorRequest`).
   - Se crea una instancia de `Author` con el nombre proporcionado.
   - Se crean dos instancias de `Book`, asociándolas al autor mediante el constructor.
   - Los libros se añaden a la lista `books` del autor.
   - Al guardar el `Author` con `authorRepository.save(author)`, los libros también se guardan automáticamente gracias a la configuración `cascade = CascadeType.ALL` en la entidad `Author`.

2. **DTO (`AuthorRequest`)**:
   - Se usa un objeto DTO (`AuthorRequest`) para recibir los datos del cliente (nombre del autor y títulos de los dos libros) en el cuerpo de la solicitud HTTP.

3. **Relación 1:N**:
   - La relación está definida en las entidades `Author` y `Book` (como en el ejemplo anterior), donde `Author` tiene una lista de `Book` y cada `Book` tiene una referencia al `Author` mediante una clave foránea (`author_id`).

### **Cómo probar el endpoint**
Puedes usar una herramienta como **Postman** o **curl** para enviar una solicitud POST al endpoint `/api/authors`. El cuerpo de la solicitud debe ser un JSON como el siguiente:

```json
{
    "name": "J.K. Rowling",
    "book1Title": "Harry Potter and the Philosopher's Stone",
    "book2Title": "Harry Potter and the Chamber of Secrets"
}
```

**Solicitud con curl**:

```bash
curl -X POST http://localhost:8080/api/authors \
-H "Content-Type: application/json" \
-d '{"name":"J.K. Rowling","book1Title":"Harry Potter and the Philosopher'\''s Stone","book2Title":"Harry Potter and the Chamber of Secrets"}'
```

**Respuesta esperada**:

```json
{
    "id": 1,
    "name": "J.K. Rowling",
    "books": [
        {
            "id": 1,
            "title": "Harry Potter and the Philosopher's Stone",
            "author_id": 1
        },
        {
            "id": 2,
            "title": "Harry Potter and the Chamber of Secrets",
            "author_id": 1
        }
    ]
}
```

### **Notas importantes**

- **Configuración previa**: Asegúrate de que las entidades `Author` y `Book`, los repositorios `AuthorRepository` y `BookRepository`, y la configuración de la base de datos (como en el `application.properties` del ejemplo anterior) estén correctamente definidos.
- **Cascade**: La propiedad `cascade = CascadeType.ALL` en la relación `@OneToMany` asegura que al guardar el `Author`, los `Book` asociados también se guarden automáticamente.
- **Base de datos**: Este ejemplo asume que usas una base de datos como H2 (como en el ejemplo anterior). Si usas otra base de datos (MySQL, PostgreSQL, etc.), ajusta la configuración en `application.properties`.
- **Validación**: Para producción, podrías añadir validaciones en el DTO (por ejemplo, con `@NotNull` o `@NotBlank` usando la dependencia `spring-boot-starter-validation`).

