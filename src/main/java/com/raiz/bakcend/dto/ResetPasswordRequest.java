package com.raiz.bakcend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ResetPasswordRequest {
    private UUID token;
    private String password;
}
