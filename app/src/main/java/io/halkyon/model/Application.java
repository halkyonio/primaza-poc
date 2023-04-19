package io.halkyon.model;

import static jakarta.persistence.CascadeType.PERSIST;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Application extends PanacheEntity {
    public String name;
    public String namespace;
    public String image;
    public String ingress;

    @ManyToOne(cascade = PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "cluster_id")
    public Cluster cluster;

    @OneToOne(mappedBy = "application")
    public Claim claim;

    public static Application findByName(String name) {
        return find("name", name).firstResult();
    }

}
