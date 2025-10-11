# Tests


Siendo SpringBoot y Java 21. Podrias explicar el siguiente código de un Test paso a paso:


```java
@WebMvcTest(ReporteOperacionesController.class)
public class ReporteOperacionesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteOperacionesService service;

@Test 
    void obtenerReporteOperacionesTotalMensual_Success() throws Exception {
        when(service.obtenerReporteMensual(
                "149", "404", "01/01/2024", "01/12/2025", 0, 10))
                .thenReturn(Optional.of(List.of(new ReporteOperacionesDTO())));
        mockMvc.perform(get("/view/iv_reporte_operaciones_total_mensual")
                        .param("P_COD_CIA", "149")
                        .param("P_SUB_EMP", "404")
                        .param("P_FECHA_DESDE", "01/01/2024")
                        .param("P_FECHA_HASTA", "01/12/2025")
                        .param("$start_index", "0")
                        .param("$count", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iv_reporte_operaciones_total_mensual"))
                .andExpect(jsonPath("$.elements").isArray());
        verify(service).obtenerReporteMensual("149", "404", "01/01/2024", "01/12/2025", 0, 10);
    }
}
```

---


Aquí tienes una explicación paso a paso clara y detallada del código de tu test con **Spring Boot y Java 21** usando `@WebMvcTest` y `MockMvc`:

---

### 1. Anotaciones y Setup Inicial:

```java
@WebMvcTest(ReporteOperacionesController.class)
```

- Esta anotación le indica a Spring Boot que ejecute un test **solo para la capa del controlador web** (`ReporteOperacionesController`).  
- Se crea un contexto mínimo que incluye solo este controlador y las dependencias relacionadas como `MockMvc`.

```java
@Autowired
private MockMvc mockMvc;
```

- **`MockMvc`** es un objeto proporcionado por Spring que simula peticiones HTTP hacia los endpoints del controlador, permitiendo probarlos sin levantar un servidor real.

```java
@MockitoBean
private ReporteOperacionesService service;
```

- Aquí podría haber un error de tipeo. Normalmente esta anotación es `@MockBean`. 
  Debería verse así:

  ```java
  @MockBean
  private ReporteOperacionesService service;
  ```

- `@MockBean` indica que se va a crear un **mock** de `ReporteOperacionesService` y que se inyectará automáticamente en el contexto de Spring para el test.

---

### 2. Método de prueba explicado paso a paso:

```java
@Test
void obtenerReporteOperacionesTotalMensual_Success() throws Exception {
```

- Declaración de un test unitario específico.

---

### 3. Definición del comportamiento simulado:

```java
when(service.obtenerReporteMensual(
    "149", "404", "01/01/2024", "01/12/2025", 0, 10))
    .thenReturn(Optional.of(List.of(new ReporteOperacionesDTO())));
```

- Se establece que, cuando el método `service.obtenerReporteMensual(...)` sea llamado con los parámetros específicos:
  - `"149"` (P_COD_CIA)
  - `"404"` (P_SUB_EMP)
  - `"01/01/2024"` (fecha desde)
  - `"01/12/2025"` (fecha hasta)
  - `0` (índice de inicio de paginación)
  - `10` (cantidad máxima de resultados)

- Entonces debe retornar una lista envuelta en un `Optional`. Esta lista contiene un objeto dummy (`new ReporteOperacionesDTO()`) que simula la respuesta esperada del servicio.

---

### 4. Simulación de la llamada HTTP usando MockMvc:

```java
mockMvc.perform(get("/view/iv_reporte_operaciones_total_mensual")
        .param("P_COD_CIA", "149")
        .param("P_SUB_EMP", "404")
        .param("P_FECHA_DESDE", "01/01/2024")
        .param("P_FECHA_HASTA", "01/12/2025")
        .param("$start_index", "0")
        .param("$count", "10"))
```

- Aquí se simula una petición HTTP GET hacia el endpoint:
  
  ```
  /view/iv_reporte_operaciones_total_mensual
  ```

- Se incluyen los parámetros de consulta que se pasarán al controlador como parte del request:
  - `P_COD_CIA=149`
  - `P_SUB_EMP=404`
  - `P_FECHA_DESDE=01/01/2024`
  - `P_FECHA_HASTA=01/12/2025`
  - `$start_index=0`
  - `$count=10`

---

### 5. Verificación de la respuesta del controlador:

```java
.andExpect(status().isOk())
```

- Se verifica que la respuesta HTTP devuelta por el controlador sea **200 OK**.

```java
.andExpect(jsonPath("$.name").value("iv_reporte_operaciones_total_mensual"))
```

- Valida que el JSON de respuesta tenga una propiedad llamada `name` con el valor esperado `"iv_reporte_operaciones_total_mensual"`.

```java
.andExpect(jsonPath("$.elements").isArray());
```

- Asegura que el JSON de respuesta tenga una propiedad `elements` que es un **arreglo JSON**.

---

### 6. Verificación de interacción con el servicio mockeado:

```java
verify(service).obtenerReporteMensual("149", "404", "01/01/2024", "01/12/2025", 0, 10);
```

- Esta línea verifica explícitamente que durante la ejecución del test se llamó exactamente una vez al método `obtenerReporteMensual` del servicio mockeado (`service`) con los parámetros especificados.

---

## Resumen simplificado del flujo:

1. Spring crea contexto limitado con `ReporteOperacionesController`.
2. `MockMvc` ejecuta una petición HTTP simulada al endpoint definido.
3. El servicio `ReporteOperacionesService` se mockea para devolver una respuesta controlada y predeterminada.
4. El controlador responde con una respuesta HTTP y JSON.
5. El test verifica el estado HTTP, contenido JSON y la interacción con el servicio.

---

### 🔎 **Puntos importantes y correcciones sugeridas:**

- Corrige la anotación de Mockito:

  ```diff
  - @MockitoBean
  + @MockBean
  ```

- Asegúrate de que la estructura JSON simulada (`ReporteOperacionesDTO`) en realidad incluya un atributo `"name"` con el valor `"iv_reporte_operaciones_total_mensual"` y un arreglo `"elements"`, de lo contrario el test fallará.

Con eso claro, tu test quedaría perfecto para validar correctamente la interacción del controlador y su servicio asociado.




---


.  
.  






# Verify


Claro, profundicemos específicamente en el paso:

## ✅ **Verificación de la interacción con el servicio mockeado**

Este paso se refiere a la siguiente línea del código:

```java
verify(service).obtenerReporteMensual("149", "404", "01/01/2024", "01/12/2025", 0, 10);
```

---

### 🔎 ¿Qué hace exactamente esta línea?

Cuando utilizas Mockito, `verify` te permite comprobar si un método de un mock fue invocado durante la ejecución de la prueba. En otras palabras, confirma que tu código realmente llamó al método esperado en el mock, con los parámetros correctos.

Aquí, la línea:

- Comprueba que el método `obtenerReporteMensual(...)` del mock `service` fue invocado exactamente una vez.
- Además, valida que la invocación haya ocurrido exactamente con los parámetros:
  - `"149"`
  - `"404"`
  - `"01/01/2024"`
  - `"01/12/2025"`
  - `0`
  - `10`

Si el método no se invocó nunca, o se invocó con parámetros distintos, esta línea generará un error, indicando que la prueba no cumplió la expectativa.

---

### 📌 **¿Por qué es importante hacer esto?**

- **Validar interacciones**: Esta técnica te permite asegurar que tu controlador llama correctamente a los métodos del servicio. No solo verificas que el resultado final sea el esperado, sino también que la comunicación entre tus capas funciona como se diseñó.

- **Evitar falsos positivos**: Si no incluyes esta verificación, tu prueba podría dar un falso positivo: parecer exitosa porque la respuesta es aparentemente correcta, pero quizás nunca ejecutó la lógica clave que estabas tratando de probar.

- **Mejorar robustez del test**: Al verificar explícitamente la interacción, te aseguras de que cambios futuros en el controlador (por ejemplo, modificaciones accidentales de parámetros o eliminación de llamadas) sean detectados inmediatamente en tus pruebas.

---

### ⚠️ **Ejemplos ilustrativos adicionales:**

Por ejemplo, supón que:

- **Caso correcto** (éxito en la prueba):

```java
verify(service).obtenerReporteMensual("149", "404", "01/01/2024", "01/12/2025", 0, 10);
// Método invocado exactamente una vez, con estos parámetros. ✅
```

- **Caso incorrecto** (error en la prueba):

Si en tu controlador escribes accidentalmente algo como:

```java
service.obtenerReporteMensual("149", "404", "05/05/2024", "01/12/2025", 0, 10);
```

Notarás que la prueba fallará con un mensaje similar a este:

```
Wanted but not invoked:
service.obtenerReporteMensual("149", "404", "01/01/2024", "01/12/2025", 0, 10);

However, there was exactly 1 interaction with this mock:
service.obtenerReporteMensual("149", "404", "05/05/2024", "01/12/2025", 0, 10);
```

Este mensaje indica claramente cuál es el error: llamaste al método con parámetros distintos a los esperados.

---

### 🚩 **Formas avanzadas de verificación (opcional)**

Si quisieras verificar también cuántas veces fue llamado un método, podrías usar lo siguiente:

```java
verify(service, times(1)).obtenerReporteMensual(...);
```

O si quisieras asegurar que nunca se llamó:

```java
verify(service, never()).obtenerReporteMensual(...);
```

---

### 📌 **Conclusión resumida:**

La línea:

```java
verify(service).obtenerReporteMensual("149", "404", "01/01/2024", "01/12/2025", 0, 10);
```

te ayuda a asegurar **de manera explícita y precisa** que la llamada al método en tu servicio mockeado ocurrió exactamente con los parámetros que esperabas, fortaleciendo la confiabilidad y precisión de tus pruebas unitarias.

Espero que esto aclare detalladamente la utilidad y el valor de este paso en tu test.