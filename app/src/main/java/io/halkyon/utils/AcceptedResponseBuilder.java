package io.halkyon.utils;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.core.Response;

public final class AcceptedResponseBuilder {

    private static final String LOCATION_HEADER = "Location";
    private static final String ERROR_MESSAGE_TEMPLATE = "<div class=\"alert alert-danger\"><strong>Error! </strong>%s</div>";
    private static final String SUCCESS_MESSAGE_TEMPLATE = "<div class=\"alert alert-success\">Created successfully for id: %s</div>";

    private static final String SUCCESS_UPDATE_MESSAGE_TEMPLATE = "<div class=\"alert alert-success\">Updated successfully for id: %s</div>";

    private final StringBuffer response = new StringBuffer();
    private final String location;
    private Response.Status status;

    private AcceptedResponseBuilder(String location) {
        this.location = location;
    }

    public <T> AcceptedResponseBuilder withErrors(Set<ConstraintViolation<T>> errors) {
        for (ConstraintViolation<?> error : errors) {
            response.append(String.format(ERROR_MESSAGE_TEMPLATE, error.getMessage()));
        }
        status = Response.Status.BAD_REQUEST;

        return this;
    }

    public AcceptedResponseBuilder withUpdateSuccessMessage(Long id) {
        response.append(String.format(SUCCESS_UPDATE_MESSAGE_TEMPLATE, id));
        status = Response.Status.CREATED;
        return this;
    }

    public AcceptedResponseBuilder withSuccessMessage(Long id) {
        response.append(String.format(SUCCESS_MESSAGE_TEMPLATE, id));
        status = Response.Status.CREATED;
        return this;
    }

    public Response build() {
        return Response.status(status).entity(StandardCharsets.UTF_8.encode(response.toString()).array())
                .header(LOCATION_HEADER, location).build();
    }

    public static AcceptedResponseBuilder withLocation(String location) {
        return new AcceptedResponseBuilder(location);
    }

}
