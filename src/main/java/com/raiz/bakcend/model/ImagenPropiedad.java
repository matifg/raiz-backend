package com.raiz.bakcend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.UUID;

@Data
@Entity
@Table(name = "imagenes_propiedad")
public class ImagenPropiedad {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String url;

    @ManyToOne
    @JoinColumn(name = "propiedad_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Propiedad propiedad;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "creado_en")
    private java.time.OffsetDateTime creadoEn;
}