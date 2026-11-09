package com.kaif.apitestplatform.testgen;

import com.kaif.apitestplatform.entity.ApiParameter;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record FieldSpec(
        String type, String format, List<String> enumValues,
        BigDecimal minimum, BigDecimal maximum,
        Integer minLength, Integer maxLength
) {
    public static FieldSpec fromParameter(ApiParameter p){
        List<String> values=(p.getEnumValues() == null || p.getEnumValues().isBlank())
                ? List.of()
                : Arrays.asList(p.getEnumValues().split(","));
        return new FieldSpec(p.getType(),p.getFormat(),values,
                p.getMinimum(),p.getMaximum(),p.getMinLength(),p.getMaxLength());
    }

    public static FieldSpec fromSchema(JsonNode n){
        List<String> values = new ArrayList<>();
        n.path("enum").forEach(v -> values.add(v.asText()));
        return new FieldSpec(inferType(n), textOrNull(n, "format"),values,
                decimalOrNull(n,"minimum"),decimalOrNull(n,"maximum"),
                intOrNull(n,"minLength"),intOrNull(n,"maxLength"));
    }

    private static String inferType(JsonNode n){
        if (n.hasNonNull("type") && n.get("type").isTextual()) return n.get("type").asText();
        JsonNode types=n.get("types");
        if (types != null && types.isArray() && !types.isEmpty()) return types.get(0).asText();
        if (n.has("properties")) return  "object";
        if (n.has("items")) return "array";
        return null;
    }

    private static String textOrNull(JsonNode n,String k){
        return n.hasNonNull(k)? n.get(k).asText():null;
    }

    private static BigDecimal decimalOrNull(JsonNode n,String k){
        return n.hasNonNull(k) && n.get(k).isNumber() ? n.get(k).decimalValue():null;
    }

    private static Integer intOrNull(JsonNode n,String k){
        return n.hasNonNull(k) ? n.get(k).asInt():null;
    }
}
