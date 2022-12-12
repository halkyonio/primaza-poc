package io.halkyon.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import io.halkyon.services.ClaimStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;

@Entity
public class Claim extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
	public String name;
    public String serviceRequested;
    public String description;
    public String status;
    public String owner;
    @CreationTimestamp
    public Date created;
    @UpdateTimestamp
    public Date updated;
    public String errorMessage;
    // TODO: To be discussed and adapted if needed. This is not because we will delete a Claim that the service bound should be deleted and its credential
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    public Service service;
    public Integer attempts = 0;

    // Id of the application which is bound to a claim
    public Long applicationId;

    @OneToOne
    public Credential credential;
    public String url;
    public String type;
    public String database;

    public static Claim findByName(String name) {
        return find("name", name).firstResult();
    }

    public static List<Claim> listAll() {
        return findAll(Sort.ascending("name")).list();
    }

    public static List<Claim> listAvailable() {
        return find("status=:status", Collections.singletonMap("status", ClaimStatus.BIND.toString())).list();
    }

    public static List<Claim> getClaims(String name, String serviceRequested) {
        Map<String, Object> parameters = new HashMap<>();
        addIfNotNull(parameters, "name", name );
        addIfNotNull(parameters, "servicerequested", serviceRequested);

        if ( parameters.isEmpty() ) {
            return listAll();
        }

        String query = parameters.entrySet().stream()
                .map( entry -> "LOWER(" + entry.getKey() + ") " + "like :" + entry.getKey() )
                .collect( Collectors.joining(" AND ") );

        // TODO: To be reviewed in order to generate the query with multiple parameters where we could use like or not, etc
        // String query = "name like ?1 or servicerequested = ?2";
        return Claim.list(query, parameters);
    }

    private static void addIfNotNull(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, "%" + value.toLowerCase(Locale.ROOT) + "%");
        }
    }
}
