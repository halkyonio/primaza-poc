package io.halkyon.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Parameters;

public class FilterableQueryBuilder {
    private final Map<String, Object> filter = new HashMap<>();
    private final Parameters parameters = new Parameters();
    private final List<String> query = new LinkedList<>();

    public void equals(String field, Object value) {
        String param = toParamName(field);
        query.add(field + "=:" + param);
        parameters.and(param, value);
        filter.put(field, value);
    }

    public void containsIgnoreCase(String field, String value) {
        String param = toParamName(field);
        query.add("LOWER(" + field + ") like :" + param);
        parameters.and(param, "%" + value.toLowerCase(Locale.ROOT) + "%");
        filter.put(field, value);
    }

    public void startsWith(String field, String value) {
        String param = toParamName(field);
        query.add(field + " like :" + param);
        parameters.and(param, value + "%");
        filter.put(field, value);
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

    private String toParamName(String param) {
        // remove dots
        return param.replaceAll(Pattern.quote("."), "");
    }
}
