# **REDIS 2**

.  

pregunta:

En una aplicacion con Java 21, Spring Boot, y la siguiente configuración:
```
spring.redis.database=1
spring.redis.sentinel.master=${REDIS_MASTER_NAME}
spring.redis.sentinel.nodes.host=${REDIS_SENTINEL_NODES_HOST}
spring.redis.sentinel.nodes.port=${REDIS_SENTINEL_NODES_PORT}
spring.redis.sentinel.password=${REDIS_PASSWORD}
spring.redis.password=${REDIS_PASSWORD}
spring.redis.timeout=60000
```
y

```java
@Configuration
public class RedisConfig {

    @Value("${spring.redis.sentinel.master}")
    private String masterNodeName;
    @Value("${spring.redis.sentinel.nodes.host}")
    private String sentinelNodesHost;
    @Value("${spring.redis.sentinel.nodes.port}")
    private int sentinelNodesPort;
    @Value("${spring.redis.sentinel.password}")
    private String sentinelPassword;
    @Value("${spring.redis.password}")
    private String redisPassword;
    @Value("${spring.redis.database}")
    private int indexDatabase;


    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration().master(masterNodeName)
                .sentinel(RedisNode.newRedisNode()
                        .listeningAt(sentinelNodesHost,sentinelNodesPort)
                        .build());
        sentinelConfig.setSentinelPassword(RedisPassword.of(sentinelPassword));
        sentinelConfig.setPassword(RedisPassword.of(redisPassword));
        sentinelConfig.setDatabase(indexDatabase);

        return new LettuceConnectionFactory(sentinelConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(LettuceConnectionFactory connectionFactory){

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        SimpleModule module = new SimpleModule();
        objectMapper.registerModule(module);

        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jacksonSerializer.setObjectMapper(objectMapper);

        RedisSerializationContext<String, Object> serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(new StringRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(jacksonSerializer)
                .value(jacksonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}

```

.  
.  
.  

**- ¿Qué faltaría para mejorar y para crear un servicio RESTful con RedisTemplate para almacenar y recuperar los datos?**



Para mejorar la configuración actual y crear un servicio RESTful 
con RedisTemplate que almacene y recupere datos en tu aplicación 
Spring Boot con Java 21 y Redis Sentinel, hay varios aspectos a 
 considerar. 

A continuación, te detallo las mejoras posibles y cómo implementar un servicio RESTful:

---

### Mejoras a la configuración actual

1. **Soporte para múltiples nodos Sentinel**  
   En la configuración actual, solo defines un nodo Sentinel (`sentinelNodesHost` y `sentinelNodesPort`). Redis Sentinel suele trabajar con múltiples nodos para alta disponibilidad. Podrías modificar la configuración para aceptar una lista de nodos Sentinel:

   ```java
   @Value("${spring.redis.sentinel.nodes.host}")
   private String sentinelNodesHost; // Cambiar a una lista separada por comas, ej: "host1,host2,host3"
   ```

   Luego, en el `RedisSentinelConfiguration`, iterar sobre la lista:

   ```java
   RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration().master(masterNodeName);
   String[] hosts = sentinelNodesHost.split(",");
   for (String host : hosts) {
       sentinelConfig.sentinel(RedisNode.newRedisNode()
               .listeningAt(host.trim(), sentinelNodesPort)
               .build());
   }
   ```

   Esto permite que tu aplicación sea más resiliente al usar varios nodos Sentinel.

2. **Configuración de Pool de Conexiones**  
   Actualmente, no configuras un pool de conexiones para Lettuce. Puedes agregar un `ClientResources` y un `LettucePoolingClientConfiguration` para optimizar el rendimiento:

   ```java
   @Bean
   public LettuceConnectionFactory redisConnectionFactory() {
       RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
               .master(masterNodeName)
               .sentinel(RedisNode.newRedisNode()
                       .listeningAt(sentinelNodesHost, sentinelNodesPort)
                       .build());
       sentinelConfig.setSentinelPassword(RedisPassword.of(sentinelPassword));
       sentinelConfig.setPassword(RedisPassword.of(redisPassword));
       sentinelConfig.setDatabase(indexDatabase);

       LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
               .commandTimeout(Duration.ofMillis(60000))
               .poolConfig(new GenericObjectPoolConfig()) // Configuración del pool (maxTotal, maxIdle, etc.)
               .build();

       return new LettuceConnectionFactory(sentinelConfig, clientConfig);
   }
   ```

3. **Manejo de Excepciones**  
   Agrega un manejo de excepciones global para Redis (por ejemplo, `RedisConnectionException`) para que la aplicación sea más robusta.

4. **Validación de Propiedades**  
   Asegúrate de que las propiedades inyectadas (`@Value`) no sean nulas o vacías, lanzando excepciones personalizadas si falta alguna configuración crítica.

5. **Soporte para RedisTemplate (Bloqueante)**  
   Además de `ReactiveRedisTemplate`, podrías agregar un `RedisTemplate` tradicional (bloqueante) si planeas usarlo en un servicio RESTful síncrono:

   ```java
   @Bean
   public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
       RedisTemplate<String, Object> template = new RedisTemplate<>();
       template.setConnectionFactory(connectionFactory);
       template.setKeySerializer(new StringRedisSerializer());
       template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
       template.setHashKeySerializer(new StringRedisSerializer());
       template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
       template.afterPropertiesSet();
       return template;
   }
   ```

---

### Implementación de un Servicio RESTful con RedisTemplate

A continuación, te muestro cómo crear un servicio RESTful básico para almacenar y recuperar datos usando `RedisTemplate`.

#### 1. Crear un Servicio
Primero, define un servicio que use `RedisTemplate` para interactuar con Redis:

```java
@Service
public class RedisDataService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisDataService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveData(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }
}
```

#### 2. Crear un Controlador REST
Luego, implementa un controlador RESTful que use el servicio:

```java
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    private final RedisDataService redisDataService;

    public RedisController(RedisDataService redisDataService) {
        this.redisDataService = redisDataService;
    }

    @PostMapping("/data")
    public ResponseEntity<String> saveData(@RequestParam String key, @RequestBody Object value) {
        redisDataService.saveData(key, value);
        return ResponseEntity.ok("Data saved successfully");
    }

    @GetMapping("/data")
    public ResponseEntity<Object> getData(@RequestParam String key) {
        Object value = redisDataService.getData(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }

    @DeleteMapping("/data")
    public ResponseEntity<String> deleteData(@RequestParam String key) {
        redisDataService.deleteData(key);
        return ResponseEntity.ok("Data deleted successfully");
    }
}
```

#### 3. Ejemplo de Uso
- **Guardar datos**:  
  `POST /api/redis/data?key=user1`  
  Body: `{"name": "John", "age": 30}`

- **Recuperar datos**:  
  `GET /api/redis/data?key=user1`  
  Respuesta: `{"name": "John", "age": 30}`

- **Eliminar datos**:  
  `DELETE /api/redis/data?key=user1`

---

### Mejoras Adicionales para el Servicio RESTful

1. **Expiración de Claves (TTL)**  
   Agrega soporte para establecer un tiempo de vida (TTL) en las claves almacenadas:

   ```java
   public void saveData(String key, Object value, long ttlSeconds) {
       redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
   }
   ```

   Actualiza el controlador para aceptar un parámetro opcional `ttl`:

   ```java
   @PostMapping("/data")
   public ResponseEntity<String> saveData(@RequestParam String key, 
                                          @RequestBody Object value, 
                                          @RequestParam(required = false, defaultValue = "-1") long ttl) {
       if (ttl > 0) {
           redisDataService.saveData(key, value, ttl);
       } else {
           redisDataService.saveData(key, value);
       }
       return ResponseEntity.ok("Data saved successfully");
   }
   ```

2. **Validación de Entrada**  
   Usa anotaciones como `@NotNull` y `@Valid` para validar los parámetros y el cuerpo de la solicitud.

3. **Soporte para Tipos Específicos**  
   Si planeas almacenar objetos específicos (por ejemplo, una clase `User`), ajusta el `RedisTemplate` y el serializador para ese tipo:

   ```java
   @Bean
   public RedisTemplate<String, User> userRedisTemplate(LettuceConnectionFactory connectionFactory) {
       RedisTemplate<String, User> template = new RedisTemplate<>();
       template.setConnectionFactory(connectionFactory);
       template.setKeySerializer(new StringRedisSerializer());
       template.setValueSerializer(new Jackson2JsonRedisSerializer<>(User.class));
       template.afterPropertiesSet();
       return template;
   }
   ```

4. **Manejo de Errores**  
   Agrega un `@ControllerAdvice` para manejar excepciones como `RedisConnectionException` o `DataAccessException`.

5. **Documentación con OpenAPI/Swagger**  
   Integra Springdoc OpenAPI para documentar tu API RESTful automáticamente.

---

### Conclusión
Con estas mejoras, se obtiene una configuración más robusta para Redis Sentinel y
 un servicio RESTful funcional con `RedisTemplate`. 
 
Para soporte reactivo, puedes adaptar el servicio y controlador para 
usar `ReactiveRedisTemplate` en lugar de `RedisTemplate`.


.  

.  


---




# *Reactive Redis*



Para implementar un controlador RESTful reactivo utilizando `ReactiveRedisTemplate` 
en lugar de `RedisTemplate`, necesitas ajustar tanto el servicio como el controlador para
 trabajar con el paradigma reactivo de Spring WebFlux. Esto implica usar tipos como `Mono` y 
 `Flux` de Project Reactor, que son adecuados para operaciones asíncronas y no bloqueantes.

A continuación, te detallo cómo implementar el servicio y el controlador con `ReactiveRedisTemplate`:

---

### 1. Servicio con ReactiveRedisTemplate

Primero, modifica el servicio para usar `ReactiveRedisTemplate` y devolver tipos reactivos (`Mono`):

```java
@Service
public class ReactiveRedisDataService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public ReactiveRedisDataService(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    public Mono<Void> saveData(String key, Object value) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value)
                .then(); // Convertimos el Mono<Boolean> a Mono<Void>
    }

    public Mono<Object> getData(String key) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .switchIfEmpty(Mono.empty()); // Devuelve Mono vacío si no hay valor
    }

    public Mono<Void> deleteData(String key) {
        return reactiveRedisTemplate.opsForValue()
                .delete(key)
                .then(); // Convertimos el Mono<Boolean> a Mono<Void>
    }

    // Método con TTL (opcional)
    public Mono<Void> saveData(String key, Object value, long ttlSeconds) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value, Duration.ofSeconds(ttlSeconds))
                .then();
    }
}
```

**Notas:**
- `Mono<Void>` se usa para operaciones que no devuelven un valor significativo (como `save` o `delete`).
- `Mono<Object>` se usa para operaciones que devuelven un valor (como `get`).
- `.then()` convierte un `Mono<Boolean>` (resultado de operaciones como `set` o `delete`) en un `Mono<Void>` para indicar que la operación se completó sin devolver datos.

---

### 2. Controlador RESTful Reactivo

Ahora, implementa el controlador usando Spring WebFlux, que es compatible con tipos reactivos:

```java
@RestController
@RequestMapping("/api/redis")
public class ReactiveRedisController {

    private final ReactiveRedisDataService reactiveRedisDataService;

    public ReactiveRedisController(ReactiveRedisDataService reactiveRedisDataService) {
        this.reactiveRedisDataService = reactiveRedisDataService;
    }

    @PostMapping("/data")
    public Mono<ResponseEntity<String>> saveData(
            @RequestParam String key,
            @RequestBody Object value,
            @RequestParam(required = false, defaultValue = "-1") long ttl) {
        Mono<Void> saveOperation = (ttl > 0)
                ? reactiveRedisDataService.saveData(key, value, ttl)
                : reactiveRedisDataService.saveData(key, value);

        return saveOperation
                .then(Mono.just(ResponseEntity.ok("Data saved successfully")));
    }

    @GetMapping("/data")
    public Mono<ResponseEntity<Object>> getData(@RequestParam String key) {
        return reactiveRedisDataService.getData(key)
                .map(value -> ResponseEntity.ok(value))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/data")
    public Mono<ResponseEntity<String>> deleteData(@RequestParam String key) {
        return reactiveRedisDataService.deleteData(key)
                .then(Mono.just(ResponseEntity.ok("Data deleted successfully")));
    }
}
```

**Explicación del Controlador:**
- **`saveData`:** Devuelve un `Mono<ResponseEntity<String>>`. Usa un operador ternario para decidir si aplica TTL o no, y luego transforma el resultado en una respuesta HTTP con `.then()`.
- **`getData`:** Devuelve un `Mono<ResponseEntity<Object>>`. Si se encuentra un valor, lo envuelve en un `ResponseEntity.ok()`, y si no, devuelve un `404 Not Found` con `defaultIfEmpty`.
- **`deleteData`:** Devuelve un `Mono<ResponseEntity<String>>`. Transforma el resultado de la eliminación en una respuesta exitosa.

---

### 3. Ejemplo de Uso
El uso sigue siendo el mismo que en el caso síncrono, pero ahora las respuestas se manejan de forma no bloqueante:
- **Guardar datos:**  
  `POST /api/redis/data?key=user1`  
  Body: `{"name": "John", "age": 30}`  
  Respuesta: `"Data saved successfully"`

- **Recuperar datos:**  
  `GET /api/redis/data?key=user1`  
  Respuesta: `{"name": "John", "age": 30}`

- **Eliminar datos:**  
  `DELETE /api/redis/data?key=user1`  
  Respuesta: `"Data deleted successfully"`

---

### 4. Mejoras Adicionales

1. **Validación Reactiva**  
   Puedes integrar validaciones reactivas usando `@Valid` con `Mono`:
   ```java
   @PostMapping("/data")
   public Mono<ResponseEntity<String>> saveData(
           @RequestParam @NotBlank String key,
           @Valid @RequestBody Mono<Object> value,
           @RequestParam(required = false, defaultValue = "-1") long ttl) {
       return value.flatMap(val -> {
           Mono<Void> saveOperation = (ttl > 0)
                   ? reactiveRedisDataService.saveData(key, val, ttl)
                   : reactiveRedisDataService.saveData(key, val);
           return saveOperation.then(Mono.just(ResponseEntity.ok("Data saved successfully")));
       });
   }
   ```

2. **Manejo de Errores Reactivo**  
   Usa un `@ControllerAdvice` reactivo para manejar excepciones como `RedisConnectionFailureException`:
   ```java
   @RestControllerAdvice
   public class ReactiveExceptionHandler {

       @ExceptionHandler(RedisConnectionFailureException.class)
       public Mono<ResponseEntity<String>> handleRedisConnectionFailure(RedisConnectionFailureException ex) {
           return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                   .body("Redis connection failed: " + ex.getMessage()));
       }
   }
   ```

3. **Soporte para Flux**  
   Si quisieras manejar múltiples valores (por ejemplo, una lista), podrías usar `Flux` con `opsForList()` en lugar de `opsForValue()`.

4. **Configuración en application.properties**  
   Asegúrate de que tu aplicación esté configurada para WebFlux en lugar de MVC. Si usas Spring Boot Starter WebFlux, esto se configura automáticamente:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-webflux</artifactId>
   </dependency>
   ```

---

### Conclusión
Con este enfoque, tienes un controlador y servicio completamente 
reactivos basados en `ReactiveRedisTemplate`. La principal diferencia 
con el enfoque síncrono es el uso de `Mono` para operaciones asíncronas, 
lo que mejora la escalabilidad en entornos de alta concurrencia. 
Si necesitas algo más específico, como manejar estructuras de datos avanzadas (listas, sets, etc.), ¡avísame!

.  

---

.  


# Listas en REDIS


Para manejar estructuras de datos como listas en Redis usando `ReactiveRedisTemplate` en una aplicación Spring WebFlux, puedes aprovechar las operaciones específicas para listas que ofrece Redis, como `LPUSH`, `RPUSH`, `LPOP`, `RPOP`, `LRANGE`, entre otras. Estas operaciones están disponibles a través del método `opsForList()` de `ReactiveRedisTemplate`.

A continuación, te muestro cómo implementar un servicio y un controlador RESTful reactivo para trabajar con listas en Redis:

---

### 1. Servicio con Operaciones para Listas

Modificamos el servicio para incluir métodos que manejen listas:

```java
@Service
public class ReactiveRedisListService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public ReactiveRedisListService(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    // Agregar un elemento al inicio de la lista (LPUSH)
    public Mono<Long> addToListLeft(String key, Object value) {
        return reactiveRedisTemplate.opsForList()
                .leftPush(key, value);
    }

    // Agregar un elemento al final de la lista (RPUSH)
    public Mono<Long> addToListRight(String key, Object value) {
        return reactiveRedisTemplate.opsForList()
                .rightPush(key, value);
    }

    // Obtener todos los elementos de la lista (LRANGE)
    public Flux<Object> getList(String key) {
        return reactiveRedisTemplate.opsForList()
                .range(key, 0, -1); // Desde el inicio (0) hasta el final (-1)
    }

    // Eliminar y obtener el primer elemento de la lista (LPOP)
    public Mono<Object> removeFromListLeft(String key) {
        return reactiveRedisTemplate.opsForList()
                .leftPop(key);
    }

    // Eliminar y obtener el último elemento de la lista (RPOP)
    public Mono<Object> removeFromListRight(String key) {
        return reactiveRedisTemplate.opsForList()
                .rightPop(key);
    }

    // Obtener el tamaño de la lista (LLEN)
    public Mono<Long> getListSize(String key) {
        return reactiveRedisTemplate.opsForList()
                .size(key);
    }

    // Eliminar la lista completa
    public Mono<Void> deleteList(String key) {
        return reactiveRedisTemplate.delete(key).then();
    }
}
```

**Notas:**
- **`Mono<Long>`**: Métodos como `leftPush` y `rightPush` devuelven la nueva longitud de la lista.
- **`Flux<Object>`**: `range` devuelve un flujo reactivo de elementos, ideal para listas.
- **`Mono<Object>`**: `leftPop` y `rightPop` devuelven un solo elemento o un `Mono.empty()` si la lista está vacía.

---

### 2. Controlador RESTful para Listas

Ahora, implementamos un controlador que expone endpoints para interactuar con listas:

```java
@RestController
@RequestMapping("/api/redis/lists")
public class ReactiveRedisListController {

    private final ReactiveRedisListService reactiveRedisListService;

    public ReactiveRedisListController(ReactiveRedisListService reactiveRedisListService) {
        this.reactiveRedisListService = reactiveRedisListService;
    }

    // Agregar un elemento al inicio de la lista
    @PostMapping("/{key}/left")
    public Mono<ResponseEntity<String>> addToListLeft(
            @PathVariable String key,
            @RequestBody Object value) {
        return reactiveRedisListService.addToListLeft(key, value)
                .map(length -> ResponseEntity.ok("Element added to left, new size: " + length))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add element"));
    }

    // Agregar un elemento al final de la lista
    @PostMapping("/{key}/right")
    public Mono<ResponseEntity<String>> addToListRight(
            @PathVariable String key,
            @RequestBody Object value) {
        return reactiveRedisListService.addToListRight(key, value)
                .map(length -> ResponseEntity.ok("Element added to right, new size: " + length))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add element"));
    }

    // Obtener todos los elementos de la lista
    @GetMapping("/{key}")
    public Mono<ResponseEntity<Flux<Object>>> getList(@PathVariable String key) {
        Flux<Object> listFlux = reactiveRedisListService.getList(key);
        return Mono.just(ResponseEntity.ok(listFlux));
    }

    // Eliminar y obtener el primer elemento de la lista
    @DeleteMapping("/{key}/left")
    public Mono<ResponseEntity<Object>> removeFromListLeft(@PathVariable String key) {
        return reactiveRedisListService.removeFromListLeft(key)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Eliminar y obtener el último elemento de la lista
    @DeleteMapping("/{key}/right")
    public Mono<ResponseEntity<Object>> removeFromListRight(@PathVariable String key) {
        return reactiveRedisListService.removeFromListRight(key)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Obtener el tamaño de la lista
    @GetMapping("/{key}/size")
    public Mono<ResponseEntity<Long>> getListSize(@PathVariable String key) {
        return reactiveRedisListService.getListSize(key)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(0L)); // Si no existe, tamaño 0
    }

    // Eliminar la lista completa
    @DeleteMapping("/{key}")
    public Mono<ResponseEntity<String>> deleteList(@PathVariable String key) {
        return reactiveRedisListService.deleteList(key)
                .then(Mono.just(ResponseEntity.ok("List deleted successfully")));
    }
}
```

**Explicación del Controlador:**
- **`addToListLeft` y `addToListRight`:** Agregan elementos y devuelven la nueva longitud de la lista.
- **`getList`:** Devuelve un `Flux<Object>` envuelto en un `ResponseEntity`, lo que permite transmitir los elementos de la lista de forma reactiva.
- **`removeFromListLeft` y `removeFromListRight`:** Eliminan y devuelven el elemento removido, o un `404` si la lista está vacía.
- **`getListSize`:** Devuelve el tamaño de la lista.
- **`deleteList`:** Elimina la lista completamente.

---

### 3. Ejemplo de Uso

Supongamos que usamos la clave `myList` para almacenar una lista:

- **Agregar al inicio:**  
  `POST /api/redis/lists/myList/left`  
  Body: `{"name": "John"}`  
  Respuesta: `"Element added to left, new size: 1"`

- **Agregar al final:**  
  `POST /api/redis/lists/myList/right`  
  Body: `{"name": "Jane"}`  
  Respuesta: `"Element added to right, new size: 2"`

- **Obtener la lista completa:**  
  `GET /api/redis/lists/myList`  
  Respuesta (stream): `[{"name": "John"}, {"name": "Jane"}]`

- **Eliminar del inicio:**  
  `DELETE /api/redis/lists/myList/left`  
  Respuesta: `{"name": "John"}`

- **Obtener tamaño:**  
  `GET /api/redis/lists/myList/size`  
  Respuesta: `1`

- **Eliminar la lista:**  
  `DELETE /api/redis/lists/myList`  
  Respuesta: `"List deleted successfully"`

---

### 4. Mejoras Adicionales

1. **Soporte para TTL en Listas**  
   Puedes establecer un tiempo de vida para la lista completa usando `expire`:
   ```java
   public Mono<Void> setListExpiration(String key, long ttlSeconds) {
       return reactiveRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds)).then();
   }
   ```

   Y en el controlador:
   ```java
   @PostMapping("/{key}/left")
   public Mono<ResponseEntity<String>> addToListLeft(
           @PathVariable String key,
           @RequestBody Object value,
           @RequestParam(required = false, defaultValue = "-1") long ttl) {
       return reactiveRedisListService.addToListLeft(key, value)
               .flatMap(length -> {
                   if (ttl > 0) {
                       return reactiveRedisListService.setListExpiration(key, ttl)
                               .then(Mono.just(length));
                   }
                   return Mono.just(length);
               })
               .map(length -> ResponseEntity.ok("Element added to left, new size: " + length));
   }
   ```

2. **Validación de Elementos**  
   Usa `@Valid` para validar el cuerpo de la solicitud si esperas objetos específicos.

3. **Soporte para Rango Personalizado**  
   Modifica `getList` para aceptar parámetros `start` y `end`:
   ```java
   public Flux<Object> getList(String key, long start, long end) {
       return reactiveRedisTemplate.opsForList().range(key, start, end);
   }
   ```

   Y en el controlador:
   ```java
   @GetMapping("/{key}")
   public Mono<ResponseEntity<Flux<Object>>> getList(
           @PathVariable String key,
           @RequestParam(defaultValue = "0") long start,
           @RequestParam(defaultValue = "-1") long end) {
       Flux<Object> listFlux = reactiveRedisListService.getList(key, start, end);
       return Mono.just(ResponseEntity.ok(listFlux));
   }
   ```

4. **Serialización Personalizada**  
   Si las listas contienen objetos complejos, asegúrate de que el `Jackson2JsonRedisSerializer` esté configurado correctamente en tu `ReactiveRedisTemplate` (como ya lo hiciste en tu configuración inicial).

---

### Conclusión
Con este enfoque, puedes manejar listas en Redis de forma reactiva, aprovechando las capacidades de `ReactiveRedisTemplate` y Spring WebFlux. Si necesitas soporte para otras estructuras como sets, hashes o colas, puedo ayudarte a extenderlo. ¿Hay algo más que quieras explorar?