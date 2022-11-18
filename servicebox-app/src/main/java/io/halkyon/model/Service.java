package io.halkyon.model;

import static javax.persistence.CascadeType.ALL;

import java.sql.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
@Table(indexes = @Index(columnList = "name,version", unique = true))
public class Service extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank(message = "Name must not be empty")
    @FormParam
    public String name;

    @NotBlank(message = "Version must not be empty")
    @FormParam
    public String version;

    /* in form of tcp:8080*/
    @NotBlank(message = "Service endpoint must not be empty")
    @FormParam
    public String endpoint;

    public Boolean deployed;

    @JsonManagedReference
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    public List<Credential> credentials;

    @ManyToOne(cascade = ALL, fetch = FetchType.EAGER)
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

    public static List<Service> findDeployedServices() {
        return Service.find("deployed=true AND cluster != null").list();
    }
}
