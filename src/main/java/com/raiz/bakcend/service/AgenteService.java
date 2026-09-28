package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.ActualizarPerfilAgenteRequest;
import com.raiz.bakcend.dto.AgenteAdminPageResponse;
import com.raiz.bakcend.dto.AgenteAdminResponse;
import com.raiz.bakcend.dto.AgenteResponse;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.util.TelefonoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgenteService {

    private static final Logger logger = LoggerFactory.getLogger(AgenteService.class);
    private static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;
    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;
    private static final Path LOGO_DIR = Paths.get("uploads", "logos");
    private static final Path COVER_DIR = Paths.get("uploads", "covers");
    private static final String LOGO_URL_PREFIX = "/uploads/logos/";
    private static final String COVER_URL_PREFIX = "/uploads/covers/";

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

    public AgenteResponse obtenerPorId(UUID agenteId) {
        Agente agente = agenteRepository.findById(agenteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Agente no encontrado"));

        Usuario usuario = usuarioRepository.findById(agente.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return AgenteResponse.from(agente, usuario);
    }

    public Optional<AgenteResponse> obtenerPorUsuarioId(UUID usuarioId) {
        return agenteRepository.findByUsuarioId(usuarioId)
                .flatMap(agente -> usuarioRepository.findById(agente.getUsuarioId())
                        .map(usuario -> AgenteResponse.from(agente, usuario)));
    }

    public AgenteResponse actualizarPerfil(UUID usuarioId, ActualizarPerfilAgenteRequest request) {
        Agente agente = agenteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No autorizado o no es agente"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!"AGENTE".equalsIgnoreCase(usuario.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado o no es agente");
        }

        if (isBlank(request.getNombre())) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (isBlank(request.getApellido())) {
            throw new IllegalArgumentException("El apellido es obligatorio.");
        }

        usuario.setNombre(request.getNombre().trim());
        usuario.setApellido(request.getApellido().trim());
        usuario.setTelefono(TelefonoUtil.normalizarOpcional(request.getTelefono()));

        if (request.getInmobiliaria() != null) {
            agente.setInmobiliaria(blankToNull(request.getInmobiliaria()));
        }
        if (request.getLogoUrl() != null) {
            agente.setLogoUrl(blankToNull(request.getLogoUrl()));
        }
        if (request.getCoverUrl() != null) {
            agente.setCoverUrl(blankToNull(request.getCoverUrl()));
        }

        usuarioRepository.save(usuario);
        agenteRepository.save(agente);
        return AgenteResponse.from(agente, usuario);
    }

    public AgenteResponse subirLogo(UUID usuarioId, MultipartFile file) {
        Agente agente = requerirAgenteDelUsuario(usuarioId);
        validarArchivoImagen(file, MAX_LOGO_BYTES, "logo");

        String extension = extensionPermitida(file);
        String fileName = agente.getId() + "_" + UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(LOGO_DIR);
            Files.write(LOGO_DIR.resolve(fileName), file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el logo");
        }

        borrarArchivoLocalSiCorresponde(agente.getLogoUrl(), LOGO_URL_PREFIX, LOGO_DIR);

        agente.setLogoUrl(publicUploadUrl(LOGO_URL_PREFIX, fileName));
        agenteRepository.save(agente);

        return respuestaAgente(usuarioId, agente);
    }

    public AgenteResponse eliminarLogo(UUID usuarioId) {
        Agente agente = requerirAgenteDelUsuario(usuarioId);
        borrarArchivoLocalSiCorresponde(agente.getLogoUrl(), LOGO_URL_PREFIX, LOGO_DIR);
        agente.setLogoUrl(null);
        agenteRepository.save(agente);
        return respuestaAgente(usuarioId, agente);
    }

    public AgenteResponse subirCover(UUID usuarioId, MultipartFile file) {
        Agente agente = requerirAgenteDelUsuario(usuarioId);
        validarArchivoImagen(file, MAX_COVER_BYTES, "cover");

        String extension = extensionPermitida(file);
        String fileName = agente.getId() + "_" + UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(COVER_DIR);
            Files.write(COVER_DIR.resolve(fileName), file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el cover");
        }

        borrarArchivoLocalSiCorresponde(agente.getCoverUrl(), COVER_URL_PREFIX, COVER_DIR);

        agente.setCoverUrl(publicUploadUrl(COVER_URL_PREFIX, fileName));
        agenteRepository.save(agente);

        return respuestaAgente(usuarioId, agente);
    }

    public AgenteResponse eliminarCover(UUID usuarioId) {
        Agente agente = requerirAgenteDelUsuario(usuarioId);
        borrarArchivoLocalSiCorresponde(agente.getCoverUrl(), COVER_URL_PREFIX, COVER_DIR);
        agente.setCoverUrl(null);
        agenteRepository.save(agente);
        return respuestaAgente(usuarioId, agente);
    }

    public Propiedad embeberAgente(Propiedad propiedad) {
        if (propiedad == null) {
            return null;
        }
        propiedad.setAgente(resolverAgenteResponse(propiedad.getAgenteId()));
        return propiedad;
    }

    public List<Propiedad> embeberAgentes(List<Propiedad> propiedades) {
        if (propiedades == null || propiedades.isEmpty()) {
            return propiedades;
        }

        Set<UUID> agenteIds = new HashSet<>();
        for (Propiedad propiedad : propiedades) {
            if (propiedad.getAgenteId() != null) {
                agenteIds.add(propiedad.getAgenteId());
            }
        }

        Map<UUID, AgenteResponse> porId = new HashMap<>();
        if (!agenteIds.isEmpty()) {
            List<Agente> agentes = agenteRepository.findAllById(agenteIds);
            Set<UUID> usuarioIds = agentes.stream()
                    .map(Agente::getUsuarioId)
                    .collect(Collectors.toSet());
            Map<UUID, Usuario> usuarios = usuarioRepository.findAllById(usuarioIds).stream()
                    .collect(Collectors.toMap(Usuario::getId, u -> u, (a, b) -> a));

            for (Agente agente : agentes) {
                Usuario usuario = usuarios.get(agente.getUsuarioId());
                if (usuario != null) {
                    porId.put(agente.getId(), AgenteResponse.from(agente, usuario));
                }
            }
        }

        for (Propiedad propiedad : propiedades) {
            propiedad.setAgente(porId.get(propiedad.getAgenteId()));
        }
        return propiedades;
    }

    private AgenteResponse resolverAgenteResponse(UUID agenteId) {
        if (agenteId == null) {
            return null;
        }
        return agenteRepository.findById(agenteId)
                .flatMap(agente -> usuarioRepository.findById(agente.getUsuarioId())
                        .map(usuario -> AgenteResponse.from(agente, usuario)))
                .orElse(null);
    }

    private AgenteResponse respuestaAgente(UUID usuarioId, Agente agente) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return AgenteResponse.from(agente, usuario);
    }

    private Agente requerirAgenteDelUsuario(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!"AGENTE".equalsIgnoreCase(usuario.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado o no es agente");
        }
        return agenteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No autorizado o no es agente"));
    }

    private void validarArchivoImagen(MultipartFile file, long maxBytes, String tipo) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo de " + tipo + " requerido");
        }
        if (file.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El " + tipo + " no puede superar " + maxMb + "MB");
        }
        extensionPermitida(file);
    }

    private String extensionPermitida(MultipartFile file) {
        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT)
                : "";
        String original = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase(Locale.ROOT)
                : "";

        if (contentType.contains("jpeg") || contentType.contains("jpg") || original.endsWith(".jpg")
                || original.endsWith(".jpeg")) {
            return "jpg";
        }
        if (contentType.contains("png") || original.endsWith(".png")) {
            return "png";
        }
        if (contentType.contains("webp") || original.endsWith(".webp")) {
            return "webp";
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Formato no permitido (jpg, png o webp)");
    }

    private String publicUploadUrl(String urlPrefix, String fileName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(urlPrefix)
                .path(fileName)
                .toUriString();
    }

    private void borrarArchivoLocalSiCorresponde(String url, String urlPrefix, Path dir) {
        if (url == null || url.isBlank()) {
            return;
        }
        int idx = url.indexOf(urlPrefix);
        if (idx < 0) {
            return;
        }
        String fileName = url.substring(idx + urlPrefix.length());
        if (fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(dir.resolve(fileName));
        } catch (IOException e) {
            logger.warn("No se pudo borrar archivo local {}: {}", fileName, e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
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
