package io.halkyon.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Parameters;

public class FilterableQueryBuilder {
    private final Map<String, Object> filter = new HashMap<>();
    private final Parameters parameters = new Parameters();
    private final List<String> query = new LinkedList<>();

    public void equals(String param, Object value) {
        query.add(param + "=:" + param);
        parameters.and(param, value);
        filter.put(param, value);
    }

    public void containsIgnoreCase(String param, String value) {
        query.add("LOWER(" + param + ") like :" + param);
        parameters.and(param, "%" + value.toLowerCase(Locale.ROOT) + "%");
        filter.put(param, value);
    }

    public void startsWith(String param, String value) {
        query.add(param + " like :" + param);
        parameters.and(param, value + "%");
        filter.put(param, value);
    }

    public String build() {
        return query.stream().collect(Collectors.joining(" AND "));
    }

    public Parameters getParameters() {
        return parameters;
    }

    public Map<String, Object> getFilterAsMap() {
        return Collections.unmodifiableMap(filter);
    }
}
