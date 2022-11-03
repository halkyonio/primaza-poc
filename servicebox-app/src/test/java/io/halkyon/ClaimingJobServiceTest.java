package io.halkyon;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.halkyon.services.ClaimStatus;
import io.halkyon.services.ClaimingJobService;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClaimingJobServiceTest {

    @Inject
    ClaimingJobService job;

    @ConfigProperty(name = "servicebox.claiming-service.max-attempts")
    int maxAttempts;

    @Inject
    Scheduler scheduler;

    @Test
    public void testJobShouldRunWhenStartApplication(){
        // When we start the application, the claiming job is automatically triggered
        // Then:
        // - the claim "postgresql-team-dev" should have changed from status "pending" to "claimed"
        // - the claim "postgresql-team-test" should have changed from status "new" to "claimed"
        // - the claim "mysql-demo" should have changed from status "new" to "pending" because there is no service "mysql-7.5"
        // Both claims should have populated the relationship claim-service
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/postgresql-team-dev")
                .then()
                .statusCode(200)
                .body("status", is(ClaimStatus.CLAIMED.toString()))
                .body("service.name", containsString("PostgreSQL"));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/postgresql-team-test")
                .then()
                .statusCode(200)
                .body("status", is(ClaimStatus.CLAIMED.toString()))
                .body("service.name", containsString("PostgreSQL"));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/mysql-demo")
                .then()
                .statusCode(200)
                .body("status", is(ClaimStatus.PENDING.toString()))
                .body("attempts", is(1));
    }

    @Test
    public void testJobShouldMarkClaimAsErrorAfterMaxAttemptsExceeded(){
        pauseScheduler();
        // When we run the job once:
        // Then:
        // - the claim "mysql-demo" should increase the attempts to 2
        // When we run the job again:
        // Then:
        // - the claim "mysql-demo" should increase the attempts to 3 and status should have changed to "error".
        for (int attempt = 1; attempt < maxAttempts; attempt++) {
            job.execute();
            given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .get("/claims/name/mysql-demo")
                    .then()
                    .statusCode(200)
                    .body("status", is(ClaimStatus.PENDING.toString()))
                    .body("attempts", is(attempt + 1));
        }

        job.execute();
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/mysql-demo")
                .then()
                .statusCode(200)
                .body("status", is(ClaimStatus.ERROR.toString()))
                .body("attempts", is(maxAttempts));
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
