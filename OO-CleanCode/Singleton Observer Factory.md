# Patrones: Singleton Observer Factory


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

