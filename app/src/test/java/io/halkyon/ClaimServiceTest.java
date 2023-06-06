package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.halkyon.utils.TestUtils.createClusterWithServiceAvailable;
import static io.halkyon.utils.TestUtils.createServiceWithCredential;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.halkyon.model.Claim;
import io.halkyon.services.ClaimService;
import io.halkyon.services.ClaimStatus;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClaimServiceTest extends BaseTest {

    @Inject
    ClaimService claimService;

    @ConfigProperty(name = "primaza.update-claim-job.max-attempts")
    int maxAttempts;

    @Test
    public void testJobShouldMarkClaimAsErrorAfterMaxAttemptsExceeded() {
        pauseScheduler();
        Claim postgresqlClaim = createClaim("Postgresql-ClaimingServiceJobTest", "postgresqlClaimingServiceJobTest-8");
        Claim mySqlClaim = createClaim("MySQL-ClaimingServiceJobTest", "MySQLClaimingServiceJobTest-7.5");
        createClusterWithServiceAvailable("testJobShouldMarkClaimAsErrorCluster", "host:9999",
                mockKubernetesClientService, "protocol", "9999");
        createServiceWithCredential("postgresqlClaimingServiceJobTest", "8", "postgresql", "protocol:9999");
        // Given 2 claims for which only one of them (postgresql) have a matching available service (Claims are created
        // with status "new" and attempts set to 1)
        // When we run the job once:
        // Then:
        // - the claim "PostgresSQL" should change from "NEW" to "BINDABLE", attempts still to 1
        // - the claim "MySQL" should change from "new" to "pending", as no service is running for MySQL claim, should
        // increase the attempts to 2
        claimService.execute();
        Claim actualPostgresql = given().contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + postgresqlClaim.name).then().statusCode(200).extract().as(Claim.class);

        assertEquals(postgresqlClaim.name, actualPostgresql.name);
        assertEquals("postgresql", actualPostgresql.type);
        assertEquals(ClaimStatus.BINDABLE.toString(), actualPostgresql.status);
        assertEquals(1, actualPostgresql.attempts);

        Claim actualMysql = given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + mySqlClaim.name)
                .then().statusCode(200).extract().as(Claim.class);

        assertEquals(mySqlClaim.name, actualMysql.name);
        assertNull(actualMysql.type);
        assertEquals(ClaimStatus.PENDING.toString(), actualMysql.status);
        assertEquals(2, actualMysql.attempts);

        // When we repeat running the job again until reaching the maxAttempts:
        // Then:
        // - the claim "PostgresSQL" should not change (still BINDABLE)
        // - the claim "mysql-demo" should increase the attempts to 3 and status is still Pending.
        // We iterate from the number of attempts until the max attempts just for checking the "PENDING" status.
        for (int attempt = 2; attempt < maxAttempts; attempt++) {
            claimService.execute();
            actualPostgresql = given().contentType(MediaType.APPLICATION_JSON)
                    .get("/claims/name/" + postgresqlClaim.name).then().statusCode(200).extract().as(Claim.class);

            assertEquals(postgresqlClaim.name, actualPostgresql.name);
            assertEquals(ClaimStatus.BINDABLE.toString(), actualPostgresql.status);
            assertEquals(1, actualPostgresql.attempts);

            actualMysql = given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + mySqlClaim.name).then()
                    .statusCode(200).extract().as(Claim.class);

            assertEquals(mySqlClaim.name, actualMysql.name);
            assertEquals(ClaimStatus.PENDING.toString(), actualMysql.status);
            assertEquals(3, actualMysql.attempts);

        }

        // When the job runs again after reaching the maxAttempts:
        // Then:
        // - the claim "mysql-demo" should change the status to ERROR.

        claimService.execute();

        actualMysql = given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + mySqlClaim.name).then()
                .statusCode(200).extract().as(Claim.class);

        assertEquals(mySqlClaim.name, actualMysql.name);
        assertEquals(ClaimStatus.ERROR.toString(), actualMysql.status);
        assertTrue(
                actualMysql.errorMessage.startsWith(
                        String.format(ClaimService.ERROR_MESSAGE_NO_SERVICE_REGISTERED, actualMysql.serviceRequested)),
                "Unexpected error message " + actualMysql.errorMessage);
        assertEquals(maxAttempts, actualMysql.attempts);
    }

    @Test
    public void testShouldClaimServiceWhenNewClaimIsCreated() {
        pauseScheduler();
        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", "Oracle1")
                .formParam("serviceRequested", "oracle-1234").formParam("description", "Description").when()
                .post("/claims").then().statusCode(201);
        given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/Oracle1").then().statusCode(200)
                .body("status", is(ClaimStatus.PENDING.toString())).body("attempts", is(1));
    }
}
