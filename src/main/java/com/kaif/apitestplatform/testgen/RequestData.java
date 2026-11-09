package com.kaif.apitestplatform.testgen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestData {
    private Map<String,Object> pathParams = new LinkedHashMap<>();
    private Map<String, Object> queryParams = new LinkedHashMap<>();
    private Map<String,Object> headers = new LinkedHashMap<>();
    private Object body;
}
