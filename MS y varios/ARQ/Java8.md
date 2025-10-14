# ¿Cuáles son los aspectos más importantes de Java8?

<br>

Java 8 (lanzado en marzo de 2014) fue un **punto de quiebre en el lenguaje**, porque introdujo un conjunto de características que marcaron el camino hacia un estilo de programación más **funcional** y conciso.
Aquí tienes los aspectos más importantes:

<br>
<br>

# 🚀

### **Nuevas características principales**

1. **Expresiones Lambda**

   * Permiten pasar bloques de código como parámetros (programación funcional).
   * Sintaxis compacta para implementar interfaces funcionales.

   ```java
   // Antes (clase anónima)
   Runnable r = new Runnable() {
       @Override
       public void run() {
           System.out.println("Hola Mundo");
       }
   };

   // Con Lambda
   Runnable r = () -> System.out.println("Hola Mundo");
   ```

2. **Interfaces Funcionales y @FunctionalInterface**

   * Interfaces con un solo método abstracto (ej: `Runnable`, `Callable`, `Comparator`).
   * Pueden usarse como **objetivos de lambdas** o referencias a métodos.

3. **Streams API**

   * Procesamiento de colecciones de forma **declarativa** (map, filter, reduce).
   * Permite trabajar en modo **paralelo** (`parallelStream()`).

   ```java
   List<String> nombres = Arrays.asList("Ana", "Pedro", "Juan");
   nombres.stream()
          .filter(n -> n.startsWith("A"))
          .map(String::toUpperCase)
          .forEach(System.out::println);
   ```

4. **Method References**

   * Sintaxis simplificada para llamar métodos existentes en lugar de lambdas.

   ```java
   nombres.forEach(System.out::println);
   ```
   
   * Nuevos métodos como `forEach`, `removeIf`, `replaceAll` en colecciones.

	Ejemplo:  
	```lista.forEach(System.out::println)```.

5. **Default y Static Methods en Interfaces**

   * Permiten agregar métodos a interfaces sin romper compatibilidad con implementaciones existentes.

   ```java
   interface Figura {
       double area();
       default void imprimir() {
           System.out.println("Soy una figura");
       }
   }
   ```

6. **Nueva API de Fechas y Tiempos (java.time)**

   * Inspirada en Joda-Time.
   * Inmutable y segura para hilos.

   ```java
   LocalDate hoy = LocalDate.now();
   LocalDate cumple = LocalDate.of(2025, 9, 24);
   Period diff = Period.between(hoy, cumple);
   ```

---

### ⚡ Mejoras adicionales

* **Optional**: evita el `NullPointerException`.

  ```java
  Optional<String> nombre = Optional.ofNullable(getNombre());
  nombre.ifPresent(System.out::println);
  ```
* **Nashorn JavaScript Engine**: ejecutar JavaScript desde la JVM.
* **Parallel Arrays (Arrays.parallelSort)**: algoritmos paralelos de ordenamiento.
* **Collectors** en `Stream`: para agrupar, reducir o transformar resultados.

  ```java
  Map<Integer, List<String>> agrupados = 
      nombres.stream().collect(Collectors.groupingBy(String::length));
  ```

---

✅ **En resumen:**
Los cambios más importantes fueron **Lambdas, Streams, Optional y la nueva API de fecha/hora**, que introdujeron programación funcional en Java y una forma más fluida de trabajar con colecciones.

<br>
<br>


---




<br>
<br>
<br>




## Más ejemplos de 1, 2 y 3



Ejemplos adicionales y más detallados para los 
puntos 1 (**Expresiones Lambda**), 2 (**API Stream**) y 
3 (**Interfaces Funcionales y `@FunctionalInterface`**).


---

<br>


### 1. Expresiones Lambda
Las expresiones lambda permiten escribir código más conciso al tratar funciones como ciudadanos de primera clase. Se utilizan principalmente con interfaces funcionales y la API Stream.

#### Ejemplo 1: Ordenar una lista con Lambda
```java
import java.util.Arrays;
import java.util.List;

public class LambdaExample {
    public static void main(String[] args) {
        List<String> nombres = Arrays.asList("Juan", "Ana", "Pedro", "Beatriz");

        // Ordenar alfabéticamente usando una expresión lambda
        nombres.sort((a, b) -> a.compareTo(b));

        System.out.println("Nombres ordenados: " + nombres);
        // Salida: [Ana, Beatriz, Juan, Pedro]
    }
}
```
**Explicación**: En lugar de implementar un `Comparator` completo, la lambda `(a, b) -> a.compareTo(b)` define el criterio de ordenación de forma compacta.

#### Ejemplo 2: Crear un hilo con Lambda
```java
public class LambdaThreadExample {
    public static void main(String[] args) {
        // Crear un hilo usando una lambda en lugar de una clase anónima
        Runnable tarea = () -> System.out.println("Hilo ejecutado por: " + Thread.currentThread().getName());
        new Thread(tarea).start();
    }
}
```
**Explicación**: La lambda `() -> System.out.println(...)` reemplaza la necesidad de una clase anónima para implementar la interfaz `Runnable`, simplificando el código.

#### Ejemplo 3: Filtrar y transformar con Lambda
```java
import java.util.Arrays;
import java.util.List;

public class LambdaFilterExample {
    public static void main(String[] args) {
        List<Integer> numeros = Arrays.asList(1, 2, 3, 4, 5);

        // Filtrar números pares y multiplicarlos por 10
        numeros.forEach(n -> {
            if (n % 2 == 0) {
                System.out.println(n * 10);
            }
        });
        // Salida: 20, 40
    }
}
```
**Explicación**: La lambda `n -> { ... }` se usa con `forEach` para procesar cada elemento de la lista, aplicando un filtro y una transformación.

---

<br>

### 2. API Stream
La API Stream permite procesar colecciones de datos de forma declarativa, con operaciones como filtrado, mapeo, reducción, etc. Es ideal para manejar grandes conjuntos de datos.


#### Ejemplo 1: Filtrar y Contar Elementos

```java
import java.util.Arrays;
import java.util.List;

public class StreamCountExample {
    public static void main(String[] args) {
        List<String> palabras = Arrays.asList("sol", "luna", "estrella", "cielo", "nube");

        // Contar palabras con más de 4 letras
        long contador = palabras.stream()
                               .filter(p -> p.length() > 4)
                               .count();

        System.out.println("Palabras con más de 4 letras: " + contador);
        // Salida: 2 (estrella, cielo)
    }
}
```
**Explicación**: El stream filtra las palabras con `filter(p -> p.length() > 4)` y usa `count()` para obtener el número de elementos que cumplen la condición.

#### Ejemplo 2: Transformar y Recolectar

```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamMapExample {
    public static void main(String[] args) {
        List<Integer> numeros = Arrays.asList(1, 2, 3, 4, 5);

        // Duplicar cada número y recolectar en una nueva lista
        List<Integer> duplicados = numeros.stream()
                                         .map(n -> n * 2)
                                         .collect(Collectors.toList());

        System.out.println("Números duplicados: " + duplicados);
        // Salida: [2, 4, 6, 8, 10]
    }
}
```
**Explicación**: La operación `map(n -> n * 2)` transforma cada elemento multiplicándolo por 2, y `collect(Collectors.toList())` convierte el stream en una lista.

#### Ejemplo 3: Reducción para Sumar
```java
import java.util.Arrays;
import java.util.List;

public class StreamReduceExample {
    public static void main(String[] args) {
        List<Integer> numeros = Arrays.asList(1, 2, 3, 4, 5);

        // Sumar todos los números
        int suma = numeros.stream()
                          .reduce(0, (a, b) -> a + b);

        System.out.println("Suma total: " + suma);
        // Salida: 15
    }
}
```
**Explicación**: La operación `reduce(0, (a, b) -> a + b)` acumula los elementos del stream, sumándolos desde un valor inicial de 0.

---

### 3. Interfaces Funcionales y `@FunctionalInterface`
Una interfaz funcional tiene un solo método abstracto y puede usarse con expresiones lambda. La anotación `@FunctionalInterface` asegura que la interfaz cumpla con este requisito.

#### Ejemplo 1: Usar `Predicate` para Filtrar
```java
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PredicateExample {
    public static void main(String[] args) {
        List<Integer> numeros = Arrays.asList(1, 2, 3, 4, 5, 6);

        // Definir un Predicate para números impares
        Predicate<Integer> esImpar = n -> n % 2 != 0;

        // Filtrar números impares
        numeros.stream()
               .filter(esImpar)
               .forEach(System.out::println);
        // Salida: 1, 3, 5
    }
}
```
**Explicación**: La interfaz funcional `Predicate` define el método `test`. La lambda `n -> n % 2 != 0` implementa este método para filtrar números impares.

#### Ejemplo 2: Crear una Interfaz Funcional Personalizada

```java
@FunctionalInterface
interface Operacion {
    int aplicar(int a, int b);
}

public class CustomFunctionalInterface {
    public static void main(String[] args) {
        // Definir una lambda para la interfaz personalizada
        Operacion suma = (a, b) -> a + b;
        Operacion resta = (a, b) -> a - b;

        // Usar las lambdas
        System.out.println("Suma: " + suma.aplicar(5, 3)); // Salida: 8
        System.out.println("Resta: " + resta.aplicar(5, 3)); // Salida: 2
    }
}
```
**Explicación**: La interfaz `Operacion` está anotada con `@FunctionalInterface` y define un solo método abstracto `aplicar`. Las lambdas `(a, b) -> a + b` y `(a, b) -> a - b` implementan esta interfaz.

#### Ejemplo 3: Usar `Consumer` para Imprimir Personalizado

```java
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConsumerExample {
    public static void main(String[] args) {
        List<String> nombres = Arrays.asList("Ana", "Juan", "Pedro");

        // Definir un Consumer para imprimir con formato
        Consumer<String> imprimirConPrefijo = nombre -> System.out.println("Nombre: " + nombre);

        // Aplicar el Consumer a cada elemento
        nombres.forEach(imprimirConPrefijo);
        // Salida:
        // Nombre: Ana
        // Nombre: Juan
        // Nombre: Pedro
    }
}
```
**Explicación**: La interfaz funcional `Consumer` define el método `accept`. La lambda `nombre -> System.out.println("Nombre: " + nombre)` implementa este método para procesar cada elemento.

---

### Resumen
- **Expresiones Lambda**: Simplifican la implementación de interfaces funcionales, como en ordenaciones, hilos o bucles.
- **API Stream**: Permite operaciones funcionales como filtrado (`filter`), transformación (`map`) y reducción (`reduce`) sobre colecciones.
- **Interfaces Funcionales**: Interfaces como `Predicate`, `Consumer` o personalizadas con `@FunctionalInterface` son la base para usar lambdas y streams.



<br>
<br>


---


<br>
<br>
<br>





## Más ejemplos de 4



Explico con más detalle los **métodos por defecto** (`default`) y **métodos estáticos** (`static`) en interfaces, introducidos en Java 8, y proporciono ejemplos prácticos para ilustrar su uso. Estos métodos permiten añadir funcionalidad a interfaces sin romper la compatibilidad con clases que ya las implementan.

---

### Métodos por Defecto (`default`) en Interfaces
Los métodos por defecto permiten definir una implementación predeterminada en una interfaz. Esto es útil para añadir nuevas funcionalidades a interfaces existentes sin obligar a todas las clases que las implementan a proporcionar una implementación.

#### Características:
- Se declaran con la palabra clave `default`.
- Las clases que implementan la interfaz pueden usar la implementación predeterminada o sobrescribirla.
- Resuelven el problema de compatibilidad hacia atrás al extender interfaces.

#### Ejemplo 1: Método por Defecto Simple
```java
interface Vehiculo {
    void conducir();

    // Método por defecto
    default void sonarClaxon() {
        System.out.println("¡Beep beep!");
    }
}

class Coche implements Vehiculo {
    @Override
    public void conducir() {
        System.out.println("Coche conduciendo...");
    }
}

class Moto implements Vehiculo {
    @Override
    public void conducir() {
        System.out.println("Moto conduciendo...");
    }

    // Sobrescribir el método por defecto
    @Override
    public void sonarClaxon() {
        System.out.println("¡Pii pii!");
    }
}

public class DefaultMethodExample {
    public static void main(String[] args) {
        Vehiculo coche = new Coche();
        coche.conducir(); // Salida: Coche conduciendo...
        coche.sonarClaxon(); // Salida: ¡Beep beep!

        Vehiculo moto = new Moto();
        moto.conducir(); // Salida: Moto conduciendo...
        moto.sonarClaxon(); // Salida: ¡Pii pii!
    }
}
```
**Explicación**:
- La interfaz `Vehiculo` define un método abstracto `conducir` y un método por defecto `sonarClaxon`.
- `Coche` usa la implementación predeterminada de `sonarClaxon`.
- `Moto` sobrescribe `sonarClaxon` con su propia implementación.

#### Ejemplo 2: Resolución de Conflictos entre Interfaces
Si una clase implementa varias interfaces con métodos por defecto que tienen el mismo nombre, debe sobrescribir el método para resolver el conflicto.

```java
interface Dispositivo {
    default void encender() {
        System.out.println("Dispositivo encendido genéricamente");
    }
}

interface Telefono {
    default void encender() {
        System.out.println("Teléfono encendido con pantalla táctil");
    }
}

class Smartphone implements Dispositivo, Telefono {
    // Resolver conflicto sobrescribiendo el método
    @Override
    public void encender() {
        // Llamar a la implementación de una interfaz específica si se desea
        Dispositivo.super.encender();
        System.out.println("Smartphone listo");
    }
}

public class DefaultMethodConflictExample {
    public static void main(String[] args) {
        Smartphone smartphone = new Smartphone();
        smartphone.encender();
        // Salida:
        // Dispositivo encendido genéricamente
        // Smartphone listo
    }
}
```
**Explicación**:
- `Smartphone` implementa dos interfaces con métodos por defecto `encender`.
- Debe sobrescribir `encender` para evitar ambigüedad.
- Usa `Dispositivo.super.encender()` para invocar la implementación de `Dispositivo`.

---

### Métodos Estáticos (`static`) en Interfaces
Los métodos estáticos en interfaces permiten definir utilidad o métodos auxiliares directamente en la interfaz, sin necesidad de instanciar una clase. Son similares a los métodos estáticos de clases.

#### Características:
- Se declaran con la palabra clave `static`.
- No pueden ser sobrescritos por clases que implementan la interfaz.
- Se invocan directamente usando el nombre de la interfaz (e.g., `Interfaz.metodoEstatico()`).

#### Ejemplo 1: Método Estático para Crear Instancias
```java
interface Figura {
    void dibujar();

    // Método estático
    static Figura crearCirculo() {
        return new Circulo();
    }
}

class Circulo implements Figura {
    @Override
    public void dibujar() {
        System.out.println("Dibujando un círculo");
    }
}

public class StaticMethodExample {
    public static void main(String[] args) {
        // Crear una instancia usando el método estático
        Figura circulo = Figura.crearCirculo();
        circulo.dibujar(); // Salida: Dibujando un círculo
    }
}
```
**Explicación**:
- El método estático `crearCirculo` en la interfaz `Figura` actúa como una fábrica para crear instancias de `Circulo`.
- Se invoca directamente con `Figura.crearCirculo()`.

#### Ejemplo 2: Método Estático para Utilidad
```java
interface Calculadora {
    int operar(int a, int b);

    // Método estático para utilidad
    static int sumar(int a, int b) {
        return a + b;
    }
}

class Suma implements Calculadora {
    @Override
    public int operar(int a, int b) {
        return a + b;
    }
}

public class StaticMethodUtilityExample {
    public static void main(String[] args) {
        // Usar el método estático directamente
        int resultado = Calculadora.sumar(10, 5);
        System.out.println("Suma: " + resultado); // Salida: Suma: 15

        // Usar una implementación de la interfaz
        Calculadora suma = new Suma();
        System.out.println("Operar: " + suma.operar(10, 5)); // Salida: Operar: 15
    }
}
```

**Explicación**:
- El método estático `sumar` en `Calculadora` proporciona una utilidad que no requiere una instancia.
- Se invoca con `Calculadora.sumar(10, 5)` sin necesidad de crear un objeto.

---

### Resumen
- **Métodos por Defecto**: Añaden funcionalidad a interfaces con una implementación predeterminada, permitiendo flexibilidad para sobrescribirlas. Son útiles para extender interfaces sin romper código existente.
- **Métodos Estáticos**: Proveen utilidades o fábricas asociadas a la interfaz, invocadas directamente sin instanciar.

Si necesitas más ejemplos, explicaciones sobre casos específicos o cómo combinar estos métodos con otras características de Java 8 (como lambdas o streams).