# Navegacion



Porque son dos conceptos relacionados, pero distintos:

* **`1:N` expresa la cardinalidad del modelo relacional.**
* **`List<OrderItem>` expresa cómo representamos el lado “N” dentro del objeto Java.**

## 1. Relación `Order` → `OrderItem`

En el modelo:

```text
Order 1 ───── N OrderItem
```

Una orden puede tener muchos ítems. Por eso, en Java, `Order` necesita una colección para contenerlos:

```java
@OneToMany(mappedBy = "order")
private List<OrderItem> items = new ArrayList<>();
```

La `List` representa los múltiples `OrderItem` asociados a una misma orden.

En la base de datos, normalmente no existe una columna `items` dentro de `orders`. La relación se almacena mediante una clave foránea en `order_items`:

```text
orders
------
id
status

order_items
-----------
id
quantity
order_id   ← FK hacia orders.id
```

Ejemplo:

```text
orders

id
--
10
```

```text
order_items

id    order_id    quantity
--------------------------
100   10          2
101   10          1
102   10          5
```

La orden `10` tiene tres ítems. En Java, esos tres registros se representan como:

```java
order.getItems()
```

que devuelve:

```java
List<OrderItem>
```

---

# 2. Relación `Customer` → `Order`

El modelo también indica:

```text
Customer 1 ───── N Order
```

Un cliente puede tener muchas órdenes.

Sin embargo, en el ejemplo mostrado, la relación fue modelada únicamente desde `Order`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private Customer customer;
```

Esto significa:

```text
Muchas órdenes pertenecen a un cliente
```

Desde `Order` puedes navegar hacia `Customer`:

```java
order.getCustomer();
```

Pero desde `Customer` no puedes navegar directamente hacia sus órdenes, porque no declaramos:

```java
private List<Order> orders;
```

Es decir, la relación está modelada como **unidireccional**.

---

# 3. Relación unidireccional

La entidad `Order` conoce al cliente:

```java
@Entity
public class Order {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

Pero `Customer` no conoce sus órdenes:

```java
@Entity
public class Customer {

    @Id
    private Long id;

    private String name;
}
```

La relación sigue siendo `1:N` en la base de datos, aunque no exista una colección en `Customer`.

Esto es perfectamente válido.

La cardinalidad física está dada por la clave foránea:

```text
orders.customer_id
```

Varios registros de `orders` pueden tener el mismo `customer_id`.

---

# 4. Relación bidireccional

También podríamos agregar una colección en `Customer`:

```java
@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();

    public List<Order> getOrders() {
        return orders;
    }
}
```

Y mantener en `Order`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private Customer customer;
```

Ahora la relación es bidireccional:

```java
order.getCustomer();
```

y también:

```java
customer.getOrders();
```

Visualmente:

```text
Customer
   │
   │ getOrders()
   ▼
List<Order>

Order
   │
   │ getCustomer()
   ▼
Customer
```

---

# 5. Por qué `Order` tiene `List<OrderItem>`

Porque en ese caso queríamos navegar naturalmente desde una orden hacia sus ítems:

```java
Order order = repository.findById(id).orElseThrow();

for (OrderItem item : order.getItems()) {
    System.out.println(item.getProduct().getName());
}
```

En el dominio, una orden casi siempre necesita conocer sus líneas o ítems.

Además, normalmente `OrderItem` forma parte del agregado `Order`. Por eso suele tener sentido que `Order` administre su colección:

```java
public void addItem(OrderItem item) {
    items.add(item);
    item.assignOrder(this);
}
```

Esto mantiene ambos lados sincronizados:

```java
order.getItems().add(item);
item.setOrder(order);
```

---

# 6. Por qué `Customer` no tenía `List<Order>`

Fue una simplificación deliberada.

No siempre conviene representar todas las relaciones posibles en ambas direcciones.

Aunque un cliente tenga miles de órdenes, muchas operaciones solo necesitan:

```java
order.getCustomer();
```

y no:

```java
customer.getOrders();
```

Agregar la colección:

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

puede traer varios problemas:

* mayor complejidad del modelo;
* riesgo de cargas accidentales;
* consultas N+1;
* problemas de serialización JSON;
* ciclos entre `Customer` y `Order`;
* colecciones potencialmente enormes;
* mayor dificultad con `equals()`, `hashCode()` y `toString()`.

Por eso se suele modelar solo la navegación que realmente necesita el caso de uso.

---

# 7. La cardinalidad no obliga a declarar una colección

Una relación `1:N` no obliga a tener una `List` en el lado `1`.

Puedes tener únicamente:

```java
@ManyToOne
private Customer customer;
```

Eso ya representa correctamente la relación desde el lado de muchas órdenes.

La base de datos contiene:

```text
orders.customer_id
```

La colección en `Customer` es opcional desde el punto de vista del modelo Java.

---

# 8. ¿Por qué `List` y no `Set`?

Podemos representar una relación `1:N` con distintas colecciones:

```java
List<OrderItem>
Set<OrderItem>
Collection<OrderItem>
```

## `List`

```java
private List<OrderItem> items;
```

Se utiliza cuando:

* el orden de los elementos importa;
* se permiten elementos conceptualmente repetidos;
* se quiere acceder por posición;
* el dominio se comporta como una secuencia.

Ejemplo:

```java
items.get(0);
```

Si el orden debe persistirse, se puede usar:

```java
@OrderColumn(name = "item_position")
@OneToMany(mappedBy = "order")
private List<OrderItem> items;
```

---

## `Set`

```java
private Set<OrderItem> items = new HashSet<>();
```

Se utiliza cuando:

* no deben existir duplicados;
* el orden no importa;
* la identidad lógica del elemento está bien definida;
* `equals()` y `hashCode()` están implementados correctamente.

Ejemplo:

```java
@OneToMany(mappedBy = "order")
private Set<OrderItem> items = new HashSet<>();
```

---

## `Collection`

```java
private Collection<OrderItem> items;
```

Es más abstracta, pero expresa menos intención sobre orden o duplicados.

---

# 9. Ejemplo completo bidireccional

## `Customer`

```java
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();

    protected Customer() {
    }

    public void addOrder(Order order) {
        orders.add(order);
        order.assignCustomer(this);
    }

    public List<Order> getOrders() {
        return orders;
    }
}
```

## `Order`

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public void assignCustomer(Customer customer) {
        this.customer = customer;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
    }
}
```

Ahora existen dos relaciones bidireccionales:

```text
Customer 1 ─── N Order
Order    1 ─── N OrderItem
```

Y cada lado `1` contiene una colección:

```java
Customer.orders
Order.items
```

---

# 10. Tabla resumen

| Relación                | Lado individual     | Lado múltiple en Java              |
| ----------------------- | ------------------- | ---------------------------------- |
| `Customer 1:N Order`    | `Order.customer`    | Opcionalmente `Customer.orders`    |
| `Order 1:N OrderItem`   | `OrderItem.order`   | `Order.items`                      |
| `Product 1:N OrderItem` | `OrderItem.product` | Opcionalmente `Product.orderItems` |

La palabra **opcionalmente** es importante: JPA no exige modelar ambos lados.

---

# Idea clave

La relación:

```text
Order 1:N OrderItem
```

se expresa en Java con:

```java
@OneToMany
private List<OrderItem> items;
```

porque una orden contiene múltiples ítems.

En cambio, aunque:

```text
Customer 1:N Order
```

también sea cierto, el ejemplo solo necesitaba navegar desde `Order` hacia `Customer`, por lo que se declaró únicamente:

```java
@ManyToOne
private Customer customer;
```

La cardinalidad de la base de datos y la dirección de navegación del modelo Java no son exactamente lo mismo.
