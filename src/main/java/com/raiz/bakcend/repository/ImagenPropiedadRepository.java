package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.ImagenPropiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
public interface ImagenPropiedadRepository extends JpaRepository<ImagenPropiedad, UUID> {

    List<ImagenPropiedad> findByPropiedadId(UUID propiedadId);

}
