# Strategy




El **patrón Strategy** es un patrón de diseño **comportamental** que permite definir una familia de algoritmos, encapsular cada uno de ellos y hacerlos intercambiables. Este patrón permite que el algoritmo varíe independientemente de los clientes que lo utilizan, promoviendo la flexibilidad y la reutilización del código. A diferencia del **Template Method**, que usa herencia para personalizar pasos de un algoritmo, Strategy emplea **composición** para cambiar el comportamiento dinámicamente.

### Características del Patrón Strategy
- **Propósito**: Encapsular algoritmos intercambiables y permitir cambiarlos en tiempo de ejecución.
- **Cuándo usarlo**:
  - Cuando tienes múltiples formas de realizar una tarea y quieres evitar condicionales complejos (como muchos `if-else`).
  - Cuando necesitas cambiar el comportamiento de un objeto dinámicamente.
  - Para aislar la lógica de un algoritmo y hacer el código más mantenible.
- **Componentes**:
  - **Contexto**: Clase que usa una estrategia y mantiene una referencia a un objeto Strategy.
  - **Interfaz Strategy**: Define un contrato común para todos los algoritmos (generalmente una interfaz o clase abstracta).
  - **Estrategias Concretas**: Implementaciones específicas de la interfaz Strategy.
- **Ventajas**:
  - Promueve el principio de **abierto/cerrado** (abierto para extensión, cerrado para modificación).
  - Reduce el uso de condicionales.
  - Permite cambiar algoritmos en tiempo de ejecución.
- **Desventajas**:
  - Aumenta el número de clases (una por cada estrategia).
  - El cliente debe conocer las estrategias disponibles para elegir la adecuada.

### Estructura del Patrón
1. **Interfaz Strategy**: Declara un método que todas las estrategias concretas deben implementar.
2. **Estrategias Concretas**: Implementan el algoritmo específico.
3. **Contexto**: Contiene una referencia a una estrategia y delega la ejecución del algoritmo a esa estrategia.

### Ejemplo en Java: Procesamiento de Pagos
Imagina una tienda online que permite pagar con diferentes métodos (tarjeta de crédito, PayPal, criptomonedas). Cada método tiene su propia lógica, pero el proceso de pago debe ser manejado de forma uniforme.

```java
// Interfaz Strategy
interface EstrategiaPago {
    boolean procesarPago(double monto);
}

// Estrategias Concretas
class PagoTarjeta implements EstrategiaPago {
    private String numeroTarjeta;
    private String titular;

    public PagoTarjeta(String numeroTarjeta, String titular) {
        this.numeroTarjeta = numeroTarjeta;
        this.titular = titular;
    }

    @Override
    public boolean procesarPago(double monto) {
        System.out.println("Procesando pago de $" + monto + " con tarjeta " + numeroTarjeta);
        // Lógica simulada para validar tarjeta
        return true;
    }
}

class PagoPayPal implements EstrategiaPago {
    private String email;

    public PagoPayPal(String email) {
        this.email = email;
    }

    @Override
    public boolean procesarPago(double monto) {
        System.out.println("Procesando pago de $" + monto + " con PayPal (" + email + ")");
        // Lógica simulada para PayPal
        return true;
    }
}

class PagoCripto implements EstrategiaPago {
    private String billetera;

    public PagoCripto(String billetera) {
        this.billetera = billetera;
    }

    @Override
    public boolean procesarPago(double monto) {
        System.out.println("Procesando pago de $" + monto + " con criptomonedas (" + billetera + ")");
        // Lógica simulada para criptomonedas
        return true;
    }
}

// Contexto
class CarritoCompras {
    private EstrategiaPago estrategiaPago;

    // Cambiar la estrategia en tiempo de ejecución
    public void setEstrategiaPago(EstrategiaPago estrategiaPago) {
        this.estrategiaPago = estrategiaPago;
    }

    // Ejecutar el pago usando la estrategia seleccionada
    public boolean realizarPago(double monto) {
        if (estrategiaPago == null) {
            System.out.println("Error: No se ha seleccionado un método de pago.");
            return false;
        }
        return estrategiaPago.procesarPago(monto);
    }
}

// Uso del Patrón Strategy
public class Main {
    public static void main(String[] args) {
        CarritoCompras carrito = new CarritoCompras();

        // Configurar pago con tarjeta
        carrito.setEstrategiaPago(new PagoTarjeta("1234-5678-9012-3456", "Juan Pérez"));
        carrito.realizarPago(100.50); // Procesando pago de $100.5 con tarjeta 1234-5678-9012-3456

        // Cambiar a PayPal en tiempo de ejecución
        carrito.setEstrategiaPago(new PagoPayPal("juan@example.com"));
        carrito.realizarPago(75.20); // Procesando pago de $75.2 con PayPal (juan@example.com)

        // Cambiar a criptomonedas
        carrito.setEstrategiaPago(new PagoCripto("0x123abc"));
        carrito.realizarPago(200.00); // Procesando pago de $200.0 con criptomonedas (0x123abc)
    }
}
```

### Salida del Código

```
Procesando pago de $100.5 con tarjeta 1234-5678-9012-3456
Procesando pago de $75.2 con PayPal (juan@example.com)
Procesando pago de $200.0 con criptomonedas (0x123abc)
```

### Explicación del Ejemplo

1. **Interfaz Strategy (`EstrategiaPago`)**:
   - Define el método `procesarPago()` que todas las estrategias deben implementar.
2. **Estrategias Concretas**:
   - `PagoTarjeta`, `PagoPayPal`, y `PagoCripto` implementan la lógica específica para cada método de pago.
3. **Contexto (`CarritoCompras`)**:
   - Mantiene una referencia a una estrategia de pago (`estrategiaPago`).
   - Permite cambiar la estrategia en tiempo de ejecución con `setEstrategiaPago()`.
   - Delega la ejecución del pago al método `procesarPago()` de la estrategia seleccionada.
4. **Ventaja del Patrón**:
   - El cliente (`CarritoCompras`) no necesita conocer los detalles de cada método de pago.
   - Puedes agregar nuevas estrategias (por ejemplo, `PagoTransferenciaBancaria`) sin modificar el código del contexto.
   - Cambiar el método de pago en tiempo de ejecución es sencillo.

### Aplicaciones Prácticas
- **Sistemas de pago**: Como en el ejemplo, para soportar diferentes métodos de pago.
- **Algoritmos de ordenación**: Cambiar entre QuickSort, MergeSort, etc., según el contexto.
- **Validaciones**: Diferentes reglas de validación para formularios (por ejemplo, validar email, teléfono, etc.).
- **Compresión de archivos**: Seleccionar entre ZIP, RAR, 7z, etc., en tiempo de ejecución.

### Comparación con Template Method
- **Template Method**:
  - Usa **herencia** para definir un algoritmo con pasos personalizables.
  - El flujo del algoritmo es fijo y está definido en la clase base.
  - Menos flexible para cambios dinámicos.
- **Strategy**:
  - Usa **composición** para cambiar algoritmos completos.
  - Permite cambiar el comportamiento en tiempo de ejecución.
  - Más flexible, pero requiere más clases.

### Notas Adicionales
- **Java Moderno**: Puedes usar lambdas o funciones en Java 8+ para simplificar estrategias simples, aunque para estrategias complejas es mejor usar clases explícitas.
- **Inyección de Dependencias**: Strategy es común en frameworks como Spring, donde las estrategias se inyectan dinámicamente.
- **Extensibilidad**: Agregar una nueva estrategia solo requiere crear una nueva clase que implemente la interfaz Strategy, sin tocar el contexto.

Si necesitas otro ejemplo (por ejemplo, aplicado a un caso diferente como ordenación o compresión), una comparación más detallada con otros patrones, o ayuda para integrar Strategy en un proyecto, ¡avísame!
