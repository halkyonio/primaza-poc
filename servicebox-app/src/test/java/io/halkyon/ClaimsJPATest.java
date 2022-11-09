package io.halkyon;

import io.halkyon.model.Claim;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.transaction.Transactional;
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
        List<Claim> claims = Claim.getClaims("","mariadb-7.5");

        // then
        MatcherAssert.assertThat(claims, Matchers.hasSize(1));
    }
}
