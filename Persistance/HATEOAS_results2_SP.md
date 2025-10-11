# PROMTP:


Hi, please, make a GET HATEOAS RESTful service using Spring Boot.

The Controller should accept the following path with its mandatory parameters: 
{{base_url}}/views/Iv_transmision_umbral?P_COD_CIA=603&P_FECHA_DESDE=01%2F01%2F2024&P_FECHA_HASTA=03%2F01%2F2024&%24displayRESTfulReferences=true&%24format=JSON  
and  accept the follwing parameters with itsname, optionallity, description and type separated by commas:

TIPO , optional,Virtual DataPort type, String
ESTADO , optional,Virtual DataPort type, String
COD_SERVICIO , optional,Virtual DataPort type, String
DESC_SERVICIO , optional, Virtual DataPort type, String
P_COD_CIA , necessary, Virtual DataPort type, String
P_FECHA_DESDE ,necessary, Fecha de inicio , String
P_FECHA_HASTA ,necessary, Fecha final, String
$count,	optional, Used for pagination in view resources,	integer($int32)	 
$start_index,	optional, Used for pagination in view resources,	integer($int32)	 
$displayRESTfulReferences,optional,	By default the result of requesting a view contains in each row,a link to the row itself and for each association of the view a link to traverse the association. By using this parameter you can configure this behavior. ,	boolean	  
$expand, 	optional, Roles to expand in the query	, array[string]	 
$filter	, optional, Filters the rows of a view using any condition. Any expression that can appear in the WHERE clause of a VQL query can be used., 	string  
$format, 	optional,Defines the output format of the query, 	string	 
$groupby,	optional,Comma-separated list of fields to group by with	, array[string]  
$having,	optional,Comma-separated list of fields to add to the HAVING clause of the query	, array[string]	 
$orderby,	optional,Sorts the results by one or more fields. It is a comma-separated list of fields, each one followed by the modifier ASC (for ascending order) and DESC (for descending order).	,array[string]  
$select,	optional,Comma-separated list of fields to be returned in the result	, array[string]  
$jsoncallback,optional,	The JSON representation can return the data of a view prefixed with the name of a function. This is called JSON with padding or JSONP. That way when a browser receives the response it receives a script rather than data.	,string  
$noescapehtml,optional,  List of comma-separated fields whose values will not be HTML escaped.	,array[string]  
User-agent	, optional, By including this header communication between the web service and the Virtual DataPort will be configured with this value.,	string  

And returns:

{"name":"Iv_transmision_umbral","elements":[{"ENTIDAD":603,"ARCHIVO_COMPRIMIDO":"603G1016.ZIP","BYTES":300,"ARCHIVO":"PR090124","TIPO":"Enviado","ESTADO":"OK","FECHA_HORA":"2024-01-10T01:06:16","COD_SERVICIO":5,"DESC_SERVICIO":"RAPIPAGO","FECHA":"10/01/2024"},{"ENTIDAD":603,"ARCHIVO_COMPRIMIDO":"603G0917.ZIP","BYTES":1200,"ARCHIVO":"PR080124","TIPO":"Enviado","ESTADO":"OK","FECHA_HORA":"2024-01-09T01:08:04","COD_SERVICIO":5,"DESC_SERVICIO":"RAPIPAGO","FECHA":"09/01/2024"}],"links":[{"rel":"self","href":"http://denodo-dv-01.prod.gire.com:9090/server/rapipago/certant_api/views/Iv_transmision_umbral?P_FECHA_HASTA=10%2F01%2F2024&P_FECHA_DESDE=01%2F01%2F2024&P_COD_CIA=603&%24displayRESTfulReferences=true&%24format=JSON&%24start_index=0&%24count=2"},{"rel":"prev","title":"Previous interval","href":"?P_FECHA_HASTA=10%2F01%2F2024&%24displayRESTfulReferences=true&P_FECHA_DESDE=01%2F01%2F2024&P_COD_CIA=603&%24format=JSON&%24count=2"},{"rel":"next","title":"Next interval","href":"?P_FECHA_HASTA=10%2F01%2F2024&%24displayRESTfulReferences=true&P_FECHA_DESDE=01%2F01%2F2024&%24start_index=2&P_COD_CIA=603&%24format=JSON&%24count=2"}]}


The Service use a Repository that execute the following store procedure: 
UMBRAL.get_transmision_umbral with the following parameters:
    p_cod_cia     IN VARCHAR2 DEFAULT NULL,
    p_fecha_desde IN VARCHAR2,
    p_fecha_hasta  IN VARCHAR2,
    p_result      OUT SYS_REFCURSOR
	
and returns a list of TransmicionUmbralResult elements, where TransmicionUmbralResult represents elements or rows containing:
   ENTIDAD,
   ARCHIVO_COMPRIMIDO,
   BYTES,
   ARCHIVO,
   TIPO,
   ESTADO,
   FECHA,
   FECHA_HORA,
   COD_SERVICIO,
   DESC_SERVICIO


Please explain step y step all the necessary code at the controller, the validations beeded for the parameters. 
And the service should execute a Store Procedure through a Persistance API based on Spring Data.


____________/



field	description	type	list

$count,	Used for pagination in view resources,	integer($int32)  
$start_index,	Used for pagination in view resources,	integer($int32)  
$displayRESTfulReferences,	By default, the result of requesting a view contains, in each row, a link to the row itself and, for each association of the view, a link to traverse the association. By using this parameter, you can configure this behavior. ,	boolean  
$expand, 	Roles to expand in the query	, array[string]	  
$filter	, Filters the rows of a view using any condition. Any expression that can appear in the WHERE clause of a VQL query can be used., 	string  
$format, 	Defines the output format of the query, 	string  
$groupby,	Comma-separated list of fields to group by with	, array[string]  
$having,	Comma-separated list of fields to add to the HAVING clause of the query	, array[string]  
$orderby,	Sorts the results by one or more fields. It is a comma-separated list of fields, each one followed by the modifier ASC (for ascending order) and DESC (for descending order).	,array[string]  
$select,	Comma-separated list of fields to be returned in the result	, array[string]  
$jsoncallback,	The JSON representation can return the data of a view prefixed with the name of a function. This is called JSON with padding or JSONP. That way, when a browser receives the response, it receives a script rather than data.	,string  
$noescapehtml	,List of comma-separated fields whose values will not be HTML escaped.	,array[string]  
User-agent	,By including this header, communication between the web service and the Virtual DataPort will be configured with this value.,	string  





---




guía paso a paso para implementar dos métodos GET HATEOAS en Spring Boot. 
cómo implementar los dos métodos GET en Spring Boot usando HATEOAS, validaciones de parámetros y ejecución de un procedimiento almacenado con Spring Data.  

---

### **1. Definir la Entidad (DTO)**
Como se trata de un procedimiento almacenado que devuelve un conjunto de datos, vamos a definir un DTO que represente cada fila del resultado:

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReporteOperacionesDTO {
    private Integer empresa;
    private Integer codigoEntidad;
    private String marcaEntidad;
    private String tipoDebito;
    private String fechaDeCobro;
    private String codigoDeMoneda;
    private Integer cantidadAceptados;
    private Double importeAceptados;
    private Integer cantidadRechazados;
    private Double importeRechazados;
}
```

---

### **2. Repositorio con Spring Data JPA para Llamar al Procedimiento Almacenado**
Vamos a utilizar Spring Data JPA para llamar al procedimiento almacenado en DB2.

```java
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReporteOperacionesRepository extends CrudRepository<ReporteOperacionesDTO, Integer> {

    @Procedure(name = "sp_reporte_operaciones_diario")
    List<ReporteOperacionesDTO> obtenerReporteOperaciones(
        @Param("P_COD_CIA") String pCodCia,
        @Param("P_SUB_EMP") Integer pSubEmp,
        @Param("P_FECHA_DESDE") String pFechaDesde,
        @Param("P_FECHA_HASTA") String pFechaHasta,
        @Param("$count") Integer count,
        @Param("$start_index") Integer startIndex
    );
}
```

Aquí:
- `sp_reporte_operaciones_diario` es el procedimiento almacenado en la base de datos.
- Recibe parámetros obligatorios y opcionales para paginación.
- Retorna una lista de `ReporteOperacionesDTO`.

---

### **3. Servicio que Llama al Repositorio**
Este servicio encapsula la lógica de negocio y maneja excepciones.

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReporteOperacionesService {

    private final ReporteOperacionesRepository repository;

    public ReporteOperacionesService(ReporteOperacionesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ReporteOperacionesDTO> obtenerReporte(
            String pCodCia, Integer pSubEmp, String pFechaDesde, String pFechaHasta, Integer count, Integer startIndex) {
        
        try {
            return repository.obtenerReporteOperaciones(pCodCia, pSubEmp, pFechaDesde, pFechaHasta, count, startIndex);
        } catch (Exception e) {
            throw new RuntimeException("Error al ejecutar el procedimiento almacenado", e);
        }
    }
}
```

---

### **4. Controlador REST con HATEOAS**
Ahora implementamos el controlador con los dos endpoints requeridos.

```java
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/iv_reporte_operaciones_total_diario")
public class ReporteOperacionesController {

    private final ReporteOperacionesService service;

    public ReporteOperacionesController(ReporteOperacionesService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> obtenerReporte(
            @RequestParam("P_COD_CIA") String pCodCia,
            @RequestParam("P_SUB_EMP") Integer pSubEmp,
            @RequestParam("P_FECHA_DESDE") String pFechaDesde,
            @RequestParam("P_FECHA_HASTA") String pFechaHasta,
            @RequestParam(value = "$count", required = false) Integer count,
            @RequestParam(value = "$start_index", required = false) Integer startIndex) {

        try {
            if (pCodCia == null || pSubEmp == null || pFechaDesde == null || pFechaHasta == null) {
                return ResponseEntity.badRequest().body(Map.of("__errors__", List.of(Map.of(
                    "code", 400,
                    "message", "Parámetros obligatorios faltantes",
                    "dataSource", "API",
                    "httpStatusCode", 400,
                    "httpErrorReturnedBySource", "Bad Request"
                ))));
            }

            List<ReporteOperacionesDTO> resultados = service.obtenerReporte(pCodCia, pSubEmp, pFechaDesde, pFechaHasta, count, startIndex);

            List<EntityModel<ReporteOperacionesDTO>> entidades = resultados.stream()
                .map(reporte -> {
                    EntityModel<ReporteOperacionesDTO> entityModel = EntityModel.of(reporte);
                    entityModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(ReporteOperacionesController.class)
                            .obtenerReporte(pCodCia, pSubEmp, pFechaDesde, pFechaHasta, count, startIndex))
                            .withSelfRel());
                    return entityModel;
                }).collect(Collectors.toList());

            return ResponseEntity.ok(entidades);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("__errors__", List.of(Map.of(
                "code", 500,
                "message", e.getMessage(),
                "dataSource", "API",
                "httpStatusCode", 500,
                "httpErrorReturnedBySource", "Runtime Error"
            ))));
        }
    }

    @GetMapping("/paginado")
    public ResponseEntity<?> obtenerReportePaginado(
            @RequestParam("P_COD_CIA") String pCodCia,
            @RequestParam("P_SUB_EMP") Integer pSubEmp,
            @RequestParam("P_FECHA_DESDE") String pFechaDesde,
            @RequestParam("P_FECHA_HASTA") String pFechaHasta) {
        
        return obtenerReporte(pCodCia, pSubEmp, pFechaDesde, pFechaHasta, 5, 0);
    }
}
```

---

### **5. Explicación Paso a Paso**
1. **DTO (`ReporteOperacionesDTO`)**: Representa cada fila del resultado.
2. **Repositorio (`ReporteOperacionesRepository`)**: Llama al procedimiento almacenado.
3. **Servicio (`ReporteOperacionesService`)**: Encapsula la lógica de negocio y maneja excepciones.
4. **Controlador (`ReporteOperacionesController`)**:
   - Implementa el endpoint `GET /iv_reporte_operaciones_total_diario` con validaciones de parámetros.
   - Implementa `GET /iv_reporte_operaciones_total_diario/paginado` con paginación fija (`$count=5`, `$start_index=0`).
   - Responde con datos y enlaces HATEOAS.
   - Maneja errores `400 BAD REQUEST` y `500 RUNTIME ERROR`.

---

### **Conclusión**
- Implementamos un **controlador REST HATEOAS** con Spring Boot.
- Validamos los parámetros de entrada.
- Llamamos a un **procedimiento almacenado en DB2** usando Spring Data.
- Retornamos **JSON con enlaces HATEOAS**.
- Controlamos errores con respuestas adecuadas.

