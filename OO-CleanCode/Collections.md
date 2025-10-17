# colecciones


En Java 21, el framework de colecciones (`java.util`) es una parte fundamental de la API estándar que proporciona estructuras de datos para almacenar, manipular y procesar conjuntos de elementos. 



### Resumen de las colecciones en Java 21

El framework de colecciones se basa en la interfaz `Collection` y se extiende a otras interfaces más específicas como `List`, `Set`, `Queue`, y `Map` (aunque `Map` no extiende `Collection`). Estas interfaces son implementadas por clases concretas que ofrecen diferentes comportamientos según el caso de uso.


#### 1. Interfaces principales

- **`Collection`**: Interfaz raíz para la mayoría de las colecciones (excepto `Map`). Define operaciones básicas como añadir, eliminar y consultar elementos.
- **`List`**: Colección ordenada que permite duplicados. Ejemplos: `ArrayList`, `LinkedList`.
- **`Set`**: Colección sin duplicados. Ejemplos: `HashSet`, `LinkedHashSet`, `TreeSet`.
- **`Queue`**: Diseñada para procesar elementos en un orden específico (FIFO, LIFO, etc.). Ejemplo: `LinkedList`, `PriorityQueue`.
- **`Map`**: Almacena pares clave-valor, sin duplicados en las claves. Ejemplos: `HashMap`, `LinkedHashMap`, `TreeMap`.

#### 2. Clases principales

| Clase             | Interfaz       | Características                                                                 |
|-------------------|----------------|---------------------------------------------------------------------------------|
| `ArrayList`       | `List`         | Lista dinámica basada en un arreglo, rápida para accesos aleatorios.             |
| `LinkedList`      | `List`, `Queue`| Lista doblemente enlazada, eficiente para inserciones/eliminaciones frecuentes.  |
| `HashSet`         | `Set`          | Conjunto sin orden, sin duplicados, basado en tabla hash.                        |
| `LinkedHashSet`   | `Set`          | Conjunto con orden de inserción, sin duplicados.                                |
| `TreeSet`         | `Set`          | Conjunto ordenado, basado en un árbol binario balanceado.                        |
| `HashMap`         | `Map`          | Mapa basado en tabla hash, sin orden garantizado.                                |
| `LinkedHashMap`   | `Map`          | Mapa que mantiene el orden de inserción.                                        |
| `TreeMap`         | `Map`          | Mapa ordenado por claves, basado en un árbol binario balanceado.                 |
| `PriorityQueue`   | `Queue`        | Cola que ordena elementos según prioridad (natural o personalizada).            |


##### Comparación    

| Característica     | `Map`                | `HashMap`                   | `Set`            | `HashSet`        |
| ------------------ | -------------------- | --------------------------- | ---------------- | ---------------- |
| Tipo               | Interfaz             | Clase                       | Interfaz         | Clase            |
| Estructura         | Pares clave-valor    | Pares clave-valor           | Elementos únicos | Elementos únicos |
| Permite duplicados | Claves: ❌ Valores: ✅ | Claves: ❌ Valores: ✅        | ❌                | ❌                |
| Permite `null`     | Depende              | ✅ (1 clave, varios valores) | Depende          | ✅ (1 valor)      |
| Orden garantizado  | ❌                    | ❌                           | ❌                | ❌                |
| Basado en          | —                    | Hash table                  | —                | Hash table       |
| Thread-safe        | ❌                    | ❌                           | ❌                | ❌                |



#### 3. **Novedades en Java 21**
Java 21 introduce mejoras como **secuencias** (sequenced collections), que proporcionan una API unificada para colecciones con un orden definido (como `List`, `LinkedHashSet`, `LinkedHashMap`). Estas colecciones implementan la interfaz `SequencedCollection` (o `SequencedMap` para mapas), que ofrece métodos como:
- `getFirst()` / `getLast()`: Obtener el primer/último elemento.
- `addFirst()` / `addLast()`: Añadir elementos al inicio/fin (en colecciones que lo soporten).
- `reversed()`: Obtener una vista inversa de la colección.

Por ejemplo, en Java 21, puedes usar `List.of(...).reversed()` para obtener una lista en orden inverso de manera sencilla.

#### 4. **Cuándo usar cada colección**
- **ArrayList**: Ideal para listas de acceso aleatorio rápido, pero menos eficiente para inserciones/eliminaciones frecuentes.
- **LinkedList**: Útil para listas con muchas inserciones/eliminaciones en los extremos.
- **HashSet**: Para conjuntos donde no importa el orden y necesitas unicidad.
- **TreeSet**: Cuando necesitas un conjunto ordenado.
- **HashMap**: Para asociaciones clave-valor sin necesidad de orden.
- **TreeMap**: Cuando las claves deben estar ordenadas.
- **PriorityQueue**: Para colas con prioridad (por ejemplo, tareas ordenadas por importancia).

### Ejemplo práctico
A continuación, un ejemplo en Java 21 que demuestra el uso de `ArrayList`, `HashSet`, y `HashMap`, incluyendo un método de la nueva API de colecciones secuenciadas.

```java
import java.util.*;

public class ColeccionesEjemplo {
    public static void main(String[] args) {
        // Ejemplo con ArrayList (List)
        List<String> nombres = new ArrayList<>();
        nombres.add("Ana");
        nombres.add("Bob");
        nombres.add("Ana"); // Permite duplicados
        System.out.println("Lista: " + nombres);
        // Usando API de colecciones secuenciadas (Java 21)
        System.out.println("Lista inversa: " + nombres.reversed());

        // Ejemplo con HashSet (Set)
        Set<Integer> numeros = new HashSet<>();
        numeros.add(10);
        numeros.add(20);
        numeros.add(10); // No se agrega, elimina duplicados
        System.out.println("Conjunto: " + numeros);

        // Ejemplo con HashMap (Map)
        Map<String, Integer> edades = new HashMap<>();
        edades.put("Ana", 25);
        edades.put("Bob", 30);
        edades.put("Clara", 28);
        System.out.println("Mapa: " + edades);
        // Acceder a un valor
        System.out.println("Edad de Ana: " + edades.get("Ana"));

        // Ejemplo con PriorityQueue (Queue)
        Queue<Integer> colaPrioridad = new PriorityQueue<>();
        colaPrioridad.add(30);
        colaPrioridad.add(10);
        colaPrioridad.add(20);
        System.out.println("Cola de prioridad (menor a mayor):");
        while (!colaPrioridad.isEmpty()) {
            System.out.println(colaPrioridad.poll());
        }
    }
}
```

### Salida esperada
```
Lista: [Ana, Bob, Ana]
Lista inversa: [Ana, Bob, Ana]
Conjunto: [20, 10]
Mapa: {Ana=25, Bob=30, Clara=28}
Edad de Ana: 25
Cola de prioridad (menor a mayor):
10
20
30
```

### Explicación del ejemplo
1. **ArrayList**: Almacena una lista de nombres, permite duplicados y muestra cómo usar `reversed()` (novedad de Java 21).
2. **HashSet**: Almacena números sin duplicados, mostrando que el segundo `10` no se agrega.
3. **HashMap**: Asocia nombres con edades, permitiendo acceso rápido por clave.
4. **PriorityQueue**: Ordena los elementos automáticamente (menor a mayor) y los extrae con `poll()`.

<br>

<br>

### Notas adicionales
- **Sincronización**: Las colecciones estándar no son thread-safe. Para entornos concurrentes, usa clases como `ConcurrentHashMap` o `Collections.synchronizedList`.
- **Inmutabilidad**: Puedes crear colecciones inmutables con métodos como `List.of()`, `Set.of()`, o `Map.of()` en Java 21.
- **Performance**: La elección de la colección depende del caso de uso. Por ejemplo, `ArrayList` es más rápido para accesos aleatorios, mientras que `LinkedList` es mejor para modificaciones frecuentes.





<br>

<br>



---

<br>

<br>






# HashMap - 10 preguntas

<br>


### **1. ¿Qué es un `HashMap` en Java y cuáles son sus características principales?**
**Respuesta**:  
Un `HashMap` es una implementación de la interfaz `Map` que almacena pares clave-valor en una tabla hash. Sus características principales son:  
- No mantiene orden de inserción.  
- Permite una clave `null` y múltiples valores `null`.  
- Ofrece operaciones de tiempo constante promedio (O(1)) para `put`, `get`, y `remove`, asumiendo una buena función `hashCode`.  
- No es thread-safe; para concurrencia, se usa `ConcurrentHashMap`.  
- Usa un arreglo de buckets, con listas enlazadas o árboles (desde Java 8) para manejar colisiones.

---

### **2. ¿Cómo funciona internamente un `HashMap`? Explica el proceso de almacenamiento y recuperación de datos.**  
**Respuesta**:  
`HashMap` usa una tabla hash con un arreglo de buckets. Para almacenar un par clave-valor:  
1. La clave se pasa por `hashCode()` para calcular un índice.  
2. El índice determina el bucket donde se almacenará el par.  
3. Si hay colisión (mismo índice), se usa una lista enlazada o un árbol (si hay muchas colisiones).  
Para recuperar un valor, se calcula el índice con `hashCode()` y se busca la clave en el bucket usando `equals()`. En Java 21, la implementación sigue optimizando colisiones con árboles binarios balanceados (desde Java 8) para casos de alta densidad.

---

### **3. ¿Qué sucede si dos objetos tienen el mismo `hashCode()` en un `HashMap`?**  
**Respuesta**:  
Si dos objetos tienen el mismo `hashCode()`, se produce una colisión y ambos se almacenan en el mismo bucket. `HashMap` usa `equals()` para distinguir las claves. Si `equals()` devuelve `true`, se considera la misma clave y el valor se sobrescribe; si no, se almacenan como entradas separadas en el bucket (como lista enlazada o árbol). Una mala implementación de `hashCode()` puede causar muchas colisiones, degradando el rendimiento a O(n) o O(log n).

---

### **4. ¿Qué métodos de `HashMap` son particularmente útiles en Java 21 para operaciones avanzadas? Da un ejemplo.**  
**Respuesta**:  
Métodos como `computeIfAbsent`, `merge`, y `getOrDefault` son muy útiles. Por ejemplo, `merge` es ideal para combinar valores:  
```java
Map<String, Integer> conteo = new HashMap<>();
String[] palabras = {"java", "es", "java"};
for (String palabra : palabras) {
    conteo.merge(palabra, 1, Integer::sum);
}
System.out.println(conteo); // {java=2, es=1}
```  
`merge` añade un valor inicial (1) si la clave no existe, o combina el valor existente con el nuevo usando la función proporcionada (`Integer::sum`).

---

### **5. ¿Es posible usar un `HashMap` en un entorno concurrente? ¿Cómo lo harías thread-safe?**  
**Respuesta**:  
`HashMap` no es thread-safe, por lo que no es adecuado para entornos concurrentes sin sincronización. Para hacerlo thread-safe:  
- Usar `Collections.synchronizedMap(new HashMap<>())`, que sincroniza todas las operaciones, pero puede ser lento por el bloqueo completo.  
- Preferir `ConcurrentHashMap`, que usa bloqueos segmentados para mejor rendimiento y ofrece métodos atómicos como `putIfAbsent`.  
Ejemplo con `ConcurrentHashMap`:  
```java
ConcurrentHashMap<String, Integer> mapa = new ConcurrentHashMap<>();
mapa.putIfAbsent("Ana", 25); // Operación atómica
```

---

### **6. ¿Cómo se relaciona `HashMap` con la interfaz `SequencedMap` en Java 21?**  
**Respuesta**:  
En Java 21, la interfaz `SequencedMap` (parte de las *sequenced collections*) define métodos para mapas con orden definido, como `LinkedHashMap`. Sin embargo, `HashMap` no implementa `SequencedMap` porque no garantiza un orden específico de sus entradas. En cambio, `LinkedHashMap` y `TreeMap` sí implementan `SequencedMap`, ofreciendo métodos como `firstEntry()`, `lastEntry()`, y `reversed()`. Si necesitas estas funcionalidades, usa `LinkedHashMap` en lugar de `HashMap`.

---

### **7. ¿Cómo puedes crear un `HashMap` inmutable en Java 21? ¿Qué ventajas tiene?**  
**Respuesta**:  
En Java 21, puedes crear un `HashMap` inmutable usando:  
- `Map.of()` para mapas pequeños: `Map<String, Integer> mapa = Map.of("Ana", 25, "Bob", 30);`  
- `Map.copyOf()` para copiar un `HashMap` existente: `Map.copyOf(new HashMap<>(mapa))`.  
- `Collections.unmodifiableMap()` para envolver un `HashMap` existente.  
Ventajas:  
- Evita modificaciones accidentales, mejorando la seguridad.  
- Útil para configuraciones estáticas o datos compartidos.  
- Más ligero que un `HashMap` mutable en algunos casos.  
Desventaja: No permite modificaciones dinámicas.

---

### **8. ¿Qué es el factor de carga en un `HashMap` y cómo afecta el rendimiento?**  
**Respuesta**:  
El factor de carga es la proporción de entradas respecto a la capacidad del `HashMap` antes de redimensionar (por defecto 0.75). Un factor de carga bajo (ej. 0.5) reduce colisiones, pero usa más memoria porque el mapa se redimensiona antes. Un factor de carga alto (ej. 1.0) ahorra memoria, pero aumenta colisiones, degradando el rendimiento. Ejemplo:  
```java
HashMap<String, Integer> mapa = new HashMap<>(16, 0.5f); // Redimensiona al alcanzar 8 entradas
```  
Elegir un factor de carga depende del balance entre memoria y rendimiento.

---

### **9. ¿Cómo iterarías sobre un `HashMap` para procesar sus entradas? Explica las opciones disponibles.**  
**Respuesta**:  
Hay tres formas principales de iterar un `HashMap`:  
- `keySet()`: Itera solo las claves.  
  ```java
  for (String key : mapa.keySet()) { System.out.println(key); }
  ```  
- `values()`: Itera solo los valores.  
  ```java
  for (Integer value : mapa.values()) { System.out.println(value); }
  ```  
- `entrySet()`: Itera pares clave-valor, más eficiente si necesitas ambos.  
  ```java
  for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
      System.out.println(entry.getKey() + ": " + entry.getValue());
  }
  ```  
También puedes usar streams en Java 21:  
```java
mapa.entrySet().stream().forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
```

---

### **10. ¿Cómo usarías un `HashMap` para implementar una caché simple? Proporciona un ejemplo.**  
**Respuesta**:  
Para una caché simple, usaría `LinkedHashMap` (una variante de `HashMap`) con una política de desalojo LRU (Least Recently Used). Ejemplo:  
```java
LinkedHashMap<String, Integer> cache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
        return size() > 3; // Límite de 3 entradas
    }
};
cache.put("A", 1);
cache.put("B", 2);
cache.put("C", 3);
cache.get("A"); // Mueve A al final (LRU)
cache.put("D", 4); // Elimina B (el menos reciente)
System.out.println(cache); // {A=1, C=3, D=4}
```  
Consideraciones: Limitar el tamaño, usar `ConcurrentHashMap` para concurrencia, y elegir una política de desalojo adecuada.

---

### **Notas adicionales**
- **Java 21**: Aunque `HashMap` no implementa `SequencedMap`, su uso sigue siendo fundamental por su rendimiento. Las mejoras en Java 21, como `Map.of()`, son relevantes para casos donde la inmutabilidad es clave.  
- **Consejo para entrevistas**: Enfatiza la elección de `HashMap` para operaciones rápidas sin orden, y contrasta con `LinkedHashMap` o `TreeMap` cuando el orden importa. Practica ejemplos como conteo de frecuencias o cachés, ya que son comunes.  

Si necesitas que profundice en alguna de estas preguntas, que proporcione más ejemplos de código, o que adapte las respuestas para un contexto específico (por ejemplo, preparación para una entrevista como candidato o entrevistador), ¡avísame!





<br>

<br>



---

<br>

<br>


**¿Qué métodos de `HashMap` son particularmente útiles en Java 21 para operaciones avanzadas?**



La respuesta destacó métodos como `computeIfAbsent`, `merge`, y `getOrDefault` por su utilidad en operaciones avanzadas. A continuación, ampliaré esta respuesta con ejemplos prácticos para cada uno de estos métodos, mostrando cómo se usan en escenarios comunes. Los ejemplos estarán escritos en Java 21 y serán claros para ilustrar su aplicación en una entrevista técnica.

---

### **Respuesta ampliada: Métodos útiles de `HashMap` en Java 21**

Los métodos `computeIfAbsent`, `merge`, y `getOrDefault` son especialmente útiles en `HashMap` porque simplifican operaciones comunes como inicialización condicional, combinación de valores, y manejo de valores no encontrados. Estos métodos, disponibles desde Java 8 y plenamente soportados en Java 21, permiten escribir código más limpio y funcional. A continuación, se presentan ejemplos prácticos para cada uno.

#### **1. `getOrDefault(Object key, V defaultValue)`**
Este método devuelve el valor asociado a una clave o un valor por defecto si la clave no existe. Es útil para evitar verificaciones explícitas de `null`.

**Ejemplo: Buscar un valor en un mapa de contactos**  
Supongamos que tenemos un mapa de nombres a correos electrónicos y queremos obtener el correo de un contacto, proporcionando un valor por defecto si no existe.

```java
import java.util.HashMap;
import java.util.Map;

public class GetOrDefaultEjemplo {
    public static void main(String[] args) {
        Map<String, String> contactos = new HashMap<>();
        contactos.put("Ana", "ana@example.com");
        contactos.put("Bob", "bob@example.com");

        // Obtener correo con getOrDefault
        String correoAna = contactos.getOrDefault("Ana", "no_registrado@example.com");
        String correoClara = contactos.getOrDefault("Clara", "no_registrado@example.com");

        System.out.println("Correo de Ana: " + correoAna);
        System.out.println("Correo de Clara: " + correoClara);
    }
}
```

**Salida**:  
```
Correo de Ana: ana@example.com
Correo de Clara: no_registrado@example.com
```

**Explicación**: `getOrDefault` evita la necesidad de escribir `if (contactos.containsKey("Clara"))` o manejar `null`, haciendo el código más conciso y legible.

---

#### **2. `computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)`**
Este método calcula y asigna un valor para una clave si no existe en el mapa. Es ideal para inicializar valores de forma perezosa (lazy initialization).

**Ejemplo: Agrupar estudiantes por curso**  
Supongamos que queremos agrupar estudiantes en una lista por su curso, usando un `HashMap` donde la clave es el curso y el valor es una lista de estudiantes.

```java
import java.util.*;

record Estudiante(String nombre, String curso) {}

public class ComputeIfAbsentEjemplo {
    public static void main(String[] args) {
        List<Estudiante> estudiantes = List.of(
            new Estudiante("Ana", "Matemáticas"),
            new Estudiante("Bob", "Física"),
            new Estudiante("Clara", "Matemáticas")
        );

        Map<String, List<Estudiante>> porCurso = new HashMap<>();

        // Agrupar usando computeIfAbsent
        for (Estudiante estudiante : estudiantes) {
            porCurso.computeIfAbsent(estudiante.curso(), k -> new ArrayList<>()).add(estudiante);
        }

        System.out.println("Estudiantes por curso: " + porCurso);
    }
}
```

**Salida**:  
```
Estudiantes por curso: {Matemáticas=[Estudiante[nombre=Ana, curso=Matemáticas], Estudiante[nombre=Clara, curso=Matemáticas]], Física=[Estudiante[nombre=Bob, curso=Física]]}
```

**Explicación**: `computeIfAbsent` crea una nueva `ArrayList` para el curso si no existe en el mapa, evitando la necesidad de verificar manualmente con `containsKey` y `put`. Esto es especialmente útil para estructuras anidadas.

---

#### **3. `merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)`**
Este método combina un valor nuevo con un valor existente para una clave, o la inserta si no existe. Es perfecto para operaciones de acumulación, como contar frecuencias.

**Ejemplo: Contar frecuencias de palabras**  
Contemos la frecuencia de palabras en un texto, actualizando el conteo de forma acumulativa.

```java
import java.util.HashMap;
import java.util.Map;

public class MergeEjemplo {
    public static void main(String[] args) {
        Map<String, Integer> conteoPalabras = new HashMap<>();
        String texto = "java es genial java es divertido y java es poderoso";
        String[] palabras = texto.toLowerCase().split(" ");

        // Contar frecuencias usando merge
        for (String palabra : palabras) {
            conteoPalabras.merge(palabra, 1, Integer::sum);
        }

        System.out.println("Frecuencia de palabras: " + conteoPalabras);
    }
}
```

**Salida**:  
```
Frecuencia de palabras: {java=3, es=3, genial=1, y=1, divertido=1, poderoso=1}
```

**Explicación**: `merge` inicializa la clave con 1 si no existe, o suma 1 al valor existente usando `Integer::sum`. Esto reemplaza la lógica tradicional de `if-else` para actualizar conteos.

---

#### **4. Ejemplo combinado: Usando los tres métodos juntos**  
Para mostrar cómo estos métodos pueden trabajar en conjunto, aquí hay un ejemplo que simula un sistema de gestión de pedidos, donde usamos `getOrDefault`, `computeIfAbsent`, y `merge` para procesar datos.

```java
import java.util.*;

public class GestionPedidos {
    public static void main(String[] args) {
        Map<String, List<String>> pedidosPorCliente = new HashMap<>();
        Map<String, Integer> totalProductosPorCliente = new HashMap<>();

        // Datos de ejemplo: cliente -> producto
        List<String[]> pedidos = List.of(
            new String[]{"Ana", "Laptop"},
            new String[]{"Bob", "Teléfono"},
            new String[]{"Ana", "Tablet"},
            new String[]{"Clara", "Auriculares"}
        );

        // Procesar pedidos
        for (String[] pedido : pedidos) {
            String cliente = pedido[0];
            String producto = pedido[1];

            // Agrupar productos por cliente con computeIfAbsent
            pedidosPorCliente.computeIfAbsent(cliente, k -> new ArrayList<>()).add(producto);

            // Contar productos por cliente con merge
            totalProductosPorCliente.merge(cliente, 1, Integer::sum);
        }

        // Consultar con getOrDefault
        System.out.println("Productos de Ana: " + pedidosPorCliente.getOrDefault("Ana", List.of()));
        System.out.println("Productos de David: " + pedidosPorCliente.getOrDefault("David", List.of()));
        System.out.println("Total de productos por cliente: " + totalProductosPorCliente);
    }
}
```

**Salida**:  
```
Productos de Ana: [Laptop, Tablet]
Productos de David: []
Total de productos por cliente: {Ana=2, Bob=1, Clara=1}
```

**Explicación**:  
- `computeIfAbsent` crea una nueva lista para cada cliente si no existe, para agrupar sus productos.  
- `merge` cuenta el número de productos por cliente, acumulando el conteo.  
- `getOrDefault` permite consultar los productos de un cliente, devolviendo una lista vacía si no existe.

---

### **Ventajas de estos métodos en Java 21**
- **Concisos**: Reducen la necesidad de verificaciones manuales (`containsKey`, `null` checks).  
- **Funcionales**: Se integran bien con la programación funcional, como streams y lambdas.  
- **Robustez**: Manejan casos extremos (claves inexistentes) de forma elegante.  
- **Casos de uso comunes**: Ideales para conteo, agrupación, inicialización perezosa, y consultas seguras.  

Si necesitas más ejemplos específicos, una explicación más profunda de algún método, o quieres que adapte los ejemplos a otro escenario (por ejemplo, un caso de uso en un sistema real), ¡avísame!










---








A continuación, presento una tabla que clasifica las principales colecciones del framework de colecciones de Java 21 según la **interfaz** que implementan, si **mantienen orden** (y de qué tipo), y si **permiten duplicados**. La tabla incluye las interfaces principales (`List`, `Set`, `Queue`, `Map`) y sus implementaciones más comunes, con un enfoque claro y conciso para su uso en entrevistas o aprendizaje.

---

### **Tabla de Clasificación de Colecciones en Java 21**

| **Interfaz** | **Implementación** | **Orden** | **Duplicados** | **Descripción** |
|--------------|--------------------|-----------|----------------|---------------|
| **List** | `ArrayList` | Sí (orden de inserción) | Sí | Lista basada en un arreglo dinámico, rápida para accesos aleatorios (O(1)), pero lenta para inserciones/eliminaciones en el medio (O(n)). |
| **List** | `LinkedList` | Sí (orden de inserción) | Sí | Lista doblemente enlazada, eficiente para inserciones/eliminaciones en los extremos (O(1)), pero lenta para accesos aleatorios (O(n)). |
| **Set** | `HashSet` | No | No | Conjunto basado en tabla hash, no permite duplicados, rápido para operaciones básicas (O(1) promedio), pero sin orden garantizado. |
| **Set** | `LinkedHashSet` | Sí (orden de inserción) | No | Conjunto que mantiene el orden de inserción, ligeramente más lento que `HashSet` por el mantenimiento de la lista enlazada. |
| **Set** | `TreeSet` | Sí (orden natural o personalizado) | No | Conjunto ordenado basado en un árbol binario balanceado, con operaciones en O(log n). |
| **Queue** | `LinkedList` | Sí (orden de inserción) | Sí | Implementa `Queue` y `Deque`, útil para colas FIFO o LIFO, con acceso eficiente en los extremos. |
| **Queue** | `PriorityQueue` | Sí (orden por prioridad, natural o personalizado) | Sí | Cola que ordena elementos según su prioridad, basada en un heap, con operaciones de inserción/extracción en O(log n). |
| **Map** | `HashMap` | No | Claves: No<br>Valores: Sí | Mapa basado en tabla hash, rápido (O(1) promedio), permite una clave `null` y múltiples valores `null`, sin orden. |
| **Map** | `LinkedHashMap` | Sí (orden de inserción) | Claves: No<br>Valores: Sí | Mapa que mantiene el orden de inserción, útil para iterar en orden o implementar cachés LRU. |
| **Map** | `TreeMap` | Sí (orden natural o personalizado de claves) | Claves: No<br>Valores: Sí | Mapa ordenado por claves, basado en un árbol binario balanceado, con operaciones en O(log n). |

---

### **Notas sobre la tabla**
1. **Interfaz**: Indica la interfaz principal que implementa la colección. Todas las colecciones (excepto `Map`) extienden `Collection`, y en Java 21, las colecciones ordenadas (como `List`, `LinkedHashSet`, `LinkedHashMap`) implementan `SequencedCollection` o `SequencedMap`.  
2. **Orden**:  
   - "Sí (orden de inserción)": Los elementos se mantienen en el orden en que se añadieron.  
   - "Sí (orden natural o personalizado)": Los elementos se ordenan según su orden natural (usando `Comparable`) o un `Comparator` personalizado.  
   - "No": No hay garantía de orden (ej. `HashMap`, `HashSet`).  
3. **Duplicados**:  
   - Para `List` y `Queue`, los duplicados están permitidos (puedes tener el mismo elemento varias veces).  
   - Para `Set`, los duplicados no están permitidos (se usa `equals()` para determinar unicidad).  
   - Para `Map`, las claves deben ser únicas, pero los valores pueden repetirse.  
4. **Java 21**: La introducción de `SequencedCollection` y `SequencedMap` en Java 21 añade métodos como `getFirst()`, `getLast()`, y `reversed()` a colecciones como `ArrayList`, `LinkedList`, `LinkedHashSet`, `LinkedHashMap`, y `TreeMap`, pero no a `HashSet` ni `HashMap` debido a su falta de orden.

---

### **Ejemplo práctico para ilustrar**
Para reforzar la tabla, aquí hay un ejemplo en Java 21 que muestra el uso de varias colecciones con diferentes comportamientos de orden y duplicados:

```java
import java.util.*;

public class ColeccionesEjemplo {
    public static void main(String[] args) {
        // List: ArrayList (orden de inserción, permite duplicados)
        List<String> lista = new ArrayList<>();
        lista.add("Ana");
        lista.add("Bob");
        lista.add("Ana");
        System.out.println("ArrayList (orden, duplicados): " + lista);
        System.out.println("ArrayList inversa (Java 21): " + lista.reversed());

        // Set: HashSet (sin orden, sin duplicados)
        Set<String> conjunto = new HashSet<>();
        conjunto.add("Ana");
        conjunto.add("Bob");
        conjunto.add("Ana");
        System.out.println("HashSet (sin orden, sin duplicados): " + conjunto);

        // Set: LinkedHashSet (orden de inserción, sin duplicados)
        Set<String> conjuntoOrdenado = new LinkedHashSet<>();
        conjuntoOrdenado.add("Ana");
        conjuntoOrdenado.add("Bob");
        conjuntoOrdenado.add("Ana");
        System.out.println("LinkedHashSet (orden, sin duplicados): " + conjuntoOrdenado);

        // Map: HashMap (sin orden, claves únicas)
        Map<String, Integer> mapa = new HashMap<>();
        mapa.put("Ana", 25);
        mapa.put("Bob", 30);
        mapa.put("Ana", 26); // Sobrescribe
        System.out.println("HashMap (sin orden, claves únicas): " + mapa);

        // Map: LinkedHashMap (orden de inserción, claves únicas)
        Map<String, Integer> mapaOrdenado = new LinkedHashMap<>();
        mapaOrdenado.put("Ana", 25);
        mapaOrdenado.put("Bob", 30);
        mapaOrdenado.put("Ana", 26);
        System.out.println("LinkedHashMap (orden, claves únicas): " + mapaOrdenado);

        // Queue: PriorityQueue (orden por prioridad, permite duplicados)
        Queue<Integer> cola = new PriorityQueue<>();
        cola.add(30);
        cola.add(10);
        cola.add(10);
        System.out.println("PriorityQueue (orden por prioridad, duplicados): ");
        while (!cola.isEmpty()) {
            System.out.println(cola.poll());
        }
    }
}
```

**Salida esperada**:  
```
ArrayList (orden, duplicados): [Ana, Bob, Ana]
ArrayList inversa (Java 21): [Ana, Bob, Ana]
HashSet (sin orden, sin duplicados): [Ana, Bob]
LinkedHashSet (orden, sin duplicados): [Ana, Bob]
HashMap (sin orden, claves únicas): {Ana=26, Bob=30}
LinkedHashMap (orden, claves únicas): {Ana=26, Bob=30}
PriorityQueue (orden por prioridad, duplicados):
10
10
30
```

---

### **Explicación del ejemplo**
- **ArrayList**: Mantiene el orden de inserción y permite duplicados (`Ana` aparece dos veces). Usa `reversed()` de Java 21.  
- **HashSet**: No garantiza orden y elimina duplicados (`Ana` aparece solo una vez).  
- **LinkedHashSet**: Mantiene el orden de inserción, pero elimina duplicados.  
- **HashMap**: Sin orden, sobrescribe la clave `Ana` con el último valor.  
- **LinkedHashMap**: Mantiene el orden de inserción, sobrescribe la clave `Ana`.  
- **PriorityQueue**: Ordena por prioridad (menor a mayor), permite duplicados (`10` aparece dos veces).

---

### **Notas adicionales**
- **Uso en entrevistas**: Esta tabla es útil para explicar rápidamente las diferencias entre colecciones y justificar la elección de una sobre otra. Por ejemplo, elegirías `HashMap` para búsquedas rápidas sin orden, o `TreeSet` para mantener elementos ordenados.  
- **Java 21**: Las colecciones que implementan `SequencedCollection` o `SequencedMap` (como `LinkedHashMap`, `TreeMap`, `ArrayList`) son ideales para aprovechar métodos modernos como `reversed()`.  
- **Consideraciones prácticas**: Evalúa el caso de uso (rendimiento, orden, duplicados) para elegir la colección adecuada. Por ejemplo, usa `ArrayList` para acceso aleatorio, `HashSet` para unicidad, o `TreeMap` para claves ordenadas.
