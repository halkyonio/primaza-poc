package io.halkyon.model;

import static jakarta.persistence.CascadeType.ALL;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import io.halkyon.utils.StringUtils;
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
    public String namespace;
    /**
     * The external endpoint is provided by: - Either the user. In this scenario, the service is considered a standalone
     * service and will not be discovered in any cluster. - Otherwise, auto-populated property that is resolved using
     * the Ingress resource from the Kubernetes Service.
     */
    public String externalEndpoint;
    public Boolean available = false;
    public Boolean installable;
    public String helmRepo;
    public String helmChart;
    public String helmChartVersion;
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

    /**
     * @return true if the external endpoint was provided by the user and hence cluster is null.
     */
    @JsonIgnore
    @Transient
    public boolean isStandalone() {
        return StringUtils.isNotEmpty(externalEndpoint) && cluster == null;
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
        // TODO. This code should be reviewed as currently we check if a Service
        // part of the catalog as the property available = true
        // instead of checking if a service is running within the cluster(s).
        // This service must check using the cache, the available services
        // old code -->
        // return Service.findAll(Sort.ascending("name")).list();
        return Service.find("available=true").list();
    }
}
