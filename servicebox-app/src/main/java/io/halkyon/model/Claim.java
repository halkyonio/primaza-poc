package io.halkyon.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Claim extends PanacheEntity {

    public Long id;
    
	public String name;

    public String serviceVersion;

//    public Status status;

    public static Claim findByName(String name) {
        return find("name", name).firstResult();
    }
}
