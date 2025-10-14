# Buenas prácticas en la creación de APIs Rest

<br>

### Introducción

Una API REST se centra en las entidades de negocio (datos) expuestos como recursos que se identifican a través de URIs y que pueden ser manipuladas a través de métodos estandarizados tipo CRUD utilizando diferentes representaciones.  
Las APIs RESTful tienden a ser menos específicas para cada caso de uso y son adecuadas para un ecosistema de micro-servicios.  
> Un principio importante para el diseño y el uso de las API es la Ley de Postel, también conocida como el Principio de Robustez (véase también el RFC 1122):
 
<br>

<br>
 
##  ¿Qué es REST o Representational State Transfer?

Es un estilo de arquitectura para la creación de aplicaciones en un esquema cliente-servidor, NO UNA TECNOLOGÍA.

Se centra en la Transferencia de recursos a través de peticiones (Request) y respuestas (Response) síncronos.
Cada Transferencia devuelve un estado númerico (1XX, 2XX, 3XX, 4XX, 5XX).

En REST un objeto de negocio es considerado un Recurso.

Un Recurso puede tener múltiples representaciones.
Por lo general los Recursos son representados en un body en formato JSON o XML.

Utiliza una comunicación STATELESS.

Es un modelo adecuado para operaciones CRUD (Create, Read, Update, Delete).
Los recursos pueden ser almacenados en caché.

<br>
<br>

## Terminología en REST
* **Recurso** = entidad de negocio.
* **Verbs** = métodos HTTP (GET, POST, PUT, PATCH, DELETE, etc).
* **Body** = payloads JSON/XML.
* **URI** = Identificador Uniforme de Recursos.
* **URL** = Localizador Uniforme de Recursos (información del host + URI).
* **Idempotent** = Propiedad de una operación de ser aplicada múltiples veces sin cambiar el resultado.
* **Stateless** = El servicio no mantiene ningún estado o sesión del cliente.
* **HATEOAS** = Acrónimo de "Hypermedia As The Engine Of Application State".


<br>
<br>

## VERBOS DE CONTRATO UNIFORME (HTTP/S)
* GET: obtiene el recurso (Sólo lectura, Idempotente)
* HEAD: como GET pero sólo obtiene los metadatos (Sólo lectura, Idempotente)

* **POST**: crea un nuevo recurso (No-Idempotente)
* **PUT**: crea o actualiza (si no existe) un recurso existente (Idempotente)
* **PATCH**: modifica parcialmente un recurso existente (No-Idempotente)

* DELETE: elimina un recurso (Idempotente)

* TRACE: se hará eco de la petición recibida. Se utiliza para ver si la solicitud fue alterada por servidores intermedios (proxis).
* OPTIONS: devuelve los métodos soportados para una url especificada (Sólo lectura, Idempotente)

* Métodos idempotentes:  
	GET,  
	HEAD,  
	PUT,  
	TRACE,  
	OPTIONS.

* Métodos **NO-Idempotentes**:  
	POST (cuando se invoca la misma solicitud POST N veces, tendrá N nuevos recursos en el servidor).  
	PATCH (cuando se invoca la misma solicitud PATCH N veces, se actualizará N veces el recurso).


<br>
<br>


## Fields/Parameters

Anatomía de una url:

```
https://afiliaciones.sancorsalud.com.ar:8080/v1/afiliados/1001/personas?nombre=Juan&apellido=Perez&edad=41
```
```
protocolo://sub-dominio.dominio:puerto/api-version/recurso/id-recurso/sub-recurso?filtro1=arg1&filtro2=arg2&filtro3=arg3
```

Los fields son los valores que pueden variar entre peticiones. Existen tres tipos de fields:

1. **PATH FIELDS:** Son parámetros que ayudan a identificar un único recurso.   
Por ejemplo:  
`/afiliados/1001`, `/afiliados/1002`, `/afiliados/1003`, etc.

2. **QUERY FIELDS:** Son parámetros que sirven para filtrar una colección de recursos. A diferencia de un PATH PARAM no se asegura que se recupere un recurso único, si no que se debe esperar de 0 a N Recursos.  
Por ejemplo:  
`/personas?nombre=Juan&apellido=Perez&edad=41`,  
`/personas?nombre=John&apellido=Smith`,  
`/personas?edad=10`, etc. 
3. **BODY FIELDS:** Son parámetros en formato JSON/XML que son enviados en al servidor para crear o actualizar los datos de un recurso (se detalla en lineamientos para JSON).
 
 <br>

 
## Rangos de Status Code (HTTP/S)

100-199: información personalizada  
200-299: solicitud exitosa (200: OK, 201: CREATED, 204: NO CONTENT).  
300-399: redirecciones (301: MOVED PERMANENTLY).  
400-499: errores del cliente (400: BAD REQUEST, 401: UNAUTHORIZED, 403 FORBIDDEN, 404: NOT FOUND). 
500-599: errores del servidor (500: INTERNAL SERVER ERROR, 503: SERVICE UNAVAILABLE). 

<br>


## Modelo de madurez de Richardson (RMM):
Nivel 0 = El intercambio de POX (Plain old XML). Ej: RPC, SOAP.  
Nivel 1 = Recursos identificados por URIs.  
Nivel 2 = Verbos HTTP como operaciones Curl.  
Nivel 3 = Conocido como "La gloria de REST" o bajo el acrónimo HATEOAS, en dónde los recursos se relacionan por medio de links (Hypertext o URIs) incluidos en el payload.  


Algunas lecturas interesantes sobre el estilo de diseño de la API RESTful y la arquitectura de los servicios:

>REST API Design - Resource Modeling:
https://www.thoughtworks.com/de-de/insights/blog/rest-api-design-resource-modeling

>Richardson Maturity Model — Steps toward the glory of REST:
https://martinfowler.com/articles/richardsonMaturityModel.html

> Fielding Dissertation: Architectural Styles and the Design of Network-Based Software Architectures:
https://www.ics.uci.edu/~fielding/pubs/dissertation/top.htm


 
<br>



<br>


## Lineamientos generales

<br>



### 1. DEBE mantener las URI lo más representativas posible

Seguir como regla general el sigiente pattern: 

`
{api-version}/{nombre-aplicación}/{nombre-del-recurso-en-plural}/{id-recurso}
`

>Una URI debe representar inequivocamente a un recurso, NO UNA ACCIÓN.

Por eso: evitar colocar en POST `/crear`, en PATCH `/actualizar`, en delete `/borrar` o get `/buscar` o `/all`. Ya que queda explicito en el método que se utiliza.



### 2. DEBE existir una collerrelación entre Metodo y Status Code

Como regla general tener en cuenta que:

* **POST** se utiliza para crear un nuevo recurso, en caso de éxito devuelve status code `CREATED (201)` y sin body.
* **POST** se utiliza para correr un proceso asíncrono (batch, bulk), en caso de éxito devuelve status code `ACCEPTED (202)`.
* **PUT** se utiliza para actualizar o crear (si no existiese) un recurso, en caso de éxito devuelve status code `NO_CONTENT (204)` si se actualizó el recurso o `CREATED (201)` si se creó uno nuevo y en ambos casos sin body.
* **PATCH** se utiliza para actualizar parcialmente un recurso, en caso de éxito devuelve status code `NO_CONTENT (204)` y sin body.
* **DELETE** se utiliza para eliminar un recurso, en caso de éxito devuelve status code `NO_CONTENT (204)` y sin body.
* **GET**(sin parametros) se utiliza para obtener la colección de recursos, en caso de éxito devuelve status code `OK (200)` y la collection en el body.
* **GET** (con parametros) se utiliza para obtener un recurso en particular, en caso de éxito devuelve status code `OK (200)` y el recurso en el body.



### 3. DEBE utilizar minúsculas tanto en las URLs y en JSON, evitar camelCase

Como regla general si existe una mayúscula en la url o en los campos del body (json) es un error. 

Los lineamientos de nomenclatura específicos se desarrollan más adelante.


<br>

## Lineamientos sobre JSON

### 1. DEBERÍA pluralizar los nombres de la colecciones

Los nombres de las colecciones deben ser pluralizados para indicar que contienen múltiples valores. Esto implica a su vez que los nombres de los objetos deben ser singulares.


### 2. DEBERÍA pluralizar los nombres de la colecciones
Los nombres de las colecciones deben ser pluralizados para indicar que contienen múltiples valores. Esto implica a su vez que los nombres de los objetos deben ser singulares.

### 3. Los nombres de propiedades deberían estar en snake_case y nunca camelCase


```json
{
    "telefonos_fijos": [
        {
            "tipo": "T",
            "caracteristica": "1234 ",
            "descripcion": "123545",
            "factura_por_email": "N",
            "codigo": 3
        }
    ],
    "telefonos_celulares": [
        {
            "tipo": "C",
            "caracteristica": "3493 ",
            "descripcion": "499353",
            "factura_por_email": "X",
            "codigo": 1
        }
    ],
    "emails": [
        {
            "tipo": "E",
            "caracteristica": "",
            "descripcion": "yamilamartino8@gmail.com",
            "factura_por_email": "S",
            "codigo": 2
        },{
            "tipo": "E",
            "caracteristica": "",
            "descripcion": "yamilamartino8@hotmail.com",
            "factura_por_email": "N",
            "codigo": 4
        }
    ]
}
```

### 3. No usar null para las propiedades booleanas

Las propiedades JSON booleanas **no** deben presentarse como nulos. Un booleano es esencialmente una enumeración cerrada de dos valores, verdadero y falso. 

Si el contenido tiene un valor nulo significativo, es preferible sustituir el booleano por una enumeración de valores o estados con nombre - por ejemplo, accepted_terms_and_conditions con `true` o `false` puede sustituirse por accepted_terms_and_conditions con valores yes, no y unknown.


### 4. No debería usar null para arrays vacíos

Los valores vacíos de los arrays pueden representarse inequívocamente como la lista vacía, `[]`.

<br>

### 5. DEBE utilizar la misma semántica para las propiedades nulas y ausentes

OpenAPI 3.x permite marcar las propiedades como requeridas y como nulables para especificar si las propiedades pueden estar ausentes ({}) o ser nulas ({"ejemplo":null}). Siga las siguentes reglas para ver cuando un valor puede ser nulo o ausente:

requerida	nulable	ausente {}	null {"example":null}
TRUE	TRUE	NO	SI
FALSE	TRUE	SI	SI
TRUE	FALSE	NO	NO
FALSE	FALSE	SI	NO

Como regla general tenga en cuenta que todas las variables nulas deben ser enviadas como ausentes, excepto que sean requeridas (estén marcadas como @NotNull).

<br>

### 6. DEBERÍA nombrar las propiedades de fecha/hora con el sufijo _at

Las propiedades de fecha y hora deberían terminar con _at para distinguirlas de las propiedades booleanas que, de otro modo, tendrían nombres muy similares o incluso idénticos:

* created_at en lugar de created,
* modified_at en lugar de modified
* occurred_at en lugar de occurred
* returned_at en lugar de returned

<br>

### 7. DEBERÍA declarar los valores de las enumeraciones utilizando UPPER_SNAKE_CASE

Las enumeraciones deberían representarse como definiciones de tipo String. Los valores de las enumeraciones deben utilizar sistemáticamente el formato de mayúsculas, por ejemplo para monedas USD, EUR o ARS y para idiomas ES_SPA, ES_ARG, EN_US, EN_GB. Este enfoque permite distinguir claramente los valores de las propiedades u otros elementos.


Excepción: Esta regla no se aplica a los valores que distinguen entre mayúsculas y minúsculas procedentes de fuera del ámbito de definición de la API, por ejemplo, los códigos de idioma de la norma ISO 639-1 (El ejemplo anterior es incorrecto según esta norma).
 

<br>
<br>
<br>

## Lineamientos sobre URLs

<br>



### 1. DEBE seguir una convención de nomenclatura para nombres de host

Los nombres de host en las APIs deberían ajustarse a la nomenclatura funcional dependiendo del público como sigue:

`<functional-hostname> = <functional-name>.ams.red`

Correcto:  
`http://test-afiliaciones.ams.red`
 
 
### 2. DEBE utilizar nombres en minúsculas con guiones para URI/URL

Como regla general jamás debería haber mayùsculas en una url, y los strings deberían ser separados por un guión medio si fuera necesario.

Incorrecto:  
`/SalesForce/Products/{product-id}`

`/Abacom/ClientesPotenciales/{cliente-potencial-id}`

Correcto:  
`/salesforce/products/{product-id}`

`/abacom/clientes-potenciales/{cliente-potencial-id}`


Esto se aplica a los segmentos de la ruta únicamente y no a los nombres de los parámetros de cosulta (utilizan snake_case). 
 
 
### 3. DEBE utilizar snake_case (nunca camelCase) para los parámetros de consulta

Incorrecto:

`/asociados/usuario-web?nroDeCliente=1111&idCuenta=0001&apellido=sosa`

Correcto:  
`/asociados/usuario-web?nro_de_cliente=1111&id_cuenta=0001&apellido=sosa`
 

<br>

### 4. DEBE pluralizar los nombres de los recursos

Normalmente, se proporciona una colección de recursos cuando cuando a un método GET no se le pasa parámetros. El caso especial de un recurso singleton debe ser modelado como una colección con cardinalidad 1 y debería incluirse además la definición de max_items = min_items = 1 para hacer explícita la restricción de cardinalidad.

Incorrecto:  
`/salesforce/asset`

`/salesforce/account`

Correcto:  
`/salesforce/assets`

`/salesforce/accounts`
 
 <br>
 
 
### 5. No debería usar /api como ruta base

En la mayoría de los casos, todos los recursos proporcionados por un servicio forman parte de la API pública, y por lo tanto deberían estar disponibles bajo la ruta base "/".


Si el servicio también debe soportar APIs internas no públicas - para funciones específicas de apoyo operativo, le animamos a mantener dos especificaciones de API diferentes (Para ambas API, no debe utilizar /api como ruta base).

Consideramos que la ruta base de la API forma parte de la configuración nombre de host funcional (punto 1). Por lo tanto, esta información debe ser declarada como parte del host.


Incorrecto:

`/api/v1/salesforce/products`


Correcto:

`/v1/salesforce/products`
 
 
 
 
 
 
### 6. DEBERÍA utilizar rutas normalizadas sin segmentos de ruta vacíos ni barras finales
 
No debe especificar rutas con barras duplicadas o finales, por ejemplo, /clientes//direcciones o /clientes/. En consecuencia, tampoco debe especificar o utilizar variables de ruta con valores de cadena vacíos.

Razonamiento: Las rutas no estándar no tienen una semántica clara. Como resultado, el comportamiento de las rutas no estándar varía entre los diferentes componentes de la infraestructura HTTP. Esto puede llevar a resultados ambiguos e inesperados durante el manejo y monitoreo de las solicitudes.

Recomendamos implementar servicios robustos contra clientes que no sigan esta regla. Todos los servicios deberían normalizar las rutas de las peticiones antes de procesarlas, eliminando las barras duplicadas y las finales. Por lo tanto, las siguientes peticiones deberían referirse al mismo recurso:
 



<br>


### 7. DEBE ceñirse a los parámetros de consulta convencionales

Si proporcionas soporte de consulta para buscar, ordenar, filtrar y paginar, debes seguir las siguientes convenciones de nomenclatura:

* **q**: parámetro de consulta por defecto.
* **sort**: lista de campos separada por comas para definir el orden de clasificación. 

Para indicar la dirección de la ordenación, los campos pueden llevar el prefijo + (ascendente) o - (descendente), por ejemplo:  `/ordenes?sort=+id`

* **fields**: expresión de nombre de campo para recuperar sólo un subconjunto de campos de un recurso, por ejemplo, `/ordenes?fields=id,total,productos`

* **embed:** expresión de nombre de campo para expandir o incrustar subentidades, por ejemplo, dentro de una entidad orden, expandir el código de producto en el objeto productos. Implementar embed correctamente es difícil.
* **offset:** desplazamiento numérico del primer elemento proporcionado en una página que representa una solicitud de colección (para paginación), por ejemplo, `/ordenes?offset=2`.
* **limit:** límite sugerido por el cliente para restringir el número de entradas en una página (para paginación), por ejemplo, `/ordenes?limit=2&offset=10`
* **cursor:** un puntero a una página, que nunca debe ser inspeccionado o construido por los clientes. Suele codificar (encriptado) la posición de la página, es decir, el identificador del primer o último elemento de la página, la dirección de la paginación y los filtros de consulta aplicados para recrear la colección (para paginación).
 
 
 
<br>
<br>
<br>


## Lineamientos sobre Recursos



### 1. DEBE evitar las acciones y mantener URIs sin verbos






### 2. Una URI siempre DEBE identificar a la colección de recursos y no un recurso en particular

Cuando definimos nuestras URIs debemos pensarlas como una colección de recursos que:  

1. Pueden ser filtrados por medio de parámetros de consulta,  
por ejemplo:  
`/recursos?param0=arg0&param1=arg1&param2=arg2`

2. Cuyos elementos pueden ser identificados individualmente por medio de un id único,  
por ejemplo:  
`/recursos/1`, `/recursos/2`, `/recursos/3`


<br>
<br>


### 3. DEBE identificar los recursos y sub-recursos a través de segmentos de ruta

Algunos recursos de la API pueden contener o hacer referencia a sub-recursos. 

Los sub-recursos son partes de un recurso de nivel superior.  
Los sub-recursos deben ser referenciados por su nombre e identificador en los segmentos de la ruta como sigue:

`/recursos/{recurso-id}/subrecursos/{subrecurso-id}`

Para mejorar la experiencia del cliente, debe procurar que las URL sean intuitivas, y que cada sub-ruta sea una referencia válida a un recurso o a un conjunto de recursos. 

Por ejemplo:  
si `/partners/{partner-id}/addresses/{address-id}` es válido, 

entonces, en principio, también:  
`/partners/{partner-id}/addresses`,  
`/partners/{partner-id}` y  
`/partners` 
deben ser válidos.

Ejemplo de rutas:

`/shopping-carts/de:1681e6b88ec1/items/1`. 
`/shopping-carts/de:1681e6b88ec1`

`/contenido/imágenes/9cacb4d8`  
`/contenido`

Excepción: 

En algunas situaciones el identificador de recurso no se pasa como un segmento de ruta sino a través de la información de autorización en un Header, por ejemplo, un token de autorización o una cookie de sesión.


<br>
<br>

### 4. DEBE utilizar identificadores de recursos amigables

Para simplificar el encoding de los identificadores de recursos en las URL, su representación debe consistir únicamente en ASCII que utilicen letras, números, guión bajo, guión medio, dos puntos, punto y -en raras ocasiones- barra invertida (/).

Incorrecto:  
`PATCH /salesforce/accounts?dni={dni} # está bien para filtrar pero NO PARA IDENTIFICAR UN RECURSO`. 
`PATCH /salesforce/accounts/dni|{dni}`. 
`PATCH /salesforce/accounts/dni->{dni}`. 
`PATCH /salesforce/accounts/dni👉{dni}`. 

Correcto:  
`PATCH /salesforce/accounts/dni:{dni}`.  
`PATCH /salesforce/accounts/dni.{dni}`.  
`PATCH /salesforce/accounts/dni_{dni}`.  
`PATCH /salesforce/accounts/dni-{dni}`.  
`PATCH /salesforce/accounts/dni/{dni} # está mal pero no tan mal!!`. 


>Nota: las barras invertida sólo se permiten para identificar recursos subyacentes.

<br>
<br>

### 5. DEBE utilizar parámetros de consulta para filtrar y NO para identificar recursos

Los parámetros de query son útiles para filtrar elementos en una colección, pero no debemos utilizarlos para identificar recursos.

Los parámetros de consulta quedar reservados para el método GET pero no para POST, PUT, PATCH, DELETE.

Incorrecto:

```
PATCH /salesforce/accounts?dni={dni}  
PATCH /salesforce/accounts?id={id}
```

Correcto:

```
GET /salesforce/accounts?dni={dni}
GET /salesforce/accounts/{id}

PATCH /salesforce/accounts/dni-{dni}
PATCH /salesforce/accounts/{id}
``` 

<br>
<br>



<br>
<br>



<br>
<br>



<br>
<br>