# orientación a objetos de Java

detalles de equal, static y final




## Orientación a Objetos en Java (OOP)

La programación orientada a objetos (OOP, por sus siglas en inglés) es un paradigma fundamental en Java. Java es un lenguaje puramente orientado a objetos, lo que significa que todo se basa en clases y objetos. A continuación, te explico los conceptos principales de OOP en Java de manera clara y estructurada.

#### 1. **Conceptos Básicos**
   - **Clases y Objetos**:
     - Una **clase** es como un blueprint o plantilla que define las propiedades (atributos) y comportamientos (métodos) de un objeto. Por ejemplo, una clase `Coche` podría tener atributos como `color` y `velocidad`, y métodos como `acelerar()`.
     - Un **objeto** es una instancia de una clase. Puedes crear múltiples objetos de la misma clase, cada uno con sus propios valores. Ejemplo: `Coche miCoche = new Coche();`.
   - **Encapsulación**:
     - Consiste en ocultar los detalles internos de un objeto y exponer solo lo necesario a través de métodos públicos. Se logra usando modificadores de acceso como `private`, `protected` y `public`. Por ejemplo, los atributos suelen ser privados, y se acceden mediante getters y setters.
     - Beneficio: Protege los datos de modificaciones no autorizadas y mejora la mantenibilidad.
   - **Herencia**:
     - Permite que una clase (subclase o hija) herede atributos y métodos de otra clase (superclase o padre). Se usa la palabra clave `extends`. Ejemplo: `class Deportivo extends Coche { ... }`.
     - Java soporta herencia simple (una clase hereda de una sola superclase), pero no múltiple (para evitar complejidades como el "problema del diamante").
     - Beneficio: Reutilización de código y jerarquías lógicas.
   - **Polimorfismo**:
     - Significa "muchas formas". Permite que objetos de diferentes clases respondan al mismo método de manera distinta. Se logra mediante sobrescritura de métodos (overriding) o sobrecarga (overloading).
     - Ejemplo de overriding: Una subclase redefine un método de la superclase.
     - Ejemplo de overloading: Múltiples métodos con el mismo nombre pero diferentes parámetros en la misma clase.
     - Beneficio: Flexibilidad y código más genérico (por ejemplo, usando interfaces o clases abstractas).
   - **Abstracción**:
     - Se enfoca en mostrar solo los aspectos esenciales y ocultar los detalles de implementación. Se implementa con clases abstractas (usando `abstract`) o interfaces (usando `interface`).
     - Una clase abstracta no se puede instanciar directamente y puede tener métodos abstractos (sin cuerpo) que deben implementarse en subclases.
     - Una interfaz define un contrato de métodos que las clases deben implementar (desde Java 8, pueden tener métodos default y static).
     - Beneficio: Simplifica el diseño y promueve la modularidad.

#### 2. **Otros Elementos Importantes en OOP de Java**
   - **Constructores**: Métodos especiales para inicializar objetos. Pueden ser por defecto (sin parámetros) o parametrizados. Si no defines uno, Java proporciona un constructor por defecto.
   - **Paquetes**: Organizan clases relacionadas (como `java.util` para utilidades).
   - **Excepciones**: Java maneja errores con OOP a través de clases como `Exception` y subclases.
   - **Interfaces y Clases Abstractas**: Ayudan a lograr abstracción y polimorfismo. Desde Java 9, las interfaces pueden tener métodos privados.
   - **Enums**: Tipos especiales para definir constantes con valores fijos, como días de la semana.

Java sigue el principio "todo es un objeto" (excepto tipos primitivos como `int`, que tienen wrappers como `Integer` para tratarlos como objetos).

### Detalles sobre `equals`, `static` y `final`

Ahora, vamos a los detalles específicos que mencionaste. Estos son palabras clave o métodos clave en Java que se relacionan con OOP.

#### 1. **equals()**
   - **Descripción**: Es un método heredado de la clase `Object` (la superclase de todas las clases en Java). Por defecto, compara referencias de objetos (es decir, si dos variables apuntan al mismo objeto en memoria), no su contenido. Usa el operador `==` internamente.
   - **Uso y Sobrescritura**:
     - Para comparar contenido (por ejemplo, si dos strings tienen el mismo texto), debes sobrescribir (override) el método `equals()` en tu clase.
     - Regla general: Si sobrescribes `equals()`, también debes sobrescribir `hashCode()` para consistencia (usado en colecciones como HashMap).
     - Ejemplo:
       ```java
       public class Persona {
           private String nombre;
           private int edad;

           public Persona(String nombre, int edad) {
               this.nombre = nombre;
               this.edad = edad;
           }

           @Override
           public boolean equals(Object obj) {
               if (this == obj) return true; // Mismo objeto
               if (obj == null || getClass() != obj.getClass()) return false;
               Persona other = (Persona) obj;
               return edad == other.edad && nombre.equals(other.nombre);
           }

           @Override
           public int hashCode() {
               return Objects.hash(nombre, edad); // Usando helper de Java
           }
       }
       ```
     - **Diferencia con `==`**: `==` compara referencias (direcciones de memoria), mientras que `equals()` puede comparar valores si se sobrescribe.
     - **En Clases Comunes**: En `String`, `Integer`, etc., ya está sobrescrito para comparar contenido.
     - **Consejo**: Usa `@Override` para evitar errores al sobrescribir.

#### 2. **static**
   - **Descripción**: Indica que un miembro (variable, método o clase anidada) pertenece a la clase en sí, no a instancias individuales. Se accede sin crear objetos.
   - **Usos**:
     - **Variables static**: Compartidas por todas las instancias. Ejemplo: `static int contador = 0;` (un contador global para la clase).
     - **Métodos static**: Pueden llamarse directamente con el nombre de la clase, como `Math.sqrt(4)`. No pueden acceder a variables no static (porque no hay "this").
     - **Bloques static**: Código que se ejecuta una vez cuando la clase se carga (útil para inicializaciones).
     - **Clases static anidadas**: Pueden existir sin una instancia de la clase externa.
   - **Ejemplo**:
     ```java
     public class Utilidades {
         public static int sumar(int a, int b) {
             return a + b;
         }
     }
     // Uso: int resultado = Utilidades.sumar(5, 3);
     ```
   - **Ventajas**: Eficiencia en memoria (una sola copia), útil para utilidades o constantes.
   - **Desventajas**: No soporta polimorfismo (no se puede sobrescribir), y abusar de static puede romper principios OOP al hacer código más procedural.
   - **Nota**: `main()` es static porque se ejecuta sin instanciar la clase.

#### 3. **final**
   - **Descripción**: Indica que algo no puede modificarse después de su inicialización. Promueve inmutabilidad y seguridad.
   - **Usos**:
     - **Variables final**: Constantes. Deben inicializarse al declararse o en el constructor. Ejemplo: `final double PI = 3.1416;` (no se puede cambiar).
     - **Métodos final**: No pueden sobrescribirse en subclases. Útil para métodos críticos.
     - **Clases final**: No pueden heredarse (no se puede usar `extends`). Ejemplos built-in: `String`, `Integer`. Beneficio: Seguridad y optimización.
     - **Parámetros final**: En métodos, evita que se modifiquen dentro del método (buena práctica para claridad).
   - **Ejemplo**:
     ```java
     public final class Configuracion {
         public final int MAX_USUARIOS = 100;

         public final void imprimirMensaje() {
             System.out.println("Mensaje fijo");
         }
     }
     ```
   - **En OOP**: Ayuda a la encapsulación y previene errores en herencia. En lambdas o inner classes, variables locales deben ser final (o efectivamente final) para accesarse.
   - **Nota**: `final` no implica inmutabilidad total; por ejemplo, un array final no se puede reasignar, pero sus elementos sí se pueden cambiar.

Si necesitas ejemplos de código más detallados, ejercicios o aclaraciones sobre algún punto específico, ¡házmelo saber! Java es un lenguaje poderoso para OOP, y practicar con proyectos simples ayuda mucho a entenderlo.




<br>
<br>
<br>

---

<br>
<br>
<br>







# Patrones con ejemplos en Java


Los patrones de diseño son soluciones reutilizables para problemas comunes en el diseño de software, y en Java, son especialmente útiles en el contexto de la programación orientada a objetos. A continuación, te explico **tres patrones de diseño** populares con ejemplos en Java: **Singleton**, **Factory Method** y **Observer**. Cada ejemplo incluye una explicación breve, el código y cómo se aplica en un contexto práctico.

---

### 1. Patrón Singleton
**Categoría**: Creacional  
**Propósito**: Garantiza que una clase tenga una sola instancia y proporciona un punto de acceso global a ella.  
**Cuándo usarlo**: Cuando necesitas una única instancia de una clase, como un logger, una conexión a base de datos o un gestor de configuración.

**Ejemplo en Java**: Implementación de un Logger Singleton.

```java
public class Logger {
    // Única instancia de la clase
    private static final Logger instance = new Logger();

    // Constructor privado para evitar instanciación externa
    private Logger() {
        // Evita instanciación mediante reflexión
        if (instance != null) {
            throw new RuntimeException("Usa getInstance() para obtener la instancia.");
        }
    }

    // Método para obtener la instancia
    public static Logger getInstance() {
        return instance;
    }

    // Método de ejemplo para registrar un mensaje
    public void log(String message) {
        System.out.println("Log: " + message);
    }
}

// Uso del Singleton
public class Main {
    public static void main(String[] args) {
        Logger logger1 = Logger.getInstance();
        Logger logger2 = Logger.getInstance();

        logger1.log("Mensaje desde logger1");
        logger2.log("Mensaje desde logger2");

        // Comprobamos que son la misma instancia
        System.out.println("¿Misma instancia? " + (logger1 == logger2)); // true
    }
}
```

**Explicación**:
- El constructor es privado para evitar crear nuevas instancias.
- La instancia se crea de forma estática (inicialización ansiosa) y se accede mediante `getInstance()`.
- Este patrón es útil para recursos compartidos. En este caso, `Logger` asegura que todos los componentes usen la misma instancia para registrar mensajes.
- **Nota**: Para entornos multi-hilo, podrías usar inicialización perezosa con sincronización o una clase estática interna (Bill Pugh Singleton).

---

### 2. Patrón Factory Method
**Categoría**: Creacional  
**Propósito**: Define una interfaz para crear objetos, pero permite que las subclases decidan qué clase instanciar.  
**Cuándo usarlo**: Cuando quieres delegar la creación de objetos a subclases o necesitas flexibilidad en el tipo de objetos creados.

**Ejemplo en Java**: Creación de diferentes tipos de vehículos.

```java
// Producto abstracto
abstract class Vehiculo {
    abstract void conducir();
}

// Productos concretos
class Coche extends Vehiculo {
    @Override
    void conducir() {
        System.out.println("Conduciendo un coche.");
    }
}

class Moto extends Vehiculo {
    @Override
    void conducir() {
        System.out.println("Conduciendo una moto.");
    }
}

// Creador abstracto
abstract class FabricaVehiculos {
    // Método fábrica
    abstract Vehiculo crearVehiculo();

    // Método que usa el producto
    void usarVehiculo() {
        Vehiculo vehiculo = crearVehiculo();
        vehiculo.conducir();
    }
}

// Creadores concretos
class FabricaCoches extends FabricaVehiculos {
    @Override
    Vehiculo crearVehiculo() {
        return new Coche();
    }
}

class FabricaMotos extends FabricaVehiculos {
    @Override
    Vehiculo crearVehiculo() {
        return new Moto();
    }
}

// Uso del Factory Method
public class Main {
    public static void main(String[] args) {
        FabricaVehiculos fabricaCoches = new FabricaCoches();
        FabricaVehiculos fabricaMotos = new FabricaMotos();

        fabricaCoches.usarVehiculo(); // Conduciendo un coche.
        fabricaMotos.usarVehiculo(); // Conduciendo una moto.
    }
}
```

**Explicación**:
- La clase abstracta `FabricaVehiculos` define el método `crearVehiculo()`, que las subclases (`FabricaCoches`, `FabricaMotos`) implementan para decidir qué objeto crear.
- Esto desacopla el código cliente de la creación directa de objetos, permitiendo agregar nuevos tipos de vehículos sin modificar el código existente.
- **Aplicación práctica**: Útil en sistemas que necesitan crear objetos de diferentes familias, como interfaces gráficas (botones para Windows, macOS, etc.) o conexiones a bases de datos.

---

### 3. Patrón Observer
**Categoría**: Comportamental  
**Propósito**: Define una relación uno-a-muchos entre objetos, de modo que cuando un objeto cambia su estado, todos los dependientes son notificados y actualizados automáticamente.  
**Cuándo usarlo**: Cuando un cambio en un objeto debe propagarse a otros, como en interfaces de usuario o sistemas de suscripción.

**Ejemplo en Java**: Sistema de notificación de noticias.

```java
import java.util.ArrayList;
import java.util.List;

// Interfaz del Observador
interface Observador {
    void actualizar(String noticia);
}

// Sujeto (Observable)
class AgenciaNoticias {
    private List<Observador> observadores = new ArrayList<>();
    private String ultimaNoticia;

    // Suscribir un observador
    public void agregarObservador(Observador observador) {
        observadores.add(observador);
    }

    // Eliminar un observador
    public void eliminarObservador(Observador observador) {
        observadores.remove(observador);
    }

    // Publicar una nueva noticia y notificar a los observadores
    public void publicarNoticia(String noticia) {
        this.ultimaNoticia = noticia;
        notificarObservadores();
    }

    private void notificarObservadores() {
        for (Observador observador : observadores) {
            observador.actualizar(ultimaNoticia);
        }
    }
}

// Observadores concretos
class CanalTV implements Observador {
    private String nombre;

    public CanalTV(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public void actualizar(String noticia) {
        System.out.println(nombre + " transmite: " + noticia);
    }
}

class SitioWeb implements Observador {
    private String nombre;

    public SitioWeb(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public void actualizar(String noticia) {
        System.out.println(nombre + " publica: " + noticia);
    }
}

// Uso del Observer
public class Main {
    public static void main(String[] args) {
        AgenciaNoticias agencia = new AgenciaNoticias();

        Observador canalTV = new CanalTV("Canal 5");
        Observador sitioWeb = new SitioWeb("Noticias.com");

        agencia.agregarObservador(canalTV);
        agencia.agregarObservador(sitioWeb);

        agencia.publicarNoticia("¡Evento importante ocurrido hoy!");
        // Salida:
        // Canal 5 transmite: ¡Evento importante ocurrido hoy!
        // Noticias.com publica: ¡Evento importante ocurrido hoy!
    }
}
```

**Explicación**:
- La `AgenciaNoticias` (sujeto) mantiene una lista de observadores y notifica a todos cuando hay una nueva noticia.
- Los observadores (`CanalTV`, `SitioWeb`) implementan la interfaz `Observador` para definir cómo manejan las actualizaciones.
- Este patrón es ideal para sistemas de publicación/suscripción, como feeds de noticias, eventos en interfaces gráficas o sistemas de mensajería.
- **Nota**: Java tiene soporte nativo para Observer con la interfaz `java.util.Observer` y la clase `Observable` (aunque está obsoleta desde Java 9). Se prefieren implementaciones personalizadas o bibliotecas modernas como RxJava.

---

### Resumen de los Patrones
1. **Singleton**: Útil para recursos únicos y compartidos, como loggers o conexiones. Cuidado con su uso en entornos multi-hilo.
2. **Factory Method**: Ideal para delegar la creación de objetos y permitir extensibilidad. Facilita la adición de nuevos tipos sin modificar el código cliente.
3. **Observer**: Perfecto para sistemas donde los cambios en un objeto deben propagarse a otros, como en interfaces reactivas o sistemas de eventos.

Si quieres profundizar en algún patrón, explorar otros (como Decorator, Strategy o Adapter) o necesitas más ejemplos prácticos, ¡avísame! También puedo ayudarte a implementarlos en un proyecto específico.



<br>
<br>
<br>

---


# Patron Template

<br>
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




<br>
<br>
<br>

---



<br>
<br>
<br>






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






<br>
<br>
<br>

---



<br>
<br>
<br>




# Static e Inmutable


En Java 21, los conceptos de **static** e **inmutable** son completamente distintos y se aplican en contextos diferentes. No son términos que se comparen directamente, ya que **static** se refiere a cómo se asocia un método o variable con una clase u objeto, mientras que **inmutable** describe una propiedad de un objeto o dato que no puede ser modificado después de su creación. A continuación, te explico cada término, sus diferencias, y cómo se relacionan con métodos en Java 21, con ejemplos claros.

### **1. Métodos `static`**
- **Definición**: Un método `static` es un método que pertenece a la **clase** en lugar de a una instancia de la clase. Se define usando la palabra clave `static`.
- **Características**:
  - Se puede invocar **sin crear una instancia** de la clase, usando el nombre de la clase (e.g., `Clase.metodoEstatico()`).
  - No tiene acceso a los **atributos de instancia** ni a los métodos no estáticos de la clase, ya que no está asociado a un objeto específico.
  - Se usa para operaciones que no dependen del estado de una instancia, como funciones de utilidad, métodos de fábrica, o cálculos genéricos.
  - Los métodos `static` son **compartidos** por todas las instancias de la clase y residen en la memoria estática de la clase.
  - Ejemplo:
    ```java
    public class Calculadora {
        public static int sumar(int a, int b) {
            return a + b;
        }
    }

    public class Main {
        public static void main(String[] args) {
            int resultado = Calculadora.sumar(5, 3); // Llamada sin instancia
            System.out.println(resultado); // Imprime: 8
        }
    }
    ```

- **Cuándo usar**: Para métodos que no necesitan acceder al estado de un objeto (atributos de instancia) o que proporcionan funcionalidad general relacionada con la clase.

### **2. Inmutabilidad**
- **Definición**: Un objeto es **inmutable** si su estado no puede cambiar después de ser creado. En el contexto de métodos, la inmutabilidad no se aplica directamente al método en sí, sino al **objeto o datos** que el método manipula o devuelve.
- **Características**:
  - Un objeto inmutable no permite modificar sus atributos después de la construcción (e.g., usando `final` para los campos y sin setters).
  - Los métodos de un objeto inmutable no modifican su estado interno; si un método parece "modificar" algo, en realidad devuelve una **nueva instancia** con el estado modificado.
  - Ejemplo clásico: Clases como `String`, `Integer`, o `LocalDate` en Java son inmutables.
  - En Java 21, puedes usar **records** para crear clases inmutables de manera más sencilla, ya que los `records` son inmutables por defecto.
  - Ejemplo:
    ```java
    public record Punto(int x, int y) {
        // Método que parece modificar, pero devuelve una nueva instancia
        public Punto mover(int deltaX, int deltaY) {
            return new Punto(x + deltaX, y + deltaY);
        }
    }

    public class Main {
        public static void main(String[] args) {
            Punto p1 = new Punto(1, 2);
            Punto p2 = p1.mover(3, 4); // Crea un nuevo Punto
            System.out.println(p1); // Imprime: Punto[x=1, y=2]
            System.out.println(p2); // Imprime: Punto[x=4, y=6]
        }
    }
    ```

- **Cuándo usar**: Cuando quieres garantizar que el estado de un objeto no cambie, lo que es útil para seguridad en hilos, diseño robusto, y evitar efectos secundarios.

### **Diferencias Clave**
| **Aspecto**                | **Static**                                       | **Inmutable**                                   |
|----------------------------|--------------------------------------------------|------------------------------------------------|
| **Definición**             | Pertenece a la clase, no a una instancia.        | El estado del objeto no puede cambiar tras su creación. |
| **Aplicación**             | Se aplica a métodos o variables de la clase.     | Se aplica a objetos (no a métodos directamente). |
| **Acceso**                 | Se invoca con el nombre de la clase, sin instancia. | Requiere una instancia de un objeto inmutable. |
| **Modificación del estado**| No está relacionado con inmutabilidad; puede modificar variables estáticas. | Garantiza que el estado del objeto no cambia. |
| **Uso típico**             | Métodos de utilidad, fábricas, o funciones que no dependen de instancias. | Objetos que deben ser seguros y predecibles (e.g., `String`, `record`). |
| **Ejemplo en Java 21**     | `Math.abs(-5)` (método estático).               | `String.replace("a", "b")` (devuelve nuevo String). |

### **Relación con Métodos**
- Un método `static` **no implica inmutabilidad**. Puede modificar variables estáticas de la clase (si no están marcadas como `final`) o realizar operaciones que no afectan el estado de ningún objeto.
  - Ejemplo: Un método estático como `Collections.sort(lista)` modifica la lista pasada como parámetro, por lo que no es inmutable en su efecto.
- Un método en una clase inmutable (o en un `record`) no modifica el estado del objeto, y si parece hacerlo, devuelve una nueva instancia. Este método puede ser estático o no estático, pero la inmutabilidad se refiere al objeto, no al método.
  - Ejemplo: En un `record`, un método no estático como `punto.mover()` devuelve un nuevo `Punto` sin modificar el original.

### **Ejemplo Combinado en Java 21**
A continuación, un ejemplo que ilustra un método `static` y un objeto inmutable (usando un `record`):

```java
public record Rectangulo(int ancho, int alto) {
    // Método no estático que respeta la inmutabilidad
    public Rectangulo escalar(int factor) {
        return new Rectangulo(ancho * factor, alto * factor);
    }

    // Método estático de utilidad
    public static Rectangulo crearCuadrado(int lado) {
        return new Rectangulo(lado, lado);
    }

    // Método estático que calcula área (no modifica estado)
    public static int calcularArea(int ancho, int alto) {
        return ancho * alto;
    }
}

public class Main {
    public static void main(String[] args) {
        // Crear un rectángulo inmutable
        Rectangulo rect = new Rectangulo(5, 3);
        System.out.println(rect); // Imprime: Rectangulo[ancho=5, alto=3]

        // Método no estático que devuelve un nuevo rectángulo
        Rectangulo rectEscalado = rect.escalar(2);
        System.out.println(rectEscalado); // Imprime: Rectangulo[ancho=10, alto=6]
        System.out.println(rect); // Original no cambia: Rectangulo[ancho=5, alto=3]

        // Método estático para crear un cuadrado
        Rectangulo cuadrado = Rectangulo.crearCuadrado(4);
        System.out.println(cuadrado); // Imprime: Rectangulo[ancho=4, alto=4]

        // Método estático para calcular área
        int area = Rectangulo.calcularArea(5, 3);
        System.out.println("Área: " + area); // Imprime: Área: 15
    }
}
```

### **Explicación del Ejemplo**
- **Inmutabilidad**: El `record Rectangulo` es inmutable por diseño (sus campos `ancho` y `alto` son `final`). El método `escalar` no modifica el rectángulo original, sino que devuelve uno nuevo.
- **Métodos `static`**: Los métodos `crearCuadrado` y `calcularArea` son estáticos, no requieren una instancia de `Rectangulo`, y se usan como funciones de utilidad.
- Los métodos estáticos no tienen relación directa con la inmutabilidad del objeto, pero pueden trabajar con objetos inmutables como en este caso.

### **Java 21 y Novedades**
En Java 21, no hay cambios específicos en la semántica de `static` o inmutabilidad respecto a versiones anteriores, pero los **records** (introducidos en Java 16 como característica estándar) facilitan la creación de clases inmutables. Además, Java 21 incluye mejoras como **pattern matching** y **sealed classes**, que pueden combinarse con inmutabilidad para diseños más robustos, pero no afectan directamente la distinción entre `static` e inmutabilidad.

### **Conclusión**
- **`static`**: Se refiere a métodos o variables asociados a la clase, no a instancias, y se usa para funciones que no dependen del estado de un objeto. No tiene relación con inmutabilidad.
- **Inmutable**: Se aplica a objetos cuyo estado no cambia tras su creación. Los métodos de objetos inmutables (estáticos o no) no modifican el estado, y si generan un cambio, devuelven una nueva instancia.
- En Java 21, los `records` son una forma práctica de garantizar inmutabilidad, mientras que los métodos `static` son útiles para operaciones de utilidad o creación de instancias.



<br>
<br>



---

<br>
<br>
<br>
<br>





# ACID




ACID properties are a set of principles that ensure reliable and consistent database transactions, especially in relational database management systems (RDBMS).

The acronym stands for **Atomicity**, **Consistency**, **Isolation**, and **Durability**.  

These properties collectively guarantee that database transactions are processed correctly even in the presence of errors, failures, or concurrent operations.

### Atomicity
Atomicity ensures that a transaction is treated as a single, indivisible unit of work.  
Either all operations within the transaction are completed successfully (committed), or none of them are applied (rolled back). This prevents partial updates that could leave the database in an inconsistent state. For example, in a bank transfer, debiting one account and crediting another must both succeed or both fail.

### Consistency
Consistency guarantees that a transaction brings the database from one valid state to another, adhering to all defined rules, constraints, triggers, and data integrity checks (e.g., primary keys, foreign keys, or custom business logic). If a transaction violates any consistency rules, it is rolled back. This property maintains the overall integrity of the data across the database.

### Isolation
Isolation ensures that transactions are executed independently of one another. Concurrent transactions do not interfere with each other, preventing issues like dirty reads (reading uncommitted data), non-repeatable reads, or phantom reads. Isolation levels (e.g., read uncommitted, read committed, repeatable read, serializable) can be adjusted to balance consistency with performance.

### Durability
Durability means that once a transaction is committed, its changes are permanently saved to the database, even in the event of a system failure like a power outage. This is typically achieved through techniques like write-ahead logging (WAL) or persistent storage to non-volatile media.

<br>

ACID compliance is crucial for applications requiring high reliability, such as financial systems, but it can introduce overhead in terms of performance. Modern databases often provide configurable ACID support, and NoSQL databases may prioritize scalability over full ACID compliance (e.g., via BASE properties: Basically Available, Soft state, Eventual consistency).
