package com.kaif.apitestplatform.dto.response;

import com.kaif.apitestplatform.entity.ApiParameter;

import java.math.BigDecimal;

public record ApiParameterResponse(
        String name, String location, boolean required,
        String type, String format, BigDecimal minimum,
        BigDecimal maximum,Integer minLength,Integer maxLength,
        String pattern, String enumValues) {
    public static ApiParameterResponse from(ApiParameter p){
        return new ApiParameterResponse(
                p.getName(),p.getLocation(),p.isRequired(),
                p.getType(),p.getFormat(),
                p.getMinimum(),p.getMaximum(),
                p.getMinLength(), p.getMaxLength(),
                p.getPattern(),p.getEnumValues()
        );
    }
}
