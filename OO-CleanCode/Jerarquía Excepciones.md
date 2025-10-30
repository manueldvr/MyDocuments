# Jerarquía de Excepciones en Java




```
Throwable
├── Error
│   ├── VirtualMachineError
│   │   ├── OutOfMemoryError
│   │   ├── StackOverflowError
│   │   └── ...
│   ├── LinkageError
│   │   ├── ClassNotFoundException
│   │   ├── NoClassDefFoundError
│   │   └── ...
│   ├── AWTError
│   └── ... (otros errores del sistema)
│
└── Exception
    ├── RuntimeException (Unchecked)
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── IllegalArgumentException
    │   ├── ClassCastException
    │   ├── ArithmeticException
    │   └── ... (no requieren try-catch)
    │
    └── Checked Exceptions (requieren manejo)
        ├── IOException
        │   ├── FileNotFoundException
        │   ├── EOFException
        │   └── ...
        ├── SQLException
        ├── ClassNotFoundException
        ├── ParseException
        ├── InterruptedException
        └── ... (deben ser capturadas o declaradas con throws)              
```

## ¿A partir de cuál puede extender el programador?

#### El programador puede extender de `Exception` o de `RuntimeException` para crear sus propias excepciones.

**Reglas clave:**


¡Hola! Claro que sí. A continuación te presento un **diagrama de clasificación de las excepciones en Java** (en formato de texto estructurado, fácil de visualizar y convertir a diagrama), y luego te explico **desde cuál puede extender el programador**.

---

### **Jerarquía de Excepciones en Java**

```
Throwable
├── Error
│   │
│   ├── VirtualMachineError
│   │   └── OutOfMemoryError ,  StackOverflowError , ...
│.  │
│   ├── LinkageError
│   │   ├── ClassNotFoundException ,  NoClassDefFoundError ,  ...
│   │
│   ├── AWTError
│   └── ... (otros errores del sistema)
│
└── Exception
    ├── RuntimeException (Unchecked)
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── IllegalArgumentException
    │   ├── ClassCastException
    │   ├── ArithmeticException
    │   └── ... (no requieren try-catch)
    │
    └── Checked Exceptions (requieren manejo)
        ├── IOException
        │   ├── FileNotFoundException
        │   ├── EOFException
        │   └── ...
        ├── SQLException
        ├── ClassNotFoundException
        ├── ParseException
        ├── InterruptedException
        └── ... (deben ser capturadas o declaradas con throws)
```

<br>


### **¿A partir de cuál puede extender el programador?**

> **El programador puede extender de `Exception` o de `RuntimeException` para crear sus propias excepciones.**

<br>

#### Reglas clave:

| Clase base             | Tipo                  | ¿Puede extender el programador? | Notas |
|------------------------|------------------------|-------------------------------|-------|
| `Throwable`            | Superclase raíz        | ⚠️ **No recomendado**         | Solo Oracle lo hace. |
| `Error`                | Errores graves del JVM | ❌ **NO**                      | No debes crear tus propios `Error`. Son para fallos del sistema. |
| `Exception`            | Excepción general      | ✅ **SÍ** (checked)            | Crea excepciones **checked** (obligan a `try-catch` o `throws`). |
| `RuntimeException`     | Excepción en tiempo de ejecución | ✅ **SÍ** (unchecked) | Crea excepciones **unchecked** (no obligan manejo explícito). |

<br>
<br>

### Ejemplos de extensiones válidas:

```java
// Excepción checked personalizada
public class SaldoInsuficienteException extends Exception {
    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}

// Excepción unchecked personalizada
public class UsuarioNoAutorizadoException extends RuntimeException {
    public UsuarioNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
```

<br>
<br>

### Recomendaciones

- Usa **`RuntimeException`** para errores de lógica o programación (como `NullPointer`, `IllegalArgument`).
- Usa **`Exception`** (checked) para condiciones recuperables que el llamador **debe manejar** (como archivo no encontrado, conexión fallida).





```
```