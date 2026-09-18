package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PublicacionEstado;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.service.AdminAgentesCacheService;
import com.raiz.bakcend.service.PropiedadPortadaService;
import com.raiz.bakcend.service.PropiedadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropiedadOwnershipTest {

    @Mock
    private PropiedadRepository propiedadRepository;
    @Mock
    private AgenteRepository agenteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AdminAgentesCacheService adminAgentesCacheService;
    @Mock
    private PropiedadPortadaService propiedadPortadaService;

    private PropiedadController controller;

    private final UUID propiedadId = UUID.randomUUID();
    private final UUID agenteDuenioId = UUID.randomUUID();
    private final UUID usuarioDuenioId = UUID.randomUUID();
    private final UUID usuarioOtroId = UUID.randomUUID();
    private final UUID usuarioAdminId = UUID.randomUUID();
    private final UUID agenteOtroId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PropiedadService propiedadService = new PropiedadService(agenteRepository, usuarioRepository);
        controller = new PropiedadController(
                propiedadRepository,
                agenteRepository,
                adminAgentesCacheService,
                propiedadPortadaService,
                propiedadService);
    }

    @Test
    void duenoPuedeEditar() {
        Propiedad existente = propiedadExistente();
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(existente));
        stubUsuarioAgente(usuarioDuenioId, "AGENTE", agenteDuenioId);
        when(propiedadRepository.save(any(Propiedad.class))).thenAnswer(inv -> inv.getArgument(0));

        Propiedad resultado = controller.actualizar(
                propiedadId, bodyActualizacion(), auth(usuarioDuenioId));

        assertNotNull(resultado);
        assertEquals("Nuevo titulo", resultado.getTitulo());
        verify(propiedadRepository).save(existente);
    }

    @Test
    void otroUsuarioNoPuedeEditar() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedadExistente()));
        stubUsuarioAgente(usuarioOtroId, "AGENTE", agenteOtroId);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.actualizar(propiedadId, bodyActualizacion(), auth(usuarioOtroId)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(propiedadRepository, never()).save(any());
    }

    @Test
    void adminPuedeEditar() {
        Propiedad existente = propiedadExistente();
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(existente));
        stubUsuario(usuarioAdminId, "ADMIN");
        when(propiedadRepository.save(any(Propiedad.class))).thenAnswer(inv -> inv.getArgument(0));

        Propiedad resultado = controller.actualizar(
                propiedadId, bodyActualizacion(), auth(usuarioAdminId));

        assertNotNull(resultado);
        verify(propiedadRepository).save(existente);
        verify(agenteRepository, never()).findByUsuarioId(usuarioAdminId);
    }

    @Test
    void duenoPuedeEliminar() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedadExistente()));
        stubUsuarioAgente(usuarioDuenioId, "AGENTE", agenteDuenioId);

        controller.eliminar(propiedadId, auth(usuarioDuenioId));

        verify(propiedadRepository).deleteById(propiedadId);
        verify(adminAgentesCacheService).evictAll(anyString());
    }

    @Test
    void otroUsuarioNoPuedeEliminar() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedadExistente()));
        stubUsuarioAgente(usuarioOtroId, "AGENTE", agenteOtroId);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.eliminar(propiedadId, auth(usuarioOtroId)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(propiedadRepository, never()).deleteById(any());
    }

    @Test
    void adminPuedeEliminar() {
        when(propiedadRepository.findById(propiedadId)).thenReturn(Optional.of(propiedadExistente()));
        stubUsuario(usuarioAdminId, "ADMIN");

        controller.eliminar(propiedadId, auth(usuarioAdminId));

        verify(propiedadRepository).deleteById(propiedadId);
        verify(adminAgentesCacheService).evictAll(anyString());
    }

    private Propiedad propiedadExistente() {
        Propiedad propiedad = new Propiedad();
        propiedad.setId(propiedadId);
        propiedad.setAgenteId(agenteDuenioId);
        propiedad.setTitulo("Titulo original");
        propiedad.setPublicacionEstado(PublicacionEstado.BORRADOR);
        return propiedad;
    }

    private Propiedad bodyActualizacion() {
        Propiedad body = new Propiedad();
        body.setTitulo("Nuevo titulo");
        body.setPublicacionEstado(PublicacionEstado.BORRADOR);
        return body;
    }

    private Authentication auth(UUID usuarioId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(usuarioId.toString());
        return authentication;
    }

    private void stubUsuario(UUID usuarioId, String rol) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setRol(rol);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    }

    private void stubUsuarioAgente(UUID usuarioId, String rol, UUID agenteId) {
        stubUsuario(usuarioId, rol);
        Agente agente = new Agente();
        agente.setId(agenteId);
        agente.setUsuarioId(usuarioId);
        when(agenteRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(agente));
    }
}
