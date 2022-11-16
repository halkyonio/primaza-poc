package io.halkyon.model;

import static javax.persistence.CascadeType.ALL;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.sql.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    public Boolean deployed;

    @JsonManagedReference
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    public List<Credential> credentials;

    @ManyToOne(cascade = ALL)
    @JoinColumn(name = "cluster_id")
    public Cluster cluster;

    @FormParam
    public Date created;

    public static Service findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Service> listAll() {
        return findAll(Sort.ascending("name")).list();
    }


}
