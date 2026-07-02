package com.raiz.bakcend.dto.brevo;

import lombok.Data;

@Data
public class BrevoErrorResponse {
    private String message;
    private String code;
}
