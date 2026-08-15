// 40. Implementación final recomendada


@Configuration
@EnableConfigurationProperties(CustomerClientProperties.class)
public class CustomerClientConfiguration {

    @Bean
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerClientProperties properties,
            CorrelationIdInterceptor correlationInterceptor
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        "X-Application-Name",
                        "claims-service"
                )
                .requestInterceptor(
                        correlationInterceptor
                )
                .defaultStatusHandler(
                        HttpStatusCode::is5xxServerError,
                        (request, response) -> {
                            throw new CustomerServiceException(
                                    "Customer API returned "
                                            + response.getStatusCode()
                            );
                        }
                )
                .build();
    }
}
