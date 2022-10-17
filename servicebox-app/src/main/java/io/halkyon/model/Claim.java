package io.halkyon.model;

import java.util.List;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Claim extends PanacheEntity {
    
	public String name;

    public static List<Claim> findByName() {
        // return find("owner = ?1 ORDER BY id", user).list();
        return null;
    }
}
