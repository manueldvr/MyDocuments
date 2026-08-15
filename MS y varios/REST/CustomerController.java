//16. Ejemplo completo con Spring Boot
/**
 * Diseñar URIs orientadas a recursos, no a acciones.
 * Utilizar el método HTTP adecuado para cada operación.
 * Respetar la idempotencia de GET, PUT y DELETE.
 * Aprovechar la caché HTTP cuando los datos sean relativamente estables.
 * Implementar ETag para reducir tráfico innecesario.
 * Devolver códigos de estado HTTP precisos.
 * Mantener un formato uniforme para los errores.
 * Versionar la API antes de introducir cambios incompatibles.
 * Documentar la API con OpenAPI/Swagger.
 *
 **/
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {

        CustomerDTO dto = service.findById(id);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .eTag("\"customer-" + dto.id() + "-" + dto.version() + "\"")
                .body(dto);
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> create(
            @RequestBody @Valid CustomerDTO dto) {

        CustomerDTO created = service.create(dto);

        URI location = URI.create("/api/v1/customers/" + created.id());

        return ResponseEntity.created(location)
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}
