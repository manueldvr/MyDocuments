# **Redis**





Si ya estás usando un **repositorio `TransmisionUmbralRepository` que extiende `JpaRepository<TransmisionUmbral, Long>`**, puedes integrar Redis para **cachear** las consultas y evitar acceder repetidamente a la base de datos.  

---

### 🔹 **Opciones para Integrar Redis con `JpaRepository`**
####  Opción 1: Cachear las respuestas de los métodos del repositorio  
####  Opción 2: Usar `RedisTemplate` para almacenar y recuperar los datos manualmente  
####  Opción 3: Implementar un servicio híbrido que combine Redis con JPA  

A continuación, te explico cada una con ejemplos.

---

## ✅ **Opción 1: Cachear el resultado de métodos en el repositorio**
Spring permite cachear el resultado de métodos de repositorios con `@Cacheable`.  

### 1️⃣ **Habilitar caché en la aplicación**
Si aún no lo hiciste, añade la anotación `@EnableCaching`:

```java
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
}
```

### 2️⃣ **Cachear métodos en el repositorio**
Modifica tu repositorio `TransmisionUmbralRepository`:

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransmisionUmbralRepository extends JpaRepository<TransmisionUmbral, Long> {

    @Cacheable(value = "transmisionUmbral", key = "#id")
    TransmisionUmbral findById(long id);
}
```

👉 **Explicación:**  
- La primera vez que se llama `findById(id)`, la consulta se ejecutará en la base de datos y el resultado se guardará en **Redis**.  
- Las siguientes veces, el dato se obtendrá directamente desde Redis sin tocar la BD.  

Si quieres invalidar la caché cuando un registro cambie, agrega:

```java
@CacheEvict(value = "transmisionUmbral", key = "#id")
void deleteById(Long id);
```

Esto eliminará el registro cacheado cuando sea eliminado de la base de datos.

---

## ✅ **Opción 2: Usar `RedisTemplate` Manualmente**
Si necesitas más control, puedes usar `RedisTemplate` para almacenar los datos después de obtenerlos del repositorio.

### 1️⃣ **Configurar `RedisTemplate`**
Si aún no lo hiciste, crea esta configuración:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### 2️⃣ **Modificar el Servicio para cachear en Redis**
Crea un servicio que combine Redis y JPA:

```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TransmisionUmbralService {

    private final TransmisionUmbralRepository transmisionUmbralRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public TransmisionUmbralService(TransmisionUmbralRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.transmisionUmbralRepository = repository;
        this.redisTemplate = redisTemplate;
    }

    public TransmisionUmbral getById(Long id) {
        String key = "transmisionUmbral:" + id;

        // 1️⃣ Intentar obtener desde Redis
        TransmisionUmbral cacheado = (TransmisionUmbral) redisTemplate.opsForValue().get(key);
        if (cacheado != null) {
            System.out.println("🔴 Obtenido desde Redis");
            return cacheado;
        }

        // 2️⃣ Si no está en caché, buscar en la BD
        TransmisionUmbral entidad = transmisionUmbralRepository.findById(id).orElse(null);
        if (entidad != null) {
            System.out.println("🟢 Guardado en Redis");
            redisTemplate.opsForValue().set(key, entidad, 10, TimeUnit.MINUTES);
        }
        return entidad;
    }

    public void deleteById(Long id) {
        transmisionUmbralRepository.deleteById(id);
        redisTemplate.delete("transmisionUmbral:" + id);
    }
}
```

✅ **Ventajas**:  
- Permite configurar el **tiempo de vida (TTL)** de los datos en Redis.  
- Puedes guardar solo ciertos datos, evitando sobrecargar Redis.  
- Control total sobre qué almacenar y cuándo eliminarlo.  

---

## ✅ **Opción 3: Usar un Servicio Híbrido**
Si quieres una solución flexible, combina ambas estrategias:

```java
@Service
public class TransmisionUmbralService {

    private final TransmisionUmbralRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    public TransmisionUmbralService(TransmisionUmbralRepository repository, RedisTemplate<String, Object> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Cacheable(value = "transmisionUmbral", key = "#id")
    public TransmisionUmbral getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @CacheEvict(value = "transmisionUmbral", key = "#id")
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
```

---

## 🚀 **¿Cuál opción elegir?**
| Estrategia | Pros | Contras |
|------------|------|---------|
| **1️⃣ `@Cacheable` en el repositorio** | Fácil de implementar, automático | Menos control sobre TTL y eliminación manual |
| **2️⃣ `RedisTemplate` manual** | Control total sobre TTL y eliminación | Más código y manejo manual de caché |
| **3️⃣ Híbrido (`@Cacheable` + `RedisTemplate`)** | Equilibrio entre simplicidad y control | Puede ser más complejo |

Si solo necesitas **cachear consultas básicas**, usa `@Cacheable`.  
Si necesitas **control total (TTL, eliminación manual, formatos específicos, etc.)**, usa `RedisTemplate`.  

---

## 🎯 **Conclusión**
- Puedes integrar Redis con `JpaRepository` usando `@Cacheable` o `RedisTemplate`.  
- Redis ayuda a reducir el acceso a la BD y mejorar la performance.  
- La estrategia depende de cuánto control necesitas sobre los datos en caché.  

