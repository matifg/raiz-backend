package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.CreateUsuarioRequest;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.TokenVerificacion;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.util.TelefonoUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AgenteRepository agenteRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminAgentesCacheService adminAgentesCacheService;
    private final TokenVerificacionService tokenVerificacionService;
    private final VerificacionEmailService verificacionEmailService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    public AuthService(
            UsuarioRepository usuarioRepository,
            AgenteRepository agenteRepository,
            BCryptPasswordEncoder passwordEncoder,
            AdminAgentesCacheService adminAgentesCacheService,
            TokenVerificacionService tokenVerificacionService,
            VerificacionEmailService verificacionEmailService) {
        this.usuarioRepository = usuarioRepository;
        this.agenteRepository = agenteRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminAgentesCacheService = adminAgentesCacheService;
        this.tokenVerificacionService = tokenVerificacionService;
        this.verificacionEmailService = verificacionEmailService;
    }

    @Transactional
    public Usuario register(CreateUsuarioRequest req) {
        if (usuarioRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setApellido(req.getApellido());
        usuario.setEmail(req.getEmail());
        if (req.getRol().equalsIgnoreCase("AGENTE")) {
            usuario.setTelefono(TelefonoUtil.normalizarYPersistir(req.getTelefono()));
        } else {
            usuario.setTelefono(req.getTelefono());
        }
        usuario.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        usuario.setRol(req.getRol());
        usuario.setActivo(true);
        usuario.setMembresiaActiva(false);
        usuario.setEmailVerificado(false);

        usuario = usuarioRepository.save(usuario);

        log.info("Registrando usuario email={} rol={} emailVerificado={}", req.getEmail(), req.getRol(), usuario.getEmailVerificado());

        if (req.getRol().equalsIgnoreCase("AGENTE")) {
            Agente agente = new Agente();
            agente.setUsuarioId(usuario.getId());
            agente.setActivo(true);
            agenteRepository.save(agente);
            adminAgentesCacheService.evictAll("agent-registered userId=" + usuario.getId());
            log.info("Agente creado para usuario_id={}", usuario.getId());
        }

        TokenVerificacion tokenVerificacion = tokenVerificacionService.crearToken(usuario);
        scheduleVerificationEmailAfterCommit(usuario, tokenVerificacion.getToken());

        return usuario;
    }

    /**
     * Envía el mail solo después del commit, para no mandar un token que luego se revierta
     * ni hacer rollback del usuario si Brevo falla.
     */
    private void scheduleVerificationEmailAfterCommit(Usuario usuario, UUID token) {
        String email = usuario.getEmail();
        String nombre = usuario.getNombre();

        Runnable send = () -> {
            try {
                Usuario snapshot = new Usuario();
                snapshot.setEmail(email);
                snapshot.setNombre(nombre);
                verificacionEmailService.enviarEmailVerificacion(snapshot, token);
                log.info("Email de verificación enviado a {}", email);
            } catch (Exception ex) {
                log.error(
                        "Usuario/token ya persistidos pero falló el email de verificación email={} token={}",
                        email,
                        token,
                        ex);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }
}
