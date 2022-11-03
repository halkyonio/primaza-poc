package io.halkyon.model;

import java.sql.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
public class Claim extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "claimSeq", sequenceName = "claim_id_seq", allocationSize = 1, initialValue = 7)
    @GeneratedValue(generator = "claimSeq")
    public Long id;

    @FormParam
	public String name;
    @FormParam
    public String serviceRequested;

    public Claim() {}
    public Claim(String name, String serviceRequested, String description, String status, String owner, Date created) {
        this.name = name;
        this.serviceRequested = serviceRequested;
        this.description = description;
        this.status = status;
        this.owner = owner;
        this.created = created;
    }

    @FormParam
    public String description;
    @FormParam
    public String status;
    @FormParam
    public String owner;
    @FormParam
    public Date created;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    public Service service;
    public Integer attempts = 0;

    public static Claim findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Claim> listAll() {
        return findAll(Sort.ascending("name")).list();
    }
}
