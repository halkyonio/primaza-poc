package io.halkyon;

public class ClaimSpec {

    // Add Spec information here

    public String serviceRequested;
    public String description;
    public String status;
    public String owner;
    // public Date created;
    // public Date updated;
    // public String errorMessage;
    // public Integer attempts = 0;
    public String service;
    public Credential credential;
    public String application;
    public String url;
    public String type;

    public void setServiceRequested(String serviceRequested) {
        this.serviceRequested = serviceRequested;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setType(String type) {
        this.type = type;
    }
}
