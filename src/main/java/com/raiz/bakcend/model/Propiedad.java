package com.raiz.bakcend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "propiedades")
public class Propiedad {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    // El campo "id" es la clave primaria de la tabla, y se define como un UUID
    // (Universally Unique Identifier)
    private UUID id;

    @Column(nullable = false, length = 150)
    // No puede ser nulo, y tiene un máximo de 150 caracteres
    private String titulo;

    @Column(columnDefinition = "TEXT")
    // No puede ser nulo, y tiene un máximo de 500 caracteres
    private String descripcion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String direccion;

    @Column(nullable = false, length = 100)
    // No puede ser nulo, y tiene un máximo de 100 caracteres
    private String ciudad;

    @Column(nullable = false, scale = 2, precision = 14)
    // No puede ser nulo, y tiene 2 decimales y un máximo de 14 dígitos en total
    private BigDecimal precio;

    @Column(name = "superficie_m2", nullable = false, scale = 2, precision = 10)
    // No puede ser nulo, y tiene 2 decimales y un máximo de 10 dígitos en total
    private BigDecimal superficieM2;

    @Column(nullable = false)
    private Integer habitaciones;

    @Column(nullable = false)
    private Integer banios;

    @Column(nullable = false, length = 100)
    private String estado;

    @Column(name = "creado_en", updatable = false, insertable = false)
    // Tipo de dato para fecha y hora con zona horaria
    // Este formato permite almacenar la fecha y hora exacta en que se creó la
    // propiedad, incluyendo la información de la zona horaria
    private OffsetDateTime creadoEn;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "tipo_id", nullable = false)
    private Integer tipoId;

    @Column(name = "agente_id", nullable = false)
    private UUID agenteId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "propiedad_badges",
        joinColumns = @JoinColumn(name = "propiedad_id")
    )
    @Column(name = "badge")
    private List<String> badges;

    @OneToMany(mappedBy = "propiedad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<ImagenPropiedad> imagenes;

    @Column(name = "operacion", length = 20)
    private String operacion;

    @Column(name = "moneda", length = 3)
    private String moneda = "USD";

}
