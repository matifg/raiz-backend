package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.AgenteAdminPageResponse;
import com.raiz.bakcend.dto.AgenteAdminResponse;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AgenteService.class);

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;
    private final PropiedadRepository propiedadRepository;
    private final AdminAgentesCacheService adminAgentesCacheService;

    @Autowired
    public AgenteService(
            UsuarioRepository usuarioRepository,
            AgenteRepository agenteRepository,
            PropiedadRepository propiedadRepository,
            AdminAgentesCacheService adminAgentesCacheService) {
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
        this.propiedadRepository = propiedadRepository;
        this.adminAgentesCacheService = adminAgentesCacheService;
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
        String cacheKey = adminAgentesCacheService.buildKey(page, size);
        long cacheLookupStart = System.nanoTime();
        AgenteAdminPageResponse cached = adminAgentesCacheService.get(page, size);

        if (cached != null) {
            long cacheTimeMs = (System.nanoTime() - cacheLookupStart) / 1_000_000;
            logger.info("[ADMIN_CACHE] CACHE TIME={}ms key={}", cacheTimeMs, cacheKey);
            return cached;
        }

        long dbStart = System.nanoTime();
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
        long dbTimeMs = (System.nanoTime() - dbStart) / 1_000_000;
        logger.info("[ADMIN_CACHE] DB TIME={}ms key={}", dbTimeMs, cacheKey);
        adminAgentesCacheService.put(page, size, response);
        return response;
    }
}
