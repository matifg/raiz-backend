package com.raiz.bakcend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.raiz.bakcend.dto.LocalidadResponse;
import com.raiz.bakcend.dto.ProvinciaResponse;
import com.raiz.bakcend.service.GeorefService;

@RestController
@RequestMapping("/ubicaciones")
public class UbicacionController {

    private final GeorefService georefService;

    public UbicacionController(GeorefService georefService) {
        this.georefService = georefService;
    }

    @GetMapping("/provincias")
    public List<ProvinciaResponse> listarProvincias() {
        return georefService.listarProvincias();
    }

    @GetMapping("/localidades")
    public List<LocalidadResponse> listarLocalidades(@RequestParam String provincia) {
        return georefService.listarLocalidades(provincia);
    }
}
