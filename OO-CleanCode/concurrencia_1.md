# Concurrencia

<br>

Concurrencia en Java: cuál es la diferencia entre @Async, Future, Completable Future, y Executor Service API?


En Java, la concurrencia es un tema amplio, y las herramientas que mencionas (`@Async`, `Future`, `CompletableFuture`, y `ExecutorService`) son componentes clave para manejarla, pero tienen propósitos y características distintas. A continuación, te explico cada una y sus diferencias de manera clara y estructurada:

### 1. **`@Async`**
- **Qué es**: Una anotación de Spring Framework que permite ejecutar métodos de forma asíncrona en un hilo separado.
- **Cómo funciona**: Cuando marcas un método con `@Async`, Spring lo ejecuta en un hilo gestionado por un `Executor` (normalmente un `ThreadPoolExecutor` configurado en el contexto de Spring). El método debe estar en una clase marcada como `@Component` o similar y, para que funcione, necesitas habilitar el soporte asíncrono con `@EnableAsync` en la configuración de Spring.
- **Características**:
  - Simplifica la ejecución asíncrona al abstraer la gestión de hilos.
  - Puede devolver `void` (para tareas fire-and-forget) o un `Future`/`CompletableFuture` para obtener resultados.
  - No maneja directamente los hilos; depende de un `Executor` subyacente.
- **Uso típico**: Ejecutar tareas en segundo plano en aplicaciones Spring, como enviar correos o procesar datos sin bloquear el hilo principal.
- **Ejemplo**:
  ```java:disable-run
  @Service
  public class MyService {
      @Async
      public CompletableFuture<String> doWork() {
          return CompletableFuture.completedFuture("Resultado");
      }
  }
  ```
- **Limitaciones**: Es específico de Spring, no es parte del núcleo de Java. Requiere configuración adicional para manejar excepciones o personalizar el `Executor`.

---

### 2. **`Future`**
- **Qué es**: Una interfaz de Java (`java.util.concurrent.Future`) que representa el resultado de una operación asíncrona que puede completarse en el futuro.
- **Cómo funciona**: Es un contenedor para un resultado que no está disponible inmediatamente. Se usa con un `ExecutorService` para obtener el resultado de una tarea enviada a un hilo.
- **Características**:
  - Permite consultar el estado de la tarea (`isDone`), obtener el resultado (`get`) o cancelarla (`cancel`).
  - El método `get` es bloqueante, lo que significa que el hilo que lo llama espera hasta que el resultado esté disponible.
  - No soporta operaciones avanzadas como encadenar tareas o manejar excepciones de manera fluida.
- **Uso típico**: Ejecutar tareas simples en hilos separados y recuperar su resultado cuando esté listo.
- **Ejemplo**:
  ```java
  ExecutorService executor = Executors.newFixedThreadPool(2);
  Future<String> future = executor.submit(() -> "Resultado");
  String resultado = future.get(); // Bloquea hasta que el resultado esté disponible
  executor.shutdown();
  ```
- **Limitaciones**: `Future` es limitado para flujos asíncronos complejos, ya que no permite encadenar operaciones ni manejar resultados de forma reactiva.

---

### 3. **`CompletableFuture`**
- **Qué es**: Una clase de Java (`java.util.concurrent.CompletableFuture`) introducida en Java 8 que extiende `Future` y ofrece un enfoque más flexible y funcional para la programación asíncrona.
- **Cómo funciona**: Permite encadenar operaciones asíncronas, manejar resultados y excepciones de forma no bloqueante, y combinar múltiples tareas. Se puede completar manualmente o mediante un `Executor`.
- **Características**:
  - Soporta programación reactiva con métodos como `thenApply`, `thenCompose`, `thenCombine`, etc.
  - Permite manejar excepciones de manera elegante con `exceptionally` o `handle`.
  - Puede ejecutarse en un `Executor` personalizado o en el `ForkJoinPool` por defecto.
  - No bloquea el hilo principal a menos que llames a `get` o `join`.
- **Uso típico**: Flujos asíncronos complejos, como procesar datos en varias etapas o combinar resultados de múltiples tareas.
- **Ejemplo**:
  ```java
  CompletableFuture.supplyAsync(() -> "Hola")
      .thenApply(s -> s + " Mundo")
      .thenAccept(System.out::println); // Imprime "Hola Mundo"
  ```
- **Ventajas sobre `Future`**: Es mucho más expresivo y permite construir flujos asíncronos complejos sin bloquear.

---

### 4. **`ExecutorService`**
- **Qué es**: Una interfaz de Java (`java.util.concurrent.ExecutorService`) que gestiona un grupo de hilos (`ThreadPool`) para ejecutar tareas de forma concurrente.
- **Cómo funciona**: Proporciona un marco para enviar tareas (como `Runnable` o `Callable`) y gestionar su ejecución en hilos reutilizables. Es la base para `Future` y `CompletableFuture`.
- **Características**:
  - Permite crear diferentes tipos de pools de hilos (fixed, cached, single-threaded, etc.) con `Executors`.
  - Gestiona el ciclo de vida de los hilos (creación, reutilización, cierre).
  - Soporta tareas síncronas (`execute`) y asíncronas (`submit`).
- **Uso típico**: Ejecutar múltiples tareas concurrentes, como procesar solicitudes en un servidor o realizar cálculos pesados en paralelo.
- **Ejemplo**:
  ```java
  ExecutorService executor = Executors.newFixedThreadPool(2);
  executor.submit(() -> System.out.println("Tarea ejecutada"));
  executor.shutdown();
  ```
- **Limitaciones**: Es de bajo nivel comparado con `CompletableFuture` o `@Async`, ya que requiere gestionar manualmente las tareas y los resultados.

---

### **Diferencias clave**
| Característica            | `@Async`                              | `Future`                              | `CompletableFuture`                   | `ExecutorService`                     |
|---------------------------|---------------------------------------|---------------------------------------|---------------------------------------|---------------------------------------|
| **Nivel**                 | Alto (Spring)                        | Medio (Java)                         | Medio/Alto (Java)                    | Bajo (Java)                          |
| **Uso principal**         | Tareas asíncronas en Spring          | Resultado de tarea asíncrona simple  | Flujos asíncronos complejos          | Gestión de hilos y tareas            |
| **Bloqueante**            | No (si retorna `Future`/`CompletableFuture`) | Sí (`get`)                        | No (a menos que uses `get`/`join`)   | Depende de la tarea                  |
| **Encadenamiento**        | No directamente                      | No                                   | Sí (métodos como `thenApply`)        | No, pero soporta múltiples tareas    |
| **Manejo de excepciones** | Básico (necesita configuración)      | Básico (vía `get`)                  | Avanzado (`exceptionally`, `handle`) | Básico (vía `Future`)               |
| **Dependencias**          | Spring Framework                    | Java estándar                        | Java estándar                        | Java estándar                       |
| **Flexibilidad**          | Media (abstracción de Spring)        | Baja                                 | Alta                                 | Alta (pero manual)                  |

---

### **Cuándo usar cada uno**
- **`@Async`**: Úsalo en aplicaciones Spring para tareas asíncronas simples, como enviar correos o procesar datos en segundo plano, sin preocuparte por gestionar hilos manualmente.
- **`Future`**: Úsalo para tareas asíncronas simples en Java puro donde necesitas un resultado, pero no requieres encadenar operaciones o manejar flujos complejos.
- **`CompletableFuture`**: Ideal para flujos asíncronos complejos, encadenamiento de tareas, o cuando necesitas un enfoque reactivo y no bloqueante.
- **`ExecutorService`**: Úsalo cuando necesitas control total sobre la gestión de hilos, como configurar un pool de hilos personalizado o ejecutar múltiples tareas concurrentes.

---

### **Conclusión**
- Si trabajas con **Spring**, `@Async` es la opción más sencilla para tareas asíncronas.
- Si necesitas algo más avanzado en **Java puro**, `CompletableFuture` es la mejor opción para flujos modernos y reactivos.
- `Future` es útil para casos simples, pero está limitado en comparación con `CompletableFuture`.
- `ExecutorService` es la base para todos los anteriores y se usa cuando necesitas gestionar hilos directamente.



<br>
<br>
<br>

---


<br>
<br>


# CompletableFuture



A continuación, te proporciono ejemplos prácticos de `CompletableFuture` en Java para ilustrar su uso en diferentes escenarios comunes. Cada ejemplo está diseñado para mostrar una característica clave de `CompletableFuture`, desde operaciones básicas hasta casos más avanzados. Todos los ejemplos son funcionales y están comentados para mayor claridad.

---

### **1. Ejemplo básico: Ejecución asíncrona simple**
Este ejemplo muestra cómo usar `CompletableFuture` para ejecutar una tarea asíncrona y obtener su resultado.

```java
import java.util.concurrent.CompletableFuture;

public class BasicExample {
    public static void main(String[] args) {
        // Ejecuta una tarea asíncrona que devuelve un String
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // Simula una tarea que toma tiempo
                return "¡Hola desde CompletableFuture!";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Maneja el resultado de forma no bloqueante
        future.thenAccept(result -> System.out.println(result));

        // Mantén el programa vivo para ver el resultado
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Explicación**:
- `supplyAsync` ejecuta una tarea en un hilo del `ForkJoinPool` por defecto.
- `thenAccept` procesa el resultado de forma no bloqueante cuando la tarea se completa.
- Simulamos una tarea lenta con `Thread.sleep`.

**Salida**:
```
¡Hola desde CompletableFuture!
```

---

### **2. Encadenamiento de operaciones asíncronas**
Este ejemplo muestra cómo encadenar múltiples operaciones asíncronas con `thenApply` y `thenCompose`.

```java
import java.util.concurrent.CompletableFuture;

public class ChainingExample {
    public static void main(String[] args) {
        // Tarea 1: Obtener un nombre
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return "Alice";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Tarea 2: Transformar el nombre (encadenar con thenApply)
        CompletableFuture<String> transformed = future.thenApply(name -> {
            return "Hola, " + name + "!";
        });

        // Tarea 3: Obtener otro dato asíncrono basado en el resultado (encadenar con thenCompose)
        CompletableFuture<String> finalResult = transformed.thenCompose(greeting ->
            CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(1000);
                    return greeting + " Bienvenida al sistema.";
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));

        // Imprimir resultado
        finalResult.thenAccept(System.out::println);

        // Esperar para ver el resultado
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Explicación**:
- `thenApply` transforma el resultado de una tarea en otro valor.
- `thenCompose` permite encadenar otra tarea asíncrona que devuelve un `CompletableFuture`.
- Cada etapa se ejecuta en un hilo separado, manteniendo el flujo no bloqueante.

**Salida**:
```
Hola, Alice! Bienvenida al sistema.
```

---

### **3. Combinar múltiples CompletableFutures**
Este ejemplo muestra cómo combinar resultados de varias tareas asíncronas con `thenCombine`.

```java
import java.util.concurrent.CompletableFuture;

public class CombineExample {
    public static void main(String[] args) {
        // Tarea 1: Obtener un número
        CompletableFuture<Integer> numberFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return 42;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Tarea 2: Obtener un texto
        CompletableFuture<String> textFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return "Respuesta";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Combinar los resultados
        CompletableFuture<String> combined = numberFuture.thenCombine(textFuture, (number, text) -> {
            return text + ": " + number;
        });

        // Imprimir resultado
        combined.thenAccept(System.out::println);

        // Esperar para ver el resultado
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Explicación**:
- `thenCombine` espera a que dos `CompletableFuture` se completen y combina sus resultados.
- Ideal para escenarios donde necesitas los resultados de múltiples tareas asíncronas.

**Salida**:
```
Respuesta: 42
```

---

### **4. Manejo de excepciones**
Este ejemplo muestra cómo manejar excepciones en un flujo asíncrono con `exceptionally`.

```java
import java.util.concurrent.CompletableFuture;

public class ExceptionHandlingExample {
    public static void main(String[] args) {
        // Tarea que falla
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                throw new RuntimeException("Algo salió mal");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Manejar excepción
        CompletableFuture<String> recovered = future.exceptionally(throwable -> {
            System.err.println("Error: " + throwable.getMessage());
            return "Resultado por defecto";
        });

        // Imprimir resultado
        recovered.thenAccept(System.out::println);

        // Esperar para ver el resultado
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Explicación**:
- `exceptionally` captura cualquier excepción en el flujo y permite devolver un valor por defecto o realizar una acción correctiva.
- Es útil para evitar que los errores rompan el flujo asíncrono.

**Salida**:
```
Error: java.lang.RuntimeException: Algo salió mal
Resultado por defecto
```

---

### **5. Uso con Executor personalizado**
Este ejemplo muestra cómo usar un `Executor` personalizado para controlar los hilos en los que se ejecutan las tareas.

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomExecutorExample {
    public static void main(String[] args) {
        // Crear un ExecutorService con un pool de 2 hilos
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Tarea asíncrona con Executor personalizado
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return "Tarea ejecutada en " + Thread.currentThread().getName();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);

        // Imprimir resultado
        future.thenAccept(System.out::println);

        // Apagar el executor
        executor.shutdown();

        // Esperar para ver el resultado
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Explicación**:
- `supplyAsync` acepta un `Executor` como segundo argumento para controlar el pool de hilos.
- Esto es útil en aplicaciones donde necesitas limitar o personalizar el número de hilos.

**Salida** (el nombre del hilo puede variar):
```
Tarea ejecutada en pool-1-thread-1
```

---

### **6. Ejecución paralela de múltiples tareas**
Este ejemplo muestra cómo ejecutar varias tareas en paralelo y esperar a que todas se completen con `allOf`.

```java
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;

public class ParallelTasksExample {
    public static void main(String[] args) {
        // Crear varias tareas asíncronas
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return "Tarea 1 completada";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1500);
                return "Tarea 2 completada";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(800);
                return "Tarea 3 completada";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // Esperar a que todas las tareas se completen
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(task1, task2, task3);

        // Recolectar resultados
        allTasks.thenRun(() -> {
            String results = Arrays.asList(task1, task2, task3)
                    .stream()
                    .map(CompletableFuture::join) // Obtener resultados
                    .collect(Collectors.joining(", "));
            System.out.println("Resultados: " + results);
        });

        // Esperar para ver el resultado
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**Explicación**:
- `allOf` espera a que múltiples `CompletableFuture` se completen.
- Usamos `join` para obtener los resultados de cada tarea una vez que todas han terminado.
- Ideal para ejecutar tareas independientes en paralelo.

**Salida**:
```
Resultados: Tarea 1 completada, Tarea 2 completada, Tarea 3 completada
```

---

### **Cuándo usar cada ejemplo**
- **Ejemplo 1**: Para tareas asíncronas simples donde solo necesitas un resultado.
- **Ejemplo 2**: Cuando necesitas encadenar transformaciones o tareas dependientes.
- **Ejemplo 3**: Para combinar resultados de múltiples fuentes asíncronas.
- **Ejemplo 4**: Cuando necesitas manejar errores de forma robusta en flujos asíncronos.
- **Ejemplo 5**: Si quieres controlar el pool de hilos para optimizar recursos.
- **Ejemplo 6**: Para ejecutar múltiples tareas independientes en paralelo y esperar su finalización.

---

### **Notas finales**
- Los ejemplos usan `Thread.sleep` para simular tareas lentas, pero en aplicaciones reales podrías estar llamando a APIs, consultando bases de datos, o procesando archivos.
- Para aplicaciones de producción, considera usar un `Executor` personalizado para gestionar mejor los hilos.
- Si necesitas integrar `CompletableFuture` con Spring, puedes combinarlo con `@Async` para obtener lo mejor de ambos mundos.

Si quieres profundizar en algún caso específico o adaptar uno de estos ejemplos a un escenario particular, ¡avísame!
