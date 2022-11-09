package io.halkyon;

import io.halkyon.model.Claim;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
public class ClaimsJPATest {

    public class ClaimRepository implements PanacheRepository<Claim> {
    }

    @Test
    public void testQueryByName() {
        // when
        List<Claim> claims = Claim.getClaims("mysql-demo","");

        // then
        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }

    @Test
    public void testQueryByServiceRequested() {
        // when
        List<Claim> claims = Claim.getClaims("","mariadb-7.5");

        // then
        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }

    @Test
    public void testUsingRepositoryAndQuery() {
        String name = "mysql-demo";
        String serviceRequested= "";
        ClaimRepository cr = new ClaimRepository();

        String query = "name = ?1 or servicerequested = ?2";
        List<Claim> claims = cr.list(query, name, serviceRequested);

        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }

    // Test is failing using like :-(
    @Test
    public void testUsingRepositoryAndQueryWithLike() {
        String name = "mysql";
        String serviceRequested= "";
        ClaimRepository cr = new ClaimRepository();

        String query = "name like ?1 or servicerequested = ?2";
        List<Claim> claims = cr.list(query, name, serviceRequested);

        MatcherAssert.assertThat(claims, Matchers.hasSize(0));
    }
}
