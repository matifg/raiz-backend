package com.raiz.bakcend.dto;

import lombok.Data;

@Data
public class ActualizarPerfilAgenteRequest {
    private String nombre;
    private String apellido;
    private String telefono;
}
