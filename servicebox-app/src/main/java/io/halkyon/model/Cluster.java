package io.halkyon.model;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
public class Cluster extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @NotBlank(message = "Name must not be empty")
    public String name;
    @NotBlank(message = "URL must not be empty")
    public String url;
    @NotBlank(message = "Environment must not be empty")
    public String environment;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    public String kubeConfig;
    public Date created;

    @JsonIgnore
    @OneToMany(mappedBy = "cluster", fetch = FetchType.LAZY)
    public Set<Service> services = new HashSet<>();

    public static Cluster findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Cluster> listAll() {
        return findAll(Sort.ascending("name")).list();
    }


}
