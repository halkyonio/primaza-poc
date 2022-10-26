package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.service.ClaimStatus;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.util.List;

@Path("/claim")
public class ClaimResource {

    @GET
    public TemplateInstance claim() {
        return Templates.claimForm().data("tile","Claim form");
    }

}
