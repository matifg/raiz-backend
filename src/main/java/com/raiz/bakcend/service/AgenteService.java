package com.raiz.bakcend.service;

import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AgenteService {

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;

    public AgenteService(UsuarioRepository usuarioRepository, AgenteRepository agenteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
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
}
