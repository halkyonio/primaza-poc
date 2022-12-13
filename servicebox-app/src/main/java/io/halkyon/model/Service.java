package io.halkyon.model;

import static javax.persistence.CascadeType.ALL;

import java.sql.Date;
import java.util.List;
import java.util.regex.Pattern;

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
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @NotBlank(message = "Type must not be empty")
    @FormParam
    public String type;

    /* in form of tcp:8080*/
    @NotBlank(message = "Service endpoint must not be empty")
    @javax.validation.constraints.Pattern(regexp = "\\w+:\\d+", message = "Wrong format in service endpoint. It must be 'protocol:port'")
    @FormParam
    public String endpoint;

    @FormParam
    public String externalEndpoint;

    @FormParam
    public String database;

    public String namespace;
    public Boolean available;
    public Date created;

    @JsonManagedReference
    @OneToMany(mappedBy = "service", fetch = FetchType.EAGER)
    public List<Credential> credentials;

    @ManyToOne(cascade = ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "cluster_id")
    public Cluster cluster;

    @JsonIgnore
    @Transient
    public String getProtocol() {
        return splitEndpoint()[0];
    }

    @JsonIgnore
    @Transient
    public String getPort() {
        return splitEndpoint()[1];
    }

    private String[] splitEndpoint() {
        return endpoint.split(Pattern.quote(":"));
    }

    public static Service findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Service> listAll() {
        return findAll(Sort.ascending("name")).list();
    }

    public static List<Service> findAvailableServices() {
        return Service.find("available=true AND cluster != null").list();
    }
}
