# SECURITY

index:

- SQL Injection
- XSS
- Ejemplos





## ¿Qué es SQL Injection?

**SQL Injection (Inyección SQL)** es una vulnerabilidad de seguridad común en aplicaciones que interactúan con bases de datos relacionales. Ocurre cuando un atacante inserta código SQL malicioso en una consulta a través de entradas de usuario no validadas o sanitizadas. Esto permite al atacante manipular la consulta SQL original, potencialmente accediendo a datos sensibles, modificando registros, eliminando tablas o incluso ejecutando comandos del sistema.

#### Ejemplo básico:
Imagina una consulta SQL en tu API para autenticar un usuario:
```sql
SELECT * FROM usuarios WHERE nombre = '" + request.getParameter("nombre") + "' AND password = '" + request.getParameter("password") + "';
```
Si el atacante ingresa como "nombre": `admin' OR '1'='1` , la consulta se convierte en:
```sql
SELECT * FROM usuarios WHERE nombre = 'admin' OR '1'='1' AND password = '...';
```
Esto ignora la verificación de contraseña y permite acceso no autorizado.

#### En el contexto de APIs RESTful con Spring Boot:
En APIs RESTful (que usan HTTP methods como GET, POST, etc.), SQL Injection puede ocurrir en endpoints que procesan parámetros de consulta (query params), cuerpos JSON o paths. Por ejemplo, en un controlador Spring Boot:
```java
@GetMapping("/usuarios/{id}")
public Usuario getUsuario(@PathVariable String id) {
    // Consulta directa con id sin sanitización
    String sql = "SELECT * FROM usuarios WHERE id = " + id;
    // ...
}
```
Si `id` es `1; DROP TABLE usuarios; --`, se ejecuta una eliminación destructiva.

**Prevención en Spring Boot:**
- Usa **Prepared Statements** o **JPA/Hibernate** con consultas parametrizadas (e.g., `@Query("SELECT u FROM Usuario u WHERE u.id = :id")` y `query.setParameter("id", id)`).
- Valida y sanitiza inputs con anotaciones como `@Valid` y Bean Validation.
- Evita concatenación de strings en consultas SQL.


<br>
<br>

<br>
<br>


## ¿Qué es XSS?



**XSS (Cross-Site Scripting)** es otra vulnerabilidad web donde un atacante inyecta scripts maliciosos (generalmente JavaScript) en el contenido de una página web, que luego se ejecuta en el navegador de un usuario legítimo. Esto puede robar cookies, sesiones, redirigir a sitios falsos o defacear la página.

Hay tres tipos principales:
- **Reflejado (Reflected XSS)**: El script se inyecta vía URL y se refleja inmediatamente.
- **Almacenado (Stored XSS)**: El script se guarda en la base de datos y se sirve a múltiples usuarios.
- **DOM-based XSS**: Manipula el DOM del cliente-side sin interacción server-side.

#### Ejemplo básico:
Si una API devuelve HTML no escapado con input de usuario:
- Input malicioso: `<script>alert('XSS')</script>`
- Respuesta: `<p>Bienvenido, <script>alert('XSS')</script></p>`
Esto ejecuta el script en el cliente.

#### En el contexto de APIs RESTful con Spring Boot:
APIs RESTful suelen devolver JSON, no HTML, por lo que XSS puro es menos común (ya que JSON no se interpreta como HTML). Sin embargo, puede ocurrir si:
- La API genera HTML dinámico (e.g., para emails o vistas embebidas).
- El frontend (e.g., Angular/React) renderiza datos de la API sin escapar.
- En endpoints que sirven contenido mixto (JSON con HTML snippets).

Ejemplo en Spring Boot:
```java
@PostMapping("/comentarios")
public ResponseEntity<String> addComentario(@RequestBody String comentario) {
    // Guardar directamente sin escape
    return ResponseEntity.ok("<div>" + comentario + "</div>");
}
```
Si `comentario` incluye `<script>stealCookies()</script>`, se inyecta al renderizar.

**Prevención en Spring Boot:**
- Escapa outputs con **Thymeleaf** o **JSP** (usa `${comentario}` para auto-escape).
- Para JSON, usa librerías como Jackson con filtros de escape.
- Implementa **Content Security Policy (CSP)** headers vía filtros Spring.
- Valida inputs estrictamente (e.g., regex para permitir solo texto plano).

### Relación general en desarrollo de APIs RESTful con Spring Boot
En Spring Boot, estas vulnerabilidades surgen principalmente por falta de validación de inputs/outputs en controladores (@RestController). Usa **Spring Security** para autenticación/autorización, y sigue OWASP Top 10 guidelines. Prueba con herramientas como OWASP ZAP o SonarQube. Recuerda: las APIs deben ser stateless y seguras por diseño, priorizando sanitización en capas (input validation, query params, bodies).





<br>
<br>

<br>
<br>






## ejemplos prácticos de prevención de SQL Injection

A continuación, te proporciono **ejemplos prácticos de prevención de SQL Injection** en el contexto de una API RESTful desarrollada con **Spring Boot**. Los ejemplos se centran en las mejores prácticas para evitar esta vulnerabilidad, usando herramientas y técnicas comunes en Spring Boot.

---

### 1. Uso de **Prepared Statements** con JDBC
En lugar de concatenar cadenas en consultas SQL, utiliza **Prepared Statements** para parametrizar los valores de entrada, lo que evita que el input del usuario sea interpretado como código SQL.

**Ejemplo vulnerable:**
```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{nombre}")
    public List<Map<String, Object>> getUsuario(@PathVariable String nombre) {
        // ¡Vulnerable a SQL Injection!
        String sql = "SELECT * FROM usuarios WHERE nombre = '" + nombre + "'";
        return jdbcTemplate.queryForList(sql);
    }
}
```
Si el atacante envía `nombre = admin' OR '1'='1`, la consulta podría devolver todos los usuarios.

**Solución segura con Prepared Statement:**
```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{nombre}")
    public List<Map<String, Object>> getUsuario(@PathVariable String nombre) {
        String sql = "SELECT * FROM usuarios WHERE nombre = ?";
        return jdbcTemplate.queryForList(sql, nombre);
    }
}
```
**Por qué es seguro:** El `?` actúa como marcador de posición, y `JdbcTemplate` sanitiza automáticamente el valor de `nombre`, evitando que se interprete como parte de la consulta SQL.

---

### 2. Uso de **Spring Data JPA** con consultas parametrizadas
Spring Data JPA es una forma común de interactuar con bases de datos en Spring Boot. Puedes usar consultas parametrizadas con `@Query` para evitar SQL Injection.

**Ejemplo vulnerable:**
```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query(value = "SELECT * FROM usuarios WHERE nombre = '" + nombre + "'", nativeQuery = true)
    List<Usuario> findByNombre(String nombre);
}
```
Esto es vulnerable porque el parámetro `nombre` se concatena directamente en la consulta.

**Solución segura con parámetros nombrados:**
```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query(value = "SELECT * FROM usuarios WHERE nombre = :nombre", nativeQuery = true)
    List<Usuario> findByNombre(@Param("nombre") String nombre);
}
```
**Controlador asociado:**
```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/{nombre}")
    public List<Usuario> getUsuario(@PathVariable String nombre) {
        return usuarioRepository.findByNombre(nombre);
    }
}
```
**Por qué es seguro:** El parámetro `:nombre` es tratado como un valor, no como parte de la consulta SQL, gracias a la vinculación automática de parámetros en JPA.

---

### 3. Uso de métodos derivados en **Spring Data JPA**
Spring Data JPA permite crear consultas automáticamente basadas en nombres de métodos, lo que elimina la necesidad de escribir consultas SQL manualmente y reduce el riesgo de inyección.

**Ejemplo seguro:**
```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByNombre(String nombre);
}
```
**Controlador:**
```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/{nombre}")
    public List<Usuario> getUsuario(@PathVariable String nombre) {
        return usuarioRepository.findByNombre(nombre);
    }
}
```
**Por qué es seguro:** Spring Data JPA genera internamente una consulta parametrizada, evitando cualquier posibilidad de inyección SQL.

---

### 4. Validación de entradas con **Bean Validation**
Además de parametrizar consultas, es crucial validar las entradas del usuario para restringir los valores permitidos. Usa anotaciones de **Bean Validation** (JSR-380) en los DTOs o parámetros.

**Ejemplo con validación:**
```java
public class UsuarioDTO {
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El nombre solo puede contener letras y números")
    private String nombre;

    // Getters y setters
}
```
**Controlador con validación:**
```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/buscar")
    public List<Usuario> buscarUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        return usuarioRepository.findByNombre(usuarioDTO.getNombre());
    }
}
```
**Por qué es seguro:** La anotación `@Valid` asegura que el input cumpla con las reglas definidas (e.g., solo letras y números), reduciendo la probabilidad de inyectar código malicioso.

---

### 5. Configuración de **Spring Security** para proteger endpoints
Spring Security puede ayudar a limitar el acceso a endpoints sensibles, reduciendo el impacto de posibles vulnerabilidades. Por ejemplo, restringe quién puede acceder a ciertas consultas.

**Ejemplo:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN") // Solo admins acceden
                .anyRequest().authenticated()
            )
            .httpBasic();
        return http.build();
    }
}
```
**Por qué es seguro:** Limita el acceso a endpoints que podrían ser objetivo de SQL Injection, asegurando que solo usuarios autenticados y autorizados puedan interactuar con ellos.

---

### 6. Escapar entradas con **Hibernate Validator**
Si necesitas manejar consultas dinámicas (por ejemplo, filtros avanzados), asegúrate de sanitizar las entradas con validaciones estrictas o herramientas como **Hibernate Validator**.

**Ejemplo de sanitización manual:**
```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/buscar")
    public List<Map<String, Object>> buscarUsuario(@RequestParam String nombre) {
        // Sanitización manual
        if (!nombre.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Nombre inválido");
        }
        String sql = "SELECT * FROM usuarios WHERE nombre = ?";
        return jdbcTemplate.queryForList(sql, nombre);
    }
}
```
**Por qué es seguro:** La validación con expresiones regulares asegura que el input cumpla con un formato esperado antes de usarlo en la consulta.

---

### Recomendaciones adicionales
- **Usa un ORM como Hibernate**: Evita consultas SQL nativas siempre que sea posible, ya que los ORMs manejan la parametrización automáticamente.
- **Habilita logs de consultas**: Configura logs en tu base de datos o aplicación para detectar intentos de inyección.
- **Pruebas de seguridad**: Usa herramientas como **OWASP ZAP** o **Burp Suite** para simular ataques de SQL Injection.
- **Configura WAF**: Un Web Application Firewall puede detectar y bloquear intentos de inyección.
