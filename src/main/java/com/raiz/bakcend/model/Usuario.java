package com.raiz.bakcend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 50)
    private String telefono;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String rol; // Ej: "AGENTE", "ADMIN", "CLIENTE"

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    @Column(name = "membresia_activa")
    private Boolean membresiaActiva = false;
}
