package io.halkyon.model;

import static javax.persistence.CascadeType.ALL;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.List;
import java.util.Set;

@Entity
public class Application extends PanacheEntity {
    public String name;
    public String namespace;
    public String image;
    public String url;

    @ManyToOne(cascade = ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "cluster_id")
    public Cluster cluster;

    @JsonManagedReference
    @OneToOne(mappedBy = "application")
    public Claim claim;
}
