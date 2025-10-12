# Patron Template

<br>
<br>


El **patrón Template Method** es un patrón de diseño **comportamental** que define el esqueleto de un algoritmo en una clase base, permitiendo que las subclases redefinan ciertos pasos del algoritmo sin alterar su estructura general.  
Es ideal para situaciones donde varios procesos comparten una estructura común, pero difieren en detalles específicos.

### Características del Patrón Template Method
- **Propósito**: Proporciona un marco genérico para un algoritmo, delegando la implementación de pasos específicos a subclases.
- **Cuándo usarlo**:
  - Cuando tienes un algoritmo con pasos fijos, pero algunos pasos varían según el contexto.
  - Para evitar duplicación de código en procesos similares.
  - Para permitir que las subclases personalicen partes de un algoritmo sin cambiar su flujo general.
- **Componentes**:
  - **Clase Abstracta**: Define el método plantilla (template method) con el esqueleto del algoritmo y declara métodos abstractos o "hook" (gancho) que las subclases implementan.
  - **Clases Concretas**: Implementan los métodos abstractos o sobrescriben los métodos hook para personalizar el comportamiento.
- **Ventajas**:
  - Promueve la reutilización de código.
  - Mantiene la estructura del algoritmo consistente.
  - Facilita la extensión mediante nuevas subclases.
- **Desventajas**:
  - Puede limitar la flexibilidad si el algoritmo base es muy rígido.
  - Dependencia de la herencia, lo que puede complicar el diseño en algunos casos.

### Estructura del Patrón
1. **Método Plantilla**: Método en la clase base (generalmente `final`) que define el orden de ejecución de los pasos.
2. **Métodos Abstractos**: Pasos que las subclases deben implementar obligatoriamente.
3. **Métodos Hook**: Métodos opcionales con implementación predeterminada que las subclases pueden sobrescribir.

### Ejemplo en Java: Procesamiento de Pedidos
Supongamos que queremos modelar el proceso de preparación de pedidos en una tienda online, donde el proceso general es el mismo (recibir pedido, procesar pago, preparar producto, enviar), pero los detalles varían según el tipo de producto (físico o digital).

```java
// Clase abstracta con el método plantilla
abstract class ProcesadorPedido {
    // Método plantilla (final para evitar que se sobrescriba)
    public final void procesarPedido() {
        recibirPedido();
        procesarPago();
        prepararProducto();
        enviarProducto();
        notificarCliente(); // Método hook
    }

    // Métodos comunes (fijos para todas las subclases)
    private void recibirPedido() {
        System.out.println("Recibiendo el pedido del cliente.");
    }

    private void procesarPago() {
        System.out.println("Procesando el pago del pedido.");
    }

    // Métodos abstractos que las subclases deben implementar
    abstract void prepararProducto();
    abstract void enviarProducto();

    // Método hook (opcional, con implementación por defecto)
    protected void notificarCliente() {
        System.out.println("Notificando al cliente: Pedido procesado.");
    }
}

// Subclase para productos físicos
class PedidoFisico extends ProcesadorPedido {
    @Override
    void prepararProducto() {
        System.out.println("Preparando producto físico: Empaquetando en caja.");
    }

    @Override
    void enviarProducto() {
        System.out.println("Enviando producto físico por correo.");
    }

    // Sobrescribiendo el método hook
    @Override
    protected void notificarCliente() {
        System.out.println("Notificando al cliente: Producto físico enviado con número de seguimiento.");
    }
}

// Subclase para productos digitales
class PedidoDigital extends ProcesadorPedido {
    @Override
    void prepararProducto() {
        System.out.println("Preparando producto digital: Generando enlace de descarga.");
    }

    @Override
    void enviarProducto() {
        System.out.println("Enviando producto digital por correo electrónico.");
    }
}

// Uso del Template Method
public class Main {
    public static void main(String[] args) {
        System.out.println("Procesando un pedido físico:");
        ProcesadorPedido pedidoFisico = new PedidoFisico();
        pedidoFisico.procesarPedido();

        System.out.println("\nProcesando un pedido digital:");
        ProcesadorPedido pedidoDigital = new PedidoDigital();
        pedidoDigital.procesarPedido();
    }
}
```

### Salida del Código
```
Procesando un pedido físico:
Recibiendo el pedido del cliente.
Procesando el pago del pedido.
Preparando producto físico: Empaquetando en caja.
Enviando producto físico por correo.
Notificando al cliente: Producto físico enviado con número de seguimiento.

Procesando un pedido digital:
Recibiendo el pedido del cliente.
Procesando el pago del pedido.
Preparando producto digital: Generando enlace de descarga.
Enviando producto digital por correo electrónico.
Notificando al cliente: Pedido procesado.
```

### Explicación del Ejemplo
1. **Clase Abstracta (`ProcesadorPedido`)**:
   - Define el método plantilla `procesarPedido()`, que establece el flujo del algoritmo: recibir, pagar, preparar, enviar y notificar.
   - `recibirPedido()` y `procesarPago()` son pasos comunes, implementados directamente.
   - `prepararProducto()` y `enviarProducto()` son abstractos, forzando a las subclases a implementarlos.
   - `notificarCliente()` es un método hook con una implementación por defecto, que las subclases pueden sobrescribir si lo desean.
2. **Subclases (`PedidoFisico`, `PedidoDigital`)**:
   - Implementan los pasos específicos (`prepararProducto` y `enviarProducto`) según el tipo de producto.
   - `PedidoFisico` sobrescribe el método hook `notificarCliente()` para personalizar la notificación.
3. **Método Plantilla**:
   - Garantiza que todos los pedidos sigan el mismo flujo, pero permite personalización en los pasos clave.

### Aplicaciones Prácticas
- **Frameworks**: Muchos frameworks de Java, como Servlets (`HttpServlet`), usan Template Method. Por ejemplo, el método `service()` es el método plantilla, y las subclases implementan `doGet()` o `doPost()`.
- **Procesamiento de datos**: Algoritmos de importación/exportación de datos donde el flujo (lectura, transformación, escritura) es fijo, pero los detalles varían.
- **Juegos**: Un proceso general de "jugar una partida" (iniciar, jugar, terminar) donde los detalles dependen del tipo de juego.

### Notas Adicionales
- **Inmutabilidad del flujo**: El uso de `final` en el método plantilla asegura que las subclases no alteren el orden o la estructura del algoritmo.
- **Alternativas**: Si no quieres depender de herencia, considera el patrón **Strategy** para una mayor flexibilidad mediante composición.
- **Java Moderno**: En Java 8+, puedes usar interfaces con métodos `default` para simular partes del patrón Template sin necesidad de clases abstractas.

