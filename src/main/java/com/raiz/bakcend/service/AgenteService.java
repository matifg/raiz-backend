package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.AgenteAdminPageResponse;
import com.raiz.bakcend.dto.AgenteAdminResponse;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgenteService {

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;
    private final PropiedadRepository propiedadRepository;

    @Autowired
    public AgenteService(UsuarioRepository usuarioRepository, AgenteRepository agenteRepository, PropiedadRepository propiedadRepository) {
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
        this.propiedadRepository = propiedadRepository;
    }

    /**
     * Busca el agente asociado al usuario autenticado.
     */
    public Optional<Agente> getAgenteForUserEmail(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) return Optional.empty();

        Usuario usuario = usuarioOpt.get();
        if (!"AGENTE".equalsIgnoreCase(usuario.getRol())) return Optional.empty();

        return agenteRepository.findByUsuarioId(usuario.getId());
    }

    public Optional<Agente> getAgenteByUsuarioId(java.util.UUID usuarioId) {
        return agenteRepository.findByUsuarioId(usuarioId);
    }


    public AgenteAdminPageResponse listarAgentesAdminPaginado(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Agente> agentesPage = agenteRepository.findAll(pageable);

        List<AgenteAdminResponse> agentes = agentesPage.getContent().stream().map(agente -> {
            Usuario usuario = usuarioRepository.findById(agente.getUsuarioId()).orElse(null);
            if (usuario == null) return null;
            AgenteAdminResponse dto = new AgenteAdminResponse();
            dto.setUsuarioId(usuario.getId());
            dto.setNombre(usuario.getNombre());
            dto.setApellido(usuario.getApellido());
            dto.setEmail(usuario.getEmail());
            dto.setMembresiaActiva(usuario.getMembresiaActiva());
            dto.setCantidadPropiedades(propiedadRepository.findByAgenteId(agente.getId()).size());
            return dto;
        }).filter(dto -> dto != null).collect(Collectors.toList());

        AgenteAdminPageResponse response = new AgenteAdminPageResponse();
        response.setAgentes(agentes);
        response.setTotal(agentesPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPages(agentesPage.getTotalPages());
        return response;
    }
}
