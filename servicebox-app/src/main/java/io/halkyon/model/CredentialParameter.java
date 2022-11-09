package io.halkyon.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class CredentialParameter extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "creParamSeq", sequenceName = "cre_param_id_seq", allocationSize = 1, initialValue = 5)
    @GeneratedValue(generator = "creParamSeq")
    public Long id;
    public String paramName;
    public String paramValue;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "credential_id", nullable = false)
    public Credential credential;
}
