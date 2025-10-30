# Comparable *vs* Comparator






Interfaces **`Comparable<T>`** y **`Comparator<T>`** en Java, **como si estuviéramos en una sesión de pair programming**, con:

- Definición clara  
- Métodos  
- Cuándo usar cada una  
- Ejemplos reales  
- Ventajas / desventajas  
- Diálogo simulado de entrevista


<br>


## 1. `Comparable<T>` – "Yo sé cómo ordenarme"

### Definición
> **Interfaz** que una clase **implementa** para definir su **orden natural**.

```java
public interface Comparable<T> {
    int compareTo(T o);
}
```


<br>


### Características

| Característica | Descripción |
|----------------|-----------|
| **Método** | `int compareTo(T o)` |
| **Ubicación** | **Dentro de la clase** |
| **Orden** | Único (orden natural) |
| **Modifica clase** | Sí |
| **Ejemplos nativos** | `String`, `Integer`, `LocalDate`, `BigDecimal` |



<br>



### Ejemplo: `Tarea` con `Comparable`

```java
@Getter
@RequiredArgsConstructor
public class Tarea implements Comparable<Tarea> {
    private final String descripcion;
    private final int prioridad;

    @Override
    public int compareTo(Tarea otra) {
        return Integer.compare(this.prioridad, otra.prioridad);
        // 1 = más alta → sale primero
    }
}
```

<br>


### Uso

```java
List<Tarea> tareas = List.of(
    new Tarea("Email", 3),
    new Tarea("Llamada", 1)
);

Collections.sort(tareas); // ← usa compareTo()
```

> **Salida**: `[Llamada (1), Email (3)]`



<br>



## 2. `Comparator<T>` – "Tú decides cómo ordenar"

### Definición
> **Interfaz externa** que define **cómo comparar dos objetos**.

```java
public interface Comparator<T> {
    int compare(T o1, T o2);
    boolean equals(Object obj); // opcional
}
```

---

### Características

| Característica | Descripción |
|----------------|-----------|
| **Método** | `int compare(T o1, T o2)` |
| **Ubicación** | **Fuera de la clase** |
| **Flexibilidad** | Múltiples comparadores |
| **Modifica clase** | No |
| **Formas** | Clase anónima, lambda, `Comparator.comparing()` |

---

### Ejemplo: `Comparator` para `Tarea`

```java
// 1. Por prioridad
Comparator<Tarea> porPrioridad = Comparator.comparingInt(Tarea::getPrioridad);

// 2. Por descripción
Comparator<Tarea> porDescripcion = Comparator.comparing(Tarea::getDescripcion);

// 3. Combinado (prioridad + descripción)
Comparator<Tarea> complejo = porPrioridad.thenComparing(porDescripcion);
```

---

### Uso

```java
List<Tarea> tareas = ...;

tareas.stream().sorted(porPrioridad).toList();
Collections.sort(tareas, porDescripcion);
```

---

## 3. Comparación Directa

| Aspecto | `Comparable<T>` | `Comparator<T>` |
|--------|------------------|------------------|
| **Interfaz** | `Comparable<T>` | `Comparator<T>` |
| **Método** | `compareTo(T)` | `compare(T, T)` |
| **¿Dentro de la clase?** | Yes | No |
| **¿Múltiples órdenes?** | No (solo 1) | Yes |
| **¿Modifica la clase?** | Yes | No |
| **¿Para `PriorityQueue`?** | Por defecto | Con constructor |
| **¿Para `TreeSet`?** | Yes | Yes |
| **¿Ejemplo típico?** | `String` | Filtros dinámicos |

---

## 4. Reglas del `int` devuelto

| Valor | Significado |
|------|------------|
| `< 0` | `o1` va **antes** que `o2` |
| `= 0` | `o1` y `o2` son **iguales** |
| `> 0` | `o1` va **después** que `o2` |

```java
// Ejemplo
return Integer.compare(this.prioridad, otra.prioridad);
// Si prioridad=1 y otra=3 → devuelve -2 → 1 va antes
```

---

## 5. Diálogo de Pair Programming (Entrevista)

| Rol | Diálogo |
|-----|--------|
| **Navigator** | _"¿Cómo ordenamos las tareas?"_ |
| **Driver** | _"Por prioridad. ¿Usamos `Comparable` o `Comparator`?"_ |
| **Navigator** | _"¿Cuántos órdenes vamos a necesitar?"_ |
| **Driver** | _"Solo prioridad por ahora."_ |
| **Navigator** | _"Entonces `Comparable` está bien. Es más simple."_ |
| **Driver** | _"Pero si después quieren ordenar por nombre..."_ |
| **Navigator** | _"¡Buen punto! Mejor `Comparator` para flexibilidad."_ |
| **Driver** | _"Ok, uso `Comparator.comparingInt(Tarea::getPrioridad)`"_ |
| **Navigator** | _"Perfecto. ¿Y si hay empate en prioridad?"_ |
| **Driver** | _"Agrego `.thenComparing(Tarea::getDescripcion)`"_ |

---

## 6. Buenas Prácticas

| Práctica | Código |
|--------|-------|
| Usa `Integer.compare()` | `Integer.compare(a, b)` |
| Usa `Comparator.comparing()` | `Comparator.comparingInt(T::getId)` |
| Combina con `thenComparing()` | `.thenComparing(T::getNombre)` |
| Usa lambdas | `(a, b) -> a.getEdad() - b.getEdad()` |
| Evita `a - b` | Puede causar **overflow** |

---

## 7. Ejemplo Completo (con Lombok)

```java
@Getter
@RequiredArgsConstructor
public class Tarea {
    private final String descripcion;
    private final int prioridad;
}

// Comparator externo
Comparator<Tarea> orden = Comparator
    .comparingInt(Tarea::getPrioridad)
    .thenComparing(Tarea::getDescripcion);
```

---

## 8. ¿Cuándo usar cada uno?

| Situación | Usa |
|---------|-----|
| Orden natural único | `Comparable` |
| Múltiples criterios | `Comparator` |
| No puedes modificar clase | `Comparator` |
| `PriorityQueue` por defecto | `Comparable` |
| `PriorityQueue` personalizado | `Comparator` |
| Clase de terceros | `Comparator` |

---

## 9. Mini Resumen (para entrevista)

> **"`Comparable` es el orden natural que la clase define con `compareTo`. `Comparator` es una estrategia externa con `compare`, permite múltiples órdenes sin modificar la clase."**

---

## Bonus: `record` (Java 17+) con `Comparable`

```java
public record Tarea(String descripcion, int prioridad) 
    implements Comparable<Tarea> {

    @Override
    public int compareTo(Tarea otra) {
        return Integer.compare(this.prioridad, otra.prioridad);
    }
}
```

→ Automático, inmutable, limpio.

