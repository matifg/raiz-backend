package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.CreateUsuarioRequest;
import com.raiz.bakcend.model.TokenVerificacion;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRegisterRolTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AgenteRepository agenteRepository;
    @Mock
    private AdminAgentesCacheService adminAgentesCacheService;
    @Mock
    private TokenVerificacionService tokenVerificacionService;
    @Mock
    private VerificacionEmailService verificacionEmailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository,
                agenteRepository,
                new BCryptPasswordEncoder(),
                adminAgentesCacheService,
                tokenVerificacionService,
                verificacionEmailService);
    }

    @Test
    void registroNormalAsignaCliente() {
        CreateUsuarioRequest request = requestBase();
        request.setRol(null);

        Usuario creado = registrar(request);

        assertEquals("CLIENTE", creado.getRol());
        verify(agenteRepository, never()).save(any());
    }

    @Test
    void registroConRolAdminSigueSiendoCliente() {
        CreateUsuarioRequest request = requestBase();
        request.setRol("ADMIN");

        Usuario creado = registrar(request);

        assertEquals("CLIENTE", creado.getRol());
        verify(agenteRepository, never()).save(any());
    }

    @Test
    void registroConRolArbitrarioSigueSiendoCliente() {
        CreateUsuarioRequest request = requestBase();
        request.setRol("SUPER_HACKER");

        Usuario creado = registrar(request);

        assertEquals("CLIENTE", creado.getRol());
        verify(agenteRepository, never()).save(any());
    }

    private Usuario registrar(CreateUsuarioRequest request) {
        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario usuario = inv.getArgument(0);
            usuario.setId(UUID.randomUUID());
            return usuario;
        });

        TokenVerificacion token = new TokenVerificacion();
        token.setToken(UUID.randomUUID());
        when(tokenVerificacionService.crearToken(any(Usuario.class))).thenReturn(token);

        Usuario resultado = authService.register(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("CLIENTE", captor.getValue().getRol());

        return resultado;
    }

    private CreateUsuarioRequest requestBase() {
        CreateUsuarioRequest request = new CreateUsuarioRequest();
        request.setNombre("Ana");
        request.setApellido("Perez");
        request.setEmail("ana@example.com");
        request.setTelefono("111");
        request.setPassword("secreto123");
        return request;
    }
}
