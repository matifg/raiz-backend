package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.model.Agente;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/propiedades")
public class PropiedadController {

    private final PropiedadRepository propiedadRepository;
    private final AgenteRepository agenteRepository;

    public PropiedadController(PropiedadRepository propiedadRepository, AgenteRepository agenteRepository) {
        this.propiedadRepository = propiedadRepository;
        this.agenteRepository = agenteRepository;
    }

    @GetMapping
    public List<Propiedad> listar() {
        return propiedadRepository.findAll();
    }

    @GetMapping("/{id}")
    public Propiedad obtener(@PathVariable UUID id) {
        return propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada con ID: " + id));
    }

    @PostMapping
    public Propiedad crear(@RequestBody Propiedad propiedad, Authentication authentication) {
        // Asegura que el ID sea null para que Hibernate lo genere
        propiedad.setId(null);

        // Obtener el userId del JWT
        String userId = authentication.getName();
        java.util.UUID usuarioId = java.util.UUID.fromString(userId);

        // Buscar el agente correspondiente
        Agente agente = agenteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Agente no encontrado"));

        // Setear el agenteId antes de guardar
        propiedad.setAgenteId(agente.getId());

        return propiedadRepository.save(propiedad);
    }

    @GetMapping("/agente/{agenteId}")
    public List<Propiedad> listarPorAgente(@PathVariable UUID agenteId) {
        return propiedadRepository.findByAgenteIdWithImagenes(agenteId);
    }

    @PutMapping("/{id}")
    public Propiedad actualizar(
            @PathVariable UUID id,
            @RequestBody Propiedad propiedadActualizada) {
        Propiedad propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada con ID: " + id));

        propiedad.setTitulo(propiedadActualizada.getTitulo());
        propiedad.setDescripcion(propiedadActualizada.getDescripcion());
        propiedad.setDireccion(propiedadActualizada.getDireccion());
        propiedad.setCiudad(propiedadActualizada.getCiudad());
        propiedad.setPrecio(propiedadActualizada.getPrecio());
        propiedad.setSuperficieM2(propiedadActualizada.getSuperficieM2());
        propiedad.setHabitaciones(propiedadActualizada.getHabitaciones());
        propiedad.setBanios(propiedadActualizada.getBanios());
        propiedad.setEstado(propiedadActualizada.getEstado());
        propiedad.setOperacion(propiedadActualizada.getOperacion());
        propiedad.setMoneda(propiedadActualizada.getMoneda());
        propiedad.setZona(propiedadActualizada.getZona());

        return propiedadRepository.save(propiedad);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        if (!propiedadRepository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Propiedad no encontrada"
            );
        }
        propiedadRepository.deleteById(id);
    }
}
