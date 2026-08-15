// 40. Implementación final recomendada

@Component
public class CustomerClient {

    private final RestClient restClient;


    public CustomerClient(RestClient customerRestClient) {
        this.restClient = customerRestClient;
    }


    public CustomerDTO findById(Long id) {
        CustomerDTO customer = restClient.get()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new CustomerNotFoundException(id);
                        }
                )
                .body(CustomerDTO.class);
        if (customer == null) {
            throw new CustomerServiceException(
                    "Customer API returned an empty body"
            );
        }
        return customer;
    }

    public List<CustomerDTO> findByStatus(
            CustomerStatus status) {
        List<CustomerDTO> customers =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/customers")
                                .queryParam(
                                        "status",
                                        status
                                )
                                .build())
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<>() {
                                }
                        );
        return customers != null
                ? List.copyOf(customers)
                : List.of();
    }


    public CustomerDTO create(
            CreateCustomerRequest request) {
        CustomerDTO customer = restClient.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        "Idempotency-Key",
                        UUID.randomUUID().toString()
                )
                .body(request)
                .retrieve()
                .body(CustomerDTO.class);

        if (customer == null) {
            throw new CustomerServiceException(
                    "Customer API returned an empty body"
            );
        }
        return customer;
    }


    public void delete(Long id) {
            restClient.delete()
                .uri("/api/customers/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
