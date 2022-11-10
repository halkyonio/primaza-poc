package io.halkyon.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

import javax.persistence.*;

import java.sql.Date;
import java.util.List;

@Entity
@Table(indexes = @Index(columnList = "name,version", unique = true))
public class Service extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @FormParam
    public String name;

    @FormParam
    public String version;

    /* in form of tcp:8080*/
    @FormParam
    public String endpoint;

    @FormParam
    public Boolean deployed;

    @FormParam
    public Date created;

    public static Service findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Service> listAll() {
        return findAll(Sort.ascending("name")).list();
    }


}
