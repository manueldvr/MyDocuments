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
