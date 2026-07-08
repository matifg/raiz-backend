package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PublicacionEstado;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.service.AdminAgentesCacheService;
import com.raiz.bakcend.service.PropiedadPortadaService;
import com.raiz.bakcend.service.PropiedadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/propiedades")
public class PropiedadController {

    private final PropiedadRepository propiedadRepository;
    private final AgenteRepository agenteRepository;
    private final AdminAgentesCacheService adminAgentesCacheService;
    private final PropiedadPortadaService propiedadPortadaService;
    private final PropiedadService propiedadService;

    public PropiedadController(
            PropiedadRepository propiedadRepository,
            AgenteRepository agenteRepository,
            AdminAgentesCacheService adminAgentesCacheService,
            PropiedadPortadaService propiedadPortadaService,
            PropiedadService propiedadService) {
        this.propiedadRepository = propiedadRepository;
        this.agenteRepository = agenteRepository;
        this.adminAgentesCacheService = adminAgentesCacheService;
        this.propiedadPortadaService = propiedadPortadaService;
        this.propiedadService = propiedadService;
    }

    @GetMapping
    public List<Propiedad> listar() {
        return propiedadPortadaService.aplicarPortadas(
                propiedadRepository.findPublicadasConMembresiaActiva(PublicacionEstado.PUBLICADA));
    }

    @GetMapping("/{id}")
    public Propiedad obtener(@PathVariable UUID id, Authentication authentication) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        propiedadService.validarPuedeVer(authentication, propiedad);
        return propiedadPortadaService.aplicarPortada(propiedad);
    }

    @PostMapping
    public Propiedad crear(@RequestBody Propiedad propiedad, Authentication authentication) {
        propiedad.setId(null);

        String userId = authentication.getName();
        UUID usuarioId = UUID.fromString(userId);

        Agente agente = agenteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Agente no encontrado"));

        propiedad.setAgenteId(agente.getId());
        propiedadService.prepararNueva(propiedad);

        Propiedad creada = propiedadRepository.save(propiedad);
        adminAgentesCacheService.evictAll("property-created propiedadId=" + creada.getId());
        return creada;
    }

    @GetMapping("/agente/{agenteId}")
    public List<Propiedad> listarPorAgente(
            @PathVariable UUID agenteId,
            Authentication authentication) {
        List<Propiedad> propiedades = propiedadService.puedeGestionarAgente(authentication, agenteId)
                ? propiedadRepository.findByAgenteIdWithImagenes(agenteId)
                : propiedadRepository.findPublicadasByAgenteIdConMembresiaActiva(
                        agenteId, PublicacionEstado.PUBLICADA);
        return propiedadPortadaService.aplicarPortadas(propiedades);
    }

    @PutMapping("/{id}")
    public Propiedad actualizar(
            @PathVariable UUID id,
            @RequestBody Propiedad propiedadActualizada) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada con ID: " + id));

        propiedadService.aplicarActualizacion(propiedad, propiedadActualizada);

        Propiedad guardada = propiedadRepository.save(propiedad);
        adminAgentesCacheService.evictAll("property-updated propiedadId=" + guardada.getId());
        return guardada;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        if (!propiedadRepository.existsById(id)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Propiedad no encontrada"
            );
        }
        propiedadRepository.deleteById(id);
        adminAgentesCacheService.evictAll("property-deleted propiedadId=" + id);
    }
}
