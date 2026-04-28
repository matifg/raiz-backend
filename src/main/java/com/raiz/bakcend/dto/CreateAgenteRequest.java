package com.raiz.bakcend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateAgenteRequest {

    private String nombre;
    private String email;
    private String password;
    private UUID inmobiliariaId;
}