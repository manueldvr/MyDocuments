# 4 Spring & Spring Boot


**Review**

- **Dependency Injection/IoC:** constructor vs setter vs field injection; testability implications.
- **Spring Boot starters and auto-configuration concept** reasoning about what gets created.
- **Configuration:** profiles, properties, externalized config; environment differences.
- **Web layer:** MVC basics, controllers, validation, exception handling, serialization (JSON ↔ objects).
- **AOP basics** (why proxies matter for transactions/security).
- **Spring Boot operational basics:** health/readiness checks, logging configuration.




The examples below assume Java 17 and Spring Boot 3.x, which uses Spring Framework 6 and Jakarta packages such as `jakarta.validation`.

## 1. The Spring lifecycle as the common foundation

When a Spring Boot application starts:

```java
SpringApplication.run(Application.class, args);
```

the simplified lifecycle is:

1. Spring Boot prepares the `Environment`.
2. Configuration files, environment variables and active profiles are resolved.
3. Component scanning and auto-configuration produce bean definitions.
4. The IoC container creates beans.
5. Dependencies are injected.
6. Initialization callbacks run.
7. `BeanPostProcessor` components can replace beans with proxies.
8. The web server starts accepting requests.
9. Actuator exposes operational information about the running application.

This explains several important behaviors:

* Configuration must be available before configuration-dependent beans are created.
* Constructor dependencies must exist when the bean is instantiated.
* `@Transactional`, `@Async`, caching and method security usually require a proxy.
* A class created manually with `new` is not automatically managed or proxied by Spring.
* MVC controllers are singleton Spring beans by default.

---

# 2. Dependency Injection and IoC

## Inversion of Control

Without IoC, a class creates or locates its dependencies:

```java
public class PaymentService {

    private final PaymentRepository repository =
            new JpaPaymentRepository();
}
```

`PaymentService` controls the creation of its dependency and is coupled to a concrete implementation.

With IoC, the Spring container creates and connects the objects:

```java
@Service
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }
}
```

The class says what it needs, while Spring decides which object to supply.

Dependency Injection is the mechanism Spring uses to implement IoC. Spring describes the objects it manages as beans, represented internally by `BeanDefinition` metadata. [Spring Framework dependency injection](https://docs.spring.io/spring-framework/reference/6.2/core/beans/dependencies/factory-collaborators.html)

## How beans are discovered

### Component scanning

```java
@Repository
public class JpaPaymentRepository implements PaymentRepository {
}
```

```java
@Service
public class PaymentService {
}
```

Common stereotype annotations:

| Annotation        | Typical layer                      |
| ----------------- | ---------------------------------- |
| `@Component`      | General Spring component           |
| `@Service`        | Business service                   |
| `@Repository`     | Persistence component              |
| `@Controller`     | MVC controller returning views     |
| `@RestController` | REST controller                    |
| `@Configuration`  | Bean definitions and configuration |

`@SpringBootApplication` contains:

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

Component scanning normally starts from the package containing the main application class and scans its subpackages.

A conventional structure is:

```text
com.example.payment
├── PaymentApplication
├── controller
├── service
├── repository
└── configuration
```

### Explicit bean creation

For a third-party class or custom construction logic:

```java
@Configuration
public class PaymentConfiguration {

    @Bean
    PaymentClient paymentClient(PaymentProperties properties) {
        return new PaymentClient(
                properties.baseUrl(),
                properties.timeout()
        );
    }
}
```

Spring calls the `@Bean` method and registers its returned object as a bean.

## Constructor injection

```java
@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final FraudClient fraudClient;

    public PaymentService(
            PaymentRepository repository,
            FraudClient fraudClient
    ) {
        this.repository = repository;
        this.fraudClient = fraudClient;
    }
}
```

If a class has only one constructor, `@Autowired` is unnecessary.

### Advantages

* Dependencies are explicit.
* Mandatory dependencies cannot be omitted.
* Fields can be `final`.
* The object is valid after construction.
* Easy to test without starting Spring.
* Circular dependencies are detected instead of hidden.

Example unit test:

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository repository;

    @Mock
    FraudClient fraudClient;

    @Test
    void shouldCreatePayment() {
        PaymentService service =
                new PaymentService(repository, fraudClient);

        // Test service directly
    }
}
```

This is the recommended injection style for required dependencies.

A constructor with too many parameters often indicates that the class has too many responsibilities.

## Setter injection

```java
@Service
public class ReportService {

    private ReportExporter exporter =
            new DefaultReportExporter();

    @Autowired(required = false)
    public void setExporter(ReportExporter exporter) {
        this.exporter = exporter;
    }
}
```

Setter injection can be appropriate for:

* Optional dependencies.
* Dependencies with a reasonable default.
* Objects that genuinely need reconfiguration.
* Some legacy or third-party integration cases.

Disadvantages:

* The object may temporarily be incomplete.
* Dependencies cannot generally be `final`.
* Required dependencies are less obvious.
* Production code may change dependencies after construction.

For mandatory dependencies, constructor injection is clearer.

## Field injection

```java
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository repository;
}
```

It works because a Spring post-processor uses reflection to assign the field after constructing the object.

However, it is generally discouraged.

### Problems with field injection

* The dependency is hidden from the constructor.
* The field cannot normally be `final`.
* Plain unit tests cannot instantiate the class in a valid state.
* Tests may require Spring, reflection or annotation-based mock injection.
* A class can accumulate too many dependencies without making the design problem obvious.
* The object may exist briefly before its dependencies are assigned.

Constructor injection:

```java
PaymentService service =
        new PaymentService(repository);
```

Field injection:

```java
PaymentService service = new PaymentService();
// repository is null unless Spring or reflection modifies it
```

## Multiple implementations

If two beans implement the same interface:

```java
@Component
public class StripePaymentGateway
        implements PaymentGateway {
}
```

```java
@Component
public class BankPaymentGateway
        implements PaymentGateway {
}
```

Spring cannot choose automatically:

```java
public PaymentService(PaymentGateway gateway) {
}
```

Resolve it with `@Qualifier`:

```java
public PaymentService(
        @Qualifier("bankPaymentGateway")
        PaymentGateway gateway
) {
    this.gateway = gateway;
}
```

or make one the default:

```java
@Primary
@Component
public class BankPaymentGateway
        implements PaymentGateway {
}
```

## Implementing libraries

| Capability                              | Implementation                       |
| --------------------------------------- | ------------------------------------ |
| IoC container                           | Spring Framework `spring-context`    |
| Bean creation and dependency resolution | `spring-beans`                       |
| Annotation-based injection              | `spring-context`                     |
| Boot application context                | `spring-boot`                        |
| Unit testing                            | JUnit Jupiter                        |
| Mock dependencies                       | Mockito                              |
| Spring integration tests                | `spring-test` and `spring-boot-test` |

Usually these are obtained through Spring Boot starters rather than declared individually.

---

# 3. Spring Boot starters and auto-configuration

## Starters

A starter is a curated dependency descriptor. It collects libraries commonly required for a capability.

For example:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

Conceptually, this provides:

* Spring MVC
* An embedded servlet container, normally Tomcat
* Jackson JSON integration
* Validation and logging integration, depending on Boot version and selected starters
* Supporting Spring infrastructure

The starter itself generally contains little or no application behavior. Its purpose is dependency management.

Common starters:

| Starter                                      | Purpose                                   |
| -------------------------------------------- | ----------------------------------------- |
| `spring-boot-starter`                        | Core Boot functionality and logging       |
| `spring-boot-starter-web`                    | Spring MVC, REST and embedded Tomcat      |
| `spring-boot-starter-webflux`                | Reactive web applications                 |
| `spring-boot-starter-data-jpa`               | JPA, Hibernate and Spring Data JPA        |
| `spring-boot-starter-jdbc`                   | JDBC and connection pooling               |
| `spring-boot-starter-validation`             | Jakarta Bean Validation                   |
| `spring-boot-starter-security`               | Spring Security                           |
| `spring-boot-starter-oauth2-resource-server` | OAuth2 Bearer/JWT API security            |
| `spring-boot-starter-actuator`               | Health, metrics and operational endpoints |
| `spring-boot-starter-test`                   | JUnit, Spring Test, Mockito and AssertJ   |

## Auto-configuration

Auto-configuration contains real configuration classes that conditionally create beans.

For example, when Boot detects:

* Spring MVC on the classpath,
* a servlet web application,
* no conflicting user configuration,

it configures objects such as:

* `DispatcherServlet`
* MVC handler mappings
* HTTP message converters
* Embedded servlet container
* Error handling infrastructure

Spring Boot auto-configuration is based principally on conditional annotations:

```java
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnProperty
@ConditionalOnWebApplication
@ConditionalOnBean
```

Conceptual example:

```java
@AutoConfiguration
@ConditionalOnClass(PaymentClient.class)
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PaymentClient paymentClient(PaymentProperties properties) {
        return new PaymentClient(properties.baseUrl());
    }
}
```

The bean is created only if:

1. `PaymentClient` is on the classpath.
2. The application has not already defined a `PaymentClient`.

Auto-configuration is designed to “back off” when the application supplies its own bean. [Spring Boot auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)

## Reasoning about what gets created

When investigating an unexpected bean, ask:

1. Which starter or dependency placed the relevant class on the classpath?
2. Which auto-configuration class responded to that class?
3. Which conditions matched?
4. Did Boot find an existing user-defined bean?
5. Which configuration properties affected the result?

Useful diagnostic options:

```bash
java -jar application.jar --debug
```

or:

```properties
debug=true
```

This produces a condition evaluation report showing:

* Positive matches
* Negative matches
* Unconditional classes

Actuator can also expose:

```text
/actuator/conditions
/actuator/beans
/configprops
```

These endpoints contain sensitive internal information and should not normally be publicly exposed.

## Example: `DataSource`

When the application includes:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

and configuration such as:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payments
    username: payment_app
    password: secret
```

Boot sees JDBC classes and datasource properties and may create:

* A `DataSource`, commonly HikariCP
* `JdbcTemplate`
* Transaction-management infrastructure
* Database health indicators if Actuator is present

If the application defines its own `DataSource`:

```java
@Bean
DataSource dataSource() {
    return customDataSource();
}
```

the relevant default auto-configuration normally backs off.

---

# 4. Configuration, profiles and environments

## Externalized configuration

Externalized configuration separates deployable code from environment-specific values.

The same JAR can run in development, testing and production:

```text
payment-api.jar
```

while each environment supplies different:

* Database URLs
* Timeouts
* Feature flags
* Log levels
* External API endpoints
* Pool sizes

Spring Boot can obtain configuration from:

* `application.properties`
* `application.yml`
* Profile-specific files
* Environment variables
* JVM system properties
* Command-line arguments
* Imported configuration files
* Kubernetes ConfigMaps and mounted files
* External configuration systems

Later or higher-priority property sources can override lower-priority sources. [Spring Boot externalized configuration](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html)

## Base configuration

```yaml
spring:
  application:
    name: payment-api

server:
  port: 8080

payment:
  timeout: 2s
  max-attempts: 3
```

## Profiles

A profile activates configuration or beans for a particular environment or scenario.

Files:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

`application.yml`:

```yaml
spring:
  application:
    name: payment-api

payment:
  timeout: 2s
```

`application-dev.yml`:

```yaml
payment:
  base-url: http://localhost:8090
  timeout: 10s

logging:
  level:
    com.example.payment: DEBUG
```

`application-prod.yml`:

```yaml
payment:
  base-url: https://payments.internal.example
  timeout: 2s

logging:
  level:
    com.example.payment: INFO
```

Activate a profile:

```bash
java -jar application.jar --spring.profiles.active=prod
```

or:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar application.jar
```

Profile-specific files override common configuration. If multiple profiles are active, order matters and later profiles can override earlier ones. [Spring Boot profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)

## Profile-specific beans

```java
@Configuration
@Profile("dev")
public class DevelopmentPaymentConfiguration {

    @Bean
    PaymentGateway fakePaymentGateway() {
        return new FakePaymentGateway();
    }
}
```

```java
@Configuration
@Profile("prod")
public class ProductionPaymentConfiguration {

    @Bean
    PaymentGateway paymentGateway() {
        return new RealPaymentGateway();
    }
}
```

Profiles affect bean-definition registration. If `prod` is inactive, its bean definitions are not added to the application context.

Use profiles for broad environmental differences. For individual optional features, a property condition is often clearer:

```java
@Bean
@ConditionalOnProperty(
    name = "features.fraud-check",
    havingValue = "true"
)
FraudChecker fraudChecker() {
    return new RemoteFraudChecker();
}
```

## `@Value`

```java
@Component
public class PaymentClient {

    public PaymentClient(
            @Value("${payment.base-url}") String baseUrl,
            @Value("${payment.timeout:2s}") Duration timeout
    ) {
        // ...
    }
}
```

Useful for one or two values, but it becomes difficult to maintain for structured configuration.

## `@ConfigurationProperties`

```java
@ConfigurationProperties(prefix = "payment")
@Validated
public record PaymentProperties(
        @NotBlank String baseUrl,
        @NotNull Duration timeout,
        @Min(1) int maxAttempts
) {
}
```

Enable it:

```java
@ConfigurationPropertiesScan
@SpringBootApplication
public class PaymentApplication {
}
```

Configuration:

```yaml
payment:
  base-url: https://payments.example.com
  timeout: 2s
  max-attempts: 3
```

Advantages over extensive `@Value` use:

* Type-safe binding
* Grouped configuration
* Validation during startup
* Better IDE metadata
* Easier testing
* Support for types such as `Duration`, `DataSize`, lists and maps

If a required configuration value is missing or invalid, application startup should fail rather than allowing a partially configured client into production.

## Environment variable mapping

This property:

```properties
payment.client.base-url=https://example.com
```

can typically be supplied as:

```bash
PAYMENT_CLIENT_BASE_URL=https://example.com
```

Spring’s relaxed binding handles the naming conversion.

## Secrets

Profiles are not a secret-management mechanism. Avoid storing production passwords and tokens in:

```text
application-prod.yml
```

Instead, inject them from:

* HashiCorp Vault
* Kubernetes Secrets
* AWS Secrets Manager
* Azure Key Vault
* Google Secret Manager
* Environment-specific deployment infrastructure

Implementing modules:

| Capability                 | Implementation                                     |
| -------------------------- | -------------------------------------------------- |
| Property resolution        | Spring Framework `Environment`                     |
| Boot configuration loading | Spring Boot Config Data API                        |
| Typed binding              | Spring Boot `@ConfigurationProperties`             |
| Validation                 | Jakarta Validation and Hibernate Validator         |
| Profiles                   | Spring Framework `@Profile` and Boot profile files |
| Vault integration          | Spring Vault / Spring Cloud Vault                  |
| Central configuration      | Spring Cloud Config                                |

---

# 5. Spring MVC web layer

## Request lifecycle

For a typical REST request:

```text
HTTP request
    ↓
Servlet filters
    ↓
DispatcherServlet
    ↓
HandlerMapping
    ↓
Controller method
    ↓
Service
    ↓
Return value
    ↓
HttpMessageConverter
    ↓
HTTP response
```

`DispatcherServlet` is Spring MVC’s front controller. It coordinates request mapping, parameter resolution, controller invocation, exception handling and response serialization.

Spring Boot configures this infrastructure when `spring-boot-starter-web` is present.

## REST controller

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(
            @PathVariable Long id
    ) {
        return service.findById(id);
    }
}
```

`@RestController` is effectively:

```java
@Controller
@ResponseBody
```

A return value annotated with `@ResponseBody` is serialized by an `HttpMessageConverter`. [Spring MVC `@ResponseBody`](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-methods/responsebody.html)

Common MVC annotations:

| Annotation        | Purpose                          |
| ----------------- | -------------------------------- |
| `@RequestMapping` | Common mapping rules             |
| `@GetMapping`     | Handle HTTP GET                  |
| `@PostMapping`    | Handle HTTP POST                 |
| `@PutMapping`     | Handle HTTP PUT                  |
| `@PatchMapping`   | Handle HTTP PATCH                |
| `@DeleteMapping`  | Handle HTTP DELETE               |
| `@PathVariable`   | Read a path segment              |
| `@RequestParam`   | Read a query parameter           |
| `@RequestHeader`  | Read a header                    |
| `@RequestBody`    | Deserialize the body             |
| `@ResponseStatus` | Select a response status         |
| `ResponseEntity`  | Control headers, status and body |

## Validation

Request DTO:

```java
public record PaymentRequest(

        @NotBlank
        @Size(max = 100)
        String description,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        @FutureOrPresent
        LocalDate paymentDate
) {
}
```

Controller:

```java
@PostMapping
public PaymentResponse create(
        @Valid @RequestBody PaymentRequest request
) {
    return service.create(request);
}
```

For invalid JSON input, deserialization can fail before the controller method is called.

For structurally valid JSON with invalid field values, Bean Validation runs and generally produces `MethodArgumentNotValidException`.

For direct method-parameter constraints:

```java
@GetMapping
public List<PaymentResponse> search(
        @RequestParam
        @Min(0)
        int offset,

        @RequestParam
        @Min(1)
        @Max(100)
        int limit
) {
    return service.search(offset, limit);
}
```

Depending on the Spring Framework version and method-validation configuration, violations are reported through method-validation exceptions such as `HandlerMethodValidationException`. Spring MVC supports global customization through `@ControllerAdvice`. [Spring MVC validation](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-validation.html)

Dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Implementation:

* Jakarta Bean Validation defines annotations and APIs.
* Hibernate Validator provides the default implementation.
* Spring MVC invokes validation during request processing.

## Exception handling

Local handler:

```java
@ExceptionHandler(PaymentNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
ProblemDetail handleNotFound(
        PaymentNotFoundException exception
) {
    ProblemDetail problem = ProblemDetail.forStatus(
            HttpStatus.NOT_FOUND
    );

    problem.setTitle("Payment not found");
    problem.setDetail(exception.getMessage());

    return problem;
}
```

Global handler:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            PaymentNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Payment not found");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Request validation failed"
                );

        return ResponseEntity.badRequest().body(problem);
    }
}
```

Exception-handling infrastructure includes:

* `HandlerExceptionResolver`
* `@ExceptionHandler`
* `@ControllerAdvice`
* `ResponseEntityExceptionHandler`
* `ProblemDetail`, based on RFC 9457 semantics

Keep controllers focused on HTTP concerns and services focused on business rules.

## JSON serialization and deserialization

Given:

```java
public record PaymentRequest(
        String description,
        BigDecimal amount
) {
}
```

incoming JSON:

```json
{
  "description": "Invoice 123",
  "amount": 1500.50
}
```

is converted into a `PaymentRequest` by:

```text
HTTP body
   ↓
MappingJackson2HttpMessageConverter
   ↓
Jackson ObjectMapper
   ↓
PaymentRequest
```

The reverse happens for responses:

```text
PaymentResponse
   ↓
Jackson ObjectMapper
   ↓
JSON response
```

Date formatting example:

```java
public record PaymentResponse(
        Long id,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate paymentDate
) {
}
```

Global Jackson configuration:

```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    deserialization:
      fail-on-unknown-properties: true
```

A custom mapper can be configured, but replacing Boot’s mapper entirely may remove useful auto-configured modules. Prefer:

```java
@Bean
Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder ->
            builder.featuresToEnable(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );
}
```

Implementing libraries:

| Capability                  | Implementation                                    |
| --------------------------- | ------------------------------------------------- |
| Servlet web framework       | Spring MVC, `spring-webmvc`                       |
| Front controller            | Spring MVC `DispatcherServlet`                    |
| JSON conversion             | Jackson Databind                                  |
| HTTP conversion abstraction | Spring `HttpMessageConverter`                     |
| Validation API              | Jakarta Bean Validation                           |
| Validation implementation   | Hibernate Validator                               |
| Embedded server             | Tomcat by default; Jetty or Undertow alternatives |
| Error representation        | Spring Framework `ProblemDetail`                  |

---

# 6. AOP and why proxies matter

## What AOP solves

Aspect-Oriented Programming extracts behavior that applies across many components.

Examples:

* Transaction management
* Authorization
* Caching
* Method execution timing
* Auditing
* Retry
* Asynchronous execution

Without AOP, transaction code might look like:

```java
transaction.begin();

try {
    Payment payment = repository.save(...);
    transaction.commit();
    return payment;
} catch (Exception exception) {
    transaction.rollback();
    throw exception;
}
```

With declarative transaction management:

```java
@Transactional
public Payment create(PaymentRequest request) {
    return repository.save(Payment.from(request));
}
```

The transaction behavior is not implemented by the annotation itself. The annotation is metadata interpreted by Spring infrastructure.

## Proxy lifecycle

Suppose the container creates:

```java
@Service
public class PaymentService {

    @Transactional
    public Payment create(PaymentRequest request) {
        return repository.save(...);
    }
}
```

The simplified startup sequence is:

1. Spring creates `PaymentService`.
2. Dependencies are injected.
3. A post-processor detects transactional metadata.
4. Spring creates a proxy around `PaymentService`.
5. Other beans receive the proxy, not necessarily the raw object.

Runtime call:

```text
Controller
   ↓
PaymentService proxy
   ↓ starts transaction
Real PaymentService
   ↓
Repository
   ↓
Proxy commits or rolls back
```

Spring AOP uses:

* JDK dynamic proxies when proxying suitable interfaces.
* Class-based proxies when proxying a concrete class.

## Self-invocation problem

```java
@Service
public class PaymentService {

    public void process() {
        savePayment(); // direct call on this
    }

    @Transactional
    public void savePayment() {
        // ...
    }
}
```

If another bean calls `process()`, that call passes through the proxy. But the internal `savePayment()` call is equivalent to:

```java
this.savePayment();
```

It does not leave the target object and return through the proxy. Therefore, the `@Transactional` interceptor does not run for that internal call.

In default proxy mode, only external calls entering through the proxy are intercepted. Spring explicitly documents that self-invocation does not create transactional behavior. [Spring declarative transaction management](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

A better design is to move the transactional operation into another bean:

```java
@Service
public class PaymentProcessor {

    private final PaymentWriter writer;

    public PaymentProcessor(PaymentWriter writer) {
        this.writer = writer;
    }

    public void process() {
        writer.savePayment();
    }
}
```

```java
@Service
public class PaymentWriter {

    @Transactional
    public void savePayment() {
        // ...
    }
}
```

Now the call from `PaymentProcessor` to `PaymentWriter` goes through the injected proxy.

## Manual construction bypasses proxies

This does not use the Spring-managed instance:

```java
PaymentService service = new PaymentService(repository);
service.create(request);
```

Consequently, Spring features such as these do not apply:

```java
@Transactional
@Async
@Cacheable
@PreAuthorize
@Retryable
```

unless the relevant behavior is implemented by some other mechanism.

## Initialization timing

Do not rely on transactional proxy behavior during object construction or `@PostConstruct`:

```java
@PostConstruct
@Transactional
void initialize() {
    // Do not assume a transactional proxy invocation here
}
```

The bean lifecycle and proxy may not yet be in a state where a call passes through the completed proxy.

For startup work, consider:

```java
@Component
public class StartupLoader {

    private final InitializationService service;

    public StartupLoader(InitializationService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        service.initialize();
    }
}
```

The call to another Spring bean can pass through its finished proxy.

## Implementing libraries

| Concern                     | Implementation                                          |
| --------------------------- | ------------------------------------------------------- |
| AOP abstraction and proxies | Spring AOP                                              |
| Pointcut/advice annotations | AspectJ annotations through `spring-aspects`/Spring AOP |
| Transactions                | Spring Transaction Management                           |
| JPA transactions            | Spring ORM plus JPA provider                            |
| JDBC transactions           | Spring JDBC                                             |
| Method security             | Spring Security                                         |
| Caching                     | Spring Cache abstraction                                |
| Retry                       | Spring Retry                                            |
| Asynchronous methods        | Spring Framework task execution                         |

Typical AOP dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

Transaction support is commonly brought in by data starters such as `spring-boot-starter-data-jpa` or `spring-boot-starter-jdbc`.

---

# 7. Spring Boot operational basics

## Actuator

Add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Common endpoints:

| Endpoint                | Purpose                          |
| ----------------------- | -------------------------------- |
| `/actuator/health`      | Overall application health       |
| `/actuator/info`        | Application information          |
| `/actuator/metrics`     | Available metrics                |
| `/actuator/prometheus`  | Prometheus-format metrics        |
| `/actuator/loggers`     | Inspect or modify logging levels |
| `/actuator/env`         | Configuration environment        |
| `/actuator/configprops` | Bound configuration properties   |
| `/actuator/beans`       | Spring beans                     |
| `/actuator/conditions`  | Auto-configuration conditions    |

Only `health` is commonly exposed by default over HTTP. Expose additional endpoints deliberately:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

Do not expose `env`, `beans`, `configprops`, `conditions` or `loggers` publicly without strong authentication and authorization.

## Health checks

Basic response:

```json
{
  "status": "UP"
}
```

Detailed response may include indicators for:

* Database
* Disk space
* Redis
* RabbitMQ
* Liveness/readiness state
* Custom dependencies

Boot registers health indicators conditionally. For example, a database health indicator can be created when:

* Actuator is present.
* A `DataSource` exists.
* The indicator has not been disabled.

Custom indicator:

```java
@Component
public class PaymentGatewayHealthIndicator
        implements HealthIndicator {

    private final PaymentGatewayClient client;

    public PaymentGatewayHealthIndicator(
            PaymentGatewayClient client
    ) {
        this.client = client;
    }

    @Override
    public Health health() {
        if (client.isAvailable()) {
            return Health.up().build();
        }

        return Health.down()
                .withDetail(
                        "reason",
                        "Payment gateway unavailable"
                )
                .build();
    }
}
```

Avoid slow health checks because platforms may invoke them frequently.

## Liveness versus readiness

### Liveness

Answers:

> Is this process internally alive, or should the platform restart it?

Endpoint:

```text
/actuator/health/liveness
```

Liveness should not normally depend on:

* Database availability
* Kafka availability
* External HTTP services
* Shared cache availability

If it does, an external outage could make every instance fail liveness and create a restart storm.

### Readiness

Answers:

> Can this instance currently accept useful traffic?

Endpoint:

```text
/actuator/health/readiness
```

When readiness is `DOWN` or `OUT_OF_SERVICE`, Kubernetes removes the instance from normal service routing.

A dependency should be included in readiness only when the application truly cannot serve meaningful requests without it. Spring Boot deliberately leaves this decision to the application. [Spring Boot Actuator endpoints and probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)

Enable probes outside Kubernetes:

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
```

Kubernetes example:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  periodSeconds: 5
```

## Logging

Spring Boot uses the SLF4J API and, by default, Logback as the implementation.

```java
@Service
public class PaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);

    public Payment create(PaymentRequest request) {
        log.info(
                "Creating payment for accountId={}",
                request.accountId()
        );

        return ...;
    }
}
```

Use parameterized logging:

```java
log.debug("Payment created: id={}", paymentId);
```

instead of eager string concatenation:

```java
log.debug("Payment created: id=" + paymentId);
```

Configuration:

```yaml
logging:
  level:
    root: INFO
    com.example.payment: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG

  file:
    name: logs/payment-api.log
```

Profile-specific logging:

```yaml
# application-dev.yml
logging:
  level:
    com.example.payment: DEBUG
```

```yaml
# application-prod.yml
logging:
  level:
    com.example.payment: INFO
```

Advanced Logback configuration can be placed in:

```text
logback-spring.xml
```

The `-spring` variant permits Spring Boot extensions such as profile-specific sections:

```xml
<springProfile name="dev">
    <root level="DEBUG"/>
</springProfile>

<springProfile name="prod">
    <root level="INFO"/>
</springProfile>
```

Never log:

* Passwords
* Access or refresh tokens
* Session IDs
* Database credentials
* Full payment-card data
* Unnecessarily sensitive request bodies

Implementing libraries:

| Capability                     | Implementation                         |
| ------------------------------ | -------------------------------------- |
| Operational endpoints          | Spring Boot Actuator                   |
| Health model                   | Spring Boot Actuator `HealthIndicator` |
| Metrics facade                 | Micrometer                             |
| Prometheus output              | Micrometer Prometheus Registry         |
| Logging API                    | SLF4J                                  |
| Default logging implementation | Logback                                |
| Alternative implementation     | Log4j2                                 |
| Distributed telemetry          | Micrometer Tracing/OpenTelemetry       |

Prometheus dependency:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

# 8. Consolidated implementation map

| Concept                   | Principal implementation                               |
| ------------------------- | ------------------------------------------------------ |
| IoC container             | Spring Framework Core/Context                          |
| Dependency injection      | Spring Beans and Spring Context                        |
| Component scanning        | Spring Context                                         |
| Starters                  | Spring Boot starter modules                            |
| Auto-configuration        | Spring Boot Autoconfigure                              |
| Properties and profiles   | Spring Core `Environment` plus Spring Boot Config Data |
| Typed configuration       | Spring Boot `@ConfigurationProperties`                 |
| REST controllers          | Spring MVC                                             |
| Embedded HTTP server      | Tomcat by default                                      |
| Request validation        | Jakarta Validation plus Hibernate Validator            |
| JSON serialization        | Jackson                                                |
| Exception handling        | Spring MVC                                             |
| AOP proxies               | Spring AOP                                             |
| Declarative transactions  | Spring Transaction Management                          |
| Method security           | Spring Security                                        |
| Health/readiness/liveness | Spring Boot Actuator                                   |
| Metrics                   | Micrometer                                             |
| Logging API               | SLF4J                                                  |
| Logging implementation    | Logback by default                                     |

The central mental model is: Spring first resolves the environment, converts configuration into bean definitions, creates and injects the beans, applies post-processors and proxies, and then exposes those completed objects to the MVC and operational layers. Most surprising Spring behavior becomes easier to diagnose by identifying which lifecycle stage produced it.
