package io.halkyon;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class ClaimsEndpointTest {

    @Test
    public void testClaimEntity() {
        performTest("/claims");
    }

    private void performTest(String path) {
        //List all
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(
                        containsString("mysql-demo"),
                        containsString("postgresql-team-dev"),
                        containsString("postgresql-team-test"),
                        containsString("mariadb-demo"),
                        containsString("postgresql-13"));

        //Delete the Cherry:
        given()
                .when().delete(path + "/1")
                .then()
                .statusCode(204);

        //List all, cherry should be missing now:
        given()
                .when().get(path)
                .then()
                .statusCode(200)
                .body(
                        not(containsString("mysql-demo")),
                                containsString("postgresql-team-dev"),
                                containsString("postgresql-team-test"),
                                containsString("mariadb-demo"),
                                containsString("postgresql-13"));
    }

}
