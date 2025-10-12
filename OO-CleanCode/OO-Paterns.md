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
     - 
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
   - 
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

