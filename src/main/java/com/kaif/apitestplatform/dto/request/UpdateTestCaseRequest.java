package com.kaif.apitestplatform.dto.request;

import com.kaif.apitestplatform.testgen.RequestData;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTestCaseRequest {

    @Size(max = 300, message = "Name must be at most 300 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private RequestData requestData;

    @Size(max = 50,message = "Too many status codes")
    @Pattern(regexp = "^[1-5]\\d\\d(,[1-5]\\d\\d)*$")
    private String expectedStatuses;

    private Boolean includeAuth;

    private Boolean enabled;
}
