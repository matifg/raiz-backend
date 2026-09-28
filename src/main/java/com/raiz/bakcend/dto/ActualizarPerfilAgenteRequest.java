package com.raiz.bakcend.dto;

import lombok.Data;

@Data
public class ActualizarPerfilAgenteRequest {
    private String nombre;
    private String apellido;
    private String telefono;
    /** Nombre de la inmobiliaria (opcional). null = no cambia; blank = limpia. */
    private String inmobiliaria;
    /** URL pública del logo (opcional). Si ya suben a storage externo. null = no cambia. */
    private String logoUrl;
    /** URL pública del cover/portada (opcional). null = no cambia; "" = limpia. */
    private String coverUrl;
}
