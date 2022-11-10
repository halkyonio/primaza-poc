package io.halkyon.model;

import java.sql.Date;
import java.util.List;

import javax.persistence.*;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
public class Cluster extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @FormParam
    public String name;

    @FormParam
    public String url;

    @FormParam
    public String environment;

    @FormParam
    public String kubeConfig;

    @FormParam
    public Date created;

    public static Cluster findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Cluster> listAll() {
        return findAll(Sort.ascending("name")).list();
    }


}
