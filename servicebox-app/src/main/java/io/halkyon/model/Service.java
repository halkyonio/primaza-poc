package io.halkyon.model;

import static javax.persistence.CascadeType.ALL;

import java.util.Date;
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

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    public String name;
    public String version;
    public String type;
    public String endpoint;
    public String externalEndpoint;
    public String database;
    public String namespace;
    /**
     * Auto-populated property that is resolved using the Ingress resource from the Kubernetes Service.
     */
    public String externalIp;
    public Boolean available;
    @CreationTimestamp
    public Date created;
    @UpdateTimestamp
    public Date updated;

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

    public static Service findByNameAndVersion(String name, String version) {
        return find("name=?1 AND version=?2", name, version).firstResult();
    }

    public static List<Service> listAll() {
        return findAll(Sort.ascending("name")).list();
    }

    public static List<Service> findAvailableServices() {
        return Service.find("available=true AND cluster != null").list();
    }
}
