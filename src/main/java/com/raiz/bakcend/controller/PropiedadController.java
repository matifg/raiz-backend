package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.repository.PropiedadRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/propiedades")
public class PropiedadController {

    private final PropiedadRepository propiedadRepository;

    public PropiedadController(PropiedadRepository propiedadRepository) {
        this.propiedadRepository = propiedadRepository;
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
    public Propiedad crear(@RequestBody Propiedad propiedad) {
        // Asegura que el ID sea null para que Hibernate lo genere
        propiedad.setId(null);
        return propiedadRepository.save(propiedad);
    }

    @GetMapping("/agente/{agenteId}")
    public List<Propiedad> listarPorAgente(@PathVariable UUID agenteId) {
        return propiedadRepository.findByAgenteId(agenteId);
    }
}
