package com.raiz.bakcend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "propiedades")
public class Propiedad {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String direccion;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(nullable = false, scale = 2, precision = 14)
    private BigDecimal precio;

    @Column(name = "superficie_m2", nullable = false, scale = 2, precision = 10)
    private BigDecimal superficieM2;

    @Column(nullable = false)
    private Integer habitaciones;

    @Column(nullable = false)
    private Integer banios;

    @Column(nullable = false, length = 100)
    private String estado;

    @Column(name = "creado_en", updatable = false, insertable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "tipo_id", nullable = false)
    private Integer tipoId;

    @Column(name = "agente_id", nullable = false)
    private UUID agenteId;

    @JsonIgnore
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "propiedad_badges", joinColumns = @JoinColumn(name = "propiedad_id"))
    @Column(name = "badge")
    private List<String> badges;

    @OneToMany(mappedBy = "propiedad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ImagenPropiedad> imagenes;

    @Column(name = "operacion", length = 20)
    private String operacion;

    @Column(name = "moneda", length = 3)
    private String moneda = "USD";

    @Column(name = "zona", length = 100)
    private String zona;

}