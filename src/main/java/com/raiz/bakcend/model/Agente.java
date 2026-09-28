package com.raiz.bakcend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "agentes")
public class Agente {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "inmobiliaria_id")
    private UUID inmobiliariaId;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "creado_en")
    private OffsetDateTime creadoEn;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    /** Nombre comercial / inmobiliaria (opcional). Columna histórica: nombre_inmobiliaria. */
    @Column(name = "nombre_inmobiliaria")
    private String inmobiliaria;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

}
