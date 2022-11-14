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
        List<Claim> claims = Claim.getClaims("","mariadb-10.9");

        // then
        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }

    @Test
    public void testUsingRepositoryAndQuery() {
        String name = "mysql-demo";
        String serviceRequested= "";
        Claim claim = new Claim();

        String query = "name = ?1 or servicerequested = ?2";
        List<Claim> claims = claim.list(query, name, serviceRequested);

        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }

    @Test
    public void testUsingRepositoryAndQueryWithLike() {
        String name = "mysql%";
        String serviceRequested= "";
        Claim claimEntity = new Claim();

        String query = "name like ?1 or servicerequested = ?2";
        List<Claim> claims = claimEntity.list(query, name, serviceRequested);

        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }
}
