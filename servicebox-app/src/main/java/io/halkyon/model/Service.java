package io.halkyon.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;

@Entity
public class Service extends PanacheEntity {

    public String name;

    public String version;

    /* in form of tcp:8080*/
    public String endpoint;

    public Boolean deployed;


}
