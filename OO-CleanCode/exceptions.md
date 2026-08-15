# Excepciones


## Introducción a excepciones en Java 21

Una excepción es un evento que ocurre durante la ejecución de un programa e interrumpe su flujo normal.

Por ejemplo:

```java
public class ExceptionDemo {

    public static void main(String[] args) {
        int resultado = 10 / 0;

        System.out.println(resultado);
    }
}
```

Java genera una `ArithmeticException` porque no se puede dividir un entero por cero:

```text
Exception in thread "main" java.lang.ArithmeticException: / by zero
```

## Jerarquía de excepciones

Las excepciones parten de la clase `Throwable`:

```java
Throwable
│
├── Error
│
└── Exception
      ├── RuntimeException  -Unchecked-
      │   │                 El compilador no obliga a capturarlas ni declararlas.
      │   │
      │   ├── NullPointerException
      │   ├── IllegalArgumentException
      │   ├── ArithmeticException
      │   └── IndexOutOfBoundsException
      │
      └── IOException, SQLException, ... -Checked exceptions-
                                          El compilador obliga a capturarlas.
```

### `Error`

Representa problemas graves de la JVM o del entorno:

```java
StackOverflowError
OutOfMemoryError
```

Normalmente, una aplicación **no** debería intentar recuperarse de ellos.

### `Exception`

Representa situaciones que una aplicación podría gestionar:

```java
IOException
SQLException
IllegalArgumentException
```


<br>

## Checked y Unchecked exceptions

### Checked exceptions

El compilador obliga a capturarlas o declararlas con `throws`.

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {

    public String leerArchivo(Path path) throws IOException {
        return Files.readString(path);
    }
}
```

El método que lo utiliza debe tratar la excepción:

```java
public static void main(String[] args) {
    FileService service = new FileService();

    try {
        String contenido = service.leerArchivo(Path.of("datos.txt"));
        System.out.println(contenido);
    } catch (IOException exception) {
        System.err.println("No se pudo leer el archivo");
    }
}
```

Ejemplos:

* `IOException`
* `SQLException`
* `ClassNotFoundException`

### Unchecked exceptions

Heradan de `RuntimeException`. El compilador no obliga a capturarlas ni declararlas.

```java
public double dividir(double dividendo, double divisor) {
    if (divisor == 0) {
        throw new IllegalArgumentException(
                "El divisor no puede ser cero"
        );
    }

    return dividendo / divisor;
}
```

Ejemplos:

* `NullPointerException`
* `IllegalArgumentException`
* `IllegalStateException`
* `IndexOutOfBoundsException`

Se suelen usar para representar:

* Parámetros inválidos.
* Estados incorrectos del objeto.
* Errores de programación.
* Incumplimiento de precondiciones.

## Estructura `try-catch`

El bloque `try` contiene el código que puede producir una excepción. El bloque `catch` define cómo gestionarla.

```java
try {
    int numero = Integer.parseInt("abc");
    System.out.println(numero);
} catch (NumberFormatException exception) {
    System.err.println("El valor no es un número válido");
}
```

El programa puede continuar después del `catch`:

```java
System.out.println("La aplicación continúa");
```

## Múltiples bloques `catch`

Un mismo bloque `try` puede producir diferentes excepciones:

```java
try {
    String texto = args[0];
    int numero = Integer.parseInt(texto);

    System.out.println(100 / numero);
} catch (ArrayIndexOutOfBoundsException exception) {
    System.err.println("No se recibió ningún argumento");
} catch (NumberFormatException exception) {
    System.err.println("El argumento no es numérico");
} catch (ArithmeticException exception) {
    System.err.println("El argumento no puede ser cero");
}
```

Los `catch` deben ordenarse desde la excepción más específica hasta la más general:

```java
try {
    // Operación
} catch (IllegalArgumentException exception) {
    // Excepción específica
} catch (RuntimeException exception) {
    // Excepción más general
}
```

El orden inverso no compila porque el segundo bloque sería inalcanzable.

## Multi-catch

Si varias excepciones tienen el mismo tratamiento, pueden agruparse:

```java
try {
    String valor = args[0];
    int numero = Integer.parseInt(valor);

    System.out.println(numero);
} catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
    System.err.println("Entrada inválida");
}
```

## Bloque `finally`

El bloque `finally` se ejecuta normalmente tanto si ocurre una excepción como si no:

```java
try {
    System.out.println("Ejecutando operación");
} catch (RuntimeException exception) {
    System.err.println("Ocurrió un error");
} finally {
    System.out.println("Liberando recursos");
}
```

Se utilizaba mucho para cerrar archivos, conexiones o streams. Actualmente, para recursos cerrables, se prefiere `try-with-resources`.

No es recomendable usar `return` dentro de `finally`, porque puede ocultar excepciones o reemplazar el resultado del método.

## `try-with-resources`

Cierra automáticamente los recursos que implementan `AutoCloseable`:

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReaderService {

    public void imprimirArchivo(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.lines().forEach(System.out::println);
        } catch (IOException exception) {
            System.err.println(
                    "No se pudo leer el archivo: " + exception.getMessage()
            );
        }
    }
}
```

El `BufferedReader` se cierra automáticamente, incluso si ocurre una excepción.

También pueden declararse varios recursos:

```java
try (
    var input = Files.newInputStream(Path.of("entrada.txt"));
    var output = Files.newOutputStream(Path.of("salida.txt"))
) {
    input.transferTo(output);
} catch (IOException exception) {
    System.err.println("No se pudo copiar el archivo");
}
```

Los recursos se cierran en orden inverso a su declaración.

## Diferencia entre `throw` y `throws`

### `throw`

Lanza una instancia concreta de una excepción:

```java
public void retirar(double saldo, double importe) {
    if (importe <= 0) {
        throw new IllegalArgumentException(
                "El importe debe ser mayor que cero"
        );
    }

    if (importe > saldo) {
        throw new IllegalStateException(
                "Saldo insuficiente"
        );
    }
}
```

### `throws`

Declara que un método puede propagar una excepción:

```java
public String cargarArchivo(Path path) throws IOException {
    return Files.readString(path);
}
```

En resumen:

```java
throw new IllegalArgumentException("Mensaje"); // Lanza
```

```java
void ejecutar() throws IOException { } // Declara
```

## Propagación de excepciones

Si un método no captura una excepción, esta se propaga al método que lo invocó:

```java
public String buscarConfiguracion() throws IOException {
    return leerConfiguracion();
}

private String leerConfiguracion() throws IOException {
    return Files.readString(Path.of("config.txt"));
}
```

La excepción continúa subiendo por la pila de llamadas hasta que:

* Algún método la captura.
* Llega al inicio del hilo y termina su ejecución.

## Excepciones personalizadas

Permiten expresar errores propios del dominio.

```java
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
```

Uso:

```java
public class CuentaBancaria {

    private double saldo;

    public CuentaBancaria(double saldoInicial) {
        this.saldo = saldoInicial;
    }

    public void retirar(double importe) {
        if (importe > saldo) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente para retirar " + importe
            );
        }

        saldo -= importe;
    }
}
```

También puede conservarse la causa original:

```java
public class ConfiguracionException extends RuntimeException {

    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
```

```java
public String cargarConfiguracion(Path path) {
    try {
        return Files.readString(path);
    } catch (IOException exception) {
        throw new ConfiguracionException(
                "No se pudo cargar la configuración desde " + path,
                exception
        );
    }
}
```

Esto se denomina *exception chaining*: agrega contexto sin perder la excepción original.

## Capturar o propagar

Conviene capturar una excepción cuando el método puede hacer algo útil:

* Recuperarse.
* Aplicar un valor alternativo.
* Reintentar la operación.
* Transformarla en una excepción del dominio.
* Traducirla a una respuesta HTTP en el límite de la aplicación.

Conviene propagarla cuando el método no sabe cómo resolverla.

Evita capturar una excepción únicamente para ignorarla:

```java
try {
    procesar();
} catch (Exception exception) {
    // No hacer nada: mala práctica
}
```

También evita usar `Exception` como primera opción:

```java
catch (Exception exception) {
    // Demasiado general
}
```

Es preferible capturar excepciones específicas:

```java
catch (IOException exception) {
    // Tratamiento concreto
}
```

## Mensajes útiles

Un mensaje de excepción debería explicar qué operación falló y aportar contexto seguro:

```java
throw new IllegalArgumentException(
        "La fecha desde no puede ser posterior a la fecha hasta"
);
```

En aplicaciones reales, evita incluir:

* Contraseñas.
* Tokens.
* Datos bancarios.
* Información personal sensible.

Al registrar la excepción, conserva el stack trace:

```java
logger.error("Error al procesar la operación {}", operacionId, exception);
```

No hagas esto:

```java
logger.error(exception.getMessage());
```

La segunda versión pierde la traza y dificulta encontrar el origen.

## Ejemplo completo

```java
import java.math.BigDecimal;

public class TransferenciaService {

    public void transferir(
            Cuenta origen,
            Cuenta destino,
            BigDecimal importe
    ) {
        validarImporte(importe);

        if (origen.saldo().compareTo(importe) < 0) {
            throw new SaldoInsuficienteException(
                    "La cuenta de origen no tiene saldo suficiente"
            );
        }

        origen.debitar(importe);
        destino.acreditar(importe);
    }

    private void validarImporte(BigDecimal importe) {
        if (importe == null) {
            throw new IllegalArgumentException(
                    "El importe es obligatorio"
            );
        }

        if (importe.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El importe debe ser mayor que cero"
            );
        }
    }
}
```

```java
public record Cuenta(String numero, BigDecimal saldo) {

    public void debitar(BigDecimal importe) {
        // Ejemplo simplificado
    }

    public void acreditar(BigDecimal importe) {
        // Ejemplo simplificado
    }
}
```

```java
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
```

## Buenas prácticas

* Utilizar excepciones específicas.
* No usar excepciones para controlar el flujo normal.
* No ignorar excepciones.
* Conservar la causa original al traducir una excepción.
* Usar `try-with-resources` para recursos cerrables.
* Evitar capturar `Throwable` o `Error`.
* Capturar las excepciones en la capa que pueda tratarlas correctamente.
* Crear excepciones personalizadas cuando agreguen significado al dominio.
* Incluir contexto útil en mensajes y logs.
* Evitar exponer detalles internos al cliente de una API.

La idea central es esta: **una excepción representa una situación anormal; debe gestionarse donde exista suficiente contexto para recuperarse, traducirla o informar el error correctamente.**



<br>
<br>

---

<br>
<br>

# Excepciones Personalizadas

Las excepciones personalizadas se clasifican principalmente según la clase de la cual heredan: **checked** o **unchecked**.

## 1. Excepciones personalizadas Checked

Heredan directamente de `Exception`, pero no de `RuntimeException`.

```java
public class ArchivoInvalidoException extends Exception {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public ArchivoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
```

El compilador obliga a:

* Capturarlas con `try-catch`.
* O declararlas mediante `throws`.

```java
public void procesarArchivo(Path path)
        throws ArchivoInvalidoException {

    if (Files.notExists(path)) {
        throw new ArchivoInvalidoException(
                "El archivo no existe: " + path
        );
    }
}
```

Uso:

```java
try {
    procesarArchivo(Path.of("datos.csv"));
} catch (ArchivoInvalidoException exception) {
    System.err.println(exception.getMessage());
}
```

Se usan cuando el llamador puede razonablemente recuperarse del problema.

Ejemplos:

* Archivo externo inválido.
* Recurso temporalmente inaccesible.
* Integración externa que no respondió.
* Operación cuya recuperación forma parte del contrato del método.

## 2. Excepciones personalizadas Unchecked

Heradan de `RuntimeException`.

```java
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }

    public SaldoInsuficienteException(
            String mensaje,
            Throwable causa
    ) {
        super(mensaje, causa);
    }
}
```

No es obligatorio capturarlas ni declararlas:

```java
public void retirar(BigDecimal importe) {
    if (importe.compareTo(saldo) > 0) {
        throw new SaldoInsuficienteException(
                "Saldo insuficiente"
        );
    }
}
```

Aunque es posible declararlas con `throws`, suele ser innecesario:

```java
public void retirar(BigDecimal importe)
        throws SaldoInsuficienteException {
    // Es válido, pero no obligatorio.
}
```

Se usan habitualmente para:

* Reglas de negocio incumplidas.
* Argumentos inválidos.
* Estado incorrecto de una entidad.
* Recursos no encontrados.
* Errores que se traducirán globalmente en el límite de la aplicación.

En aplicaciones Spring Boot, esta suele ser la opción más frecuente.

## Comparación

| Clasificación | Herencia           | Capturar o declarar | Uso habitual                                       |
| ------------- | ------------------ | ------------------: | -------------------------------------------------- |
| Checked       | `Exception`        |         Obligatorio | Problemas recuperables contemplados en el contrato |
| Unchecked     | `RuntimeException` |      No obligatorio | Reglas de negocio, validación y estados inválidos  |

## Clasificación por propósito

Además de la clasificación técnica, pueden organizarse semánticamente.

### Excepciones de dominio o negocio

Representan reglas propias del negocio:

```java
public class LimiteTransferenciaExcedidoException
        extends RuntimeException {

    public LimiteTransferenciaExcedidoException(
            BigDecimal importe,
            BigDecimal limite
    ) {
        super(
            "El importe %s supera el límite %s"
                .formatted(importe, limite)
        );
    }
}
```

Otros ejemplos:

```text
SaldoInsuficienteException
CuentaBloqueadaException
PagoDuplicadoException
LimiteTransferenciaExcedidoException
```

### Excepciones de aplicación

Representan fallos al ejecutar un caso de uso:

```java
public class TransferenciaException extends RuntimeException {

    public TransferenciaException(
            String mensaje,
            Throwable causa
    ) {
        super(mensaje, causa);
    }
}
```

Por ejemplo, podría envolver una excepción técnica y agregar contexto:

```java
try {
    transferenciaRepository.guardar(transferencia);
} catch (DataAccessException exception) {
    throw new TransferenciaException(
        "No se pudo registrar la transferencia",
        exception
    );
}
```

### Excepciones técnicas o de infraestructura

Representan problemas con recursos externos:

```java
public class ServicioBancarioNoDisponibleException
        extends RuntimeException {

    public ServicioBancarioNoDisponibleException(
            String mensaje,
            Throwable causa
    ) {
        super(mensaje, causa);
    }
}
```

Otros ejemplos:

```text
DatabaseOperationException
MessageBrokerException
ExternalServiceException
ConfiguracionException
```

### Excepciones de validación

Representan entradas que incumplen ciertas condiciones:

```java
public class FechaInvalidaException
        extends IllegalArgumentException {

    public FechaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
```

También pueden heredar de una excepción base del proyecto:

```java
public abstract class BusinessException
        extends RuntimeException {

    protected BusinessException(String mensaje) {
        super(mensaje);
    }
}
```

```java
public class FechaInvalidaException
        extends BusinessException {

    public FechaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
```

## Jerarquía personalizada

En proyectos grandes puede definirse una jerarquía común:

```text
RuntimeException
└── ApplicationException
    ├── BusinessException
    │   ├── SaldoInsuficienteException
    │   └── CuentaBloqueadaException
    ├── ResourceNotFoundException
    └── InfrastructureException
        └── ExternalServiceException
```

Ejemplo:

```java
public abstract class ApplicationException
        extends RuntimeException {

    protected ApplicationException(String mensaje) {
        super(mensaje);
    }

    protected ApplicationException(
            String mensaje,
            Throwable causa
    ) {
        super(mensaje, causa);
    }
}
```

```java
public abstract class BusinessException
        extends ApplicationException {

    protected BusinessException(String mensaje) {
        super(mensaje);
    }
}
```

```java
public final class CuentaBloqueadaException
        extends BusinessException {

    public CuentaBloqueadaException(String cuentaId) {
        super("La cuenta está bloqueada: " + cuentaId);
    }
}
```

No conviene crear una jerarquía demasiado profunda si no aporta un tratamiento diferente.

## ¿Cuál elegir?

Una guía práctica:

* Hereda de `Exception` cuando quieres obligar al llamador a tomar una decisión explícita.
* Hereda de `RuntimeException` cuando representa una regla de negocio, una precondición o un error que será tratado en una capa superior.
* Hereda de una excepción estándar más específica cuando la semántica coincide:

```java
public class EdadInvalidaException
        extends IllegalArgumentException {
}
```

* Conserva la causa original cuando traduzcas errores:

```java
throw new ExternalServiceException(
    "No se pudo consultar el servicio de cuentas",
    exception
);
```

En un backend moderno con Java 21 y Spring Boot, una organización común sería:

```text
RuntimeException
├── BusinessException
├── ResourceNotFoundException
├── ValidationException
└── InfrastructureException
```

Después, `@RestControllerAdvice` puede traducir cada categoría a la respuesta HTTP correspondiente.
