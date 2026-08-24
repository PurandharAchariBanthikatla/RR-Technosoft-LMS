package com.rrtechnosoft.lms.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end integration test: real Spring context, a real (ephemeral)
 * Postgres container with the actual Flyway migrations applied, a real
 * Redis container (management.health.redis is enabled, so /actuator/health
 * needs one reachable), and real HTTP calls via REST Assured against the
 * embedded server — no mocks.
 *
 * This is the pattern to follow for further *IT classes; run them with:
 *   mvn verify -Pintegration-test
 * (they're excluded from the default `mvn test` unit-test run — see pom.xml).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("rr_lms_it")
            .withUsername("lms_user")
            .withPassword("lms_test_pw");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // The DataSeeder creates this Super Admin on first startup — asserted against below.
        registry.add("app.seed.super-admin-email", () -> "it-superadmin@rrtechnosoft.com");
        registry.add("app.seed.super-admin-password", () -> "Integration@Test123");
    }

    @LocalServerPort
    private int port;

    @BeforeAll
    static void setUpRestAssured() {
        RestAssured.basePath = "/api/v1";
    }

    @Test
    void login_withSeededSuperAdminCredentials_returnsTokens() {
        RestAssured.port = port;

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"identifier": "it-superadmin@rrtechnosoft.com", "password": "Integration@Test123"}
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("user.role", equalTo("SUPER_ADMIN"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() {
        RestAssured.port = port;

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"identifier": "it-superadmin@rrtechnosoft.com", "password": "totally-wrong"}
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(401);
    }

    @Test
    void healthEndpoint_isPubliclyAccessibleAndUp() {
        RestAssured.port = port;

        given()
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
