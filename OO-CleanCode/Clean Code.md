
# Clean Code


Además del **Principio de Sustitución de Liskov (LSP)**, los otros principios **SOLID** que forman parte del marco de diseño de software orientado a objetos y que se alinean con los conceptos de *Clean Code* son los siguientes:

1. **Single Responsibility Principle (SRP) - Principio de Responsabilidad Única**  
   - **Definición**: Una clase debe tener una sola razón para cambiar, es decir, debe tener una única responsabilidad o propósito.  
   - **Explicación**: Cada clase o módulo debe enfocarse en una sola funcionalidad. Esto reduce la complejidad y hace que el código sea más fácil de entender, mantener y probar.  
   - **Ejemplo**: Una clase `Factura` no debería manejar tanto la lógica de cálculo de impuestos como la impresión del recibo. En su lugar, crea una clase `CalculadoraDeImpuestos` y otra `ImpresoraDeFacturas`.  
   - **Beneficio**: Código más modular, menos acoplamiento y cambios más seguros.

2. **Open/Closed Principle (OCP) - Principio de Abierto/Cerrado**  
   - **Definición**: Las clases deben estar abiertas para extensión, pero cerradas para modificación.  
   - **Explicación**: Debes poder añadir nuevas funcionalidades (extender) sin modificar el código existente. Esto se logra usando abstracciones (interfaces, clases abstractas) y polimorfismo.  
   - **Ejemplo**: Si tienes un sistema que dibuja formas geométricas, en lugar de modificar una clase `Dibujador` cada vez que añades una nueva forma, define una interfaz `Forma` con un método `dibujar()` y crea clases como `Circulo` o `Cuadrado` que la implementen.  
   - **Beneficio**: Reduce el riesgo de introducir errores al modificar código existente.

3. **Interface Segregation Principle (ISP) - Principio de Segregación de Interfaces**  
   - **Definición**: Los clientes no deben verse obligados a depender de interfaces que no usan.  
   - **Explicación**: Las interfaces deben ser específicas y pequeñas, en lugar de grandes y genéricas, para que las clases solo implementen lo que necesitan.  
   - **Ejemplo**: Si una interfaz `Trabajador` tiene métodos como `trabajar()`, `comer()` y `dormir()`, una clase `Robot` que implemente `Trabajador` se vería forzada a implementar `comer()` y `dormir()`, que no aplican. Mejor dividir en interfaces más específicas como `Trabajable` y `Humano`.  
   - **Beneficio**: Menor acoplamiento y clases más cohesivas.

4. **Dependency Inversion Principle (DIP) - Principio de Inversión de Dependencias**  
   - **Definición**: Los módulos de alto nivel no deben depender de módulos de bajo nivel; ambos deben depender de abstracciones. Además, las abstracciones no deben depender de detalles, sino los detalles de las abstracciones.  
   - **Beneficio**: Flexibilidad para cambiar implementaciones sin modificar el código de alto nivel.

### Contexto en *Clean Code*:
Estos principios SOLID, propuestos por Robert C. Martin, son fundamentales en *Clean Code* porque promueven un diseño de software que es:
- **Mantenible**: Fácil de modificar y extender.
- **Escalable**: Permite añadir funcionalidades con menos esfuerzo.
- **Testeable**: Facilita escribir pruebas unitarias al tener responsabilidades claras y dependencias bien gestionadas.
- **Comprensible**: Reduce la complejidad y hace el código más legible.

### Aplicación práctica:
Al seguir los principios SOLID, evitas problemas comunes como clases monolíticas, acoplamiento excesivo o jerarquías de herencia mal diseñadas. Por ejemplo:
- **SRP** te lleva a dividir una clase grande en varias más pequeñas y específicas.
- **OCP** fomenta el uso de patrones como Strategy o Factory.
- **LSP** asegura que las jerarquías de clases sean coherentes.
- **ISP** evita interfaces infladas que complican el código.
- **DIP** promueve la inyección de dependencias, una práctica común en frameworks modernos.

En resumen, los principios SOLID trabajan juntos para crear un código limpio, modular y robusto, alineándose con el objetivo de *Clean Code* de producir software de alta calidad que sea fácil de entender y mantener. Si necesitas un ejemplo más detallado de alguno de estos principios, ¡pídeme!

# LSP


**Liskov Substitution Principle (LSP)**, o Principio de Sustitución de Liskov.  
En el contexto de *Clean Code*, este principio se centra en garantizar que las clases derivadas (subclases) puedan sustituir a sus clases base (superclases) sin alterar el comportamiento correcto del programa.  
En otras palabras, los objetos de una subclase deben poder reemplazar a los objetos de su clase base sin introducir errores o comportamientos inesperados.

### Explicación sencilla del LSP:
Si tienes una clase base `A` y una clase derivada `B` que hereda de `A`, cualquier código que use `A` debería funcionar correctamente si se le pasa una instancia de `B`, sin necesidad de modificar el código o hacer suposiciones específicas sobre `B`. Esto asegura que la herencia se utilice de manera coherente y que el diseño sea robusto y mantenible.

### Clave del LSP en *Clean Code*:
- **Comportamiento coherente**: Las subclases deben respetar los contratos (métodos, propiedades, comportamientos) definidos por la clase base. No deben debilitar las precondiciones ni fortalecer las postcondiciones.
- **Evitar sorpresas**: El código que usa la clase base no debería fallar o comportarse de manera inesperada al usar una subclase.
- **Diseño limpio**: El LSP fomenta un diseño donde las jerarquías de clases son lógicas y consistentes, reduciendo la complejidad y mejorando la mantenibilidad.

### Ejemplo práctico:
Supongamos que tienes una clase base `Ave` con un método `volar()`:

```java
class Ave {
    public void volar() {
        System.out.println("El ave vuela");
    }
}
```

Y una clase derivada `Pajaro` que hereda de `Ave`:

```java
class Pajaro extends Ave {
    @Override
    public void volar() {
        System.out.println("El pájaro vuela alto");
    }
}
```

Esto cumple con el LSP porque un `Pajaro` puede sustituir a un `Ave` sin problemas: el método `volar()` sigue siendo válido y el comportamiento es consistente.

Ahora, imagina una clase `Pinguino` que también hereda de `Ave`:

```java
class Pinguino extends Ave {
    @Override
    public void volar() {
        throw new UnsupportedOperationException("Los pingüinos no vuelan");
    }
}
```

Este diseño **viola el LSP**, porque un código que espera que un `Ave` pueda `volar()` fallará si recibe un `Pinguino`. En lugar de lanzar una excepción, el diseño debería reconsiderarse, por ejemplo, separando las aves voladoras y no voladoras en jerarquías distintas:

```java
interface AveVoladora {
    void volar();
}

class Pajaro implements AveVoladora {
    public void volar() {
        System.out.println("El pájaro vuela alto");
    }
}

class Pinguino {
    public void nadar() {
        System.out.println("El pingüino nada");
    }
}
```

### Cómo aplicar el LSP en *Clean Code*:
1. **Define contratos claros**: Usa interfaces o clases base abstractas para establecer comportamientos esperados.
2. **Evita excepciones inesperadas**: Las subclases no deben introducir comportamientos que rompan las expectativas del código que usa la clase base.
3. **Revisa la jerarquía de clases**: Si una subclase no puede cumplir con el comportamiento de la clase base, considera refactorizar la jerarquía o usar composición en lugar de herencia.
4. **Piensa en el cliente**: El código que usa la clase base no debería necesitar saber qué subclase está usando.

### Beneficios en *Clean Code*:
- **Mantenibilidad**: Facilita la extensión del sistema sin introducir errores.
- **Reusabilidad**: Permite usar subclases de forma segura en cualquier lugar donde se espere la clase base.
- **Robustez**: Reduce la probabilidad de errores causados por comportamientos inesperados.

En resumen, el LSP promueve un diseño donde las jerarquías de clases son lógicas y predecibles, alineándose con los objetivos de *Clean Code* de crear software claro, mantenible y escalable. Si una subclase no puede sustituir completamente a su clase base, es una señal de que el diseño necesita ajustes.
