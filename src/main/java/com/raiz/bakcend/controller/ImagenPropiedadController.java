package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.ImagenPropiedad;
import com.raiz.bakcend.repository.ImagenPropiedadRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/imagenes")
public class ImagenPropiedadController {

    private final ImagenPropiedadRepository repository;

    public ImagenPropiedadController(ImagenPropiedadRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/propiedad/{propiedadId}")
    public List<ImagenPropiedad> listar(@PathVariable UUID propiedadId) {
        return repository.findByPropiedadId(propiedadId);
    }

    @PostMapping
    public ImagenPropiedad crear(@RequestBody ImagenPropiedad imagen) {
        imagen.setId(null);
        return repository.save(imagen);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        repository.deleteById(id);
    }
}
