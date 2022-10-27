package io.halkyon.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.util.List;

@Entity
public class Service extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "svcSeq", sequenceName = "svc_id_seq", allocationSize = 1, initialValue = 5)
    @GeneratedValue(generator = "svcSeq")
    Long id;

    public String name;

    public String version;

    /* in form of tcp:8080*/
    public String endpoint;

    public Boolean deployed;

    public static Service findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Service> listAll() {
        return findAll(Sort.ascending("name")).list();
    }


}
