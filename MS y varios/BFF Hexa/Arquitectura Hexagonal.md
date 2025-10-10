# Arquitectura Hexagonal

La **arquitectura hexagonal**, también conocida como **puertos y adaptadores**, es un enfoque arquitectónico propuesto por Alistair Cockburn. Su objetivo principal es crear sistemas más flexibles, independientes de frameworks y tecnologías externas, al mismo tiempo que facilita pruebas y mantenibilidad. 

introducción enfocada en cómo implementarla usando **Spring Boot**.

índice:
- ¿Qué es la arquitectura hexagonal?
- Implementación en Spring Boot
- refs


---

## ¿Qué es la arquitectura hexagonal?

La arquitectura hexagonal divide una aplicación en tres áreas principales:

1. **Core de negocio (Dominio)**:
   - Contiene toda la lógica central de la aplicación.
   - Es completamente independiente de frameworks, bases de datos, o cualquier tecnología externa.
   - Define interfaces para interactuar con el mundo exterior (puertos).

2. **Puertos**:
   - Son las interfaces que definen cómo el dominio interactúa con el exterior.
   - Pueden ser **entradas** (controladores, APIs) o **salidas** (repositorios, integraciones externas).

3. **Adaptadores**:
   - Son implementaciones concretas de los puertos.
   - Incluyen elementos como controladores REST (entrada) o repositorios que interactúan con bases de datos (salida).

El objetivo es que el **core del negocio** no tenga dependencia de adaptadores concretos ni de detalles técnicos.

---

## Implementación en Spring Boot

### 1. **Estructura del proyecto**

Puedes organizar tu proyecto en paquetes de la siguiente forma:

```
com.example.app
│
├── application        // Casos de uso o servicios de aplicación
├── domain             // Modelo de dominio y lógica central
│   ├── model          // Entidades y objetos de valor
│   └── ports          // Interfaces (puertos)
├── infrastructure     // Adaptadores
│   ├── input          // Entradas (REST, eventos, CLI, etc.)
│   └── output         // Salidas (bases de datos, APIs externas)
└── configuration      // Configuración de Spring Boot
```

---

### 2. **Dominio (Core)**

Aquí defines el modelo de negocio y los puertos.

#### Ejemplo de un puerto:

```java
package com.example.app.domain.ports;

import com.example.app.domain.model.Order;

public interface OrderRepository {
    void save(Order order);
    Order findById(Long id);
}
```

#### Ejemplo de una entidad del dominio:

```java
package com.example.app.domain.model;

import java.util.Objects;

public class Order {
    private Long id;
    private String customerName;

    public Order(Long id, String customerName) {
        this.id = id;
        this.customerName = customerName;
    }

    // Getters, setters, equals, hashCode y toString
}
```

---

### 3. **Aplicación (Casos de uso)**

Aquí defines la lógica específica de los casos de uso, utilizando los puertos para interactuar con el dominio.

```java
package com.example.app.application;

import com.example.app.domain.model.Order;
import com.example.app.domain.ports.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void createOrder(Long id, String customerName) {
        Order order = new Order(id, customerName);
        orderRepository.save(order);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id);
    }
}
```

---

### 4. **Infraestructura (Adaptadores)**

#### Adaptador de entrada (REST Controller):

```java
package com.example.app.infrastructure.input;

import com.example.app.application.OrderService;
import com.example.app.domain.model.Order;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public void createOrder(@RequestParam Long id, @RequestParam String customerName) {
        orderService.createOrder(id, customerName);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}
```

#### Adaptador de salida (Repositorio para DB):

```java
package com.example.app.infrastructure.output;

import com.example.app.domain.model.Order;
import com.example.app.domain.ports.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> database = new HashMap<>();

    @Override
    public void save(Order order) {
        database.put(order.getId(), order);
    }

    @Override
    public Order findById(Long id) {
        return database.get(id);
    }
}
```

---

### 5. **Configuración**

Puedes configurar los puertos y adaptadores utilizando inyección de dependencias de Spring.

```java
package com.example.app.configuration;

import com.example.app.domain.ports.OrderRepository;
import com.example.app.infrastructure.output.InMemoryOrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }
}
```

---

### Beneficios de la arquitectura hexagonal en Spring Boot

1. **Independencia tecnológica**:
   - El dominio no depende de Spring ni de frameworks externos. Puedes probarlo sin necesidad de un contenedor de Spring.

2. **Pruebas más sencillas**:
   - Como el dominio es independiente, puedes probarlo de manera aislada con mocks o stubs.

3. **Flexibilidad y mantenibilidad**:
   - Cambiar tecnologías (como el motor de base de datos o un framework) no afecta el dominio.

4. **Separación de preocupaciones**:
   - Las responsabilidades están claramente definidas, lo que hace que el código sea más comprensible.

---

### Pruebas

1. **Pruebas de unidad**:
   - Prueba el core del dominio (entidades y servicios) de forma aislada.
   
2. **Pruebas de integración**:
   - Prueba adaptadores concretos, como repositorios o controladores REST.

3. **Pruebas de aceptación**:
   - Valida el sistema completo con herramientas como Postman o frameworks como Cucumber.

---

#Refs

Finally, some useful links about Hexagonal Architecture:

https://hexagonalarchitecture.org/

https://alistaircockburn.com/Articles

https://jmgarridopaz.github.io/content/articles.html

https://www.youtube.com/playlist?list=PL1msPBH9ZGkhpANkreFA_teOnloVdLuCx