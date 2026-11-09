package com.kaif.apitestplatform.testgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaif.apitestplatform.entity.ApiDefinition;
import com.kaif.apitestplatform.entity.ApiParameter;
import com.kaif.apitestplatform.enums.TestCategory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RuleBasedTestGenerator {

    private static final int MAX_DEPTH=5;
    private static final String VALIDATION_ERRORS = "400,422";
    private static final String PATH_ERRORS = "400,404,422";
    private static final String AUTH_ERRORS = "401,403";
    private static final String DEFAULT_SUCCESS = "200,201,202,204";

    private final ObjectMapper mapper = new ObjectMapper();

    private enum Loc { PATH, QUERY, HEADER, BODY}

    private record Target (Loc loc, String name,FieldSpec spec, boolean required){
        String label(){
            return switch (loc){
                case PATH -> "path parameter '" + name + "'";
                case QUERY -> "query parameter '" + name + "'";
                case HEADER -> "header '" + name + "'";
                case BODY -> "body field '" + name + "'";
            };
        }
    }

    public List<GeneratedCase> generate(ApiDefinition api){
        List<Target> targets=new ArrayList<>();
        RequestData base = new RequestData();
        // 1. Baseline from parameters
        for (ApiParameter p: api.getParameters()){
            Loc loc = toLoc(p.getLocation());
            if (loc == null) continue;
            FieldSpec spec=FieldSpec.fromParameter(p);
            targets.add(new Target(loc,p.getName(),spec,p.isRequired() || loc == Loc.PATH));
            setValue(base,loc,p.getName(),ValueFactory.validValue(spec));
        }
        // 2. Baseline from request body
        JsonNode bodySchema  = pickComposed(readTree(api.getRequestBodySchema()));
        if (bodySchema != null){
            base.setBody(sampledFromSchema(bodySchema,0));
            addBodyTargets(bodySchema,targets);
        }
        // 3. Mutations (LinkedHashMap + Fingerprint key = automatic de-duplication)
        Map<String,GeneratedCase> out = new LinkedHashMap<>();
        String ok = successStatuses(api);
        boolean auth = api.isRequiresAuth();

        add(out,"Valid request succeeds", "All parameters and body fields hold valid values",
                TestCategory.POSITVE,base,ok,auth);

        for (Target t:targets){
            String bad = t.loc() == Loc.PATH ? PATH_ERRORS : VALIDATION_ERRORS;
            if (t.required() && t.loc()!=Loc.PATH){
                RequestData d = copy(base);
                removeValue(d,t.loc(),t.name());
                add(out,"Missing required "+ t.label(),
                        "Omits a required input; API should reject the request",
                        TestCategory.NEGATIVE,d,bad,auth);
            }
            Object wrong = ValueFactory.wrongType(t.spec());

            if (wrong!=null){
                RequestData d = copy(base);
                setValue(d,t.loc,t.name,wrong);
                add(out,"Wrong data type for "+t.label(),
                        "Sends a value that does not match the declared type",
                        TestCategory.NEGATIVE,d,bad,auth);
            }

            for (ValueFactory.Boundary b: ValueFactory.boundaries(t.spec())){
                RequestData d = copy(base);
                setValue(d,t.loc,t.name,wrong);
                add(out, "Boundary: " + b.label() + " for " + t.label(),
                        b.valid() ? "Edge of the valid range; should be accepted"
                                : "Just outside the valid range; should be rejected",
                        TestCategory.BOUNDARY, d, b.valid() ? ok : bad, auth);
            }

            Object badEnum = ValueFactory.invalidEnum(t.spec());
            if (badEnum != null){
                RequestData d = copy(base);
                setValue(d,t.loc,t.name(),badEnum);
                add(out,"Invalid enum value for "+t.label(),
                        "Sends a value outside the allowed list",
                        TestCategory.VALIDATION,d,bad,auth);
            }

            Object badFormat = ValueFactory.invalidFormat(t.spec());
            if (badFormat != null){
                RequestData d = copy(base);
                setValue(d,t.loc(),t.name(),badFormat);
                add(out,"Invalid format for "+t.label(),
                        "Sends a malformed "+t.spec().format()+" value",
                        TestCategory.VALIDATION,d,bad,auth);
            }
        }
        //4. Authentication
        if (auth){
            add(out,"Request without credentials is rejected",
                    "Endpoint requires authentication; calling it anonymously must fail",
                    TestCategory.AUTHENTICATION,copy(base), AUTH_ERRORS,false);
        }
        return new ArrayList<>(out.values());
    }

    // ---- Body Handling -----

    private void addBodyTargets(JsonNode schema, List<Target> targets){
        JsonNode props =  schema.get("properties");
        if (props==null || !props.isObject()) return;
        Set<String> required = new HashSet<>();
        schema.path("required").forEach(n-> required.add(n.asText()));
        props.fields().forEachRemaining(
                e->{
                    if (e.getValue().path("readOnly").asBoolean(false)) return;
                    targets.add(new Target(Loc.BODY,e.getKey(),
                            FieldSpec.fromSchema(e.getValue()),required.contains(e.getKey())));
                }
        );
    }

    private Object sampledFromSchema(JsonNode schema, int depth){
        schema = pickComposed(schema);
        if (schema == null || depth > MAX_DEPTH) return null;
        FieldSpec spec = FieldSpec.fromSchema(schema);

        if ("object".equals(spec.type())){
            Map<String,Object> obj = new LinkedHashMap<>();
            JsonNode props = schema.get("properties");
            if (props!= null && props.isObject() ){
                props.fields().forEachRemaining(e -> {
                    if (e.getValue().path("readOnly").asBoolean(false)) return;
                    obj.put(e.getKey(),sampledFromSchema(e.getValue(),depth+ 1));
                });
            }
            return obj;
        }
        if ("array".equals(spec.type())){
            List<Object> list = new ArrayList<>();
            JsonNode items = schema.get("items");
            if (items != null){
                Object item = sampledFromSchema(items,depth+1);
                if (item != null) list.add(item);
            }
            return list;
        }
        return ValueFactory.validValue(spec);
    }

    private JsonNode pickComposed(JsonNode s){
        if (s==null) return null;
        for (String k : List.of("oneOf","anyOf")){
            JsonNode arr = s.get(k);
            if (!s.has("type") && arr != null && arr.isArray() && arr.size() > 0) return arr.get(0);

        }
        return s;
    }

    // --------  small helpers ---------

    private Loc toLoc(String location){
        if (location == null) return null;
        return switch (location.toLowerCase()){
            case "path" -> Loc.PATH;
            case "query" -> Loc.QUERY;
            case "header" -> Loc.HEADER;
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private void setValue(RequestData d, Loc loc, String name, Object value){
        switch (loc){
            case PATH -> d.getPathParams().put(name,value);
            case QUERY -> d.getQueryParams().put(name,value);
            case HEADER -> d.getHeaders().put(name,value);
            case BODY -> {
                if (d.getBody() instanceof Map<?,?>m) ((Map<String,Object>)m).put(name,value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeValue(RequestData d, Loc loc, String name){
        switch (loc){
            case PATH -> d.getPathParams().remove(name);
            case QUERY -> d.getQueryParams().remove(name);
            case HEADER -> d.getHeaders().remove(name);
            case BODY -> {
                if (d.getBody() instanceof Map<?, ?> m) ((Map<String, Object>) m).remove(name);
            }
        }
    }

    private RequestData copy(RequestData d){
        return mapper.convertValue(d,RequestData.class);
    }

    private String successStatuses(ApiDefinition api){
        JsonNode node = readTree(api.getResponseSchemas());
        if (node == null) return DEFAULT_SUCCESS;
        List<String> codes = new ArrayList<>();
        node.fieldNames().forEachRemaining(c -> {
            if (c.matches("2\\d\\d")) codes.add(c);
        });
        return codes.isEmpty() ? DEFAULT_SUCCESS : String.join(",",codes);
    }

    private JsonNode readTree(String json){
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        }catch (JsonProcessingException e){
            return null;
        }
    }

    private void add(Map<String,GeneratedCase> out, String name,
                     String desc, TestCategory cat, RequestData data,String expected,
                     boolean includeAuth){
        String json;
        try {
            json = mapper.writeValueAsString(data);
        }catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize generated request",e);
        }
        out.putIfAbsent(json + "|" + expected + "|" + includeAuth,
                new GeneratedCase(name,desc,cat,json,expected,includeAuth));
    }
}

