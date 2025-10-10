# Lambda & Streams



<br>
<br>


# Lambda


Lambda expressions, introduced in Java 8, provide a concise way to implement **functional interfaces** (interfaces with a single abstract method, like `Runnable` or `Comparator`).  
They enable functional programming by treating code as data, making your code more readable and reducing boilerplate compared to anonymous inner classes.

/-------------------------------
#### Basic Syntax
A lambda expression has 3 parts:
- **Parameters** (optional, in parentheses): Can be empty, named, or typed.
- **Arrow** (`->`): Separates parameters from the body.
- **Body**: Either a single expression (implicit return) or a block `{}` with statements.

Examples:
```java
// Simple lambda for Runnable (no parameters)
Runnable hello = () -> System.out.println("Hello, Lambda!");

// Lambda with parameters for a custom functional interface
interface Adder {
    int add(int a, int b);
}
Adder sum = (a, b) -> a + b;  // Returns 5 for sum.add(2, 3)

// Multi-statement body
Comparator<String> lengthComparator = (s1, s2) -> {
    int len1 = s1.length();
    int len2 = s2.length();
    return Integer.compare(len1, len2);
};
```
/-------------------------------


<br>

#### Key Features
- **Type Inference**: Java infers types from context (e.g., method arguments).
- **Method References**: Shorthand for lambdas, like `String::length` instead of `s -> s.length()`.
- **Variable Capture**: Lambdas can access outer variables (effectively final).
- **In Java 21**: Enhanced with pattern matching for switch expressions (though not directly for lambdas, it integrates well in functional contexts).

Lambdas shine in APIs like Collections, Streams, and Optional.



<br>
<br>

---

<br>





# Ejemplos Avanzados

La API de Streams en Java es poderosa para procesar datos de forma declarativa y paralela.

En ejemplos avanzados, exploramos operaciones como `flatMap`, `collectors` personalizados, agrupaciones complejas, reducciones y streams paralelos.

Usaré una clase simple `Persona` para contextualizar los ejemplos (con `nombre`, `edad` y `ciudad`).

Primero, definamos la clase de ejemplo:
```java
import java.util.List;
import java.util.Objects;
import java.util.Arrays;
import java.util.List;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.stream.Collectors;


record Persona(String nombre, int edad, String ciudad) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Persona persona = (Persona) o;
        return edad == persona.edad && Objects.equals(nombre, persona.nombre) && Objects.equals(ciudad, persona.ciudad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, edad, ciudad);
    }
}
```

Y una lista de prueba:
```java
List<Persona> personas = Arrays.asList(
    new Persona("Ana", 25, "Madrid"),
    new Persona("Bob", 30, "Barcelona"),
    new Persona("Carlos", 25, "Madrid"),
    new Persona("Diana", 35, "Valencia"),
    new Persona("Eva", 30, "Barcelona")
);
```

<br>

#### 1. **FlatMap: Aplanar colecciones anidadas**
Útil para "aplanar" estructuras anidadas, como listas dentro de objetos. Supongamos que cada `Persona` tiene una lista de hobbies (agregamos un campo temporal para el ejemplo).

```java
// Supongamos que agregamos hobbies a cada Persona (usando un mapa temporal)
var personasConHobbies = personas.stream()
    .collect(Collectors.toMap(
        p -> p.nombre(),
        p -> p,
        (existing, replacement) -> existing
    ));

// Ejemplo: Obtener todos los hobbies únicos de todas las personas
// (Imaginemos que cada persona tiene hobbies como List<String>)
List<String> todosHobbies = personas.stream()
    .flatMap(p -> Arrays.asList("lectura", "deporte", "cine").stream())  // Hobbies simulados
    .distinct()
    .sorted()
    .collect(Collectors.toList());

// Salida: [cine, deporte, lectura]
System.out.println(todosHobbies);
```

**Explicación**: `flatMap` transforma cada elemento en un stream y los concatena en uno solo, ideal para procesar arrays o listas anidadas sin bucles anidados.

<br>

#### 2. **Agrupación y particionamiento con Collectors**
Agrupa datos por criterios y realiza operaciones agregadas, como conteos o promedios.

```java
Map<Integer, List<Persona>> porEdad = personas.stream()
    .collect(Collectors.groupingBy(Persona::edad));

// Ejemplo: {25=[Ana, Carlos], 30=[Bob, Eva], 35=[Diana]}

Map<String, Long> conteoPorCiudad = personas.stream()
    .collect(Collectors.groupingBy(Persona::ciudad, Collectors.counting()));

// Ejemplo: {Barcelona=2, Madrid=2, Valencia=1}

// Particionamiento (true/false): Mayores de 28 años
Map<Boolean, List<Persona>> particionEdad = personas.stream()
    .collect(Collectors.partitioningBy(p -> p.edad() > 28));

// Ejemplo: {false=[Ana, Bob, Carlos, Eva], true=[Diana]}
```

**Explicación**: `groupingBy` crea mapas con claves dinámicas; combínalo con `counting()`, `summingInt()` o `averagingInt()` para estadísticas. `partitioningBy` es un caso especial para booleanos.

#### 3. **Reducción personalizada con Collectors y Optional**
Realiza reducciones complejas, como encontrar el "mejor" elemento basado en un criterio.

```java
import java.util.Optional;
import java.util.Comparator;

// Encontrar la persona más joven en cada ciudad (usando reduce)
Map<String, Optional<Persona>> masJovenPorCiudad = personas.stream()
    .collect(Collectors.groupingBy(
        Persona::ciudad,
        Collectors.reducing(
            (p1, p2) -> p1.edad() < p2.edad() ? p1 : p2  // Comparador simple
        )
    ));

// Salida ejemplo: {Barcelona=Optional[Bob], Madrid=Optional[Ana], Valencia=Optional[Diana]}

// O usando minBy para más elegancia
Map<String, Optional<Persona>> masJovenElegante = personas.stream()
    .collect(Collectors.groupingBy(
        Persona::ciudad,
        Collectors.minBy(Comparator.comparingInt(Persona::edad))
    ));
```

**Explicación**: `reducing` permite acumuladores personalizados (como en `reduce()`). `minBy` o `maxBy` usan comparadores para selecciones. Devuelve `Optional` para manejar casos vacíos.

#### 4. **Streams paralelos para rendimiento**
En Java 21, con *virtual threads*, los streams paralelos (`parallel()`) escalan bien en multicore. Úsalos con precaución para evitar race conditions.

```java
import java.util.concurrent.TimeUnit;

// Sumar edades en paralelo (simula datos grandes)
long sumaEdadesParalelo = personas.parallelStream()
    .mapToInt(Persona::edad)
    .sum();

// Para medir tiempo (en un conjunto grande)
long inicio = System.nanoTime();
long suma = personas.parallelStream()
    .filter(p -> p.ciudad().startsWith("M"))
    .mapToLong(Persona::edad)
    .sum();
long fin = System.nanoTime();
System.out.printf("Tiempo: %.2f ms%n", (fin - inicio) / 1_000_000.0);

// Nota: En datasets pequeños, el overhead puede ser mayor que el beneficio.
```

**Explicación**: `parallelStream()` divide el trabajo en hilos (usa ForkJoinPool). En Java 21, intégralo con `Executors.newVirtualThreadPerTaskExecutor()` para streams asíncronos. Siempre prueba el rendimiento.

#### 5. **Collector personalizado: Join con prefijo/sufijo**
Crea un collector para formatear salidas complejas.

```java
import java.util.stream.Collector;
import java.util.stream.Collectors;

Collector<CharSequence, StringBuilder, String> joinPersonalizado = Collector.of(
    StringBuilder::new,  // Supplier
    StringBuilder::append,  // Accumulator
    StringBuilder::append,  // Combiner (para paralelo)
    sb -> "[" + sb.toString() + "]"  // Finisher
);

String resultado = personas.stream()
    .map(Persona::nombre)
    .collect(joinPersonalizado);

// Salida: [AnaBobCarlosDianaEva]
```

**Explicación**: `Collector.of` permite definir un recolector desde cero. Útil para lógica no cubierta por collectors estándar como `joining()`.

Estos ejemplos muestran cómo las Streams van más allá de filtros y mapas básicos, habilitando pipelines funcionales eficientes. En Java 21, considera usar records (como en el ejemplo) para datos inmutables. Si quieres profundizar en un ejemplo específico, como integración con reactive streams o testing, ¡dímelo! 🚀

































<br>
<br>

---

<br>

# Streams

Streams, also from Java 8, allow you to process sequences of elements (e.g., from collections) in a declarative, functional way.  
They're lazy (operations aren't executed until a terminal operation is called) and support parallelism for performance.

#### Core Concepts
- **Stream Creation**: From collections, arrays, or generators.
  ```java
  List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
  Stream<String> stream = names.stream();  // Sequential
  Stream<String> parallelStream = names.parallelStream();  // Parallel
  ```
- **Operations**:
  - **Intermediate** (return a new Stream, chainable): `filter`, `map`, `flatMap`, `sorted`, `distinct`, `limit`.
  - **Terminal** (produce a result or side-effect, trigger computation): `collect`, `forEach`, `reduce`, `anyMatch`.  

- **No Modification**: Streams don't alter the source; they're for read-only processing.

<br>

#### Example: Basic Pipeline
Filter even numbers, square them, and collect into a list:
```java
import java.util.*;
import java.util.stream.Collectors;

List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
List<Integer> evensSquared = numbers.stream()
    .filter(n -> n % 2 == 0)          // Intermediate: Keep evens [2, 4]
    .map(n -> n * n)                  // Intermediate: Square [4, 16]
    .collect(Collectors.toList());    // Terminal: [4, 16]

System.out.println(evensSquared);  // Output: [4, 16]
```

<br>

#### Advanced Example: Grouping and Reduction
Group names by length and find the longest:
```java
Map<Integer, List<String>> groupedByLength = names.stream()
    .collect(Collectors.groupingBy(String::length));  // {3=[Bob], 5=[Alice], 7=[Charlie]}

Optional<String> longest = names.stream()
    .reduce((s1, s2) -> s1.length() > s2.length() ? s1 : s2);  // "Charlie"
```

#### Parallel Streams
For large datasets, use `parallelStream()` to leverage multiple cores:
```java
long count = numbers.parallelStream()
    .filter(n -> n > 10)
    .count();  // Faster on multi-core for heavy ops
```
**Caution**: Parallelism isn't always faster; use for CPU-bound tasks, not I/O.

<br>

#### Java 21 Enhancements
- **Virtual Threads** (Project Loom): Streams integrate seamlessly with virtual threads for better concurrency in reactive apps.
- **Pattern Matching**: Improves stream usage in switch expressions for more expressive filtering/mapping.
- **Preview Features**: Scoped values and structured concurrency enhance stream-based async processing.

### Lambdas + Streams: Why They Pair Well
Streams rely heavily on lambdas for operations like `filter` or `map`. This combo promotes immutable, chainable code—ideal for data processing pipelines. For more, check Oracle's Java 21 docs or experiment in an IDE! If you have a specific use case, I can dive deeper.



<br>
<br>

---
<br>


## ejemplos



```java

// Online Java Compiler
// Use this editor to write, compile and run your Java code online
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Comparator;

// -------------------------
public class EmployeeApp {
    public record EmployeeDTO(String name, int age, String department, double salary, boolean isActive) {
    }
    public static void main(String[] args) {
        // Lista de empleados
        List<EmployeeDTO> employees = List.of(
            new EmployeeDTO("Ana", 28, "Engineering", 75000.0, true),
            new EmployeeDTO("Bob", 35, "Sales", 60000.0, false),
            new EmployeeDTO("Carlos", 28, "Engineering", 80000.0, true),
            new EmployeeDTO("Diana", 40, "HR", 65000.0, true),
            new EmployeeDTO("Eva", 30, "Sales", 62000.0, true)
        );
        // Pregunta 1: ¿Cuáles son los nombres de los empleados activos en el departamento de "Engineering"?
        List<String> activeEngineers = employees
          .stream()
          .filter(e -> e.isActive() && e.department().equals("Engineering"))
          .map(EmployeeDTO::name)
          .toList();
        System.out.println("Ingenieros activos: " + activeEngineers);

        //Pregunta 2: ¿Cuál es el salario promedio de los empleados mayores de 30 años?
        Double ave = employees.stream()
            .filter(e-> e.age() > 30)
            .mapToDouble(EmployeeDTO::salary)
            .average()
            .orElse(0.0);
        System.out.printf("Salario promedio (>30 años): %.2f%n", ave);

        //Pregunta 3: Agrupa los empleados por departamento y cuenta cuántos hay en cada uno.
        Map<String, Long> empByDept = employees
          .stream()
          .collect(Collectors.groupingBy(EmployeeDTO::department, Collectors.counting()));
        System.out.println("Conteo por departamento: " + empByDept);

        // Pregunta 4: Encuentra al empleado con el salario más alto en "Sales".
        Optional<EmployeeDTO> highestPaidSales = employees.stream()
          .filter(e-> e.department().equals("Sales"))
          .max(Comparator.comparingDouble(EmployeeDTO::salary));
        highestPaidSales.ifPresent(emp-> System.out.println("Mayor salario en Sales: " + emp));

        // Pregunta 5: Convierte los nombres de los empleados activos en una cadena separada por comas.
        String names = employees.stream()
            .filter(e-> e.isActive())
            .map(EmployeeDTO::name)
            .collect(Collectors.joining(", "));
        System.out.println("Nombres activos: " + names);
    }
}

// -------------------------
record Persona(String nombre, int edad, String ciudad) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Persona persona = (Persona) o;
        return edad == persona.edad && Objects.equals(nombre, persona.nombre) && Objects.equals(ciudad, persona.ciudad);
    }
    @Override
    public int hashCode() {
        return Objects.hash(nombre, edad, ciudad);
    }
}
// -------------------------
/*
class Main {

    public static void main(String[] args) {
        System.out.println("Try programiz.pro");
        List<Persona> personas = Arrays.asList(
            new Persona("Ana", 25, "Madrid"),
            new Persona("Bob", 30, "Barcelona"),
            new Persona("Carlos", 25, "Madrid"),
            new Persona("Diana", 35, "Valencia"),
            new Persona("Eva", 30, "Barcelona")
        );

        Map<Integer, List<Persona>> porEdad = personas.stream()
            .collect(Collectors.groupingBy(Persona::edad));
        System.out.println(porEdad);

        // Ejemplo: {25=[Ana, Carlos], 30=[Bob, Eva], 35=[Diana]}

        Map<String, Long> conteoPorCiudad = personas.stream()
            .collect(Collectors.groupingBy(Persona::ciudad, Collectors.counting()));
        System.out.println(conteoPorCiudad);
        // Ejemplo: {Barcelona=2, Madrid=2, Valencia=1}

        // Particionamiento (true/false): Mayores de 28 años
        Map<Boolean, List<Persona>> particionEdad = personas.stream()
            .collect(Collectors.partitioningBy(p -> p.edad() > 28));
        System.out.println(particionEdad);

    }
}*/
```

<br>

<br>
<br>


---

<br>
<br>





### Operaciones de Streams en Java 21 con Lambdas: Todas las Posibilidades Explicadas

En Java 21, la **API de Streams** es una herramienta poderosa para procesar colecciones de datos de forma funcional, usando **lambdas** para definir comportamientos.  

Usaré tu DTO `EmployeeDTO` para mostrar **todas las operaciones de Streams** permitidas, organizadas en categorías (intermedias, terminales y especializadas), con ejemplos prácticos que cubren las variaciones de sintaxis de lambdas vistas anteriormente (básica, bloque, sin parámetros, un parámetro, con `var`, referencias a métodos).  

Cada ejemplo incluye una explicación, código, salida y esquema visual, asegurando claridad y relevancia con tu contexto de `EmployeeDTO`.

---

#### **Contexto: Lista de Ejemplo**
Usamos el mismo DTO y lista:

```java
import java.util.List;
import java.util.stream.Collectors;

public record EmployeeDTO(String name, int age, String department, double salary, boolean isActive) {}
```

```java
import java.util.List;

List<EmployeeDTO> employees = List.of(
    new EmployeeDTO("Ana", 28, "Engineering", 75000.0, true),
    new EmployeeDTO("Bob", 35, "Sales", 60000.0, false),
    new EmployeeDTO("Carlos", 28, "Engineering", 80000.0, true),
    new EmployeeDTO("Diana", 40, "HR", 65000.0, true),
    new EmployeeDTO("Eva", 30, "Sales", 62000.0, true)
);
```

---

### **Tipos de Operaciones en Streams**
Las operaciones de Streams se dividen en:

1. **Intermedias**: Transforman o filtran el flujo, retornando otro `Stream` (o `DoubleStream`, `IntStream`, etc.). Son "lazy" (no se ejecutan hasta que una operación terminal las activa).
2. **Terminales**: Producen un resultado final (e.g., lista, número, mapa) o un efecto secundario (e.g., imprimir). Ejecutan el pipeline.
3. **Especializadas**: Operaciones en streams primitivos (`IntStream`, `LongStream`, `DoubleStream`) o paralelos.

A continuación, cubro **todas** las operaciones permitidas, agrupadas por categoría, con ejemplos que usan diferentes sintaxis de lambdas y referencias a métodos.


---


### **1. Operaciones Intermedias**

Estas operaciones transforman el flujo sin producir un resultado final:

- filter
- max
- flatMap
- mapToInt / ToLong / ToDouble / ToInt functions
- sorted
- peek
- distinct
- limit / skip
- takeWhile / dropWhile



#### **1.1 filter(Predicate<T>)**

- **Qué hace**: Filtra elementos según un `Predicate` (condición booleana).
- **Ejemplo**: Obtener empleados activos con salario > 65000, usando lambda básica.

```java
List<EmployeeDTO> highPaidActive = employees.stream()
    .filter(emp -> emp.isActive() && emp.salary() > 65000.0)  // Lambda básica
    .collect(Collectors.toList());  // Terminal (explicada más abajo)

System.out.println("Activos con salario > 65000: " + highPaidActive);
// Salida: [EmployeeDTO[name=Ana, ...], EmployeeDTO[name=Carlos, ...]]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → filter(emp -> emp.isActive() && emp.salary() > 65000.0) → [Ana, Carlos]
  ```

#### **1.2 map(Function<T, R>)**

- **Qué hace**: Transforma cada elemento en otro usando una función.
- **Ejemplo**: Mapear a nombres en mayúsculas, usando referencia a método.

```java
List<String> upperNames = employees.stream()
    .map(EmployeeDTO::name)  // Referencia: emp -> emp.name()
    .map(String::toUpperCase)  // Referencia: s -> s.toUpperCase()
    .toList();

System.out.println("Nombres en mayúsculas: " + upperNames);
// Salida: [ANA, BOB, CARLOS, DIANA, EVA]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → map(EmployeeDTO::name) → [Ana, Bob, ...]
                      → map(String::toUpperCase) → [ANA, BOB, ...]
  ```

#### **1.3 flatMap(Function<T, Stream<R>>)**

- **Qué hace**: Aplana una estructura anidada (e.g., listas dentro de listas) en un solo flujo.
- **Ejemplo**: Aplanar una lista simulada de "hobbies" por empleado, usando bloque.

```java
import java.util.Arrays;

List<String> allHobbies = employees.stream()
    .flatMap(emp -> {  // Bloque
        List<String> hobbies = emp.isActive() ? Arrays.asList("lectura", "deporte") : List.of();
        return hobbies.stream();
    })
    .distinct()
    .toList();

System.out.println("Hobbies únicos: " + allHobbies);
// Salida: [lectura, deporte]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → flatMap(emp -> { return hobbies.stream(); }) → [lectura, deporte, ...]
                      → distinct() → [lectura, deporte]
  ```

#### **1.4 mapToInt/ToLong/ToDouble/ToInt Function, etc.**

- **Qué hace**: Convierte a un stream primitivo (`IntStream`, `LongStream`, `DoubleStream`).
- **Ejemplo**: Extraer salarios como `DoubleStream`, usando `var`.

```java
double totalSalary = employees.stream()
    .mapToDouble(var emp -> emp.salary())  // var en lambda
    .sum();  // Terminal

System.out.printf("Suma de salarios: %.2f%n", totalSalary);
// Salida: Suma de salarios: 332000.00
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → mapToDouble(var emp -> emp.salary()) → [75000.0, 60000.0, ...]
  ```

#### **1.5 sorted(Comparator<T>)**
- **Qué hace**: Ordena el flujo según un comparador.
- **Ejemplo**: Ordenar por salario descendente, usando lambda con bloque.

```java
List<EmployeeDTO> sortedBySalary = employees.stream()
    .sorted((var e1, var e2) -> {  // Lambda con var
        return Double.compare(e2.salary(), e1.salary()); // Descendente
    })
    .toList();

System.out.println("Ordenados por salario: " + sortedBySalary);
// Salida: [Carlos (80000.0), Ana (75000.0), Diana (65000.0), Eva (62000.0), Bob (60000.0)]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → sorted((e1, e2) -> Double.compare(...)) → [Carlos, Ana, ...]
  ```

#### **1.6 peek(Consumer<T>)**

- **Qué hace**: Ejecuta un efecto secundario (e.g., logging) sin modificar el flujo.
- **Ejemplo**: Imprimir nombres durante el procesamiento, usando sin paréntesis.

```java
List<String> activeNames = employees.stream()
    .filter(emp -> emp.isActive())
    .peek(emp -> System.out.println("Procesando: " + emp.name()))  // Sin paréntesis
    .map(EmployeeDTO::name)
    .toList();

// Salida:
// Procesando: Ana
// Procesando: Carlos
// Procesando: Diana
// Procesando: Eva
// [Ana, Carlos, Diana, Eva]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → peek(emp -> System.out.println(...)) → [Ana, Carlos, ...]
  ```

#### **1.7 distinct()**
- **Qué hace**: Elimina duplicados (basado en `equals`).
- **Ejemplo**: Departamentos únicos.

```java
List<String> uniqueDepts = employees.stream()
    .map(EmployeeDTO::department)
    .distinct()
    .toList();

System.out.println("Departamentos únicos: " + uniqueDepts);
// Salida: [Engineering, Sales, HR]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → map(EmployeeDTO::department) → [Engineering, Sales, ...]
                      → distinct() → [Engineering, Sales, HR]
  ```

#### **1.8 limit(long maxSize) / skip(long n)**

- **Qué hace**: Limita el número de elementos o salta los primeros.
- **Ejemplo**: Primeros 2 empleados activos.

```java
List<EmployeeDTO> firstTwoActive = employees.stream()
    .filter(emp -> emp.isActive())
    .limit(2)
    .toList();

System.out.println("Primeros 2 activos: " + firstTwoActive);
// Salida: [Ana, Carlos]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → filter(emp -> emp.isActive()) → [Ana, Carlos, Diana, Eva]
                      → limit(2) → [Ana, Carlos]
  ```

#### **1.9 takeWhile(Predicate<T>) / dropWhile(Predicate<T>)**

- **Qué hace**: Toma o descarta elementos mientras se cumpla una condición (Java 9+).
- **Ejemplo**: Tomar empleados hasta encontrar uno inactivo.

```java
List<EmployeeDTO> untilInactive = employees.stream()
    .takeWhile(emp -> emp.isActive())  // Para al encontrar el primero inactivo
    .toList();

System.out.println("Hasta el primer inactivo: " + untilInactive);
// Salida: [Ana] (para en Bob, que es inactivo)
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → takeWhile(emp -> emp.isActive()) → [Ana]
  ```


---

### **2. Operaciones Terminales**

Estas operaciones cierran el flujo y producen un resultado o efecto.

#### **2.1 collect(Collector)**

- **Qué hace**: Recolecta elementos en una estructura (e.g., lista, mapa).
- **Ejemplo**: Agrupar por departamento y estado, usando múltiples lambdas (como en tus ejemplos previos).

```java
import java.util.Map;
import java.util.stream.Collectors;

Map<String, Map<Boolean, Long>> deptAndActiveCount = employees.stream()
    .collect(Collectors.groupingBy(
        EmployeeDTO::department,
        Collectors.groupingBy(
            EmployeeDTO::isActive,
            Collectors.counting()
        )
    ));

System.out.println("Conteo por departamento y estado: " + deptAndActiveCount);
// Salida: {Engineering={true=2, false=0}, Sales={true=1, false=1}, HR={true=1, false=0}}
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → groupingBy(EmployeeDTO::department, groupingBy(EmployeeDTO::isActive, counting()))
                      → {Engineering={true=2, false=0}, ...}
  ```

#### **2.2 forEach(Consumer<T>)**

- **Qué hace**: Aplica un efecto secundario a cada elemento.
- **Ejemplo**: Imprimir nombres con lambda sin parámetros en un contexto simulado.

```java
employees.stream()
    .filter(emp -> emp.salary() > 60000.0)
    .forEach(emp -> System.out.println(() -> "Nombre: " + emp.name()));  // Lambda anidada (poco común)

System.out.println("Nombres con salario > 60000: " + namesAbove60000);
// Salida:
// Nombre: Ana
// Nombre: Carlos
// Nombre: Diana
// Nombre: Eva
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → filter(emp -> emp.salary() > 60000.0) → [Ana, Carlos, Diana, Eva]
                      → forEach(emp -> System.out.println(...))
  ```

#### **2.3 reduce(BinaryOperator<T>)**

- **Qué hace**: Combina elementos en un solo valor.
- **Ejemplo**: Encontrar el empleado con mayor salario, usando lambda con `var`.

```java
import java.util.Optional;

Optional<EmployeeDTO> highestPaid = employees.stream()
    .reduce((var e1, var e2) -> e1.salary() > e2.salary() ? e1 : e2);

highestPaid.ifPresent(emp -> System.out.println("Mayor salario: " + emp.name()));
// Salida: Mayor salario: Carlos
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → reduce((e1, e2) -> ...) → Optional[Carlos]
  ```

#### **2.4 min/max(Comparator<T>)**

- **Qué hace**: Encuentra el elemento mínimo/máximo según un comparador.
- **Ejemplo**: Empleado más joven, usando referencia a método.

```java
import java.util.Comparator;

Optional<EmployeeDTO> youngest = employees.stream()
    .min(Comparator.comparingInt(EmployeeDTO::age));

youngest.ifPresent(emp -> System.out.println("Más joven: " + emp.name()));
// Salida: Más joven: Ana

```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → min(Comparator.comparingInt(EmployeeDTO::age)) → Optional[Ana]
  ```

#### **2.5 count()**

- **Qué hace**: Cuenta los elementos en el flujo.
- **Ejemplo**: Contar empleados activos.

```java
long activeCount = employees.stream()
    .filter(emp -> emp.isActive())
    .count();

System.out.println("Empleados activos: " + activeCount);
// Salida: Empleados activos: 4

```
- **Esquema**:

  ```
  Stream<EmployeeDTO> → filter(emp -> emp.isActive()) → [Ana, Carlos, Diana, Eva]
                      → count() → 4
  ```

#### **2.6 anyMatch/allMatch/noneMatch(Predicate<T>)**

- **Qué hace**: Verifica si algún/todos/ningún elemento cumple una condición.
- **Ejemplo**: ¿Todos los empleados activos tienen salario > 50000?

```java
boolean allActiveHighSalary = employees.stream()
    .filter(emp -> emp.isActive())
    .allMatch(emp -> emp.salary() > 50000.0);

System.out.println("¿Todos los activos tienen salario > 50000?: " + allActiveHighSalary);
// Salida: true
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → filter(emp -> emp.isActive()) → [Ana, Carlos, Diana, Eva]
                      → allMatch(emp -> emp.salary() > 50000.0) → true
  ```

#### **2.7 findFirst/findAny()**

- **Qué hace**: Devuelve el primer elemento o uno arbitrario (útil en streams paralelos).
- **Ejemplo**: Primer empleado en Engineering.

```java
Optional<EmployeeDTO> firstEngineer = employees.stream()
    .filter(emp -> emp.department().equals("Engineering"))
    .findFirst();

firstEngineer.ifPresent(emp -> System.out.println("Primer ingeniero: " + emp.name()));
// Salida: Primer ingeniero: Ana
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → filter(emp -> emp.department().equals("Engineering")) → [Ana, Carlos]
                      → findFirst() → Optional[Ana]
  ```

---

### **3. Operaciones Especializadas**

Estas aplican a streams primitivos (`IntStream`, `LongStream`, `DoubleStream`) o streams paralelos.

#### **3.1 Primitivas: sum(), average(), summaryStatistics()**

- **Qué hace**: Operaciones numéricas en `IntStream`, `LongStream`, `DoubleStream`.
- **Ejemplo**: Estadísticas de salarios.

```java
import java.util.DoubleSummaryStatistics;

DoubleSummaryStatistics stats = employees.stream()
    .mapToDouble(EmployeeDTO::salary)
    .summaryStatistics();

System.out.printf("Estadísticas: Promedio=%.2f, Mín=%.2f, Máx=%.2f%n",
    stats.getAverage(), stats.getMin(), stats.getMax());
// Salida: Estadísticas: Promedio=66400.00, Mín=60000.00, Máx=80000.00
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → mapToDouble(EmployeeDTO::salary) → [75000.0, 60000.0, ...]
                      → summaryStatistics() → {avg=66400.0, min=60000.0, max=80000.0}
  ```

#### **3.2 parallelStream()**

- **Qué hace**: Procesa el flujo en paralelo usando hilos (en Java 21, optimizado con hilos virtuales).
- **Ejemplo**: Sumar salarios en paralelo con hilos virtuales.

```java
import java.util.concurrent.Executors;

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        double parallelSum = employees.parallelStream()
            .filter(var emp -> emp.isActive())  // var en lambda
            .mapToDouble(EmployeeDTO::salary)
            .sum();
        System.out.printf("Suma salarios activos (paralelo): %.2f%n", parallelSum);
    }).get();
// Salida: Suma salarios activos (paralelo): 252000.00
}
```

- **Esquema**:

  ```
  parallelStream() → filter(var emp -> emp.isActive()) → [Ana, Carlos, Diana, Eva]
                   → mapToDouble(EmployeeDTO::salary) → [75000.0, 80000.0, ...]
                   → sum() → 252000.0
  ```

---

### **4. Manejo de Excepciones en Streams**

Lambdas en Streams no permiten `throws`, así que manejamos excepciones localmente.

**Ejemplo**: Filtrar con validación que puede lanzar excepción.

```java
List<String> safeNames = employees.stream()
    .map(emp -> {
        try {
            if (emp.salary() < 0) throw new IllegalArgumentException("Salario inválido");
            return emp.name();
        } catch (IllegalArgumentException e) {
            return "Error: " + emp.name();
        }
    })
    .toList();

System.out.println("Nombres seguros: " + safeNames);
// Salida: [Ana, Bob, Carlos, Diana, Eva]
```

- **Esquema**:

  ```
  Stream<EmployeeDTO> → map(emp -> { try { ... } catch { ... } }) → [Ana, Bob, ...]
  ```

---

### **5. Resumen de Operaciones**

| **Categoría** | **Operaciones** | **Ejemplo de Lambda** | **Uso** |
|---------------|-----------------|-----------------------|---------|
| **Intermedias** | `filter`, `map`, `flatMap`, `mapToInt/Long/Double`, `sorted`, `peek`, `distinct`, `limit`, `skip`, `takeWhile`, `dropWhile` | `emp -> emp.isActive()`, `EmployeeDTO::name` | Transformar/filtrar flujo |
| **Terminales** | `collect`, `forEach`, `reduce`, `min/max`, `count`, `anyMatch/allMatch/noneMatch`, `findFirst/findAny` | `(e1, e2) -> e1.salary() > e2.salary() ? e1 : e2` | Producir resultado |
| **Especializadas** | `sum`, `average`, `summaryStatistics` (en `IntStream`, etc.), `parallelStream` | `var emp -> emp.salary()` | Operaciones numéricas o paralelas |

---

### **6. Consejos y Notas (Java 21)**

- **Sintaxis de Lambdas**: Usa referencias a métodos (`EmployeeDTO::name`) para simplicidad, `var` para legibilidad, y bloques para lógica compleja.
- **Java 21**: Aprovecha `toList()` (inmutable), hilos virtuales para `parallelStream`, y pattern matching en excepciones.
- **Rendimiento**: Usa `parallelStream` solo con datos grandes; prueba siempre.
- **Excepciones**: Maneja dentro de lambdas o usa métodos auxiliares para mantener Streams limpios.

Si necesitas un ejemplo completo en Spring Boot, más casos con excepciones, o enfocarte en una operación específica (e.g., `flatMap` avanzado), ¡dímelo! 🚀

---

¡Ahora deberías poder copiarlo fácilmente seleccionando el texto. Si el problema persiste, prueba recargar la página o usar un navegador diferente. ¿Quieres que lo envíe en un formato diferente, como un archivo de texto? 😊
