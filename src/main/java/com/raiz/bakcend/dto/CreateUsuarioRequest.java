package com.raiz.bakcend.dto;

import lombok.Data;

@Data
public class CreateUsuarioRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String password;
    private String rol; // Ej: "AGENTE", "ADMIN", "CLIENTE"
}
