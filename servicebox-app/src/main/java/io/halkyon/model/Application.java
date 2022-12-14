package io.halkyon.model;

import static javax.persistence.CascadeType.ALL;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Application extends PanacheEntity {
    public String name;
    public String namespace;
    public String image;

    @ManyToOne(cascade = ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "cluster_id")
    public Cluster cluster;

    @OneToOne(mappedBy = "application")
    public Claim claim;

    public static Application findByName(String name) {
        return find("name", name).firstResult();
    }

}
