package io.halkyon.model;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.halkyon.services.ClusterStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
public class Cluster extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    public String url;
    public String environment;
    public ClusterStatus status;
    public String errorMessage;
    /**
     * Namespace where to search services and applications.
     */
    public String namespace;
    /**
     * Internal namespaces which should be excluded from the services looking about the services or applications
     * available Separate the namespaces within the string using a ";" character Example: default;kube-system;ingress
     */
    public String excludedNamespaces;

    public String token;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    public String kubeConfig;
    @CreationTimestamp
    public Date created;
    @UpdateTimestamp
    public Date updated;

    @JsonIgnore
    @OneToMany(mappedBy = "cluster", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    public Set<Service> services = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "cluster", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    public Set<Application> applications = new HashSet<>();

    public Application getApplicationByNameAndNamespace(String appName, String appNamespace) {
        return applications.stream()
                .filter(a -> Objects.equals(a.name, appName) && Objects.equals(a.namespace, appNamespace)).findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Cluster))
            return false;
        Cluster cluster = (Cluster) o;
        return Objects.equals(id, cluster.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Cluster findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Cluster> listAll() {
        return findAll(Sort.ascending("name")).list();
    }
}
