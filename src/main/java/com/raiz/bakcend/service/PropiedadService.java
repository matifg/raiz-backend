package com.raiz.bakcend.service;

import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PublicacionEstado;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PropiedadService {

    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;

    public PropiedadService(AgenteRepository agenteRepository, UsuarioRepository usuarioRepository) {
        this.agenteRepository = agenteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public PublicacionEstado resolverPublicacionEstado(PublicacionEstado estado) {
        return estado != null ? estado : PublicacionEstado.PUBLICADA;
    }

    public void validarParaPublicacion(Propiedad propiedad) {
        List<String> faltantes = new ArrayList<>();

        if (isBlank(propiedad.getTitulo())) faltantes.add("titulo");
        if (isBlank(propiedad.getDireccion())) faltantes.add("direccion");
        if (isBlank(propiedad.getCiudad())) faltantes.add("ciudad");
        if (propiedad.getPrecio() == null) faltantes.add("precio");
        if (propiedad.getSuperficieM2() == null) faltantes.add("superficieM2");
        if (propiedad.getHabitaciones() == null) faltantes.add("habitaciones");
        if (propiedad.getBanios() == null) faltantes.add("banios");
        if (isBlank(propiedad.getEstado())) faltantes.add("estado");
        if (propiedad.getTipoId() == null) faltantes.add("tipoId");

        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Campos obligatorios para publicar: " + String.join(", ", faltantes));
        }
    }

    public void aplicarActualizacion(Propiedad destino, Propiedad origen) {
        destino.setTitulo(origen.getTitulo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setDireccion(origen.getDireccion());
        destino.setCiudad(origen.getCiudad());
        destino.setPrecio(origen.getPrecio());
        destino.setOcultarPrecio(origen.getOcultarPrecio());
        destino.setSuperficieM2(origen.getSuperficieM2());
        destino.setHabitaciones(origen.getHabitaciones());
        destino.setBanios(origen.getBanios());
        destino.setEstado(origen.getEstado());
        destino.setOperacion(origen.getOperacion());
        destino.setMoneda(origen.getMoneda());
        destino.setZona(origen.getZona());
        destino.setTipoId(origen.getTipoId());
        if (origen.getPublicacionEstado() != null) {
            destino.setPublicacionEstado(origen.getPublicacionEstado());
        }

        if (destino.getPublicacionEstado() == PublicacionEstado.PUBLICADA) {
            validarParaPublicacion(destino);
        }
    }

    public void prepararNueva(Propiedad propiedad) {
        propiedad.setPublicacionEstado(resolverPublicacionEstado(propiedad.getPublicacionEstado()));

        if (propiedad.getPublicacionEstado() == PublicacionEstado.PUBLICADA) {
            validarParaPublicacion(propiedad);
        }
    }

    public boolean esPublicada(Propiedad propiedad) {
        PublicacionEstado estado = propiedad.getPublicacionEstado();
        return estado == null || estado == PublicacionEstado.PUBLICADA;
    }

    public boolean puedeVerBorrador(Authentication authentication, Propiedad propiedad) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        UUID usuarioId = UUID.fromString(authentication.getName());
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return true;
        }

        return agenteRepository.findByUsuarioId(usuarioId)
                .map(agente -> agente.getId().equals(propiedad.getAgenteId()))
                .orElse(false);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
