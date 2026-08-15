# 6) Security (Spring Security + web basics)
**Review**

<br>

- **OWASP Top 10** awareness and mitigations (injection, XSS, CSRF, sensitive data exposure, insufficient logging).
- **Authentication vs authorization**; common patterns for APIs.
- **JWT:** structure, validation, expiration/refresh; where to store tokens for web vs mobile.
- **OAuth2/OpenID** Connect high-level flows; **SSO** basics.
- **Security headers** (CORS, cache-control, clickjacking protections).
- **Secure password storage and secrets** handling concepts.

<br>

---

A useful distinction:

* Spring Security handles authentication, authorization, common web protections, OAuth2/OIDC integration and password encoding.
* OWASP provides security guidance, not a runtime implementation.
* Some vulnerabilities—SQL injection, XSS, secret leakage, insecure business rules—cannot be solved by Spring Security alone.

---

<br>
<br>


## 1. OWASP Top 10 awareness and mitigations

The [OWASP Top 10](https://owasp.org/Top10/2025/) is an awareness standard describing major web application security risks.

### Injection

Injection occurs when untrusted input becomes part of a command or query.

Examples:

```java
// Vulnerable
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

```java
// Safer: parameterized query
jdbcTemplate.query(
    "SELECT * FROM users WHERE username = ?",
    rowMapper,
    username
);
```

For JPA:

```java
@Query("select u from User u where u.username = :username")
Optional<User> findByUsername(@Param("username") String username);
```

Principal mitigations:

* Parameterized queries and prepared statements.
* Spring Data JPA repositories and bound query parameters.
* Validate input with allowlists.
* Never concatenate user input into SQL, JPQL, operating-system commands or expressions.
* Run the database account with minimum privileges.

Implementations:

* Spring Data JPA/Hibernate
* Spring JDBC `JdbcTemplate`
* Jakarta Bean Validation/Hibernate Validator
* Database permissions
* OWASP Dependency-Check for vulnerable dependencies
* Semgrep, SonarQube, Snyk or CodeQL for static analysis

Validation helps, but parameterization is the primary SQL-injection defense. Injection includes SQL, NoSQL, OS-command, LDAP and expression-language injection, among others. [OWASP Injection guidance](https://owasp.org/Top10/2021/A03_2021-Injection/)

### Cross-Site Scripting — XSS

XSS occurs when untrusted data is rendered as executable HTML or JavaScript.

Mitigations:

* Encode output according to its destination: HTML, attribute, JavaScript or URL.
* Avoid rendering untrusted HTML.
* Sanitize HTML when rich-text input is genuinely required.
* Use Content Security Policy.
* Avoid unsafe frontend APIs such as `innerHTML`.
* Do not rely exclusively on input validation.

Implementations:

* Thymeleaf automatic output escaping with `th:text`
* OWASP Java Encoder
* OWASP Java HTML Sanitizer
* Spring Security for Content Security Policy headers
* Frontend framework escaping, such as React’s normal JSX interpolation

Example:

```html
<!-- Escaped by Thymeleaf -->
<span th:text="${userComment}"></span>

<!-- Dangerous if userComment is untrusted -->
<div th:utext="${userComment}"></div>
```

Spring Security cannot prevent XSS if the application deliberately inserts unsafe HTML.

### Cross-Site Request Forgery — CSRF

CSRF makes a browser send an authenticated request without the user’s intention. It is primarily relevant when authentication credentials are attached automatically, especially cookies.

Spring Security enables CSRF protection by default for unsafe methods such as `POST`. [Spring Security CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

For a server-rendered application using session cookies:

```java
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(Customizer.withDefaults())
        .build(); // CSRF remains enabled
}
```

For a stateless API where the client explicitly sends a Bearer token in the `Authorization` header:

```java
http
    .csrf(csrf -> csrf.disable())
    .oauth2ResourceServer(oauth2 -> oauth2.jwt());
```

Disabling CSRF is reasonable only after verifying that authentication does not depend on automatically attached browser credentials. If JWT authentication uses cookies, CSRF protection is still necessary.

Implementations:

* Spring Security CSRF filters and tokens
* `CookieCsrfTokenRepository` for browser clients
* SameSite cookie settings
* Angular’s built-in XSRF convention

### Sensitive data exposure / cryptographic failures

The newer OWASP terminology emphasizes cryptographic failures rather than treating exposure as the root problem. [OWASP Cryptographic Failures](https://owasp.org/Top10/2021/A02_2021-Cryptographic_Failures/)

Mitigations:

* Use HTTPS/TLS everywhere.
* Never place passwords, access tokens or personal data in logs.
* Encrypt sensitive data at rest when required.
* Do not write custom cryptographic algorithms.
* Minimize collection and retention of sensitive information.
* Apply appropriate `Cache-Control` headers.
* Keep secrets outside source code and container images.
* Avoid returning internal exception details to clients.

Implementations:

* TLS in the reverse proxy, application server or cloud load balancer
* Java Cryptography Architecture
* Spring Security
* Spring Vault and HashiCorp Vault
* Kubernetes Secrets, preferably combined with an external secret manager
* AWS Secrets Manager, Azure Key Vault or Google Secret Manager
* Logback/Log4j2 masking filters

### Insufficient security logging

Security events should be distinguishable from ordinary application debugging.

Record events such as:

* Successful and failed authentication
* Access denied decisions
* Password or MFA changes
* Refresh-token reuse or revocation
* Administrative operations
* Account lockouts
* Suspicious request patterns

Do not log:

* Passwords
* Full access or refresh tokens
* Session identifiers
* Client secrets
* Sensitive personal or financial data

Implementations:

* SLF4J with Logback or Log4j2
* Spring Security authentication and authorization events
* Spring Boot Actuator
* Micrometer
* OpenTelemetry
* Centralized platforms such as ELK/OpenSearch, Splunk, Grafana Loki or a SIEM

Logging should be connected to alerts; merely storing events is insufficient. OWASP specifically recommends monitoring patterns such as repeated authentication failures and bursts of login attempts. [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)

---

## 2. Authentication versus authorization

### Authentication

Authentication answers:

> Who are you?

Examples:

* Username and password
* Client certificate
* API key
* OAuth2 access token
* OIDC login
* Passkey or multifactor authentication

In Spring Security, successful authentication produces an `Authentication` object stored in the `SecurityContext`.

```java
Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

String username = authentication.getName();
```

### Authorization

Authorization answers:

> What are you allowed to do?

Examples:

* Only administrators may delete users.
* A customer may access only their own account.
* A token must contain the `payments.write` scope.

Request-level authorization:

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
    .anyRequest().authenticated()
);
```

Method-level authorization:

```java
@EnableMethodSecurity
@Configuration
class MethodSecurityConfiguration {
}
```

```java
@PreAuthorize("hasAuthority('SCOPE_payments.write')")
public Payment createPayment(PaymentRequest request) {
    // ...
}
```

Object-level checks are also necessary:

```java
@PreAuthorize("#customerId == authentication.name or hasRole('ADMIN')")
public Customer getCustomer(String customerId) {
    // ...
}
```

A role check alone does not prevent one valid customer from requesting another customer’s resource. This is a common broken-access-control problem. [OWASP Broken Access Control](https://owasp.org/Top10/2021/A01_2021-Broken_Access_Control/)

### Common API patterns

| Pattern                   | Appropriate use                                     | Main limitation                                     |
| ------------------------- | --------------------------------------------------- | --------------------------------------------------- |
| HTTP Basic                | Internal tools over TLS, simple machine integration | Sends credentials on every request                  |
| Session cookie            | Traditional web application or BFF                  | Server-side session state; requires CSRF protection |
| API key                   | Identifying a calling application                   | Usually does not represent an end user              |
| OAuth2 Bearer token       | Distributed APIs and delegated access               | Token theft and lifecycle must be managed           |
| mTLS                      | High-trust service-to-service communication         | Certificate management complexity                   |
| OAuth2 Client Credentials | Service-to-service authorization                    | Represents the client, not a human user             |

Implementations:

* Spring Security
* Spring Security OAuth2 Resource Server
* Spring Security OAuth2 Client
* Spring Authorization Server
* Identity providers such as Keycloak, Auth0, Okta, Microsoft Entra ID or Amazon Cognito

---

## 3. JWT

A JSON Web Token normally contains three Base64URL-encoded sections:

```text
header.payload.signature
```

### Header

```json
{
  "alg": "RS256",
  "kid": "key-2026-01",
  "typ": "JWT"
}
```

### Payload

```json
{
  "iss": "https://identity.example.com",
  "sub": "user-123",
  "aud": "payments-api",
  "scope": "payments.read payments.write",
  "iat": 1785570000,
  "exp": 1785570900
}
```

### Signature

The signature protects integrity and authenticity. It does not encrypt the payload. Anyone possessing the token can usually decode its claims, so secrets and sensitive personal data must not be placed in a normal signed JWT.

### Required validation

An API should validate:

* Cryptographic signature
* Allowed algorithm
* `iss`: expected issuer
* `aud`: intended API
* `exp`: expiration time
* `nbf`: not valid before, when present
* Required scopes or authorities
* Key identity and key rotation
* Reasonable clock skew

Never select an unsafe verification algorithm merely because the token header requests it.

Spring configuration:

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    return http
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt())
        .build();
}
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.example.com
```

Spring obtains the provider metadata and signing keys, verifies the signature and validates standard claims. JWT support uses `spring-security-oauth2-resource-server`, while decoding and signature verification use `spring-security-oauth2-jose`. [Spring Security JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

Dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Spring Security commonly uses Nimbus JOSE + JWT internally. Other JWT libraries include:

* Nimbus JOSE + JWT
* JJWT
* Auth0 `java-jwt`

For an OAuth2 API, prefer Spring Security’s resource-server integration over writing a custom JWT filter.

### Expiration and refresh

Access tokens should be relatively short-lived. A refresh token is presented to the authorization server—not to the business API—to obtain a new access token.

Refresh-token protections include:

* Longer lifetime than access tokens, but still finite.
* Rotation after every use.
* Detection of refresh-token reuse.
* Revocation on logout, credential compromise or account disablement.
* Binding to the appropriate client.
* Secure server-side storage where possible.

Current OAuth security guidance is defined in [RFC 9700](https://datatracker.ietf.org/doc/rfc9700/).

JWT access tokens are difficult to revoke immediately without additional infrastructure. Options include:

* Short expiration
* Revocation or deny list
* Token-version checks
* Opaque tokens with introspection

Opaque token introspection is often more suitable when immediate centralized revocation is a major requirement. [Spring Security opaque tokens](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/opaque-token.html)

### Token storage

| Client                        | Recommended approach                                                         |
| ----------------------------- | ---------------------------------------------------------------------------- |
| Traditional web application   | Server-side session plus secure session cookie                               |
| Browser SPA                   | Prefer a Backend-for-Frontend; keep OAuth tokens on the server               |
| Browser cookie authentication | `Secure`, `HttpOnly`, appropriate `SameSite`; add CSRF protection            |
| Pure SPA handling tokens      | Prefer memory over persistent browser storage; use Authorization Code + PKCE |
| Android                       | Android Keystore-backed secure storage                                       |
| iOS                           | Keychain                                                                     |
| Backend service               | Secret manager or controlled server-side token store                         |

Avoid persisting long-lived access or refresh tokens in `localStorage`: any successful XSS running in that origin can access them. An `HttpOnly` cookie blocks direct JavaScript access but is automatically attached by the browser, which introduces CSRF considerations.

---

## 4. OAuth2 and OpenID Connect

OAuth2 and OIDC solve related but different problems:

* OAuth2: delegated authorization—access to a protected resource.
* OpenID Connect: authentication and user identity built on OAuth2.

### Main roles

* Resource owner: usually the user
* Client: application requesting access
* Authorization server: authenticates and issues tokens
* Resource server: API accepting access tokens

### Authorization Code with PKCE

Recommended for browser and mobile applications:

1. Client redirects the user to the authorization server.
2. The user authenticates there.
3. Authorization server redirects back with a short-lived authorization code.
4. Client exchanges the code plus PKCE verifier for tokens.
5. Client calls the API using the access token.

PKCE protects intercepted authorization codes. The Implicit flow should not be selected for new systems. Current OAuth guidance favors Authorization Code with PKCE and other protections described in [RFC 9700](https://datatracker.ietf.org/doc/rfc9700/).

### Client Credentials

Used for service-to-service communication:

```text
Service A → Authorization Server: client credentials
Service A ← Authorization Server: access token
Service A → Service B: Bearer access token
```

There is no end user. The token represents Service A.

### OIDC additions

OIDC adds:

* ID token
* UserInfo endpoint
* Standard identity claims
* Discovery metadata
* Login/session conventions

Important distinction:

* Access token: sent to an API.
* ID token: tells the client about the authenticated user.
* Refresh token: sent only to the authorization server.

An ID token should not normally be used as a business API access token.

### SSO

Single Sign-On means the user authenticates with a central identity provider and can then access several applications.

For example:

```text
Application A ─┐
Application B ─┼── Identity Provider
Application C ─┘
```

Each application still has its own authorization rules. SSO centralizes authentication; it does not imply that every authenticated user can access every application.

Implementations:

* Spring Security OAuth2 Client: application login and outbound OAuth clients
* Spring Security Resource Server: APIs validating tokens
* Spring Authorization Server: implementing an authorization server
* Keycloak: complete identity provider with OIDC, OAuth2 and SSO
* Auth0, Okta, Entra ID, Cognito: managed providers

Spring Security provides comprehensive servlet-based OAuth2 client and resource-server support. [Spring Security OAuth2 documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

---

## 5. Security headers, CORS and caching

### CORS

CORS determines which browser origins may call an API. It is a browser policy, not an authentication or authorization mechanism.

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
        List.of("https://app.example.com")
    );
    configuration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE")
    );
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-CSRF-TOKEN")
    );
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

```java
http.cors(Customizer.withDefaults());
```

Do not combine credentialed requests with a wildcard origin. CORS must be processed before security authentication because browser preflight requests do not normally contain authentication cookies. [Spring Security CORS documentation](https://docs.spring.io/spring-security/reference/reactive/integrations/cors.html)

CORS does not stop Postman, `curl` or another backend from calling the API.

### Cache-Control

Sensitive responses should generally not be stored in shared or browser caches:

```http
Cache-Control: no-store
Pragma: no-cache
```

However, not every response should be marked `no-store`; public immutable resources can safely benefit from caching.

Spring Security supplies secure cache-control defaults for protected responses, and applications can customize them through header configuration.

### Clickjacking

Clickjacking embeds a page inside a malicious frame and tricks the user into interacting with it.

Relevant headers:

```http
X-Frame-Options: DENY
```

or a Content Security Policy:

```http
Content-Security-Policy: frame-ancestors 'none'
```

Spring Security provides `X-Frame-Options: DENY` by default. Configure frame access explicitly if the application genuinely needs it.

### Additional important headers

```http
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

Example configuration:

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp ->
        csp.policyDirectives(
            "default-src 'self'; " +
            "script-src 'self'; " +
            "object-src 'none'; " +
            "frame-ancestors 'none'"
        )
    )
    .frameOptions(frame -> frame.deny())
    .referrerPolicy(referrer -> referrer
        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy
            .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
    )
);
```

A restrictive Content Security Policy must be adapted and tested against the frontend; blindly adding one can break scripts, styles, fonts or external resources.

---

## 6. Secure password storage

Passwords should be hashed, not reversibly encrypted.

A password hashing algorithm must be:

* Salted
* Intentionally slow
* Configured with an appropriate work factor
* Upgradeable as hardware improves

Recommended algorithms:

* Argon2id
* bcrypt
* scrypt
* PBKDF2

Spring Security provides `PasswordEncoder`.

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

It stores an algorithm identifier with the hash:

```text
{bcrypt}$2a$10$...
```

Usage:

```java
String encoded = passwordEncoder.encode(rawPassword);

boolean matches =
        passwordEncoder.matches(loginPassword, encoded);
```

Do not:

* Store plaintext passwords.
* Hash them with plain MD5, SHA-1 or SHA-256.
* use a fixed application-wide salt.
* Compare hashes manually.
* Log incoming credentials.

Libraries:

* Spring Security Crypto / `PasswordEncoder`
* Bouncy Castle when specialized cryptography is required
* Argon2 implementation used through Spring Security
* A provider-managed identity platform, which can avoid local password storage entirely

Spring Security’s delegating encoder is particularly useful for gradual migration:

```text
{bcrypt}old hash
{argon2}new hash
```

A successful login can trigger re-encoding when the stored algorithm or work factor is obsolete.

---

## 7. Secrets handling

Secrets include:

* Database passwords
* OAuth client secrets
* Private signing keys
* API keys
* Encryption keys
* TLS private keys

Basic principles:

* Never commit secrets to Git.
* Do not place production secrets directly in `application.yml`.
* Keep different secrets for development, testing and production.
* Grant access using least privilege.
* Rotate secrets.
* Audit secret access.
* Do not expose secrets through Actuator, logs or error messages.
* Prefer short-lived workload credentials over permanent credentials.

Environment-variable configuration:

```yaml
spring:
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

This avoids committing the value, but environment variables are not a complete secret-management system. Depending on the platform, they may be visible to administrators, process-inspection tools, debug dumps or misconfigured diagnostics.

Better production implementations:

* HashiCorp Vault with Spring Vault
* Kubernetes Secrets plus External Secrets Operator
* AWS Secrets Manager
* Azure Key Vault
* Google Secret Manager
* Cloud workload identity or IAM roles
* Sealed Secrets or SOPS for encrypted configuration in Git

Private JWT signing keys should normally belong to the authorization server. Business APIs should receive only the public verification keys through a trusted JWKS endpoint.

## Recommended Spring Boot API stack

For a typical production API:

| Requirement                               | Recommended implementation                            |
| ----------------------------------------- | ----------------------------------------------------- |
| Authentication                            | External OIDC/OAuth2 provider                         |
| Token validation                          | Spring Security OAuth2 Resource Server                |
| Authorization                             | Request rules plus `@PreAuthorize`                    |
| JWT implementation                        | Spring Security OAuth2 JOSE/Nimbus                    |
| Password hashing, if local accounts exist | Spring Security `PasswordEncoder`                     |
| Input validation                          | Jakarta Validation/Hibernate Validator                |
| SQL safety                                | Spring Data JPA or parameterized Spring JDBC          |
| Browser protection                        | Spring Security CSRF and headers                      |
| CORS                                      | Spring Framework CORS integrated with Spring Security |
| Secrets                                   | Vault or cloud secret manager                         |
| Audit and monitoring                      | SLF4J, Actuator, Micrometer, OpenTelemetry and SIEM   |
| Dependency scanning                       | OWASP Dependency-Check, Dependabot, Snyk or similar   |

The most important architectural rule is that security is layered. JWT validation does not replace authorization; CORS does not replace authentication; validation does not replace parameterized SQL; and Spring Security does not correct unsafe output rendering or leaked secrets automatically.







<br>
<br>
<br>

---

---

<br>
<br>
<br>


# Injection Implementations


**Good solutions for parameterization in Java 17 + Spring Boot** (to prevent SQL injection)

The core rule remains the same: **never concatenate user-controlled data into SQL/JPQL strings**. Always bind values as parameters so the driver/ORM treats them strictly as data.

Here are the practical, recommended approaches ranked by common usage in modern Spring Boot applications:

### 1. Spring Data JPA Repositories (Preferred for most cases)

Spring Data JPA automatically uses parameterized queries under the hood.

**Derived query methods** (safest and simplest):
```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmailAndActive(String email, boolean active);
    // Spring generates: WHERE email = ? AND active = ?
}
```

**`@Query` with named parameters** (recommended over positional):
```java
@Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
List<User> findByEmailAndStatus(@Param("email") String email,
                                @Param("status") UserStatus status);
```

**Native queries** still require parameters:
```java
@Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
Optional<User> findByEmailNative(@Param("email") String email);
```

Avoid anything like:
```java
// BAD – vulnerable
@Query("SELECT u FROM User u WHERE u.email = '" + email + "'")
```

### 2. JdbcTemplate / NamedParameterJdbcTemplate

When you need raw SQL or are not using JPA:

```java
@Autowired
private NamedParameterJdbcTemplate namedJdbc;

public User findByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email = :email";
    MapSqlParameterSource params = new MapSqlParameterSource("email", email);
    return namedJdbc.queryForObject(sql, params, new UserRowMapper());
}
```

Or classic `JdbcTemplate` with `?` placeholders:
```java
jdbcTemplate.query("SELECT * FROM users WHERE email = ?",
                   new Object[]{email},
                   new UserRowMapper());
```

`NamedParameterJdbcTemplate` is generally preferred for readability and maintainability.

### 3. EntityManager / TypedQuery (JPA Criteria or JPQL)

```java
@PersistenceContext
private EntityManager em;

public List<User> findUsers(String email) {
    return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
             .setParameter("email", email)
             .getResultList();
}
```

For fully dynamic queries, prefer the **Criteria API** or **Specifications**:

```java
public List<User> search(String email, Boolean active) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> root = cq.from(User.class);

    List<Predicate> predicates = new ArrayList<>();
    if (email != null) {
        predicates.add(cb.equal(root.get("email"), email));  // still parameterized
    }
    if (active != null) {
        predicates.add(cb.equal(root.get("active"), active));
    }
    cq.where(predicates.toArray(new Predicate[0]));
    return em.createQuery(cq).getResultList();
}
```

Spring Data’s `JpaSpecificationExecutor` makes this even cleaner.

### 4. Querydsl (Excellent for complex/dynamic queries)

Type-safe and fully parameterized. Works well with Spring Boot 3 / Java 17:

```java
QUser user = QUser.user;
List<User> results = queryFactory
    .selectFrom(user)
    .where(user.email.eq(email)
           .and(user.active.eq(true)))
    .fetch();
```

### Additional Best Practices

| Practice                        | Recommendation                                      |
|--------------------------------|-----------------------------------------------------|
| Prefer named over positional parameters | Yes – less error-prone when refactoring            |
| Use Spring Data repositories   | Highest productivity + automatic parameterization  |
| Avoid string concatenation     | Never, even with “sanitized” input                 |
| Stored procedures              | Also use parameters (`CallableStatement`)          |
| Input validation               | Keep it as defense-in-depth, not the primary defense |
| Least-privilege DB users       | Always                                               |
| Logging                        | Enable `logging.level.org.hibernate.SQL=DEBUG` and `org.hibernate.type.descriptor.sql=TRACE` to verify parameters are bound |

### Quick Decision Guide

- Simple CRUD / most business queries → **Spring Data JPA repositories**
- Complex dynamic filters → **Specifications** or **Querydsl**
- Heavy raw SQL / reporting → **NamedParameterJdbcTemplate**
- Maximum type safety → **Querydsl**

With Java 17 + Spring Boot 3.x these approaches work out of the box. The moment you see string concatenation involving external input in a query, treat it as a bug.

Would you like concrete examples for a specific scenario (e.g. dynamic search with multiple optional filters, native queries with joins, or batch operations)?
