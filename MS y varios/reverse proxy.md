# **Cómo aplicar el concepto de** *reverse proxy* **en un microservicio Spring Boot**


Usando **Spring Cloud Gateway** — que es, en esencia, un *reverse proxy inteligente* diseñado específicamente para arquitecturas de microservicios.

<br>


**index:**

* Qué es Spring Cloud Gateway
* Arquitectura
* Maven y configuración
* filtros (autenticación, caché, etc
* Beneficios

<br>


## 1. Qué es Spring Cloud Gateway

**Spring Cloud Gateway** es el *reverse proxy* oficial de Spring.  
Actúa como un **API Gateway** que se coloca delante de tus microservicios para:

* Recibir todas las solicitudes externas.
* Redirigirlas al microservicio correspondiente.
* Aplicar filtros (autenticación, logging, rate limit, caché, etc.).
* Unificar los endpoints bajo una sola URL pública.

> Es la alternativa moderna a **Netflix Zuul**, totalmente reactiva y optimizada para **Spring Boot + WebFlux**.


<br>
<br>



## ⚙️ 2. Estructura típica de arquitectura


```
             🌐 Clientes (Web / Mobile / API)
                          │
                          ▼
                 🚪 Spring Cloud Gateway
                          │
 ┌────────────────────────┼─────────────────────────┐
 │                        │                         │
 ▼                        ▼                         ▼
🧩 Servicio Usuarios   💰 Servicio Pagos      📊 Servicio Reportes
 (puerto 8081)          (puerto 8082)          (puerto 8083)
```

El Gateway:

* recibe `/api/usuarios/**` → redirige a `localhost:8081`
* recibe `/api/pagos/**` → redirige a `localhost:8082`
* recibe `/api/reportes/**` → redirige a `localhost:8083`

<br>
<br>

##  3. Dependencia Maven

En tu `pom.xml` del Gateway:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

Y el *Spring Cloud BOM* en `dependencyManagement`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2024.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

<br>
<br>


## ⚙️ 4. Configuración del *reverse proxying* (YAML)

Archivo `application.yml` del Gateway:

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: usuarios-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/usuarios/**
          filters:
            - StripPrefix=1

        - id: pagos-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/pagos/**
          filters:
            - StripPrefix=1

        - id: reportes-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/reportes/**
          filters:
            - StripPrefix=1
```

### Qué hace:

* El Gateway escucha en **puerto 8080**.
* Si llega una petición `GET /api/pagos/123`, la **redirige** al microservicio de pagos (`http://localhost:8082/pagos/123`).
* Así, el cliente *nunca accede directamente* a los backends.

 Esto **es exactamente reverse proxying** dentro del ecosistema Spring.


<br>
<br>

## 🔒 5. Agregando filtros (autenticación, caché, etc.)

Spring Cloud Gateway permite filtros globales y específicos para cada ruta.

Por ejemplo, un filtro para **añadir un header**:

```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder
        .routes()
        .route("usuarios", r -> r.path("/api/usuarios/**")
        .filters(f -> f.addRequestHeader("X-Gateway", "SpringCloudGateway"))
        .uri("http://localhost:8081"))
        .build();
}
```

También puedes aplicar:

* `RequestRateLimiter` → limitar tráfico.
* `Retry` → reintentos automáticos.
* `CircuitBreaker` → tolerancia a fallos.
* `Cache` (usando Redis, por ejemplo).

---

## 6. Ejemplo de caché con Redis (reverse proxy acelerado)

Para hacer **“accelerated reverse proxying”**, agregás una capa de caché.

**Dependencia Maven:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

#### Filtro de caché simple personalizado:**



**Contexto**

`ResponseCacheFilter` implementa `GlobalFilter`,
lo que significa que intercepta **todas las peticiones** que pasan por el Gateway —
antes y después de que éstas lleguen al microservicio destino.

En este caso, usa un **Redis reactivo (`ReactiveStringRedisTemplate`)**
para guardar las respuestas ya procesadas,
de modo que peticiones repetidas se sirvan directamente desde caché (sin tocar el backend).


#### Método completo: `filter(...)`

```java
@Component
public class ResponseCacheFilter implements GlobalFilter {

    private final ReactiveStringRedisTemplate redisTemplate;

    public ResponseCacheFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = exchange.getRequest().getURI().getPath();

        return redisTemplate.opsForValue().get(key)
            .flatMap(cached -> {
                // Respuesta en caché
                exchange.getResponse().getHeaders().add("X-Cache", "HIT");
                byte[] bytes = cached.getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            })
            .switchIfEmpty(chain.filter(exchange)
                .then(Mono.defer(() -> {
                    // Guardar respuesta en caché si no estaba
                    exchange.getResponse().getBody()
                        .subscribe(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            redisTemplate.opsForValue()
                                .set(key, new String(bytes, StandardCharsets.UTF_8))
                                .subscribe();
                        });
                    return Mono.empty();
                })));
    }
}
```
#### Paso a paso explicado

##### 1. Obtener una clave de caché

```java
String key = exchange.getRequest().getURI().getPath();
```

* Se usa el **path del request** como clave (`/api/pagos/123`).
* En la práctica, podrías hacerla más específica: incluir query params o headers relevantes.
  Ejemplo:

  ```java
  String key = exchange.getRequest().getURI().toString();
  ```

##### 2. Buscar en Redis si hay respuesta cacheada

```java
return redisTemplate.opsForValue().get(key)
```

* `opsForValue().get(key)` devuelve un `Mono<String>` con la respuesta guardada (si existe).
* Si existe, entramos en el `.flatMap(cached -> { ... })`.



##### 3. Caso: existe en caché (`X-Cache: HIT`)

```java
.flatMap(cached -> {
    exchange.getResponse().getHeaders().add("X-Cache", "HIT");
    byte[] bytes = cached.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
})
```

Aquí:

* Se agrega un header `X-Cache: HIT` para marcar que la respuesta vino del caché.
* Se transforma el `String` (cuerpo en texto) en bytes y se envuelve en un `DataBuffer`.
* Se escribe el cuerpo directamente en la respuesta (`exchange.getResponse().writeWith(...)`).

👉 Resultado:
El Gateway **no llama al microservicio backend**,
devuelve la respuesta directamente desde Redis = **respuesta acelerada** ⚡.







<br>


#### Así el Gateway

* responde desde caché si la ruta ya fue consultada,
* y acelera drásticamente la entrega de contenido repetido.

<br>
<br>

## 7. Beneficios en microservicios

| Beneficio                      | Explicación                                                   |
| ------------------------------ | ------------------------------------------------------------- |
| **Desacoplamiento**            | Los clientes nunca acceden directamente a los microservicios. |
| **Seguridad**                  | Solo el Gateway expone endpoints públicos.                    |
| **Escalabilidad**              | Se balancea tráfico entre instancias.                         |
| **Optimización**               | Caching, compresión, logging, métricas centralizadas.         |
| **Facilidad de mantenimiento** | Cambiar o agregar servicios sin afectar clientes.             |


<br>
<br>

##  8. Ejemplo de prueba real

```
Cliente → GET https://api.misitio.com/api/pagos/345

Spring Cloud Gateway (8080)
  ↳ redirige internamente a → http://localhost:8082/pagos/345
  ↳ añade cabeceras, valida token JWT, usa caché Redis
  ↳ responde al cliente más rápido (sin exponer backend)
```

<br>
<br>

## ✅ En resumen

| Concepto                 | Explicación                                                            |
| ------------------------ | ---------------------------------------------------------------------- |
| **Reverse proxying**     | Un servidor intermedio recibe las peticiones y las reenvía al backend. |
| **Spring Cloud Gateway** | Implementación moderna de reverse proxy en Spring Boot.                |
| **Accelerated (caché)**  | Añadir Redis u otro mecanismo para servir respuestas más rápido.       |
| **Resultado**            | Arquitectura más rápida, segura y escalable.                           |

<br>
<br>

<br>

<br>






# Cómo agregar autenticación JWT a Spring Cloud Gateway


de modo que el Gateway actúe como reverse proxy + filtro de seguridad centralizado.

<br>
<br>


## 1. Contexto

En una arquitectura de microservicios, cada backend no debería validar tokens por separado si no es necesario.

👉 En su lugar, el **Gateway** (que ya actúa como *reverse proxy*) puede:

* interceptar cada petición entrante,
* validar el **token JWT**,
* y **solo reenviar** la solicitud al backend si el token es válido.

De esta forma:

* Los microservicios internos permanecen limpios (sin lógica de autenticación).
* El control de acceso se centraliza.
* Se reducen errores y se mejora el rendimiento.

<br>
<br>

## 2. Dependencias Maven

En tu `pom.xml` del Gateway:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

<br>
<br>

## 3. Configuración del Gateway (application.yml)

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: usuarios-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/usuarios/**
          filters:
            - StripPrefix=1
            - name: JwtAuthFilter  # Filtro de validación JWT
```

<br>
<br>

## 4. Filtro de autenticación JWT

Creamos una clase **GlobalFilter** o **GatewayFilterFactory** para validar tokens.

(Usaremos un filtro global para simplificar el ejemplo.)

```java
package com.example.gateway.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.cloud.gateway.filter.factory.rewrite.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getBytes())
                .build()
                .parseClaimsJws(token); // <-- si no lanza excepción, el token es válido

            // Continuar con la cadena de filtros
            return chain.filter(exchange);

        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
```

<br>
<br>


## 5. Propiedades en `application.yml`

```yaml
jwt:
  secret: my-super-secret-key-123456
```

>  En entornos reales, este valor debe almacenarse como **variable de entorno o en Vault**, no en texto plano.

<br>
<br>

## 6. Flujo completo

```
Cliente
   │
   ├─► Solicitud: GET /api/usuarios (con Authorization: Bearer <JWT>)
   │
   ▼
Spring Cloud Gateway
   ├─► Intercepta con JwtAuthFilter
   │      ├─ Valida firma y expiración del token
   │      └─ Extrae claims si es válido
   │
   ├─► (opcional) Agrega cabeceras con claims (ej. roles, userId)
   │
   ▼
Microservicio Usuarios (Spring Boot)
   └─► Recibe solicitud ya autenticada
```

<br>
<br>


## 💡 7. Enriqueciendo la solicitud con claims

Podés agregar los datos del token a la cabecera antes de reenviar:

```java
Claims claims = Jwts.parserBuilder()
    .setSigningKey(jwtSecret.getBytes())
    .build()
    .parseClaimsJws(token)
    .getBody();

ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
    .header("X-User-Id", claims.getSubject())
    .header("X-User-Role", (String) claims.get("role"))
    .build();

return chain.filter(exchange.mutate().request(mutatedRequest).build());
```

Ahora los microservicios pueden leer:

```java
String userId = request.getHeader("X-User-Id");
String role = request.getHeader("X-User-Role");
```

…sin tener que parsear el JWT ellos mismos.

<br>
<br>


## ⚙️ 8. Ejemplo de JWT de prueba

Podés generar uno rápido con [jwt.io](https://jwt.io/) o en Java:

```java
String jwt = Jwts.builder()
    .setSubject("user123")
    .claim("role", "ADMIN")
    .setIssuedAt(new Date())
    .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(2))))
    .signWith(Keys.hmacShaKeyFor("my-super-secret-key-123456".getBytes()))
    .compact();
```

<br>
<br>

## 9. Resultado final

* 🔁 **Reverse proxying**: el Gateway reenvía solicitudes a los microservicios.
* ⚡ **Accelerated**: puede incorporar caché (Redis, Caffeine, etc.).
* 🔒 **Autenticado**: filtra solicitudes con JWT antes de pasarlas al backend.

> El cliente externo **nunca se conecta directamente** a los microservicios.
> Solo el Gateway lo hace, después de validar que el token sea correcto.

<br>
<br>


<br>
<br>

# Extención para Gateway y uso de OAuth2 
**(como Keycloak o Auth0)**


Cómo extender esto para que el **Gateway use un servidor OAuth2 (como Keycloak o Auth0)** en lugar de validar los tokens manualmente con la clave secreta?

Así delegar la autenticación a un proveedor externo y mantener la verificación automática de JWT en el Gateway.



Idea:

Integrar **Spring Cloud Gateway + OAuth2 / JWT** usando un **servidor de identidad externo** (por ejemplo **Keycloak**, **Auth0**, o **Azure AD**).

Así el *reverse proxy* no valida tokens manualmente:  
➡️ delega la autenticación al **Authorization Server**,  
➡️ y valida automáticamente los **JWT** firmados por ese servidor.

<br>
<br>

## 1. Contexto general

En este enfoque:

```
Cliente (Frontend)
   │
   │---> [Keycloak / Auth0 / OAuth2 Provider]
   │         (autenticación y emisión de token JWT)
   │
   ▼
Spring Cloud Gateway  ← Reverse proxy + filtro OAuth2
   │
   ├─ valida JWT firmado por el Authorization Server
   │
   ▼
Microservicios internos (Spring Boot)
```


##  2. Dependencias Maven del Gateway

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

> 🔸 `spring-boot-starter-oauth2-resource-server` hace que el Gateway **actúe como Resource Server**, es decir, valida los tokens emitidos por un servidor OAuth2.


<br>
<br>

##  3. Configuración YAML para Keycloak / Auth0

Archivo `application.yml` del **Gateway**:

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      routes:
        - id: usuarios-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/usuarios/**
          filters:
            - StripPrefix=1

        - id: pagos-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/pagos/**
          filters:
            - StripPrefix=1

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.misitio.com/realms/mi-reino
          jwk-set-uri: https://auth.misitio.com/realms/mi-reino/protocol/openid-connect/certs
```

### Explicación

* `issuer-uri`: URL base del *realm* o dominio del servidor OAuth2 (Keycloak, Auth0, etc.).
* `jwk-set-uri`: endpoint donde el Gateway obtiene las claves públicas para validar la firma del JWT.

✅ No hace falta manejar manualmente el `jwt.secret` —
Spring Security descarga y cachea las claves automáticamente.


<br>
<br>

## 🔐 4. Configuración de seguridad

Creamos una clase `SecurityConfig` en el Gateway:

```java
package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/public/**").permitAll()  // endpoints públicos
                .anyExchange().authenticated()           // todo lo demás requiere JWT
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt()); // <-- validación JWT automática

        return http.build();
    }
}
```

👉 Esto convierte el Gateway en un **Resource Server** OAuth2:

* intercepta todas las solicitudes,
* valida el JWT contra el servidor OAuth2,
* y si es válido, las deja pasar a los microservicios.

<br>
<br>


##  5. Flujo completo con Keycloak / Auth0

```
(1) El cliente (React, Angular, móvil)
     └─→ /authorize → Keycloak
          (login + consentimiento)

(2) Keycloak responde con un JWT firmado (Access Token)

(3) El cliente hace:
     GET /api/pagos/123
     Authorization: Bearer <JWT>

(4) Spring Cloud Gateway:
     - Extrae el token
     - Verifica su firma con el JWK del servidor OAuth2
     - Comprueba expiración y audiencia
     - Si es válido → reenvía la solicitud

(5) El microservicio recibe la solicitud autenticada
```

<br>
<br>


## 6. Cómo el Gateway pasa información a los microservicios

Podés agregar un **filtro personalizado** para transferir claims relevantes (por ejemplo el `sub`, `email`, `roles`) al encabezado HTTP antes de reenviar:

```java
@Component
public class JwtHeaderEnricher implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .flatMap(principal -> {
                if (principal instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();
                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("X-User-Id", jwt.getSubject())
                        .header("X-User-Email", jwt.getClaimAsString("email"))
                        .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                }
                return chain.filter(exchange);
            });
    }
}
```

Ahora, tus microservicios pueden leer estos encabezados sin tener que validar el JWT directamente.

<br>
<br>


## 7. Ejemplo con Keycloak local

Si usás **Keycloak** en local:

```
issuer-uri: http://localhost:8080/realms/mi-reino
jwk-set-uri: http://localhost:8080/realms/mi-reino/protocol/openid-connect/certs
```

Y desde el frontend obtenés un token con:

```
POST /realms/mi-reino/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
grant_type=password&client_id=frontend-app&username=juan&password=1234
```

El token JWT devuelto se envía al Gateway en cada request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6...
```

<br>
<br>



## 8. Resultado

| Función                    | Gateway                                                    |
| -------------------------- | ---------------------------------------------------------- |
| **Reverse proxy**          | Reenvía las solicitudes al microservicio adecuado.         |
| **OAuth2 Resource Server** | Valida tokens firmados por Keycloak/Auth0.                 |
| **Filtro centralizado**    | Aplica autenticación antes de llegar a los microservicios. |
| **Claims enrichment**      | Inserta información útil del usuario en headers.           |

<br>
<br>

## 9. Beneficios del enfoque OAuth2 + JWT en el Gateway

* 🔒 Seguridad centralizada (los microservicios no manejan tokens).
* ⚡ Rendimiento alto: validación local del JWT sin pedir al servidor OAuth cada vez.
* 🧱 Escalabilidad: el Gateway puede crecer horizontalmente sin estado.
* 🔄 Integración estándar con cualquier Identity Provider (Keycloak, Auth0, Okta, Azure AD).
* 🧠 Simplifica los backends: solo exponen lógica de negocio, no seguridad.

<br>
<br>






<br>
<br>

<br>
<br>

<br>



# Proteger rutas específicas según roles o claims del JWT


<br>



Ya tenemos autenticación JWT configurada, así que ahora le agregamos **reglas por rol o permiso** directamente en el *reverse proxy* (Spring Cloud Gateway + Spring Security).

Por ejemplo, solo los usuarios con rol `"ADMIN"` pueden acceder a `/api/reportes/**`.

Esa es la extensión natural de este esquema de seguridad.

<br>

## 1. Concepto: Autorización en el Gateway

Spring Cloud Gateway puede inspeccionar el **JWT decodificado** y aplicar reglas del tipo:

* `ROLE_ADMIN` → puede acceder a `/api/reportes/**`
* `ROLE_USER` → puede acceder a `/api/usuarios/**`
* etc.

Esto se hace en la configuración de seguridad (`SecurityWebFilterChain`) usando expresiones como:

```java
.hasRole("ADMIN")
.hasAnyRole("ADMIN", "SUPERVISOR")
.hasAuthority("SCOPE_read")
```

<br>
<br>

## 2. Ejemplo completo

Archivo `SecurityConfig.java`:


```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

```


```java
package com.example.gateway.config;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos
                .pathMatchers("/public/**").permitAll()

                // Solo ADMIN puede acceder a reportes
                .pathMatchers("/api/reportes/**").hasRole("ADMIN")

                // Solo USER o ADMIN puede acceder a usuarios
                .pathMatchers("/api/usuarios/**").hasAnyRole("USER", "ADMIN")

                // Cualquier otro endpoint requiere autenticación
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }
}
```

> 🔐 Spring automáticamente extrae los `roles` o `authorities` desde el claim del token JWT (por ejemplo `roles`, `scope`, o `realm_access` en Keycloak).

<br>
<br>

## 3. Cómo mapea Spring los roles desde el JWT

Dependiendo del proveedor OAuth2, los roles pueden venir en distintos lugares del token.
Ejemplo típico de **Keycloak**:

```json
{
  "preferred_username": "juan",
  "realm_access": {
    "roles": ["USER", "ADMIN"]
  }
}
```

Por defecto, Spring no sabe que `"realm_access.roles"` contiene roles.  
Necesitamos decirle cómo mapear eso:


<br>
<br>

## 4. Configuración de `JwtGrantedAuthoritiesConverter`

Creamos una clase para personalizar la extracción de roles desde el JWT:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;
```

```java
package com.example.gateway.config;

@Configuration
public class JwtConverterConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extraemos roles desde realm_access.roles
            var realmRoles = ((Collection<String>) jwt.getClaimAsMap("realm_access").get("roles"))
                    .stream()
                    .map(role -> "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return realmRoles;
        });

        return converter;
    }
}
```

Y lo conectamos en `SecurityConfig`:

```java
.oauth2ResourceServer(oauth2 -> 
    oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
)
```

---

## 🧱 5. Flujo de autorización en tiempo real

1. El cliente incluye su `Bearer <token>` en el header.
2. El Gateway:

   * valida la firma y expiración del token,
   * extrae los claims,
   * convierte roles → autoridades (`ROLE_ADMIN`, etc.).
3. Spring Security aplica las reglas:

   * si coincide, reenvía la request al microservicio;
   * si no, responde `403 Forbidden`.

---

## ✅ 6. Resultado

| Ruta               | Regla                               | Resultado      |
| ------------------ | ----------------------------------- | -------------- |
| `/public/**`       | libre                               | ✅ accesible    |
| `/api/reportes/**` | requiere `ROLE_ADMIN`               | 🔒 sólo admins |
| `/api/usuarios/**` | requiere `ROLE_USER` o `ROLE_ADMIN` | 🔒             |
| demás              | requiere token válido               | 🔒             |

---

## 💬 7. Tips avanzados

* Si usás **Auth0** o **Azure AD**, los roles suelen venir en `claims` diferentes (`permissions`, `scp`, `roles`).
  Podés adaptar el `JwtGrantedAuthoritiesConverter` al nombre correcto.

* Si querés combinar roles y scopes:

  ```java
  converter.setJwtGrantedAuthoritiesConverter(new JwtGrantedAuthoritiesConverter());
  ```

  y activar `setAuthorityPrefix("ROLE_")` para mantener compatibilidad con Spring.

* Podés también enrutar dinámicamente según el rol:

  ```yaml
  routes:
    - id: admin-route
      uri: http://localhost:8083
      predicates:
        - Path=/admin/**
        - Header=X-Role, ADMIN
  ```

---

¿Querés que te muestre ahora **cómo propagar el contexto de seguridad** (claims o roles) del Gateway al microservicio interno usando *headers firmados o cifrados*, para que éste no tenga que validar JWT?
Eso suele hacerse en entornos con alta carga o muchos microservicios.
