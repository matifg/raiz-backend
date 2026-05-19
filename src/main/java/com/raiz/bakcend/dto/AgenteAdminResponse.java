package com.raiz.bakcend.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AgenteAdminResponse {
    private UUID usuarioId;
    private String nombre;
    private String apellido;
    private String email;
    private Boolean membresiaActiva;
    private Integer cantidadPropiedades;
}
