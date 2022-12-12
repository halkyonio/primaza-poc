package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.halkyon.model.Claim;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.ClaimingServiceJob;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClaimingServiceJobTest {

    @Inject
    ClaimingServiceJob job;

    @ConfigProperty(name = "servicebox.claiming-service-job.max-attempts")
    int maxAttempts;

    @Inject
    Scheduler scheduler;

    @Test
    public void testJobShouldMarkClaimAsErrorAfterMaxAttemptsExceeded(){
        pauseScheduler();
        Claim postgresqlClaim = createClaim("Postgresql-ClaimingServiceJobTest", "postgresqlClaimingServiceJobTest-8");
        Claim mySqlClaim = createClaim("MySQL-ClaimingServiceJobTest", "MySQLClaimingServiceJobTest-7.5");
        createService("postgresqlClaimingServiceJobTest", "8", "postgresql", "demo", true);
        createService("MySQLClaimingServiceJobTest", "7.5", "mysql", "demo", false);
        // Given 2 claims for which only one of them (postgresql) have a matching available service (Claims are created with status "new" and attempts set to 1)
        // When we run the job once:
        // Then:
        // - the claim "PostgresSQL" should change from "new" to "pending", attempts still to 1
        // - the claim "MySQL" should change from "new" to "pending", as no service is running for MySQL claim, should increase the attempts to 2

        job.execute();
        Claim actualPostgresql

                = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + postgresqlClaim.name)
                .then()
                .statusCode(200).extract().as(Claim.class);

        assertEquals(postgresqlClaim.name, actualPostgresql.name);
        assertEquals(ClaimStatus.BIND.toString(),actualPostgresql.status);
        assertEquals(1,actualPostgresql.attempts);

        Claim actualMysql = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + mySqlClaim.name)
                .then()
                .statusCode(200).extract().as(Claim.class);

        assertEquals(mySqlClaim.name, actualMysql.name);
        assertEquals(ClaimStatus.PENDING.toString(),actualMysql.status);
        assertEquals(2,actualMysql.attempts);

        // When we repeat running the job again until reaching the maxAttempts:
        // Then:
        // - the claim "PostgresSQL" should not change (it's already binded)
        // - the claim "mysql-demo" should increase the attempts to 3 and status is still Pending.
        // We iterate from the number of attempts until the max attempts just for checking the "PENDING" status.
        for (int attempt = 2; attempt < maxAttempts; attempt++) {
            job.execute();
            actualPostgresql = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .get("/claims/name/" + postgresqlClaim.name)
                    .then()
                    .statusCode(200).extract().as(Claim.class);

            assertEquals(postgresqlClaim.name, actualPostgresql.name);
            assertEquals(ClaimStatus.BIND.toString(),actualPostgresql.status);
            assertEquals(1,actualPostgresql.attempts);

            actualMysql = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .get("/claims/name/" + mySqlClaim.name)
                    .then()
                    .statusCode(200)
                    .extract().as(Claim.class);

            assertEquals(mySqlClaim.name, actualMysql.name);
            assertEquals(ClaimStatus.PENDING.toString(), actualMysql.status);
            assertEquals(3,actualMysql.attempts);

        }

        // When the job runs again after reaching the maxAttempts:
        // Then:
        // - the claim "mysql-demo" should change the status to ERROR.

        job.execute();

        actualMysql = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + mySqlClaim.name)
                .then()
                .statusCode(200)
                .extract().as(Claim.class);

        assertEquals(mySqlClaim.name, actualMysql.name);
        assertEquals(ClaimStatus.ERROR.toString(), actualMysql.status);
        assertEquals(String.format(ClaimingServiceJob.ERROR_MESSAGE_NO_SERVICE_FOUND, maxAttempts), actualMysql.errorMessage);
        assertEquals(maxAttempts, actualMysql.attempts);
    }

    @Test
    public void testShouldClaimServiceWhenNewClaimIsCreated(){
        pauseScheduler();
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", "Oracle1")
                .formParam("serviceRequested", "oracle-1234")
                .formParam("description", "Description")
                .when().post("/claims")
                .then()
                .statusCode(201);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/Oracle1")
                .then()
                .statusCode(200)
                .body("status", is(ClaimStatus.PENDING.toString()))
                .body("attempts", is(1));
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
