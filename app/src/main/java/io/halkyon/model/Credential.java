package io.halkyon.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
