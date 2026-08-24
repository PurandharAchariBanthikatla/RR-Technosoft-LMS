package com.rrtechnosoft.lms.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * End-to-end Finance module flow against a real Postgres + Redis (same
 * Testcontainers setup as AuthControllerIT — see that class's javadoc for
 * how to run *IT classes). Exercises the path that matters most: a fee
 * structure is created, assigned to a real student, a manual payment is
 * recorded against it, and the ledger (StudentFee status + balance) is
 * asserted end-to-end through real HTTP calls — no service mocks anywhere
 * in this test.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinanceFlowIT {

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
        registry.add("app.seed.super-admin-email", () -> "it-superadmin@rrtechnosoft.com");
        registry.add("app.seed.super-admin-password", () -> "Integration@Test123");
    }

    @LocalServerPort
    private int port;

    private String adminToken;

    @BeforeAll
    static void setUpRestAssured() {
        RestAssured.basePath = "/api/v1";
    }

    @BeforeEach
    void loginAsAdmin() {
        RestAssured.port = port;
        adminToken = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"identifier": "it-superadmin@rrtechnosoft.com", "password": "Integration@Test123"}
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .extract().path("accessToken");
    }

    @Test
    void feeLifecycle_assignThenPayInFull_marksTheStudentFeeAsPaid() {
        // 1. Create a course (fee structures may optionally reference one, keep it simple with none)
        String feeStructureId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "IT Test Course Fee",
                          "totalAmount": 1000,
                          "currency": "INR",
                          "installments": [ { "installmentNumber": 1, "amount": 1000, "dueAfterDays": 0 } ]
                        }
                        """)
        .when()
                .post("/finance/fee-structures")
        .then()
                .statusCode(201)
                .body("currency", equalTo("INR"))
                .extract().path("id");

        // 2. Create a student
        String studentId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "fullName": "Finance IT Student",
                          "initialPassword": "StudentPass123"
                        }
                        """)
        .when()
                .post("/students/manage")
        .then()
                .statusCode(201)
                .extract().path("id");

        // 3. Assign the fee structure to the student
        String studentFeeId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "studentId": "%s",
                          "feeStructureId": "%s",
                          "startDate": "2026-01-01"
                        }
                        """.formatted(studentId, feeStructureId))
        .when()
                .post("/finance/student-fees")
        .then()
                .statusCode(201)
                .body("status", equalTo("PENDING"))
                .body("netPayable", equalTo(1000))
                .extract().path("id");

        // 4. Record a manual payment for the full amount
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "studentFeeId": "%s",
                          "amount": 1000,
                          "method": "CASH",
                          "note": "IT test — front desk cash payment"
                        }
                        """.formatted(studentFeeId))
        .when()
                .post("/finance/payments/manual")
        .then()
                .statusCode(201)
                .body("status", equalTo("SUCCESS"))
                .body("amount", equalTo(1000));

        // 5. The student fee record should now show PAID with zero balance
        given()
                .header("Authorization", "Bearer " + adminToken)
        .when()
                .get("/finance/student-fees/" + studentFeeId)
        .then()
                .statusCode(200)
                .body("status", equalTo("PAID"))
                .body("balanceDue", equalTo(0))
                .body("amountPaid", equalTo(1000));
    }

    @Test
    void assign_rejectsMissingFeeStructureAndCustomPlan() {
        String studentId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "fullName": "Underspecified Fee Student", "initialPassword": "StudentPass123" }
                        """)
        .when()
                .post("/students/manage")
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "studentId": "%s", "startDate": "2026-01-01" }
                        """.formatted(studentId))
        .when()
                .post("/finance/student-fees")
        .then()
                .statusCode(400);
    }
}
