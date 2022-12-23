package io.halkyon.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

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
        return find("status=:status", Collections.singletonMap("status", ClaimStatus.BIND.toString())).list();
    }
}
