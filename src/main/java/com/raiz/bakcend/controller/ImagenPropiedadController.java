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
        if (!repository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Imagen no encontrada"
            );
        }
        repository.deleteById(id);
        System.out.println("Imagen eliminada: " + id);
    }

    @PostMapping("/upload")
public String upload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
    try {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        java.nio.file.Path path = java.nio.file.Paths.get("uploads/" + fileName);
        java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.write(path, file.getBytes());

        return "http://localhost:8080/uploads/" + fileName;

    } catch (Exception e) {
        throw new RuntimeException("Error subiendo imagen");
    }
}

}
