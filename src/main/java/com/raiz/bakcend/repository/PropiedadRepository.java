package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PropiedadOrigen;
import com.raiz.bakcend.model.PublicacionEstado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropiedadRepository extends JpaRepository<Propiedad, UUID> {

    boolean existsByOrigenAndExternalId(PropiedadOrigen origen, String externalId);

    Optional<Propiedad> findByOrigenAndExternalId(PropiedadOrigen origen, String externalId);

    boolean existsByWordpressPageId(String wordpressPageId);

    /**
     * Lock exclusivo de fila para serializar publicación WordPress de la misma propiedad.
     * Debe usarse dentro de {@code @Transactional}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Propiedad p WHERE p.id = :id")
    Optional<Propiedad> findByIdForUpdate(@Param("id") UUID id);

    // 🔹 método original (lo podés dejar)
    List<Propiedad> findByAgenteId(UUID agenteId);

    List<Propiedad> findByPublicacionEstado(PublicacionEstado publicacionEstado);

    @Query("""
            SELECT p FROM Propiedad p
            WHERE p.publicacionEstado = :estado
            AND EXISTS (
                SELECT 1 FROM Agente a
                WHERE a.id = p.agenteId
                AND EXISTS (
                    SELECT 1 FROM Usuario u
                    WHERE u.id = a.usuarioId AND u.membresiaActiva = true
                )
            )
            """)
    List<Propiedad> findPublicadasConMembresiaActiva(PublicacionEstado estado);

    // 🔥 MÉTODO OPTIMIZADO (SIN N+1), se ejecuta en un solo query
    @Query("""
            SELECT DISTINCT p FROM Propiedad p
            LEFT JOIN FETCH p.imagenes
            WHERE p.agenteId = :agenteId
            """)
    List<Propiedad> findByAgenteIdWithImagenes(UUID agenteId);

    @Query("""
            SELECT DISTINCT p FROM Propiedad p
            LEFT JOIN FETCH p.imagenes
            WHERE p.agenteId = :agenteId
            AND p.publicacionEstado = :estado
            AND EXISTS (
                SELECT 1 FROM Agente a
                WHERE a.id = :agenteId
                AND EXISTS (
                    SELECT 1 FROM Usuario u
                    WHERE u.id = a.usuarioId AND u.membresiaActiva = true
                )
            )
            """)
    List<Propiedad> findPublicadasByAgenteIdConMembresiaActiva(UUID agenteId, PublicacionEstado estado);
}