# Excepciones


```
Throwable
|
├── Error (e.g., `OutOfMemoryError`, `StackOverflowError`)
|
└── Exception
     |
     ├── Checked
     |      (`IOException`, `FileNotFoundException`)
     |      Deben ser manejadas explícitamente con `try-catch` o declaradas con
     |      throws en la firma del método.
     |      Se verifica en tiempo de compilación, obliga al programador a
     |      considerar posibles errores recuperables.
     |
     └── Unchecked - RuntimeException
          (`NullPointerException`, `ArithmeticException`)
          No es obligatorio manejarlas o declararlas.
```

### Excepciones Personalizadas
Puedes crear tus propias excepciones heredando de `Exception` (para checked)
o `RuntimeException` (para unchecked).

Ejemplo:
```java
public class InvalidEmployeeException extends RuntimeException {
    public InvalidEmployeeException(String message) {
        super(message);
    }
}
```
