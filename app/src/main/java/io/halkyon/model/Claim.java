package io.halkyon.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.halkyon.services.ClaimStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
public class Claim extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    public String serviceRequested;
    public String description;
    public String status;
    public String owner;
    @CreationTimestamp
    public Date created;
    @UpdateTimestamp
    public Date updated;
    public String errorMessage;
    public Integer attempts = 0;
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    public Service service;
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "credential_id", referencedColumnName = "id")
    public Credential credential;
    @JsonIgnore
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "application_id", referencedColumnName = "id")
    public Application application;
    public String url;
    public String type;

    public static Claim findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Claim> listAll() {
        return findAll(Sort.ascending("name")).list();
    }

    public static List<Claim> listAvailable() {
        // TODO: To be reviewed to support to display claims when status is pending or bindable
        // find("status in :statuses", Collections.singletonMap("statuses",
        // Arrays.asList(ClaimStatus.PENDING.toString(), ClaimStatus.BINDABLE.toString()))).list();
        return find("status=:status", Collections.singletonMap("status", ClaimStatus.BINDABLE.toString())).list();
    }
}
