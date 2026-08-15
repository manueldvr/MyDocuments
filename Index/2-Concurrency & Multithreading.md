# 2 - Concurrency & Multithreading

<br>

With focus in: Ability to reason about shared state and to propose a safe design (immutability, confinement, message passing) rather than patching with ‘synchronized everywhere’.

1. Java Memory Model basics: visibility, happens-before, volatile semantics.
2. Synchronization/locks: intrinsic locks, ReentrantLock, deadlocks/livelocks, lock ordering.
3. Atomic types and concurrent collections.
4. Producer–consumer pattern; thread interruption and graceful shutdown.
5. Diagnosing concurrency bugs: deadlock detection, thread dumps, typical symptoms.


<br>


# Concurrencia y multithreading en Java 17

La dificultad principal de la concurrencia no consiste en crear varios threads, sino en razonar sobre:

* qué estado comparten;
* quién puede modificarlo;
* cuándo los cambios se vuelven visibles;
* qué operaciones deben ser atómicas;
* cómo detener correctamente el procesamiento;
* cómo detectar errores que solamente aparecen bajo determinadas condiciones.

La primera pregunta no debería ser:

> “¿Dónde agrego `synchronized`?”

Sino:

> “¿Puedo evitar compartir estado mutable?”

## Diseño seguro antes de sincronizar

Estas estrategias deberían considerarse en este orden aproximado:

### 1. Inmutabilidad

Un objeto inmutable no cambia después de construirse, por lo que puede compartirse entre threads sin sincronización adicional.

```java
public record Payment(
        String id,
        BigDecimal amount,
        Instant createdAt
) {}
```

Los `record` son buenos candidatos para transportar datos inmutables, siempre que sus componentes también sean inmutables.

Esto no sería completamente inmutable:

```java
public record Order(String id, List<String> items) {}
```

Aunque el `record` no permita reemplazar `items`, alguien podría modificar la lista.

Una versión más segura:

```java
public record Order(String id, List<String> items) {

    public Order {
        items = List.copyOf(items);
    }
}
```

### 2. Confinamiento

El estado pertenece exclusivamente a un thread o a una operación.

```java
public BigDecimal calculateTotal(List<BigDecimal> amounts) {
    BigDecimal total = BigDecimal.ZERO;

    for (BigDecimal amount : amounts) {
        total = total.add(amount);
    }

    return total;
}
```

`total` es una variable local. Cada invocación tiene su propia copia, por lo que no existe estado compartido.

Una causa frecuente de errores en aplicaciones Spring es convertir innecesariamente una variable local en un campo mutable de un bean singleton.

Incorrecto:

```java
@Service
public class PaymentService {

    private BigDecimal currentTotal;

    public BigDecimal calculate(List<BigDecimal> amounts) {
        currentTotal = BigDecimal.ZERO;

        for (BigDecimal amount : amounts) {
            currentTotal = currentTotal.add(amount);
        }

        return currentTotal;
    }
}
```

Como el servicio normalmente es singleton, dos requests pueden modificar `currentTotal` simultáneamente.

### 3. Message passing

Los threads intercambian mensajes mediante una cola, en lugar de modificar directamente el mismo objeto.

```text
Productores → BlockingQueue → Consumidores
```

La cola representa un límite claro de propiedad y coordinación. Este modelo suele ser más fácil de razonar que varios threads modificando una misma estructura.

### 4. Sincronización explícita

Si realmente debe existir estado mutable compartido, entonces se utilizan:

* `synchronized`;
* locks;
* variables atómicas;
* colecciones concurrentes;
* primitivas de coordinación.

La sincronización debería proteger invariantes concretas, no agregarse indiscriminadamente.

---

# 1. Java Memory Model

El Java Memory Model —JMM— define las reglas mediante las cuales los threads observan las operaciones de memoria realizadas por otros threads.

No debe asumirse que todos los threads ven inmediatamente el mismo valor de una variable.

## Visibilidad

Consideremos:

```java
public class Worker {

    private boolean running = true;

    public void execute() {
        while (running) {
            doWork();
        }
    }

    public void stop() {
        running = false;
    }
}
```

Un thread ejecuta `execute()` y otro ejecuta `stop()`.

Parece lógico esperar que el primer thread vea `running == false`. Sin embargo, sin sincronización, el JMM no garantiza cuándo —o incluso si— observará el cambio.

El compilador, la JVM o el procesador pueden:

* conservar el valor en un registro o caché;
* reordenar ciertas instrucciones;
* evitar lecturas repetidas que parecen innecesarias.

Este es un problema de visibilidad.

## Atomicidad

Una operación atómica se observa como una sola operación indivisible.

Una lectura o escritura simple de una referencia o de la mayoría de los primitivos es atómica. Pero una operación compuesta normalmente no lo es.

```java
counter++;
```

Conceptualmente implica:

```java
int current = counter;
int updated = current + 1;
counter = updated;
```

Dos threads pueden leer el mismo valor y sobrescribir sus resultados.

Por ejemplo:

```text
counter = 10

Thread A lee 10
Thread B lee 10
Thread A escribe 11
Thread B escribe 11
```

El resultado es `11`, aunque se realizaron dos incrementos.

Esto es una race condition.

## Ordenamiento

El orden escrito en el código fuente no siempre coincide exactamente con el orden interno de ejecución.

La JVM y el procesador pueden reordenar instrucciones si eso no modifica el resultado observable desde un único thread.

En concurrencia, ese reordenamiento puede importar si no se establecen relaciones de sincronización.

## Happens-before

La relación happens-before es una garantía de orden y visibilidad.

Si una acción A happens-before de una acción B:

1. los efectos de A son visibles para B;
2. A se considera ordenada antes que B.

No significa simplemente que A ocurrió antes según el reloj. Es una garantía formal del JMM.

Algunas reglas importantes:

### Orden dentro de un thread

Las acciones anteriores de un thread happens-before de sus acciones posteriores, según el orden del programa.

### Liberación y adquisición de un monitor

Todo lo realizado antes de liberar un monitor happens-before de la adquisición posterior del mismo monitor.

```java
synchronized (lock) {
    sharedValue = 100;
}
```

Si otro thread entra posteriormente a un bloque sincronizado usando el mismo `lock`, verá los cambios anteriores.

### Escritura y lectura de `volatile`

Una escritura sobre una variable `volatile` happens-before de una lectura posterior de esa misma variable.

### Inicio de un thread

Las acciones anteriores a `thread.start()` son visibles para el thread iniciado.

```java
config = loadConfig();

Thread thread = new Thread(() -> use(config));
thread.start();
```

### Finalización y `join()`

Las acciones realizadas por un thread happens-before del retorno exitoso de `join()` en otro thread.

```java
thread.start();
thread.join();

// Aquí se ven los efectos producidos por thread.
```

### `ExecutorService` y tareas

Las acciones anteriores al envío de una tarea son visibles para esa tarea. La obtención del resultado mediante `Future.get()` también establece garantías de visibilidad.

---

## Semántica de `volatile`

`volatile` garantiza principalmente:

* visibilidad;
* restricciones de reordenamiento;
* lectura y escritura de la variable con semántica de sincronización.

Ejemplo apropiado:

```java
public class Worker {

    private volatile boolean running = true;

    public void execute() {
        while (running) {
            doWork();
        }
    }

    public void stop() {
        running = false;
    }
}
```

Cuando un thread escribe:

```java
running = false;
```

el thread que vuelve a leer `running` puede observar el nuevo valor.

## Lo que `volatile` no garantiza

No convierte operaciones compuestas en atómicas.

Esto continúa siendo incorrecto:

```java
private volatile int counter;

public void increment() {
    counter++;
}
```

`volatile` hace visible el valor, pero dos threads todavía pueden perder incrementos.

Para solucionar esto podrían utilizarse:

```java
private final AtomicInteger counter = new AtomicInteger();

public void increment() {
    counter.incrementAndGet();
}
```

o:

```java
private int counter;

public synchronized void increment() {
    counter++;
}
```

## Cuándo utilizar `volatile`

Es apropiado cuando:

* un thread escribe una señal y otros la leen;
* el nuevo valor no depende del valor anterior;
* no debe preservarse una invariantes entre varias variables;
* la operación necesaria es una lectura o escritura independiente.

Ejemplos comunes:

* una bandera de configuración;
* una referencia a un snapshot inmutable;
* indicadores simples de estado.

```java
private volatile Configuration configuration;

public void reload() {
    configuration = loadImmutableConfiguration();
}
```

La referencia se reemplaza de manera segura. Los lectores utilizan un snapshot inmutable.

---

# 2. Sincronización y locks

## Locks intrínsecos y `synchronized`

Todo objeto Java puede actuar como monitor o lock intrínseco.

Método sincronizado:

```java
public synchronized void deposit(BigDecimal amount) {
    balance = balance.add(amount);
}
```

Es aproximadamente equivalente a:

```java
public void deposit(BigDecimal amount) {
    synchronized (this) {
        balance = balance.add(amount);
    }
}
```

También puede usarse un objeto privado:

```java
public class Account {

    private final Object lock = new Object();
    private BigDecimal balance = BigDecimal.ZERO;

    public void deposit(BigDecimal amount) {
        synchronized (lock) {
            balance = balance.add(amount);
        }
    }
}
```

Un lock privado evita que código externo sincronice accidentalmente sobre el mismo objeto.

## ¿Qué protege el lock?

El lock no “protege automáticamente una variable”. Protege una sección de código siempre que todos los accesos respeten el mismo protocolo.

Incorrecto:

```java
public synchronized void deposit(BigDecimal amount) {
    balance = balance.add(amount);
}

public BigDecimal getBalance() {
    return balance;
}
```

La escritura está sincronizada, pero la lectura no sigue el mismo mecanismo.

Una alternativa:

```java
public synchronized BigDecimal getBalance() {
    return balance;
}
```

Lo importante es documentar la relación:

```java
// Guarded by this
private BigDecimal balance;
```

## Proteger invariantes

Supongamos:

```java
private int available;
private int reserved;
```

La regla de negocio es:

```text
available + reserved = total
```

No basta con hacer cada variable visible independientemente. Ambas deben modificarse como una única transición consistente:

```java
public synchronized void reserve(int quantity) {
    if (available < quantity) {
        throw new IllegalStateException("Insufficient stock");
    }

    available -= quantity;
    reserved += quantity;
}
```

El lock protege la invariantes completa.

---

## `ReentrantLock`

`ReentrantLock` ofrece exclusión mutua como `synchronized`, pero con opciones adicionales:

* `tryLock()`;
* adquisición interrumpible;
* timeout;
* política de fairness;
* múltiples `Condition`.

```java
private final Lock lock = new ReentrantLock();
private int balance;

public void deposit(int amount) {
    lock.lock();

    try {
        balance += amount;
    } finally {
        lock.unlock();
    }
}
```

El `unlock()` debe estar dentro de `finally`. De lo contrario, una excepción podría dejar el lock adquirido permanentemente.

## `tryLock()`

Permite evitar una espera indefinida:

```java
if (lock.tryLock()) {
    try {
        updateResource();
    } finally {
        lock.unlock();
    }
} else {
    handleBusyResource();
}
```

Con timeout:

```java
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try {
        updateResource();
    } finally {
        lock.unlock();
    }
}
```

## Adquisición interrumpible

```java
lock.lockInterruptibly();

try {
    updateResource();
} finally {
    lock.unlock();
}
```

El thread puede responder a una interrupción mientras espera el lock.

## `synchronized` o `ReentrantLock`

Usar `synchronized` cuando:

* se necesita exclusión mutua simple;
* la sección crítica es pequeña;
* no se necesitan timeouts ni adquisición interrumpible;
* se busca menor complejidad.

Usar `ReentrantLock` cuando se necesita concretamente alguna de sus capacidades adicionales. No debería elegirse solamente porque parece “más avanzado”.

---

## Deadlock

Existe un deadlock cuando un conjunto de threads espera indefinidamente recursos retenidos por los otros.

```java
public void transfer(Account source, Account target, int amount) {
    synchronized (source) {
        synchronized (target) {
            source.withdraw(amount);
            target.deposit(amount);
        }
    }
}
```

Podría ocurrir:

```text
Thread A: bloquea cuenta 1 y espera cuenta 2
Thread B: bloquea cuenta 2 y espera cuenta 1
```

Ninguno puede avanzar.

## Lock ordering

Una solución es definir un orden global de adquisición.

```java
public void transfer(Account source, Account target, int amount) {
    Account first =
            source.id().compareTo(target.id()) < 0 ? source : target;

    Account second =
            first == source ? target : source;

    synchronized (first) {
        synchronized (second) {
            source.withdraw(amount);
            target.deposit(amount);
        }
    }
}
```

Todos los threads adquieren los locks en el mismo orden.

Si los identificadores pudieran coincidir, habría que definir también un mecanismo de desempate.

Otras estrategias contra deadlocks:

* evitar locks anidados;
* reducir el tiempo dentro del lock;
* no llamar servicios externos sosteniendo un lock;
* utilizar `tryLock()` con timeout;
* modelar la operación mediante un único propietario del estado;
* pasar mensajes en lugar de bloquear múltiples objetos.

---

## Livelock

En un livelock, los threads no están bloqueados. Continúan ejecutándose, pero reaccionan mutuamente de forma que ninguno progresa.

Ejemplo conceptual:

```text
Thread A detecta conflicto y cede.
Thread B detecta conflicto y cede.
Ambos reintentan simultáneamente.
Ambos vuelven a ceder.
```

Puede aparecer con reintentos demasiado coordinados.

Mitigaciones:

* backoff aleatorio;
* límites de reintentos;
* prioridades;
* coordinación central;
* evitar algoritmos excesivamente “corteses”.

---

## Starvation

Un thread sufre starvation cuando nunca obtiene suficiente acceso a CPU o a un recurso porque otros threads lo monopolizan.

Posibles causas:

* secciones críticas demasiado largas;
* demasiados threads;
* prioridades mal utilizadas;
* pools saturados;
* locks no justos bajo alta contención.

`ReentrantLock` permite solicitar fairness:

```java
Lock lock = new ReentrantLock(true);
```

Pero la equidad puede reducir el rendimiento. No debería activarse sin una necesidad concreta y medición.

---

# 3. Tipos atómicos y colecciones concurrentes

## Tipos atómicos

El paquete `java.util.concurrent.atomic` proporciona operaciones thread-safe sin utilizar un lock explícito en el código cliente.

Ejemplos:

* `AtomicInteger`;
* `AtomicLong`;
* `AtomicBoolean`;
* `AtomicReference`;
* `LongAdder`;
* `LongAccumulator`.

```java
private final AtomicInteger counter = new AtomicInteger();

public int increment() {
    return counter.incrementAndGet();
}
```

Otras operaciones:

```java
counter.get();
counter.set(10);
counter.addAndGet(5);
counter.compareAndSet(15, 20);
```

## Compare-and-set

`compareAndSet(expected, newValue)` actualiza el valor solamente si todavía coincide con el valor esperado.

```java
AtomicReference<State> state =
        new AtomicReference<>(State.CREATED);

boolean changed = state.compareAndSet(
        State.CREATED,
        State.PROCESSING
);
```

Esto sirve para implementar transiciones de estado:

```text
CREATED → PROCESSING
```

Solo un thread puede ganar la transición desde `CREATED`.

## Actualizaciones atómicas de objetos inmutables

```java
private final AtomicReference<Statistics> statistics =
        new AtomicReference<>(new Statistics(0, BigDecimal.ZERO));

public void register(BigDecimal amount) {
    statistics.updateAndGet(current ->
            new Statistics(
                    current.count() + 1,
                    current.total().add(amount)
            )
    );
}
```

```java
public record Statistics(
        long count,
        BigDecimal total
) {}
```

Se actualiza una referencia completa a un estado inmutable, evitando exponer un objeto parcialmente modificado.

## `LongAdder`

Bajo alta contención, muchos threads actualizando un `AtomicLong` compiten por el mismo valor.

`LongAdder` distribuye internamente las actualizaciones y luego calcula la suma.

```java
private final LongAdder requestCounter = new LongAdder();

public void registerRequest() {
    requestCounter.increment();
}

public long totalRequests() {
    return requestCounter.sum();
}
```

Es apropiado para:

* métricas;
* estadísticas;
* contadores con muchas escrituras.

No es ideal cuando se necesita que cada incremento devuelva un valor global exacto y secuencial.

---

## Colecciones concurrentes

Una colección tradicional no se vuelve segura simplemente porque su referencia sea `volatile`.

Incorrecto:

```java
private volatile Map<String, Payment> payments = new HashMap<>();
```

La referencia es visible, pero las operaciones internas del `HashMap` siguen sin ser thread-safe.

### `ConcurrentHashMap`

```java
private final ConcurrentMap<String, Payment> payments =
        new ConcurrentHashMap<>();
```

Permite acceso concurrente eficiente y ofrece operaciones compuestas atómicas.

## Error check-then-act

Esto no es atómico:

```java
if (!payments.containsKey(id)) {
    payments.put(id, payment);
}
```

Entre la comprobación y el `put`, otro thread podría insertar el mismo ID.

Utilizar:

```java
Payment existing = payments.putIfAbsent(id, payment);

if (existing != null) {
    throw new IllegalStateException("Payment already exists");
}
```

O:

```java
Payment payment = payments.computeIfAbsent(
        id,
        this::loadPayment
);
```

Hay que mantener breve y controlada la función de `computeIfAbsent`; no conviene usarla para trabajos lentos o efectos secundarios complejos.

### Otras colecciones útiles

| Colección               | Uso principal                               |
| ----------------------- | ------------------------------------------- |
| `ConcurrentHashMap`     | Mapa con lecturas y escrituras concurrentes |
| `ConcurrentLinkedQueue` | Cola no bloqueante                          |
| `CopyOnWriteArrayList`  | Muchas lecturas y muy pocas escrituras      |
| `BlockingQueue`         | Productor–consumidor y backpressure         |
| `ConcurrentSkipListMap` | Mapa concurrente ordenado                   |
| `ConcurrentSkipListSet` | Conjunto concurrente ordenado               |

## `CopyOnWriteArrayList`

Cada modificación crea una nueva copia interna.

```java
private final List<Listener> listeners =
        new CopyOnWriteArrayList<>();
```

Es apropiada para listas pequeñas que cambian poco, como listeners o configuraciones.

Es costosa si hay muchas escrituras.

## Una operación thread-safe no vuelve atómica toda una secuencia

Aunque cada método de una colección sea seguro, una secuencia de varios métodos puede tener una race condition.

Por eso deben buscarse operaciones como:

* `putIfAbsent`;
* `compute`;
* `computeIfAbsent`;
* `replace`;
* `remove(key, value)`.

Estas expresan la operación compuesta directamente.

---

# 4. Productor–consumidor

El patrón productor–consumidor desacopla:

* quién produce el trabajo;
* quién lo procesa;
* la velocidad de ambos.

`BlockingQueue` es una implementación natural de este patrón.

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);
```

El tamaño limitado es importante: evita que una producción ilimitada consuma toda la memoria.

## Productor

```java
public class Producer {

    private final BlockingQueue<Task> queue;

    public Producer(BlockingQueue<Task> queue) {
        this.queue = queue;
    }

    public void produce(Task task) throws InterruptedException {
        queue.put(task);
    }
}
```

Si la cola está llena, `put()` espera.

Esto genera backpressure: el productor reduce su velocidad cuando el consumidor no puede seguirle el ritmo.

## Consumidor

```java
public class Consumer implements Runnable {

    private final BlockingQueue<Task> queue;

    public Consumer(BlockingQueue<Task> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Task task = queue.take();
                process(task);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void process(Task task) {
        // Procesamiento
    }
}
```

`take()` espera si la cola está vacía.

---

## Interrupción de threads

Una interrupción es una solicitud cooperativa de cancelación. No mata forzosamente el thread.

```java
thread.interrupt();
```

Esto:

* activa el estado de interrupción;
* provoca `InterruptedException` en ciertos métodos bloqueantes.

Entre los métodos sensibles a interrupciones se encuentran:

* `Thread.sleep()`;
* `Thread.join()`;
* `BlockingQueue.put()` y `take()`;
* `Future.get()`;
* muchas operaciones de sincronización.

## No ignorar `InterruptedException`

Incorrecto:

```java
try {
    queue.take();
} catch (InterruptedException exception) {
    // Ignorada
}
```

El thread pierde la señal de cancelación y puede continuar cuando debería detenerse.

Una respuesta común:

```java
try {
    queue.take();
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    return;
}
```

Se restaura el indicador porque lanzar `InterruptedException` normalmente limpia el estado de interrupción.

Si el método puede propagar la excepción, suele ser todavía mejor:

```java
public Task waitForTask() throws InterruptedException {
    return queue.take();
}
```

La capa que conoce el ciclo de vida decide cómo finalizar.

---

## Graceful shutdown con `ExecutorService`

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

try {
    executor.submit(task1);
    executor.submit(task2);
} finally {
    executor.shutdown();

    try {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            List<Runnable> pending = executor.shutdownNow();

            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate");
            }
        }
    } catch (InterruptedException exception) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

### `shutdown()`

* deja de aceptar nuevas tareas;
* permite terminar las tareas enviadas;
* no espera a que terminen.

### `awaitTermination()`

Espera durante un tiempo determinado a que finalicen las tareas.

### `shutdownNow()`

* intenta interrumpir los threads activos;
* devuelve tareas que todavía no comenzaron;
* no garantiza que las tareas activas terminen.

Las tareas deben cooperar con la interrupción.

## Poison pill

Otra estrategia de finalización para productor–consumidor es insertar un mensaje especial:

```java
public record Task(String value) {
    public static final Task POISON_PILL = new Task("__STOP__");
}
```

Consumidor:

```java
while (true) {
    Task task = queue.take();

    if (task == Task.POISON_PILL) {
        break;
    }

    process(task);
}
```

Consideraciones:

* suele necesitarse una señal por consumidor;
* el mensaje no debe confundirse con una tarea real;
* hay que decidir si primero se drenan todas las tareas;
* la interrupción continúa siendo útil para cancelaciones inmediatas.

---

# 5. Diagnóstico de bugs de concurrencia

Los errores concurrentes suelen ser:

* intermitentes;
* difíciles de reproducir;
* sensibles a carga y timing;
* alterados por el logging o el debugger;
* dependientes del número de CPU.

Agregar un `sleep()` puede ocultar o provocar el problema, pero no demuestra que esté solucionado.

## Síntomas frecuentes

### Race condition

* valores incorrectos de forma ocasional;
* actualizaciones perdidas;
* entidades duplicadas;
* estados imposibles;
* pruebas que fallan esporádicamente;
* errores que desaparecen al depurar.

### Problema de visibilidad

* un thread no observa una bandera actualizada;
* un loop nunca termina;
* se leen configuraciones antiguas;
* un objeto parece parcialmente inicializado.

### Deadlock

* la aplicación permanece activa pero no progresa;
* requests detenidos indefinidamente;
* uso bajo de CPU;
* threads en estado `BLOCKED`;
* pool HTTP completamente ocupado.

### Livelock

* CPU activa;
* logs repetidos de reintentos;
* no existe progreso real;
* los threads cambian continuamente de estado.

### Starvation o pool exhaustion

* tareas pequeñas esperan demasiado;
* colas internas crecen;
* todos los threads del pool están bloqueados;
* timeouts en cascada;
* alta latencia aunque la CPU no esté saturada.

---

## Thread dump

Un thread dump muestra el estado y stack trace de los threads de la JVM.

Herramientas habituales:

```bash
jcmd <pid> Thread.print
```

```bash
jstack <pid>
```

También puede enviarse `SIGQUIT` en sistemas Unix:

```bash
kill -3 <pid>
```

Esto no finaliza normalmente la JVM; solicita que imprima un thread dump en su salida estándar o logs.

## Estados comunes

| Estado          | Significado general                          |
| --------------- | -------------------------------------------- |
| `NEW`           | Thread creado pero no iniciado               |
| `RUNNABLE`      | Ejecutándose o preparado para ejecutar       |
| `BLOCKED`       | Esperando entrar a un monitor `synchronized` |
| `WAITING`       | Esperando indefinidamente otra acción        |
| `TIMED_WAITING` | Esperando con timeout                        |
| `TERMINATED`    | Finalizó                                     |

Hay que interpretar el estado junto con el stack trace. Un thread `WAITING` no necesariamente representa un problema: los workers inactivos de un pool suelen esperar correctamente.

## Ejemplo conceptual de deadlock

Un thread dump podría mostrar:

```text
"thread-1"
    waiting to lock Account@B
    locked Account@A

"thread-2"
    waiting to lock Account@A
    locked Account@B
```

Esto revela el ciclo:

```text
thread-1 → espera B → retenido por thread-2
thread-2 → espera A → retenido por thread-1
```

La JVM puede incluir una sección como:

```text
Found one Java-level deadlock
```

## Cómo analizar un thread dump

1. Buscar detección automática de deadlock.
2. Revisar threads `BLOCKED`.
3. Identificar qué lock están esperando.
4. Buscar qué thread posee ese lock.
5. Comparar varios dumps tomados con algunos segundos de diferencia.
6. Detectar stacks que no cambian.
7. Revisar si pools completos están esperando IO, base de datos o servicios externos.
8. Relacionar los threads con métricas de colas, pools y latencia.

Un solo thread dump es una fotografía. Varios dumps permiten saber si existe progreso.

---

# Errores de diseño habituales

## Mantener un lock durante una operación remota

```java
synchronized (lock) {
    paymentClient.createPayment(request);
}
```

La llamada podría tardar varios segundos. Mientras tanto, todos los threads que necesitan ese lock quedan bloqueados.

Conviene separar:

1. lectura o transición mínima del estado;
2. llamada remota fuera del lock;
3. actualización final protegida.

Pero esa separación también debe considerar fallos y consistencia. En sistemas distribuidos suelen ser más apropiados:

* idempotencia;
* estados explícitos;
* eventos;
* outbox pattern;
* compensaciones.

## Sincronizar sobre objetos públicos o mutables

Evitar:

```java
synchronized (request) {
    // ...
}
```

No se controla quién más utiliza ese objeto como lock.

Preferir:

```java
private final Object lock = new Object();
```

## Crear demasiados threads

```java
for (Task task : tasks) {
    new Thread(() -> process(task)).start();
}
```

Esto puede causar:

* consumo excesivo de memoria;
* context switching;
* saturación de CPU;
* presión sobre base de datos y servicios externos.

Preferir un `ExecutorService` con capacidad controlada.

## Mezclar trabajos CPU-bound y bloqueantes

Una tarea que espera diez segundos por una API externa puede ocupar un thread que otras tareas necesitan.

En Java 17, normalmente conviene separar pools según el tipo de trabajo:

* pool CPU-bound;
* pool para IO bloqueante;
* scheduler;
* consumidores de mensajes.

El tamaño del pool debe relacionarse con:

* cantidad de CPU;
* proporción entre espera y procesamiento;
* capacidad de la base de datos;
* límites de conexiones;
* latencia de dependencias.

---

# Método práctico para diseñar código concurrente

Antes de implementar, conviene responder:

1. **¿Cuál es el estado compartido?**
   Si no puede señalarse con precisión, el diseño todavía no está claro.

2. **¿Puede ser inmutable?**
   Compartir snapshots inmutables simplifica mucho la solución.

3. **¿Puede confinarse a un solo thread?**
   Un único propietario puede modificarlo y publicar resultados.

4. **¿Puede reemplazarse la mutación directa por mensajes?**
   Una `BlockingQueue` puede establecer un límite claro.

5. **¿Cuál es la invariantes?**
   Por ejemplo: saldo nunca negativo o transición única de estado.

6. **¿Qué mecanismo protege esa invariantes?**
   Un lock, operación atómica o método atómico de una colección.

7. **¿Qué ocurre si una operación tarda o falla?**
   No debe retener innecesariamente recursos compartidos.

8. **¿Cómo se cancela?**
   Las tareas deben respetar interrupciones y timeouts.

9. **¿Cómo se limita la carga?**
   Pools y colas ilimitadas trasladan el problema a memoria o latencia.

10. **¿Cómo se observará en producción?**
    Son necesarias métricas de pools, colas, tiempos de espera, rechazos y timeouts.

# Idea central

Un buen diseño concurrente busca reducir las interacciones posibles:

```text
Inmutabilidad
    ↓
Confinamiento
    ↓
Message passing
    ↓
Operaciones atómicas y colecciones concurrentes
    ↓
Locks explícitos cuando una invariantes realmente lo exige
```

La sincronización es una herramienta necesaria, pero no sustituye un modelo claro de propiedad del estado. Si muchos threads pueden modificar libremente muchos objetos, agregar más locks suele trasladar el problema hacia deadlocks, contención y código difícil de mantener.





<br>
<br>
<br>


---




<br>
<br>
<br>
<br>



# ¿Qué significa “concurrente”?

En un contexto de threads, **concurrente** significa que varias tareas pueden avanzar durante un mismo período.

Por ejemplo, dos threads pueden acceder al mismo mapa:

```text
Thread A ── agrega una operación
Thread B ── consulta una operación
```

Sus ejecuciones pueden intercalarse:

```text
Thread A: inicia put()
Thread B: inicia get()
Thread A: termina put()
Thread B: termina get()
```

No necesariamente se ejecutan exactamente al mismo tiempo. Eso sería paralelismo. La concurrencia también existe en un único núcleo cuando el sistema alterna entre threads.

# ¿Qué protege `ConcurrentHashMap`?

`ConcurrentHashMap` protege la integridad de su estructura interna frente al acceso simultáneo de varios threads.

```java
ConcurrentMap<String, Operacion> operations =
        new ConcurrentHashMap<>();
```

Permite que diferentes threads ejecuten operaciones como:

```java
operations.get(key);
operations.put(key, operation);
operations.remove(key);
```

sin corromper internamente el mapa.

También proporciona garantías de visibilidad: cuando un thread inserta correctamente un valor, otros threads pueden verlo mediante las operaciones del mapa sin que tengas que agregar `volatile` o `synchronized` alrededor del mapa.

## El problema con `HashMap`

Un `HashMap` normal no está preparado para modificaciones concurrentes:

```java
Map<String, Operacion> operations = new HashMap<>();
```

Supongamos que dos requests POST se procesan al mismo tiempo:

```text
Thread A → operations.put("A", operacionA)
Thread B → operations.put("B", operacionB)
```

Con `HashMap`, el resultado no está garantizado. Pueden producirse:

* actualizaciones perdidas;
* lecturas inconsistentes;
* resultados incorrectos;
* problemas durante el redimensionamiento;
* corrupción de su estructura interna.

Con `ConcurrentHashMap`:

```java
ConcurrentMap<String, Operacion> operations =
        new ConcurrentHashMap<>();
```

ambos threads pueden operar de forma segura sobre el mapa.

# Qué significa que una operación sea thread-safe

Una operación thread-safe puede ser ejecutada simultáneamente por varios threads sin corromper el estado compartido.

Por ejemplo:

```java
operations.put("A", operation);
```

es una operación thread-safe en `ConcurrentHashMap`.

Esto no significa que todo el mapa quede bloqueado para cada lectura o escritura. Internamente intenta permitir concurrencia entre operaciones independientes, coordinando solamente lo necesario.

Por eso normalmente escala mejor que sincronizar manualmente un mapa completo:

```java
synchronized (operations) {
    operations.put("A", operation);
}
```

# Operaciones individuales protegidas

Estas operaciones son thread-safe:

```java
operations.get(key);
operations.put(key, value);
operations.remove(key);
operations.putIfAbsent(key, value);
operations.computeIfAbsent(key, this::createValue);
operations.replace(key, newValue);
```

Cada llamada individual se ejecuta con las garantías de concurrencia definidas por `ConcurrentHashMap`.

Por ejemplo:

```java
operations.put("OP-10", operation);
```

Otro thread no observará una inserción “a medias”. Verá:

* que la clave todavía no existe; o
* la clave asociada al objeto insertado.

No verá una estructura interna parcialmente modificada.

# Lo que `ConcurrentHashMap` no protege

Este punto es el más importante: `ConcurrentHashMap` protege sus operaciones y su estructura interna, pero no convierte automáticamente todo tu código en una operación atómica.

## Una secuencia de operaciones no es automáticamente atómica

Este código tiene una race condition:

```java
if (!operations.containsKey(key)) {
    operations.put(key, operation);
}
```

Cada operación individual es segura:

```java
operations.containsKey(key);
operations.put(key, operation);
```

Pero la combinación no es atómica.

Puede ocurrir:

```text
Estado inicial: la clave OP-10 no existe

Thread A: containsKey("OP-10") → false
Thread B: containsKey("OP-10") → false

Thread A: put("OP-10", operationA)
Thread B: put("OP-10", operationB)
```

Los dos threads creyeron que podían insertar. La segunda inserción reemplaza a la primera.

La solución es utilizar una operación compuesta y atómica:

```java
Operacion previous =
        operations.putIfAbsent(key, operation);

if (previous != null) {
    throw new DuplicateOperacionException(key);
}
```

`putIfAbsent()` realiza conceptualmente:

```text
“Si la clave no existe, insertar el valor”
```

como una única operación atómica.

Solo un thread puede ganar.

# ¿Qué significa “atómica”?

Una operación atómica sucede como una unidad indivisible desde el punto de vista de otros threads.

Con:

```java
operations.putIfAbsent(key, operation);
```

otro thread no puede intercalarse entre:

1. verificar si existe la clave;
2. insertar el valor.

Porque esas dos acciones forman una sola operación atómica proporcionada por el mapa.

Sin embargo, con:

```java
if (!operations.containsKey(key)) {
    operations.put(key, operation);
}
```

hay un espacio entre la verificación y la inserción donde otro thread puede intervenir.

# ¿Protege los objetos almacenados?

No. `ConcurrentHashMap` protege el mapa, pero no convierte sus valores en thread-safe.

Consideremos:

```java
public class Operacion {

    private BigDecimal importe;

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public BigDecimal getImporte() {
        return importe;
    }
}
```

El mapa puede almacenar correctamente la operación:

```java
operations.put("OP-10", operation);
```

Pero si varios threads modifican el mismo objeto:

```java
operations.get("OP-10").setImporte(newAmount);
```

`ConcurrentHashMap` no protege esas modificaciones internas.

El mapa protege:

```text
clave → referencia al objeto
```

No protege automáticamente:

```text
estado mutable dentro del objeto
```

Por eso es preferible utilizar valores inmutables:

```java
public record Operacion(
        String id,
        BigDecimal importe,
        EstadoOperacion estado
) {}
```

Para actualizar, se reemplaza el objeto completo:

```java
operations.computeIfPresent(
        id,
        (key, current) -> new Operacion(
                current.id(),
                newAmount,
                current.estado()
        )
);
```

`computeIfPresent()` realiza atómicamente la actualización asociada a esa clave.

# Ejemplo de actualización insegura

Aunque el mapa sea concurrente, esto puede fallar:

```java
Integer current = counters.get("payments");
counters.put("payments", current + 1);
```

Dos threads podrían hacer:

```text
Valor inicial = 10

Thread A: get() → 10
Thread B: get() → 10
Thread A: put(11)
Thread B: put(11)
```

Se pierde un incremento.

Una solución con `compute()`:

```java
counters.compute(
        "payments",
        (key, current) -> current == null
                ? 1
                : current + 1
);
```

La función de actualización se ejecuta atómicamente para esa clave.

También podría utilizarse:

```java
ConcurrentMap<String, AtomicInteger> counters =
        new ConcurrentHashMap<>();

counters
        .computeIfAbsent(
                "payments",
                key -> new AtomicInteger()
        )
        .incrementAndGet();
```

# ¿Bloquea todo el mapa?

Generalmente, no.

Un `ConcurrentHashMap` está diseñado para que:

* las lecturas sean altamente concurrentes;
* varias actualizaciones puedan avanzar cuando afectan partes diferentes;
* las actualizaciones de una misma clave se coordinen correctamente;
* no sea necesario bloquear globalmente el mapa para cada operación.

Por ejemplo:

```text
Thread A → put("OP-1", operation1)
Thread B → put("OP-2", operation2)
```

Estas operaciones pueden avanzar con mucha más independencia que si todo estuviera dentro de:

```java
synchronized (operations) {
    // ...
}
```

No es necesario conocer todos sus detalles internos para utilizarlo correctamente. El punto conceptual es que evita un único lock global para la mayoría de las operaciones.

# Iteración sobre `ConcurrentHashMap`

Puedes iterar mientras otros threads modifican el mapa:

```java
operations.forEach((key, value) ->
        System.out.println(key + ": " + value)
);
```

No suele lanzar `ConcurrentModificationException`.

Pero la iteración es débilmente consistente. Puede observar:

* algunos cambios realizados durante la iteración;
* otros cambios no;
* una vista razonable, pero no necesariamente un snapshot exacto de un instante.

Si necesitas un snapshot:

```java
List<Operacion> snapshot =
        List.copyOf(operations.values());
```

Aun así, mientras se construye la copia pueden existir cambios concurrentes. La copia queda estable una vez creada, pero no necesariamente representa un único instante global exacto.

Si el negocio requiere congelar perfectamente todo el estado, será necesaria una coordinación adicional.

# Aplicado a tus requests POST

Spring procesa normalmente diferentes requests HTTP usando diferentes threads:

```text
POST request 1 → Thread HTTP 1
POST request 2 → Thread HTTP 2
POST request 3 → Thread HTTP 3
```

Todos pueden llegar al mismo bean singleton:

```java
@Repository
public class InMemoryOperacionRepository {

    private final ConcurrentMap<String, Operacion> operations =
            new ConcurrentHashMap<>();
}
```

Para insertar sin duplicados:

```java
public Operacion save(Operacion operation) {
    Operacion existing = operations.putIfAbsent(
            operation.businessKey(),
            operation
    );

    if (existing != null) {
        throw new DuplicateOperacionException(
                operation.businessKey()
        );
    }

    return operation;
}
```

Si llegan dos POST con la misma clave:

```text
Thread A → putIfAbsent("EMPRESA-10:2026-07", operationA)
Thread B → putIfAbsent("EMPRESA-10:2026-07", operationB)
```

Solo uno inserta el valor. El otro recibe la operación que ya existía y puede responder con un conflicto, por ejemplo HTTP `409 Conflict`.

# Resumen

`ConcurrentHashMap` protege:

* la estructura interna del mapa;
* las operaciones concurrentes de lectura y escritura;
* la publicación y visibilidad de claves y valores;
* ciertas operaciones compuestas cuando usas métodos como `putIfAbsent()`, `compute()` o `replace()`.

No protege automáticamente:

* el estado mutable de los objetos almacenados;
* una secuencia de varias llamadas separadas;
* reglas de negocio que involucran varias claves;
* la coordinación entre diferentes instancias de la aplicación;
* la persistencia ante reinicios.

Por eso, en tu caso, la combinación más segura es:

```text
ConcurrentHashMap
+ clave de negocio
+ putIfAbsent()
+ objetos inmutables
```

Y la idea esencial es:

> `ConcurrentHashMap` hace seguro el acceso concurrente al mapa; no hace automáticamente thread-safe todo lo que guardas ni toda la lógica que realizas alrededor de él.
