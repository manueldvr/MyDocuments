
# Clean Code


### Contexto en Clean Code:

Estos principios SOLID, propuestos por Robert C. Martin, son fundamentales en Clean Code porque promueven un diseño de software que es:
- Mantenible: Fácil de modificar y extender.
- Escalable: Permite añadir funcionalidades con menos esfuerzo.
- Testeable: Facilita escribir pruebas unitarias al tener responsabilidades claras y dependencias bien gestionadas.
- Comprensible: Reduce la complejidad y hace el código más legible.

### Aplicación práctica:

Al seguir los principios SOLID, evitas problemas comunes como clases monolíticas, acoplamiento excesivo o jerarquías de herencia mal diseñadas.  

Por ejemplo:
- SRP te lleva a dividir una clase grande en varias más pequeñas y específicas.
- O/CP fomenta el uso de patrones como Strategy o Factory.
- LSP asegura que las jerarquías de clases sean coherentes.
- ISP evita interfaces infladas que complican el código.
- DIP promueve la inyección de dependencias, una práctica común en frameworks modernos.

En resumen, los principios SOLID trabajan juntos para crear un código limpio, modular y robusto, alineándose con el objetivo de Clean Code de producir software de alta calidad que sea fácil de entender y mantener. Si necesitas un ejemplo más detallado de alguno de estos principios.

1. **Single Responsibility Principle (SRP) - Principio de Responsabilidad Única**  
   - **Definición**: Una clase debe tener una sola razón para cambiar, es decir, debe tener una única responsabilidad o propósito.  
   - **Explicación**: Cada clase o módulo debe enfocarse en una sola funcionalidad. Esto reduce la complejidad y hace que el código sea más fácil de entender, mantener y probar.  
   - **Ejemplo**: Una clase `Factura` no debería manejar tanto la lógica de cálculo de impuestos como la impresión del recibo. En su lugar, crea una clase `CalculadoraDeImpuestos` y otra `ImpresoraDeFacturas`.  
   - **Beneficio**: Código más modular, menos acoplamiento y cambios más seguros.


2. **Open/Closed Principle (OCP) - Principio de Abierto/Cerrado**  
   - **Definición**: Las clases deben estar abiertas para extensión, pero cerradas para modificación.  
   - **Explicación**: Debes poder añadir nuevas funcionalidades (extender) sin modificar el código existente. Esto se logra usando abstracciones (interfaces, clases abstractas) y polimorfismo.  
   - **Ejemplo**: Si tienes un sistema que dibuja formas geométricas, en lugar de modificar una clase `Dibujador` cada vez que añades una nueva forma, define una interfaz `Forma` con un método `dibujar()` y crea clases como `Circulo` o `Cuadrado` que la implementen.  
   - **Beneficio**: Reduce el riesgo de introducir errores al modificar código existente.


3. **Interface Segregation Principle (ISP) - Principio de Segregación de Interfaces**  
   - **Definición**: Los clientes no deben verse obligados a depender de interfaces que no usan.  
   - **Explicación**: Las interfaces deben ser específicas y pequeñas, en lugar de grandes y genéricas, para que las clases solo implementen lo que necesitan.  
   - **Ejemplo**: Si una interfaz `Trabajador` tiene métodos como `trabajar()`, `comer()` y `dormir()`, una clase `Robot` que implemente `Trabajador` se vería forzada a implementar `comer()` y `dormir()`, que no aplican. Mejor dividir en interfaces más específicas como `Trabajable` y `Humano`.  
   - **Beneficio**: Menor acoplamiento y clases más cohesivas.


4. **Dependency Inversion Principle (DIP) - Principio de Inversión de Dependencias**  
   - **Definición**: Los módulos de alto nivel no deben depender de módulos de bajo nivel; ambos deben depender de abstracciones. Además, las abstracciones no deben depender de detalles, sino los detalles de las abstracciones.  
   - **Beneficio**: Flexibilidad para cambiar implementaciones sin modificar el código de alto nivel.


5. **LSP - Liskov Substitution Principle**  
  - Los objetos de una subclase deben poder reemplazar a los objetos de su clase base sin introducir errores o comportamientos inesperados.


  #### Explicación sencilla del LSP:
Si :  
  `B`----*hereda de*----->`A`  
`B`hereda de `A` => cualquier código que use `A` debería funcionar correctamente si se le pasa una instancia de `B`, sin necesidad de modificar el código o hacer suposiciones específicas sobre `B`.   
Esto asegura que la herencia se utilice de manera coherente y que el diseño sea robusto y mantenible.

### Clave del LSP en *Clean Code*:
- **Comportamiento coherente**: Las subclases deben respetar los contratos (métodos, propiedades, comportamientos) definidos por la clase base. No deben debilitar las precondiciones ni fortalecer las postcondiciones.
- **Evitar sorpresas**: El código que usa la clase base no debería fallar o comportarse de manera inesperada al usar una subclase.
- **Diseño limpio**: El LSP fomenta un diseño donde las jerarquías de clases son **lógicas** y **consistentes**, reduciendo la complejidad y mejorando la mantenibilidad.

### Ejemplo práctico:
Supongamos que tienes una clase base `Ave` con un método `volar()`:

```java
class Ave {
    public void volar() {
        System.out.println("El ave vuela");
    }
}
```

Y una clase derivada `Pajaro` que hereda de `Ave`:

```java
class Pajaro extends Ave {
    @Override
    public void volar() {
        System.out.println("El pájaro vuela alto");
    }
}
```

Esto cumple con el LSP porque un `Pajaro` puede sustituir a un `Ave` sin problemas: el método `volar()` sigue siendo válido y el comportamiento es consistente.

Ahora, imagina una clase `Pinguino` que también hereda de `Ave`:

```java
class Pinguino extends Ave {
    @Override
    public void volar() {
        throw new UnsupportedOperationException("Los pingüinos no vuelan");
    }
}
```

Este diseño **viola el LSP**, porque un código que espera que un `Ave` pueda `volar()` fallará si recibe un `Pinguino`. En lugar de lanzar una excepción, el diseño debería reconsiderarse, por ejemplo, separando las aves voladoras y no voladoras en jerarquías distintas:

```java
interface AveVoladora {
    void volar();
}

class Pajaro implements AveVoladora {
    public void volar() {
        System.out.println("El pájaro vuela alto");
    }
}

class Pinguino {
    public void nadar() {
        System.out.println("El pingüino nada");
    }
}
```

### Cómo aplicar el LSP en *Clean Code*:
1. **Define contratos claros**: Usa interfaces o clases base abstractas para establecer comportamientos esperados.
2. **Evita excepciones inesperadas**: Las subclases no deben introducir comportamientos que rompan las expectativas del código que usa la clase base.
3. **Revisa la jerarquía de clases**: Si una subclase no puede cumplir con el comportamiento de la clase base, considera refactorizar la jerarquía o usar composición en lugar de herencia.
4. **Piensa en el cliente**: El código que usa la clase base no debería necesitar saber qué subclase está usando.

### Beneficios en *Clean Code*:
- **Mantenibilidad**: Facilita la extensión del sistema sin introducir errores.
- **Reusabilidad**: Permite usar subclases de forma segura en cualquier lugar donde se espere la clase base.
- **Robustez**: Reduce la probabilidad de errores causados por comportamientos inesperados.

En resumen, el LSP promueve un diseño donde las jerarquías de clases son lógicas y predecibles, alineándose con los objetivos de *Clean Code* de crear software claro, mantenible y escalable. Si una subclase no puede sustituir completamente a su clase base, es una señal de que el diseño necesita ajustes.



<br>

---


<br>
<br>
<br>





El **Dependency Inversion Principle (DIP)** establece:

> Los módulos de alto nivel no deben depender de módulos de bajo nivel. Ambos deben depender de abstracciones.

Además:

> Las abstracciones no deben depender de los detalles; los detalles deben depender de las abstracciones.

La idea es evitar que la lógica de negocio esté acoplada directamente a una base de datos, API, sistema de archivos, proveedor de correo, etc.

## 1. Ejemplo simple en Java

### ❌ Sin aplicar DIP

`OrderService` depende directamente de una implementación concreta:

```java
public class MySqlOrderRepository {

    public void save(Order order) {
        System.out.println("Guardando pedido en MySQL: " + order.id());
    }
}
```

```java
public record Order(Long id, String description) {
}
```

```java
public class OrderService {

    private final MySqlOrderRepository repository;

    public OrderService() {
        this.repository = new MySqlOrderRepository();
    }

    public void createOrder(Order order) {
        // Reglas de negocio...
        repository.save(order);
    }
}
```

Problemas:

* `OrderService` conoce MySQL.
* El repositorio se instancia dentro del servicio.
* Cambiar MySQL por PostgreSQL requiere modificar `OrderService`.
* El test unitario necesita trabajar con la dependencia concreta.

### ✅ Aplicando DIP

Primero se define una abstracción:

```java
public interface OrderRepository {

    void save(Order order);
}
```

La lógica de negocio depende de esa abstracción:

```java
import java.util.Objects;

public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("El pedido es obligatorio");
        }

        repository.save(order);
    }
}
```

El detalle técnico implementa la abstracción:

```java
public class MySqlOrderRepository implements OrderRepository {

    @Override
    public void save(Order order) {
        System.out.println("Guardando pedido en MySQL: " + order.id());
    }
}
```

Composición de las dependencias:

```java
public class Main {

    public static void main(String[] args) {
        OrderRepository repository = new MySqlOrderRepository();
        OrderService service = new OrderService(repository);

        service.createOrder(new Order(1L, "Notebook"));
    }
}
```

Ahora podemos sustituir MySQL sin modificar el servicio:

```java
public class InMemoryOrderRepository implements OrderRepository {

    @Override
    public void save(Order order) {
        System.out.println("Guardando pedido en memoria: " + order.id());
    }
}
```

```java
OrderRepository repository = new InMemoryOrderRepository();
OrderService service = new OrderService(repository);
```

La dependencia queda invertida:

```text
OrderService ───────► OrderRepository
                           ▲
                           │
                MySqlOrderRepository
```

El servicio ya no depende de MySQL. La implementación de MySQL depende del contrato definido por la aplicación.

## 2. Ejemplo con Spring Boot

Spring facilita la aplicación de DIP mediante la **inyección de dependencias**.

### Estructura

```text
order
├── domain
│   ├── Order.java
│   └── OrderRepository.java
├── application
│   └── OrderService.java
└── infrastructure
    ├── OrderController.java
    └── JpaOrderRepository.java
```

### Dominio

```java
package com.example.orders.domain;

public record Order(
        Long id,
        String description
) {
}
```

El contrato pertenece al dominio o a la aplicación:

```java
package com.example.orders.domain;

public interface OrderRepository {

    Order save(Order order);
}
```

### Servicio de aplicación

```java
package com.example.orders.application;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "La descripción es obligatoria"
            );
        }

        Order order = new Order(null, description);
        return repository.save(order);
    }
}
```

El servicio depende únicamente de `OrderRepository`, no de JPA, Hibernate ni una base de datos específica.

### Persistencia JPA

Entidad utilizada por infraestructura:

```java
package com.example.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class OrderEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String description;

    protected OrderEntity() {
    }

    public OrderEntity(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
```

Repositorio propio de Spring Data:

```java
package com.example.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrderRepository
        extends JpaRepository<OrderEntity, Long> {
}
```

Adaptador que conecta el dominio con Spring Data JPA:

```java
package com.example.orders.infrastructure.persistence;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository repository;

    public JpaOrderRepository(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity =
                new OrderEntity(order.id(), order.description());

        OrderEntity saved = repository.save(entity);

        return new Order(saved.getId(), saved.getDescription());
    }
}
```

### Controller

```java
package com.example.orders.infrastructure.web;

import com.example.orders.application.OrderService;
import com.example.orders.domain.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody CreateOrderRequest request) {
        return service.createOrder(request.description());
    }
}
```

```java
package com.example.orders.infrastructure.web;

public record CreateOrderRequest(String description) {
}
```

Spring encuentra `JpaOrderRepository`, detecta que implementa `OrderRepository` y lo inyecta en `OrderService`.

El flujo de dependencias queda así:

```text
OrderController
      │
      ▼
OrderService ──────► OrderRepository
                           ▲
                           │ implementa
                    JpaOrderRepository
                           │
                           ▼
               SpringDataOrderRepository
```

## Test unitario del servicio

Gracias a DIP, el servicio se puede probar sin levantar Spring ni una base de datos:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class OrderServiceTest {

    @Test
    void shouldCreateOrder() {
        OrderRepository repository = mock(OrderRepository.class);
        OrderService service = new OrderService(repository);

        Order expected = new Order(1L, "Notebook");

        when(repository.save(any(Order.class)))
                .thenReturn(expected);

        Order result = service.createOrder("Notebook");

        assertEquals(expected, result);

        verify(repository).save(
                argThat(order ->
                        order.id() == null
                        && order.description().equals("Notebook")
                )
        );
    }
}
```

## DIP no es lo mismo que inyección de dependencias

| Concepto | Significado                                                           |
| -------- | --------------------------------------------------------------------- |
| DIP      | Principio de diseño: la lógica de alto nivel depende de abstracciones |
| DI       | Técnica para proporcionar dependencias desde el exterior              |
| IoC      | El framework controla la creación y conexión de objetos               |

Spring puede inyectar una clase concreta y aun así existir acoplamiento:

```java
@Service
public class OrderService {

    private final JpaOrderRepository repository;

    public OrderService(JpaOrderRepository repository) {
        this.repository = repository;
    }
}
```

Esto utiliza inyección de dependencias, pero aplica DIP de manera deficiente porque el servicio sigue dependiendo del detalle JPA.

La versión adecuada es:

```java
private final OrderRepository repository;
```

En resumen, DIP busca esta dirección:

```text
Lógica de negocio → abstracción ← detalle tecnológico
```

Así, la base de datos, mensajería o API externa se convierten en componentes reemplazables sin modificar la lógica de negocio.
