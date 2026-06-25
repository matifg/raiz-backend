package com.raiz.bakcend.dto;

import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AgenteResponse {
    private UUID id;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;

    public static AgenteResponse from(Agente agente, Usuario usuario) {
        return new AgenteResponse(
                agente.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getTelefono(),
                usuario.getEmail());
    }
}
