# preguntas:

falta: 
	@EnableCaching en class RedisConfig.
	Configuración del Pool de Conexiones (Lettuce)

# ¿Qué es Redis?  

Redis (Remote Dictionary Server) es una base de datos en memoria, de tipo clave-valor, 
que permite almacenamiento en caché, sesiones, colas de mensajes y más. Se usa comúnmente 
para mejorar el rendimiento de aplicaciones al reducir la carga en bases de datos 
tradicionales como Oracle.

---

###  ¿Por qué usar Redis en consultas a Oracle?  
Las bases de datos relacionales como Oracle pueden ser lentas cuando se realizan consultas repetitivas sobre datos que no cambian con frecuencia. Redis ayuda a optimizar esto al almacenar los resultados en memoria, reduciendo el acceso a Oracle y mejorando la velocidad de respuesta.

---

## 🔹 Integración de Redis con Spring Boot  

### 1  **Agregar Dependencias en `pom.xml`**
Para usar Redis en Spring Boot, necesitas agregar las siguientes dependencias:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.0.0</version>
</dependency>
```

Aquí usamos **Jedis**, pero también puedes usar **Lettuce**, otro cliente popular de Redis.

---

### 2️ **Configurar Redis en `application.yml`**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 60000 # 60 segundos
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: 1000ms
```
Asegúrate de que Redis esté corriendo en el puerto 6379 o cambia la configuración según tu entorno.

---

### 3️ **Crear un Servicio con Caché para Consultas a Oracle**
Supongamos que tienes un repositorio JPA que consulta a Oracle:

```java
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByDni(String dni);
}
```

Ahora, creamos un **servicio que usa Redis para almacenar en caché** los resultados de esta consulta:

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Cacheable(value = "clientes", key = "#dni", unless = "#result == null")
    public Cliente getClientePorDni(String dni) {
        System.out.println("Consultando Oracle...");
        return clienteRepository.findByDni(dni).orElse(null);
    }
}
```
📌 **Explicación del código:**
- `@Cacheable("clientes")` → Indica que el resultado se guardará en Redis con la clave `dni`.
- `unless = "#result == null"` → Evita almacenar valores `null` en caché.
- La primera vez, se consulta Oracle; luego, Redis devolverá los datos sin ir a la BD.

---

### 4 **Habilitar Caché en Spring Boot**
En la clase de configuración principal, agrega la anotación:

```java
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
}
```
Esto habilita el soporte de caché en Spring Boot.

---

### 5️ **Prueba la Caché**
Llamamos al servicio varias veces:

```java
Cliente cliente1 = clienteService.getClientePorDni("12345678"); // → Consulta Oracle
Cliente cliente2 = clienteService.getClientePorDni("12345678"); // → Consulta Redis
```
🔹 En la primera llamada, los datos vienen de Oracle.  
🔹 En la segunda llamada, los datos se obtienen desde Redis.

---

### 6 **Eliminar Caché cuando los Datos Cambian**
Si un cliente se actualiza, debemos invalidar la caché:

```java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @CacheEvict(value = "clientes", key = "#dni")
    public Cliente actualizarCliente(String dni, Cliente nuevoCliente) {
        Cliente cliente = clienteRepository.findByDni(dni).orElseThrow();
        cliente.setNombre(nuevoCliente.getNombre());
        cliente.setEmail(nuevoCliente.getEmail());
        return clienteRepository.save(cliente);
    }
}
```


📌 **Explicación:**  
- `@CacheEvict(value = "clientes", key = "#dni")` → Elimina la entrada en caché cuando se actualiza el cliente.

---

## 🔹 Conclusión
Redis mejora el rendimiento al reducir las consultas a Oracle en datos frecuentemente accedidos.  
Spring Boot facilita la integración con anotaciones como `@Cacheable` y `@CacheEvict`.  


---  




En Spring Boot, puedes configurar varios parámetros adicionales para Redis en `application.yml`, dependiendo de si usas **Lettuce** o **Jedis** como cliente. A continuación, te explico los parámetros más importantes.

---

## 🔹 **Configuraciones Adicionales en `application.yml`**

### 1️⃣ **Configuración General**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: mySecurePassword
    ssl: false
    timeout: 60000
```
- `database: 0` → Redis permite múltiples bases de datos (`0` a `15` por defecto).  
- `password: mySecurePassword` → Configura la autenticación si Redis está protegido.  
- `ssl: false` → Si `true`, habilita conexiones seguras con TLS/SSL.  
- `timeout: 60000` → Tiempo máximo de espera antes de que falle la conexión (en milisegundos).

---

### 2️⃣ **Configuración del Pool de Conexiones (Lettuce)**
Si usas **Lettuce** (el cliente predeterminado en Spring Boot), puedes configurar el pool de conexiones:
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 2
        max-wait: 5000ms
```
- `max-active: 20` → Número máximo de conexiones activas.  
- `max-idle: 10` → Máximo de conexiones inactivas en el pool.  
- `min-idle: 2` → Mínimo de conexiones inactivas en el pool.  
- `max-wait: 5000ms` → Tiempo máximo de espera para obtener una conexión antes de lanzar un error.

---

### 3️⃣ **Configuración del Pool de Conexiones (Jedis)**
Si usas **Jedis**, cambia `lettuce.pool` por `jedis.pool`:
```yaml
spring:
  redis:
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 2
        max-wait: 5000ms
```

---

### 4️⃣ **Configuración de Sentinel (Alta Disponibilidad)**
Si usas **Redis Sentinel** (para alta disponibilidad), necesitas definir los nodos:
```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes:
        - 192.168.1.10:26379
        - 192.168.1.11:26379
        - 192.168.1.12:26379
```
- `master: mymaster` → Nombre del nodo maestro.  
- `nodes` → Lista de nodos Sentinel con IP y puerto.  

---

### 5️⃣ **Configuración de Clúster (Redis Cluster)**
Si usas un **Redis Cluster** en lugar de una sola instancia:
```yaml
spring:
  redis:
    cluster:
      nodes:
        - 192.168.1.10:6379
        - 192.168.1.11:6379
        - 192.168.1.12:6379
      max-redirects: 5
```
- `nodes` → Lista de nodos del clúster.  
- `max-redirects: 5` → Número de intentos de redirección cuando un nodo no responde.

---

### 6️⃣ **Configuración de Pub/Sub (Mensajería en Redis)**
Si usas Redis para **mensajería en tiempo real (Pub/Sub)**:
```yaml
spring:
  redis:
    pubsub:
      channels:
        - my-channel-1
        - my-channel-2
```
Esto define los canales a los que se suscribirá la aplicación.

---

### 7️⃣ **Configuración de Key Expiration (TTL)**
Para definir el tiempo de expiración de los datos en caché:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10 minutos en milisegundos
```
Esto establece un **TTL (Time-To-Live) de 10 minutos** para las claves en Redis.

---

### 8️⃣ **Configuración de Serialización**
Por defecto, Redis almacena los datos en formato binario, pero puedes cambiarlo a JSON para mejor legibilidad:
```yaml
spring:
  redis:
    serializer: jackson
```
Alternativas:
- `jackson` → Almacena los datos en formato JSON.  
- `string` → Almacena los datos como cadenas de texto.  
- `jdk` → Usa la serialización nativa de Java.

---

## 🔹 **Resumen**
| Parámetro | Descripción |
|-----------|------------|
| `host`, `port` | Configuran la conexión a Redis |
| `database` | Base de datos a utilizar (por defecto `0`) |
| `password` | Configura la autenticación |
| `ssl` | Habilita TLS/SSL si es `true` |
| `timeout` | Tiempo máximo de espera antes de que falle la conexión |
| `lettuce.pool` / `jedis.pool` | Configura el pool de conexiones |
| `sentinel.master`, `sentinel.nodes` | Configuración de Redis Sentinel (alta disponibilidad) |
| `cluster.nodes`, `max-redirects` | Configuración de Redis Cluster |
| `pubsub.channels` | Canales para mensajería en Redis |
| `cache.redis.time-to-live` | TTL para claves en caché |
| `serializer` | Formato de serialización (`jackson`, `string`, `jdk`) |

---

## 🔹 **Conclusión**
Spring Boot permite configurar Redis de manera flexible, dependiendo de si lo usas para:
- **Caché** (`time-to-live`)
- **Mensajería** (`pubsub`)
- **Alta disponibilidad** (`sentinel`)
- **Cluster** (`cluster.nodes`)
- **Seguridad** (`password`, `ssl`)
- **Optimización de conexiones** (`lettuce.pool`, `jedis.pool`)

Si necesitas más detalles sobre un caso específico, dime y lo ampliamos. 🚀