package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.CreateUsuarioRequest;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, AgenteRepository agenteRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un usuario y, si el rol es AGENTE, crea el agente asociado.
     */
    public Usuario register(CreateUsuarioRequest req) {
        // Validar email único
        if (usuarioRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setApellido(req.getApellido());
        usuario.setEmail(req.getEmail());
        usuario.setTelefono(req.getTelefono());
        usuario.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        usuario.setRol(req.getRol());
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        // Si el rol es AGENTE, crear el agente asociado
        if ("AGENTE".equalsIgnoreCase(usuario.getRol())) {
            Agente agente = new Agente();
            agente.setUsuarioId(usuario.getId());
            agenteRepository.save(agente);
        }

        return usuario;
    }
}
