package com.raiz.bakcend.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String role;
    private Boolean membresiaActiva;
    private String nombre;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, String role, Boolean membresiaActiva, String nombre) {
        this.token = token;
        this.role = role;
        this.membresiaActiva = membresiaActiva;
        this.nombre = nombre;
    }

}
