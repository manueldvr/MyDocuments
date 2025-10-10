# **Swagger**

## Schema


Para modificar el controlador `LoginController` y permitir que la lista `elements` en la respuesta pueda soportar diferentes tipos de DTOs utilizando una interfaz común llamada `BasicSchemaResponse`, mientras se genera un esquema claro en Swagger, necesitas implementar una solución que use polimorfismo con una interfaz común y configurar las anotaciones de Swagger para documentar correctamente los diferentes tipos de DTOs. 

Esto implica:

1. Definir una interfaz común `BasicSchemaResponse` que todos los DTOs implementen.
2. Crear una clase DTO para la respuesta (`EmpresasClientesResponseDTO`) que use una lista de tipo `List<BasicSchemaResponse>`.
3. Modificar el controlador para devolver esta nueva estructura.
4. Usar anotaciones de Swagger (`@Schema`, `@ApiResponse`) para documentar los posibles tipos de DTOs en la lista `elements`.

A continuación, te detallo cómo implementar esta solución paso a paso, incluyendo la configuración de Swagger para reflejar los diferentes tipos de DTOs.

---

### Solución: Usar una Interfaz Común (`BasicSchemaResponse`)

#### 1. Definir la Interfaz Común `BasicSchemaResponse`
Crea una interfaz `BasicSchemaResponse` que sirva como base para todos los DTOs que puedan aparecer en la lista `elements`.  
Esta interfaz puede incluir métodos comunes (si los hay) para garantizar consistencia entre los DTOs.

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Interfaz base para los DTOs de la respuesta")
public interface BasicSchemaResponse {
    @Schema(description = "Identificador único del elemento", example = "123")
    String getId();
}
```

#### 2. Crear DTOs que Implementen `BasicSchemaResponse`
Asegúrate de que los DTOs, como `LoginEmpresasClientesDTO` y cualquier otro tipo de DTO que el servicio pueda devolver, implementen la interfaz `BasicSchemaResponse`. Por ejemplo:

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClienteTipoA", description = "DTO para empresas clientes tipo A")
public class LoginEmpresasClientesDTO implements BasicSchemaResponse {
    @Schema(description = "Identificador único", example = "123")
    private String id;

    @Schema(description = "Nombre de la empresa", example = "Empresa A")
    private String nombre;

    @Override
    public String getId() {
        return id;
    }

    // Getters y Setters
    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
```

Otro DTO, por ejemplo, `OtroEmpresasClientesDTO`:

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClienteTipoB", description = "DTO para empresas clientes tipo B")
public class OtroEmpresasClientesDTO implements BasicSchemaResponse {
    @Schema(description = "Identificador único", example = "456")
    private String id;

    @Schema(description = "Descripción de la empresa", example = "Empresa B")
    private String descripcion;

    @Override
    public String getId() {
        return id;
    }

    // Getters y Setters
    public void setId(String id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
```

#### 3. Crear la Clase DTO de Respuesta
Crea una clase `EmpresasClientesResponseDTO` que use una lista de tipo `List<BasicSchemaResponse>` para soportar diferentes tipos de DTOs.

```java
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.Link;
import java.util.List;

@Schema(name = "EmpresasClientesResponse", description = "Respuesta de la consulta de empresas clientes")
public class EmpresasClientesResponseDTO {

    @Schema(description = "Nombre de la operación", example = "iv_empresas_clientes")
    private String name;

    @Schema(description = "Lista de elementos de diferentes tipos que implementan BasicSchemaResponse",
            subTypes = {LoginEmpresasClientesDTO.class, OtroEmpresasClientesDTO.class})
    private List<BasicSchemaResponse> elements;

    @Schema(description = "Enlaces HATEOAS relacionados con la respuesta")
    private List<Link> links;

    public EmpresasClientesResponseDTO() {}

    public EmpresasClientesResponseDTO(String name, List<? extends BasicSchemaResponse> elements, List<Link> links) {
        this.name = name;
        this.elements = (List<BasicSchemaResponse>) elements; // Cast seguro
        this.links = links;
    }

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BasicSchemaResponse> getElements() {
        return elements;
    }

    public void setElements(List<BasicSchemaResponse> elements) {
        this.elements = elements;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
```

**Nota**: La anotación `@Schema(subTypes = ...)` indica a Swagger los posibles tipos de DTOs que pueden estar en la lista `elements`. Si los DTOs tienen una propiedad discriminadora (por ejemplo, `type`), puedes usar `discriminatorProperty` y `discriminatorMapping` para una documentación más precisa, como se explica más adelante.

#### 4. Modificar el Controlador
Actualiza el método `getEmpresasClientes` para devolver un `ResponseEntity<EmpresasClientesResponseDTO>` y manejar la lista de `BasicSchemaResponse`. Asumimos que el servicio `getEmpresasClientes` ahora devuelve `Optional<List<? extends BasicSchemaResponse>>` para soportar diferentes tipos de DTOs.

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private final LoginService service;

    public LoginController(LoginService service) {
        this.service = service;
    }

    @Operation(summary = "Obtiene las empresas clientes asociadas a un CUIT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consulta exitosa",
                     content = @Content(schema = @Schema(implementation = EmpresasClientesResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "CUIT inválido")
    })
    @GetMapping("/iv_empresas_clientes")
    public ResponseEntity<EmpresasClientesResponseDTO> getEmpresasClientes(
            @Parameter(description = "CUIT del cliente (11 dígitos)", required = true, example = "20123456789")
            @RequestParam("CUIT") @NotBlank(message = "EL CUIT debe estar presente") String cuit,
            @Parameter(description = "Indica si se deben incluir referencias RESTful", example = "true")
            @RequestParam(value = "$displayRESTfulReferences", required = false, defaultValue = "true") boolean displayReferences,
            @Parameter(description = "Formato de la respuesta", example = "JSON")
            @RequestParam(value = "$format", required = false, defaultValue = "JSON") String format
    ) {
        log.info("Entering operacion Login.empresas_clientes...");

        Util.validateLogin(cuit);
        Optional<List<? extends BasicSchemaResponse>> elements = service.getEmpresasClientes(cuit, null, null);
        
        EmpresasClientesResponseDTO response;
        if (elements.isEmpty()) {
            response = new EmpresasClientesResponseDTO("iv_empresas_clientes", List.of(), List.of());
        } else {
            Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LoginController.class)
                    .getEmpresasClientes(cuit, displayReferences, format)).withSelfRel();
            response = new EmpresasClientesResponseDTO("iv_empresas_clientes", elements.get(), List.of(selfLink));
        }

        log.info("Exiting operacion Login.empresas_clientes.");
        return ResponseEntity.ok(response);
    }
}
```

#### 5. Configurar el Servicio
Asegúrate de que el método `service.getEmpresasClientes` devuelva `Optional<List<? extends BasicSchemaResponse>>` para soportar diferentes tipos de DTOs. Por ejemplo:

```java
public interface LoginService {
    Optional<List<? extends BasicSchemaResponse>> getEmpresasClientes(String cuit, String param1, String param2);
}
```

En la implementación del servicio, puedes devolver una lista que contenga instancias de `LoginEmpresasClientesDTO`, `OtroEmpresasClientesDTO`, o cualquier otro DTO que implemente `BasicSchemaResponse`.

#### 6. Configurar Swagger para Polimorfismo
Para que Swagger documente correctamente la lista `elements` que puede contener diferentes tipos de DTOs, ya hemos usado `@Schema(subTypes = ...)` en `EmpresasClientesResponseDTO`. Sin embargo, si los DTOs tienen una propiedad discriminadora (por ejemplo, un campo `type` que indica el tipo de DTO), puedes mejorar la documentación con `discriminatorProperty` y `discriminatorMapping`.

Por ejemplo, modifica los DTOs para incluir una propiedad `type`:

```java
@Schema(name = "ClienteTipoA", description = "DTO para empresas clientes tipo A")
public class LoginEmpresasClientesDTO implements BasicSchemaResponse {
    @Schema(description = "Tipo de DTO", example = "typeA")
    private String type = "typeA";

    @Schema(description = "Identificador único", example = "123")
    private String id;

    @Schema(description = "Nombre de la empresa", example = "Empresa A")
    private String nombre;

    @Override
    public String getId() {
        return id;
    }

    // Getters y Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}

@Schema(name = "ClienteTipoB", description = "DTO para empresas clientes tipo B")
public class OtroEmpresasClientesDTO implements BasicSchemaResponse {
    @Schema(description = "Tipo de DTO", example = "typeB")
    private String type = "typeB";

    @Schema(description = "Identificador único", example = "456")
    private String id;

    @Schema(description = "Descripción de la empresa", example = "Empresa B")
    private String descripcion;

    @Override
    public String getId() {
        return id;
    }

    // Getters y Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
```

Y actualiza `EmpresasClientesResponseDTO` para usar un discriminador:

```java
@Schema(name = "EmpresasClientesResponse", description = "Respuesta de la consulta de empresas clientes")
public class EmpresasClientesResponseDTO {

    @Schema(description = "Nombre de la operación", example = "iv_empresas_clientes")
    private String name;

    @Schema(description = "Lista de elementos de diferentes tipos que implementan BasicSchemaResponse",
            discriminatorProperty = "type",
            discriminatorMapping = {
                @io.swagger.v3.oas.annotations.media.DiscriminatorMapping(value = "typeA", schema = LoginEmpresasClientesDTO.class),
                @io.swagger.v3.oas.annotations.media.DiscriminatorMapping(value = "typeB", schema = OtroEmpresasClientesDTO.class)
            })
    private List<BasicSchemaResponse> elements;

    @Schema(description = "Enlaces HATEOAS relacionados con la respuesta")
    private List<Link> links;

    public EmpresasClientesResponseDTO() {}

    public EmpresasClientesResponseDTO(String name, List<? extends BasicSchemaResponse> elements, List<Link> links) {
        this.name = name;
        this.elements = (List<BasicSchemaResponse>) elements;
        this.links = links;
    }

    // Getters y Setters
}
```

### Explicación de los Cambios

1. **Interfaz `BasicSchemaResponse`**:
   - Define un contrato común para todos los DTOs, con un método `getId()` como ejemplo de propiedad común.
   - La anotación `@Schema` en la interfaz ayuda a Swagger a documentar la interfaz base.

2. **DTOs que Implementan `BasicSchemaResponse`**:
   - `LoginEmpresasClientesDTO` y `OtroEmpresasClientesDTO` implementan `BasicSchemaResponse` y tienen un campo `type` como discriminador.
   - Cada DTO usa `@Schema(name = ...)` para personalizar el nombre del esquema en Swagger (por ejemplo, `ClienteTipoA` y `ClienteTipoB`).

3. **Clase `EmpresasClientesResponseDTO`**:
   - Reemplaza `Map` por una clase estructurada con una lista de tipo `List<BasicSchemaResponse>`.
   - Usa `@Schema(subTypes = ...)` o `discriminatorProperty` para indicar a Swagger los posibles tipos de DTOs en la lista `elements`.

4. **Controlador**:
   - Cambia `ResponseEntity<?>` a `ResponseEntity<EmpresasClientesResponseDTO>` para un tipado claro.
   - Actualiza la lógica para usar `EmpresasClientesResponseDTO` en lugar de un `Map`.
   - Configura `@ApiResponse` para referenciar el esquema `EmpresasClientesResponseDTO`.

5. **Swagger con Discriminador**:
   - La propiedad `discriminatorProperty = "type"` le indica a Swagger que use el campo `type` en los DTOs para distinguir entre `ClienteTipoA` y `ClienteTipoB`.
   - Las anotaciones `discriminatorMapping` mapean los valores de `type` (`typeA`, `typeB`) a los esquemas correspondientes.

### Resultado en Swagger
En la UI de Swagger, el esquema de la respuesta se verá como:

```
EmpresasClientesResponse:
  type: object
  properties:
    name:
      type: string
      description: Nombre de la operación
      example: iv_empresas_clientes
    elements:
      type: array
      items:
        oneOf:
          - $ref: '#/components/schemas/ClienteTipoA'
          - $ref: '#/components/schemas/ClienteTipoB'
        discriminator:
          propertyName: type
          mapping:
            typeA: '#/components/schemas/ClienteTipoA'
            typeB: '#/components/schemas/ClienteTipoB'
    links:
      type: array
      items:
        type: object
        description: Enlaces HATEOAS relacionados con la respuesta

ClienteTipoA:
  type: object
  properties:
    type:
      type: string
      example: typeA
    id:
      type: string
      description: Identificador único
      example: 123
    nombre:
      type: string
      description: Nombre de la empresa
      example: Empresa A

ClienteTipoB:
  type: object
  properties:
    type:
      type: string
      example: typeB
    id:
      type: string
      description: Identificador único
      example: 456
    descripcion:
      type: string
      description: Descripción de la empresa
      example: Empresa B
```

### Notas Adicionales
- **Servicio**: Asegúrate de que `LoginService.getEmpresasClientes` devuelva una lista de tipo `List<? extends BasicSchemaResponse>`. Esto puede requerir ajustes en la lógica del servicio para manejar diferentes tipos de DTOs según el caso.
- **Propiedad Discriminadora**: El campo `type` es opcional pero recomendado para mejorar la claridad en Swagger. Si no quieres usar un discriminador, `@Schema(subTypes = ...)` es suficiente, aunque menos preciso.
- **Dependencias**: Verifica que tu proyecto use una versión compatible de `springdoc-openapi` (por ejemplo, `2.6.0` para Spring Boot 3.x):
  ```xml
  <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.6.0</version>
  </dependency>
  ```
- **Pruebas**: Accede a `http://localhost:8080/swagger-ui/index.html` para verificar que los esquemas se generen correctamente y que los tipos `ClienteTipoA` y `ClienteTipoB` aparezcan como posibles elementos en la lista `elements`.

Con esta solución, la lista `elements` puede contener diferentes tipos de DTOs que implementen `BasicSchemaResponse`, y Swagger documentará claramente cada tipo con nombres personalizados y soporte para polimorfismo.