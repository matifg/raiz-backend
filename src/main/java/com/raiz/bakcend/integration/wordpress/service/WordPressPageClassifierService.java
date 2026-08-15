package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.integration.wordpress.dto.WordPressImportabilidad;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageClasificacionResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Clasifica páginas WordPress como importables o no, según la estructura HTML
 * de Elementor detectada en el análisis de Etapa 1 (plantilla ficha = Static Grid Tab).
 * No persiste ni crea propiedades.
 */
@Service
public class WordPressPageClassifierService {

    /**
     * Marcador de plantilla A (ficha de propiedad): BDThemes Static Grid Tab.
     */
    static final String MARKER_FICHA = "bdt-ep-static-grid-tab-main-title";

    private static final Set<String> SLUGS_INSTITUCIONALES = Set.of(
            "inicio", "servicios", "home", "contacto", "nosotros", "about");

    private static final Set<String> SLUGS_CATALOGO = Set.of(
            "alquileres", "alquiler-locales", "lotes", "campo", "locales",
            "casas", "elementor-978", "elementor-310-cloned-310", "lotes-peuma");

    private static final Set<String> SLUGS_PROYECTO = Set.of(
            "proyecto", "p4de-febrero");

    private static final Set<String> SLUGS_FORMULARIO = Set.of(
            "ingreso-propiedades");

    public List<WordPressPageClasificacionResponse> clasificar(List<WordPressPageResponse> paginas) {
        if (paginas == null || paginas.isEmpty()) {
            return List.of();
        }
        return paginas.stream().map(this::clasificarUna).toList();
    }

    public WordPressPageClasificacionResponse clasificarUna(WordPressPageResponse pagina) {
        String titulo = nullSafe(pagina.getTitle());
        String slug = nullSafe(pagina.getSlug()).toLowerCase(Locale.ROOT);
        String content = nullSafe(pagina.getContent());
        String contentLower = content.toLowerCase(Locale.ROOT);
        String tituloLower = titulo.toLowerCase(Locale.ROOT);

        if (esFichaDePropiedad(content)) {
            return new WordPressPageClasificacionResponse(
                    pagina.getId(),
                    titulo,
                    pagina.getLink(),
                    WordPressImportabilidad.IMPORTABLE,
                    "contiene la estructura de ficha de propiedad.");
        }

        String motivo = resolverMotivoNoImportable(slug, tituloLower, content, contentLower);
        return new WordPressPageClasificacionResponse(
                pagina.getId(),
                titulo,
                pagina.getLink(),
                WordPressImportabilidad.NO_IMPORTABLE,
                motivo);
    }

    private boolean esFichaDePropiedad(String content) {
        return content.contains(MARKER_FICHA);
    }

    private String resolverMotivoNoImportable(
            String slug, String tituloLower, String content, String contentLower) {

        if (content.isBlank() || content.length() < 500) {
            return "contenido insuficiente o página incompleta.";
        }

        if (SLUGS_INSTITUCIONALES.contains(slug)
                || tituloLower.equals("inicio")
                || tituloLower.equals("servicios")) {
            return "página institucional.";
        }

        if (SLUGS_FORMULARIO.contains(slug)
                || content.contains("elementor-widget-form")
                || contentLower.contains("ingreso propiedades")) {
            return "página utilitaria con formulario.";
        }

        if (SLUGS_PROYECTO.contains(slug)
                || tituloLower.startsWith("proyecto ")
                || contentLower.contains("novedades del proyecto")
                || (content.contains("elementor-widget-gallery")
                        && content.contains("elementor-widget-testimonial"))) {
            return "página de proyecto o loteo, no ficha individual.";
        }

        if (SLUGS_CATALOGO.contains(slug)
                || content.contains("bdt-static-carousel")
                || contentLower.contains("más detalles")
                || esTituloCatalogo(tituloLower)) {
            return "página de listado o catálogo.";
        }

        return "no contiene la estructura de ficha de propiedad (Static Grid Tab).";
    }

    private boolean esTituloCatalogo(String tituloLower) {
        return tituloLower.equals("alquileres")
                || tituloLower.equals("alquiler locales")
                || tituloLower.equals("lotes")
                || tituloLower.equals("campos")
                || tituloLower.equals("locales")
                || tituloLower.equals("casas")
                || tituloLower.equals("lotes peuma");
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
