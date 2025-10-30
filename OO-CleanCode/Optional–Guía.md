# Optional<T> en Java 21 – Guía

<br>

### "Optional no es para reemplazar `null`, sino para evitar `NullPointerException` y hacer el código más expresivo."

<br>


## ¿Qué es Optional?
`Optional<T>` es una clase contenedora introducida en Java 8 que puede contener:

* 	Un valor no nulo → `Optional.of(valor)`
* 	Nada → `Optional.empty()`

Nunca debe contener `null` directamente.

<br>


## ¿Por qué usar Optional?



| Problema | Sin `Optional` | Con `Optional` |
|--------|----------------|----------------|
| `NullPointerException` | `user.getName().toUpperCase()` → crash | `user.getName().map(String::toUpperCase).orElse("ANÓNIMO")` → seguro |
| Código confuso | `if (name != null) ...` | `name.ifPresent(...)` |
| Métodos que "pueden no devolver nada" | `return null;` | `return Optional.empty();` |


<br>

<br>



## Métodos clave (Java 21)

| Método | Uso | Ejemplo |
|-------|-----|--------|
| `Optional.of(T)` | Crea con valor **no nulo** | `Optional.of("Hola")` |
| `Optional.ofNullable(T)` | Acepta `null` → `empty()` | `Optional.ofNullable(null)` |
| `Optional.empty()` | Sin valor | `Optional.empty()` |
| `isPresent()` | ¿Tiene valor? | `opt.isPresent()` |
| `isEmpty()` (Java 11+) | ¿Está vacío? | `opt.isEmpty()` |
| `get()` | Obtiene valor (**peligroso**) | `opt.get()` → lanza excepción si vacío |
| `orElse(T)` | Valor por defecto | `opt.orElse("default")` |
| `orElseGet(Supplier)` | Valor perezoso | `opt.orElseGet(() -> generar())` |
| `orElseThrow()` | Lanza excepción | `opt.orElseThrow(() -> new Exception())` |
| `map(Function)` | Transforma si presente | `opt.map(String::toUpperCase)` |
| `flatMap(Function)` | Para `Optional<Optional<T>>` | `opt.flatMap(...)` |
| `filter(Predicate)` | Filtra si cumple | `opt.filter(s -> s.length() > 3)` |
| `ifPresent(Consumer)` | Ejecuta si presente | `opt.ifPresent(System.out::println)` |
| `ifPresentOrElse(...)` (Java 9+) | Si presente o no | `opt.ifPresentOrElse(... , ...)` |

<br>


## Ejemplo Real: Usuario con nombre opcional

```java
public class Usuario {
    private final String nombre;

    public Usuario(String nombre) {
        this.nombre = nombre;
    }

    // Devuelve Optional para indicar que puede no haber nombre
    public Optional<String> getNombre() {
        return Optional.ofNullable(nombre);
    }
}
```

<br>


## Uso en Código (Pair Programming)

```java
public class Main {
    public static void main(String[] args) {
        Usuario usuario1 = new Usuario("Ana");
        Usuario usuario2 = new Usuario(null);

        procesarNombre(usuario1); // Ana
        procesarNombre(usuario2); // ANÓNIMO
    }

    static void procesarNombre(Usuario usuario) {
        String nombre = usuario.getNombre()
            .map(String::toUpperCase)           // Si hay nombre → mayúsculas
            .filter(n -> n.length() > 2)        // Solo si > 2 letras
            .orElse("ANÓNIMO");                 // Si no → "ANÓNIMO"

        System.out.println("Nombre procesado: " + nombre);
    }
}
```

### Salida:
```
Nombre procesado: ANA
Nombre procesado: ANÓNIMO
```


<br>


## Anti-patrones (NO hacer)

```java
// MAL: Optional en campos
public class Usuario {
    private Optional<String> nombre; // NO
}

// MAL: Optional en parámetros
public void metodo(Optional<String> param) { } // Solo en APIs públicas

// MAL: get() sin comprobar
String nombre = opt.get(); // Lanza NoSuchElementException si vacío

// MAL: crear Optional solo para devolver null
return Optional.of(null); // Lanza NullPointerException
```

<br>


## Buenas prácticas

| Regla | Ejemplo |
|------|--------|
| Usa `Optional` en **retornos** de métodos que pueden no tener valor | `Optional<Usuario> findById(Long id)` |
| Usa `orElse` / `orElseGet` para valores por defecto | `opt.orElse("Invitado")` |
| Usa `map` para transformar | `opt.map(String::trim)` |
| Usa `flatMap` con Optional anidados | `opt.flatMap(u -> u.getDireccion())` |
| **NO uses** `Optional` como parámetro (salvo APIs) | |
| **NO uses** `get()` sin `isPresent()` | |

<br>


## Java 21: Novedades con `Optional`

Aunque `Optional` no tiene **nuevos métodos en Java 21**, se integra perfectamente con **patrones y records**.

### Ejemplo con **Pattern Matching** (Java 21)

```java
Optional<String> opt = Optional.of("Hola");

if (opt instanceof Optional(var valor)) {
    System.out.println("Tiene valor: " + valor);
}
```

> **Desestructuración directa** (preview en Java 21, estable en 22+)


<br>

## Comparación: `null` vs `Optional`

| Escenario | Con `null` | Con `Optional` |
|---------|------------|----------------|
| Método que puede fallar | `String findName() { return null; }` | `Optional<String> findName()` |
| Uso | `String n = find(); if (n != null) ...` | `find().ifPresent(...)` |
| Seguridad | Riesgo de NPE | Compilador te guía |
| Legibilidad | Baja | Alta |

---

## Ejercicio Práctico (Pair Programming)

```java
// Dada una lista de usuarios, encuentra el primero con email
public Optional<Usuario> buscarConEmail(List<Usuario> usuarios) {
    return usuarios.stream()
        .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
        .findFirst();
}

// Uso:
buscarConEmail(usuarios)
    .map(Usuario::getEmail)
    .ifPresentOrElse(
        email -> System.out.println("Email: " + email),
        () -> System.out.println("No hay email")
    );
```

---

## Resumen Visual

```
Optional<T>
├── of(T)           → valor no nulo
├── ofNullable(T)   → null → empty()
├── empty()         → sin valor
├── map()           → transforma
├── flatMap()       → Optional<Optional<T>>
├── orElse()        → valor por defecto
├── orElseGet()     → supplier perezoso
├── orElseThrow()   → excepción
├── filter()        → filtra si cumple
└── ifPresent()     → acción si presente
```



<br>

## Checklist para usar `Optional`

| Check | Pregunta |
|-------|---------|
| Check | ¿El método puede no devolver valor? → Usa `Optional` |
| Check | ¿Estás transformando un valor opcional? → Usa `map` |
| Check | ¿Tienes `Optional<Optional<T>>`? → Usa `flatMap` |
| Check | ¿Necesitas valor por defecto? → `orElse` / `orElseGet` |
| Cross | ¿Estás usando `get()` sin comprobar? → Evítalo |


<br>


## Documentación oficial

- [Oracle Java 21 - Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html)

<br>

