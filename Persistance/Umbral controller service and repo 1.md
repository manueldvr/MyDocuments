Guía paso a paso para desarrollar el servicio RESTful GET con HATEOAS en Spring Boot:

---


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



---


████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████	

.  
.  
.  

# **Guía**


paso a paso para desarrollar el servicio RESTful GET con HATEOAS en Spring Boot:


## Capa Controller


### **1. Definir la Entidad `TransmisionUmbralResult`**
Esta clase representa la estructura de los datos devueltos por el procedimiento almacenado.

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransmisionUmbralResult {
    private int entidad;
    private String archivoComprimido;
    private int bytes;
    private String archivo;
    private String tipo;
    private String estado;
    private String fecha;
    private String fechaHora;
    private int codServicio;
    private String descServicio;
}
```

---

### **2. Crear el Repositorio (`TransmisionUmbralRepository`)**
Este repositorio ejecutará el procedimiento almacenado `UMBRAL.get_transmision_umbral`.

```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TransmisionUmbralRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<TransmisionUmbralResult> getTransmisionUmbral(String codCia, String fechaDesde, String fechaHasta) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("UMBRAL.get_transmision_umbral");
        
        query.registerStoredProcedureParameter("p_cod_cia", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_fecha_desde", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_fecha_hasta", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_result", void.class, ParameterMode.REF_CURSOR);
        
        query.setParameter("p_cod_cia", codCia);
        query.setParameter("p_fecha_desde", fechaDesde);
        query.setParameter("p_fecha_hasta", fechaHasta);
        
        query.execute();
        
        List<Object[]> results = query.getResultList();
        List<TransmisionUmbralResult> responseList = new ArrayList<>();

        for (Object[] row : results) {
            TransmisionUmbralResult result = new TransmisionUmbralResult();
            result.setEntidad((Integer) row[0]);
            result.setArchivoComprimido((String) row[1]);
            result.setBytes((Integer) row[2]);
            result.setArchivo((String) row[3]);
            result.setTipo((String) row[4]);
            result.setEstado((String) row[5]);
            result.setFecha((String) row[6]);
            result.setFechaHora((String) row[7]);
            result.setCodServicio((Integer) row[8]);
            result.setDescServicio((String) row[9]);
            responseList.add(result);
        }

        return responseList;
    }
}
```

---

### **3. Implementar el Servicio (`TransmisionUmbralService`)**
Esta capa maneja la lógica de negocio y validaciones.

```java
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TransmisionUmbralService {
    
    private final TransmisionUmbralRepository repository;

    public TransmisionUmbralService(TransmisionUmbralRepository repository) {
        this.repository = repository;
    }

    public List<TransmisionUmbralResult> obtenerTransmisionUmbral(String codCia, String fechaDesde, String fechaHasta) {
        validarParametros(codCia, fechaDesde, fechaHasta);
        return repository.getTransmisionUmbral(codCia, fechaDesde, fechaHasta);
    }

    private void validarParametros(String codCia, String fechaDesde, String fechaHasta) {
        if (!StringUtils.hasText(codCia)) {
            throw new IllegalArgumentException("El parámetro P_COD_CIA es obligatorio.");
        }
        if (!StringUtils.hasText(fechaDesde)) {
            throw new IllegalArgumentException("El parámetro P_FECHA_DESDE es obligatorio.");
        }
        if (!StringUtils.hasText(fechaHasta)) {
            throw new IllegalArgumentException("El parámetro P_FECHA_HASTA es obligatorio.");
        }
    }
}
```

---

### **4. Crear el Controlador (`TransmisionUmbralController`)**
Este controlador maneja las peticiones HTTP y añade enlaces HATEOAS.

```java
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/views")
public class TransmisionUmbralController {
    
    private final TransmisionUmbralService service;

    public TransmisionUmbralController(TransmisionUmbralService service) {
        this.service = service;
    }

    @GetMapping("/Iv_transmision_umbral")
    public EntityModel<List<TransmisionUmbralResult>> obtenerTransmisionUmbral(
            @RequestParam("P_COD_CIA") String codCia,
            @RequestParam("P_FECHA_DESDE") String fechaDesde,
            @RequestParam("P_FECHA_HASTA") String fechaHasta,
            @RequestParam(value = "$count", required = false) Integer count,
            @RequestParam(value = "$start_index", required = false) Integer startIndex,
            @RequestParam(value = "$displayRESTfulReferences", required = false) Boolean displayRESTfulReferences,
            @RequestParam(value = "$format", required = false) String format
    ) {
        List<TransmisionUmbralResult> resultados = service.obtenerTransmisionUmbral(codCia, fechaDesde, fechaHasta);

        // Construcción de enlaces HATEOAS
        Link selfLink = WebMvcLinkBuilder
                .linkTo(WebMvcLinkBuilder.methodOn(TransmisionUmbralController.class)
                        .obtenerTransmisionUmbral(codCia, fechaDesde, fechaHasta, count, startIndex, displayRESTfulReferences, format))
                .withSelfRel();

        Link prevLink = WebMvcLinkBuilder
                .linkTo(WebMvcLinkBuilder.methodOn(TransmisionUmbralController.class)
                        .obtenerTransmisionUmbral(codCia, fechaDesde, fechaHasta, count, startIndex != null ? startIndex - (count != null ? count : 2) : null, displayRESTfulReferences, format))
                .withRel("prev")
                .withTitle("Previous interval");

        Link nextLink = WebMvcLinkBuilder
                .linkTo(WebMvcLinkBuilder.methodOn(TransmisionUmbralController.class)
                        .obtenerTransmisionUmbral(codCia, fechaDesde, fechaHasta, count, startIndex != null ? startIndex + (count != null ? count : 2) : 2, displayRESTfulReferences, format))
                .withRel("next")
                .withTitle("Next interval");

        return EntityModel.of(resultados, selfLink, prevLink, nextLink);
    }
}
```

---

### **Explicación Paso a Paso**
1. **Entidad (`TransmisionUmbralResult`)**: Representa cada fila del resultado.
2. **Repositorio (`TransmisionUmbralRepository`)**: Ejecuta el procedimiento almacenado.
3. **Servicio (`TransmisionUmbralService`)**: Aplica validaciones a los parámetros de entrada.
4. **Controlador (`TransmisionUmbralController`)**:
   - Recibe las peticiones HTTP.
   - Llama al servicio.
   - Genera respuestas con enlaces HATEOAS.

---

### **Prueba con Postman o Navegador**
Realiza una solicitud GET a:
```
http://localhost:8080/views/Iv_transmision_umbral?P_COD_CIA=603&P_FECHA_DESDE=01/01/2024&P_FECHA_HASTA=03/01/2024&$displayRESTfulReferences=true&$format=JSON
```
El resultado incluirá los datos y los enlaces `self`, `prev` y `next`.

---

### **Conclusión**
Este código implementa un servicio RESTful GET con HATEOAS en Spring Boot, ejecutando un procedimiento almacenado mediante una API de persistencia basada en Spring Data.



.  
.  
.  




████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████


.  
.  

.  




Sí, existe otra forma de implementar `TransmisionUmbralRepository` usando **Spring Data JPA** con la anotación `@Procedure`, lo que simplifica el código y evita el uso manual de `EntityManager`.  

### **1. Implementación Usando `@Procedure` en una Interfaz**
Spring Data JPA permite mapear procedimientos almacenados directamente en una **interfaz** de repositorio, eliminando la necesidad de `EntityManager` y `StoredProcedureQuery`.  

```java
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransmisionUmbralRepository extends CrudRepository<TransmisionUmbralResult, Integer> {

    @Procedure(name = "UMBRAL.get_transmision_umbral")
    List<TransmisionUmbralResult> getTransmisionUmbral(
            @Param("p_cod_cia") String codCia, 
            @Param("p_fecha_desde") String fechaDesde, 
            @Param("p_fecha_hasta") String fechaHasta
    );
}
```

### **2. Ajustar `TransmisionUmbralResult` para Soportar `@NamedStoredProcedureQuery`**
Para que funcione correctamente, se debe modificar `TransmisionUmbralResult` agregando `@NamedStoredProcedureQuery` para definir el procedimiento almacenado.  

```java
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@NamedStoredProcedureQuery(
        name = "UMBRAL.get_transmision_umbral",
        procedureName = "UMBRAL.get_transmision_umbral",
        resultClasses = TransmisionUmbralResult.class,
        parameters = {
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_cod_cia", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_fecha_desde", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.IN, name = "p_fecha_hasta", type = String.class),
                @StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, name = "p_result", type = void.class)
        }
)
public class TransmisionUmbralResult implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int entidad;
    private String archivoComprimido;
    private int bytes;
    private String archivo;
    private String tipo;
    private String estado;
    private String fecha;
    private String fechaHora;
    private int codServicio;
    private String descServicio;
}
```

---

### **3. Beneficios de Usar `@Procedure`**
✅ **Menos código**: No se necesita `EntityManager` ni `StoredProcedureQuery`.  
✅ **Código más limpio**: Se usa una interfaz en lugar de una clase de repositorio.  
✅ **Integración nativa con Spring Data JPA**: Manejo automático de transacciones.  

### **4. Cómo se Usa en `TransmisionUmbralService`**
El servicio sigue igual, pero ahora llama directamente al método de la interfaz.

```java
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class TransmisionUmbralService {
    
    private final TransmisionUmbralRepository repository;

    public TransmisionUmbralService(TransmisionUmbralRepository repository) {
        this.repository = repository;
    }

    public List<TransmisionUmbralResult> obtenerTransmisionUmbral(String codCia, String fechaDesde, String fechaHasta) {
        validarParametros(codCia, fechaDesde, fechaHasta);
        return repository.getTransmisionUmbral(codCia, fechaDesde, fechaHasta);
    }

    private void validarParametros(String codCia, String fechaDesde, String fechaHasta) {
        if (!StringUtils.hasText(codCia)) {
            throw new IllegalArgumentException("El parámetro P_COD_CIA es obligatorio.");
        }
        if (!StringUtils.hasText(fechaDesde)) {
            throw new IllegalArgumentException("El parámetro P_FECHA_DESDE es obligatorio.");
        }
        if (!StringUtils.hasText(fechaHasta)) {
            throw new IllegalArgumentException("El parámetro P_FECHA_HASTA es obligatorio.");
        }
    }
}
```

---

### **5. Conclusión**
Si usas `@Procedure` y `@NamedStoredProcedureQuery` con `Spring Data JPA`, simplificas el código y eliminas la necesidad de `EntityManager`, haciendo el código más limpio y mantenible.

