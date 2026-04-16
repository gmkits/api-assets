package com.github.gmkits.holiday.api.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ErrorResponse {

    int status;
    String message;
    LocalDateTime timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
