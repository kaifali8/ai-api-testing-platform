package com.kaif.apitestplatform.testgen;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ValueFactory {
    static final int MAX_LEN = 10_000;
    private static final Set<String> STRICT_FORMATS = Set.of("email","date","date-time","uuid");

    record Boundary(String label,Object value,boolean valid){}

    private ValueFactory(){}

    static Object validValue(FieldSpec f){
        if (!f.enumValues().isEmpty()) return coerce(f.type(),f.enumValues().get(0));
        String type = f.type() == null ? "string" : f.type();
        return switch (type){
            case "integer" -> validInteger(f);
            case "number" -> validNumber(f);
            case "boolean" -> Boolean.TRUE;
            case "array" -> new ArrayList<>(List.of("sample"));
            case "string" -> validString(f);
            default -> "sample";
        };
    }

    static Object wrongType(FieldSpec f){
        if (f.enumValues().isEmpty() || f.type()==null) return null;
        return switch (f.type()){
          case "integer","number","boolean" -> "invalid_" + f.type();
            default -> null;
        };
    }

    static Object invalidEnum(FieldSpec f){
        return f.enumValues().isEmpty() ? null : "INVALID_ENUM_VALUE";
    }

    static Object invalidFormat(FieldSpec f){
        if (f.format() == null || (f.type()!=null && !"string".equals(f.type()))) return null;
        return switch (f.format()){
            case "email" -> "not-an-email";
            case "date" -> "not-a-date";
            case "date-time" -> "not-a-datetime";
            case "uuid" -> "not-a-uuid";
            default -> null;
        };
    }

    static List<Boundary> boundaries(FieldSpec f){
        List<Boundary> out = new ArrayList<>();
        String type = f.type() == null ? "" : f.type();

        if (type.equals("integer") || type.equals("number")){
            BigDecimal step = type.equals("integer") ? BigDecimal.ONE : new BigDecimal("0.01");
            if (f.minimum() != null){
                out.add(new Boundary("minimum value", num(type,f.minimum()),true));
                out.add(new Boundary("below minimum",num(type,f.minimum().subtract(step)),false));
            }
            if (f.maximum() != null){
                out.add(new Boundary("maximum value",num(type,f.maximum()),true));
                out.add(new Boundary("above maximum",num(type,f.maximum().add(step)),false));
            }
        } else if (type.equals("string") && !isStrictFormat(f)){
            if (f.minLength() != null && f.minLength()<=MAX_LEN){
                out.add(new Boundary("minimum length","a".repeat(f.minLength()),true));
                if (f.minLength()>0){
                    out.add(new Boundary("below minimum length","a".repeat(f.minLength()-1),false));
                }
            }
            if (f.maxLength()!=null && f.maxLength() < MAX_LEN){
                out.add(new Boundary("maximum length","a".repeat(f.maxLength()),true));
                out.add(new Boundary("above maximum length","a".repeat(f.maxLength()+1),false));
            }
        }
        return out;
    }

    private static boolean isStrictFormat(FieldSpec f){
        return f.format()!=null && STRICT_FORMATS.contains(f.format());
    }

    private static Object num(String type, BigDecimal v){
        return "integer".equals(type) ? (Object) v.longValue() : (Object) v.doubleValue();
    }

    private static Object validInteger(FieldSpec f){
        if (f.minimum()!=null) return f.minimum().longValue();
        if (f.maximum()!=null) return Math.min(1L,f.maximum().longValue());
        return 1L;
    }

    private static Object validNumber(FieldSpec f){
        if (f.minimum()!=null) return f.minimum().doubleValue();
        if (f.maximum()!=null) return Math.min(1.5,f.maximum().doubleValue());
        return 1.5;
    }

    private static String validString(FieldSpec f){
        String fmt = f.format()==null ? "" : f.format();
        String base = switch (fmt){
            case "email" -> "test@example.com";
            case "date" -> "2024-01-15";
            case "date-time" -> "2024-01-15T10:30:00Z";
            case "uuid" -> "123e4567-e89b-123d-a435-4323412599";
            case "uri", "url" -> "https://example.com";
            default -> "sample";
        };
        if (f.minLength()!=null){
            int target = Math.min(f.minLength(),MAX_LEN);
            if (base.length()<target) base = base + "a".repeat(target - base.length());
        }
        if (f.maxLength()!=null && base.length() > f.maxLength()){
            base = base.substring(0,Math.max(0,f.maxLength()));
        }
        return base;
    }

    private static Object coerce(String type, String raw){
        try {
            if ("integer".equals(type)) return Long.parseLong(raw);
            if ("number".equals(type)) return Double.parseDouble(raw);
            if ("boolean".equals(type)) return Boolean.parseBoolean(raw);
        }catch (NumberFormatException ignored){

        }
        return raw;
    }
}
