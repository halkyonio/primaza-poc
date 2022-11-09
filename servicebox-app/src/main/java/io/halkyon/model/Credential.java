package io.halkyon.model;

import java.sql.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Credential extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "creSeq", sequenceName = "cre_id_seq", allocationSize = 1, initialValue = 5)
    @GeneratedValue(generator = "creSeq")
    public Long id;

    public String name;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    public Service service;

    public String username;

    public String password;

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL)
    public List<CredentialParameter> params;
    public Date created;

    public static Credential findByName(String name) {
        return find("name", name).firstResult();
    }

}
