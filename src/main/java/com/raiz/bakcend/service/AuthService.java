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
    private final AdminAgentesCacheService adminAgentesCacheService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    public AuthService(
            UsuarioRepository usuarioRepository,
            AgenteRepository agenteRepository,
            BCryptPasswordEncoder passwordEncoder,
            AdminAgentesCacheService adminAgentesCacheService) {
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminAgentesCacheService = adminAgentesCacheService;
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
        usuario.setMembresiaActiva(false);

        usuario = usuarioRepository.save(usuario);

        log.info("Registrando usuario email={} rol={}", req.getEmail(), req.getRol());

        // Si el rol es AGENTE, crear el agente asociado
        if (req.getRol().equalsIgnoreCase("AGENTE")) {
            Agente agente = new Agente();
            agente.setUsuarioId(usuario.getId());
            agente.setActivo(true);
            agenteRepository.save(agente);
            adminAgentesCacheService.evictAll("agent-registered userId=" + usuario.getId());
            log.info("Agente creado para usuario_id={}", usuario.getId());
        }

        return usuario;
    }
}
