package io.halkyon.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Credential extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    @NotNull
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    public Service service;

    public String type;

    public String vaultKvPath;

    public String username;

    public String password;

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CredentialParameter> params = new ArrayList<>();
    @CreationTimestamp
    public Date created;
    @UpdateTimestamp
    public Date updated;

    public static Credential findByName(String name) {
        return find("name", name).firstResult();
    }

}
