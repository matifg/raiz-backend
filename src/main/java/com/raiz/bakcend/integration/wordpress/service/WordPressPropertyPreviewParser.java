package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.integration.wordpress.dto.WordPressPageResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPropertyPreviewResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae título, descripción e imágenes de una página IMPORTABLE (plantilla ficha).
 * No persiste ni descarga archivos.
 */
@Service
public class WordPressPropertyPreviewParser {

    /** Descripción repetida N veces (una por slide). */
    static final String SELECTOR_EXCERPT = ".bdt-ep-static-grid-tab-excerpt";

    /** Imágenes de la galería Static Grid Tab (detalle + thumbnail). */
    static final String SELECTOR_IMAGENES =
            ".bdt-ep-static-grid-tab-image img, .bdt-ep-static-grid-tab-thumbnail img";

    /** Sufijo de tamaño WP: foto-300x300.jpg → foto.jpg */
    private static final Pattern WP_SIZE_SUFFIX =
            Pattern.compile("-(\\d+)x(\\d+)(?=\\.[a-zA-Z0-9]+$)");

    private static final Pattern SRCSET_CANDIDATE =
            Pattern.compile("(\\S+)\\s+(\\d+)w");

    public WordPressPropertyPreviewResponse parse(WordPressPageResponse pagina) {
        String html = pagina.getContent() == null ? "" : pagina.getContent();
        String baseUri = pagina.getLink() != null ? pagina.getLink() : "";
        Document doc = Jsoup.parse(html, baseUri);

        return new WordPressPropertyPreviewResponse(
                pagina.getId(),
                pagina.getTitle(),
                extraerDescripcion(doc),
                List.copyOf(extraerImagenes(doc)));
    }

    private String extraerDescripcion(Document doc) {
        Elements excerpts = doc.select(SELECTOR_EXCERPT);
        Set<String> unicas = new LinkedHashSet<>();
        for (Element excerpt : excerpts) {
            String texto = normalizarTexto(excerpt.text());
            if (!texto.isEmpty()) {
                unicas.add(texto);
            }
        }
        if (unicas.isEmpty()) {
            return "";
        }
        // Suele ser la misma descripción N veces; si hubiera variantes, se concatenan.
        return String.join("\n\n", unicas);
    }

    private List<String> extraerImagenes(Document doc) {
        Elements imgs = doc.select(SELECTOR_IMAGENES);
        Set<String> unicas = new LinkedHashSet<>();
        for (Element img : imgs) {
            String mejor = resolverMejorUrl(img);
            if (mejor == null || mejor.isBlank()) {
                continue;
            }
            if (!mejor.toLowerCase(Locale.ROOT).contains("/wp-content/uploads/")) {
                continue;
            }
            unicas.add(aTamanoCompleto(mejor));
        }
        return new ArrayList<>(unicas);
    }

    /**
     * Elige la URL de mayor resolución disponible (srcset w, luego src / data-src)
     * y la deja lista para normalizar a full-size.
     */
    private String resolverMejorUrl(Element img) {
        String mejor = null;
        int mejorAncho = -1;

        String srcset = firstNonBlank(img.attr("srcset"), img.attr("data-srcset"));
        if (srcset != null) {
            Matcher matcher = SRCSET_CANDIDATE.matcher(srcset);
            while (matcher.find()) {
                String url = absolutizar(img, matcher.group(1));
                int ancho = Integer.parseInt(matcher.group(2));
                if (ancho > mejorAncho) {
                    mejorAncho = ancho;
                    mejor = url;
                }
            }
        }

        if (mejor != null) {
            return mejor;
        }

        if (img.hasAttr("src")) {
            String absSrc = img.absUrl("src");
            if (!absSrc.isBlank()) {
                return absSrc;
            }
        }

        return absolutizar(
                img,
                firstNonBlank(img.attr("data-src"), img.attr("data-lazy-src"), img.attr("src")));
    }

    private String absolutizar(Element img, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
            return url.startsWith("//") ? "https:" + url : url;
        }
        img.attr("data-wp-abs-tmp", url);
        String abs = img.absUrl("data-wp-abs-tmp");
        img.removeAttr("data-wp-abs-tmp");
        return abs.isBlank() ? url : abs;
    }

    static String aTamanoCompleto(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        int query = url.indexOf('?');
        String path = query >= 0 ? url.substring(0, query) : url;
        String queryPart = query >= 0 ? url.substring(query) : "";
        return WP_SIZE_SUFFIX.matcher(path).replaceFirst("") + queryPart;
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
