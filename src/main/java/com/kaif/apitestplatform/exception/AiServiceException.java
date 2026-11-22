package com.kaif.apitestplatform.exception;

import org.springframework.http.HttpStatus;

public class AiServiceException extends RuntimeException{
    private final HttpStatus status;


    public AiServiceException(String message,HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
