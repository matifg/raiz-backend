package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.ImagenPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ImagenPropiedadRepository extends JpaRepository<ImagenPropiedad, UUID> {

    List<ImagenPropiedad> findByPropiedadId(UUID propiedadId);

    @Query("""
            SELECT i FROM ImagenPropiedad i
            JOIN FETCH i.propiedad p
            WHERE p.id IN :propiedadIds
            ORDER BY p.id, i.orden ASC NULLS LAST, i.creadoEn ASC NULLS LAST
            """)
    List<ImagenPropiedad> findByPropiedad_IdInOrderByOrdenAscCreadoEnAsc(
            @Param("propiedadIds") Collection<UUID> propiedadIds);

}
