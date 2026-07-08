package com.raiz.bakcend.controller;

import com.raiz.bakcend.dto.ActualizarOrdenImagenRequest;
import com.raiz.bakcend.dto.ImagenOrdenItem;
import com.raiz.bakcend.dto.ImagenPropiedadResponse;
import com.raiz.bakcend.model.ImagenPropiedad;
import com.raiz.bakcend.service.ImagenPropiedadService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/imagenes")
public class ImagenPropiedadController {

    private final ImagenPropiedadService service;

    public ImagenPropiedadController(ImagenPropiedadService service) {
        this.service = service;
    }

    @GetMapping("/propiedad/{propiedadId}")
    public List<ImagenPropiedadResponse> listar(
            @PathVariable UUID propiedadId,
            Authentication authentication) {
        return service.listarPorPropiedad(propiedadId, authentication);
    }

    @PostMapping
    public ImagenPropiedadResponse crear(
            @RequestBody ImagenPropiedad imagen,
            Authentication authentication) {
        return service.crear(imagen, authentication);
    }

    @PutMapping("/{id}")
    public ImagenPropiedadResponse actualizarOrden(
            @PathVariable UUID id,
            @RequestBody ActualizarOrdenImagenRequest request,
            Authentication authentication) {
        return service.actualizarOrden(id, request.getOrden(), authentication);
    }

    @PutMapping("/propiedad/{propiedadId}/orden")
    public List<ImagenPropiedadResponse> reordenarPropiedad(
            @PathVariable UUID propiedadId,
            @RequestBody List<ImagenOrdenItem> items,
            Authentication authentication) {
        return service.reordenarPropiedad(propiedadId, items, authentication);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id, Authentication authentication) {
        service.eliminar(id, authentication);
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
