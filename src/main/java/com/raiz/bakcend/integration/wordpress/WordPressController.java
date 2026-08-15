package com.raiz.bakcend.integration.wordpress;

import com.raiz.bakcend.integration.wordpress.client.WordPressClient;
import com.raiz.bakcend.integration.wordpress.dto.WordPressImportabilidad;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageClasificacionResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressImageImportResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPropertyImportRequest;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPropertyPreviewResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPublishResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressUnpublishResponse;
import com.raiz.bakcend.integration.wordpress.service.WordPressPageClassifierService;
import com.raiz.bakcend.integration.wordpress.service.WordPressPropertyImageImportService;
import com.raiz.bakcend.integration.wordpress.service.WordPressPropertyImportService;
import com.raiz.bakcend.integration.wordpress.service.WordPressPropertyPreviewParser;
import com.raiz.bakcend.integration.wordpress.service.WordPressPropertyPublishService;
import com.raiz.bakcend.model.Propiedad;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/integration/wordpress")
public class WordPressController {

    private final WordPressClient wordPressClient;
    private final WordPressPageClassifierService classifierService;
    private final WordPressPropertyPreviewParser previewParser;
    private final WordPressPropertyImportService importService;
    private final WordPressPropertyImageImportService imageImportService;
    private final WordPressPropertyPublishService publishService;

    public WordPressController(
            WordPressClient wordPressClient,
            WordPressPageClassifierService classifierService,
            WordPressPropertyPreviewParser previewParser,
            WordPressPropertyImportService importService,
            WordPressPropertyImageImportService imageImportService,
            WordPressPropertyPublishService publishService) {
        this.wordPressClient = wordPressClient;
        this.classifierService = classifierService;
        this.previewParser = previewParser;
        this.importService = importService;
        this.imageImportService = imageImportService;
        this.publishService = publishService;
    }

    @GetMapping("/pages")
    public List<WordPressPageResponse> listarPaginas() {
        return wordPressClient.listarPaginas();
    }

    @GetMapping("/pages/clasificacion")
    public List<WordPressPageClasificacionResponse> clasificarPaginas() {
        return classifierService.clasificar(wordPressClient.listarPaginas());
    }

    @GetMapping("/pages/{id}/preview")
    public WordPressPropertyPreviewResponse previewPagina(@PathVariable Long id) {
        WordPressPageResponse pagina = wordPressClient.obtenerPagina(id);
        WordPressPageClasificacionResponse clasificacion = classifierService.clasificarUna(pagina);

        if (clasificacion.clasificacion() != WordPressImportabilidad.IMPORTABLE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La página no es IMPORTABLE: " + clasificacion.motivo());
        }

        return previewParser.parse(pagina);
    }

    @PostMapping("/pages/{id}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public Propiedad importarPagina(
            @PathVariable Long id,
            @RequestBody WordPressPropertyImportRequest request,
            Authentication authentication) {
        return importService.importar(id, request, authentication);
    }

    @PostMapping("/pages/{id}/import/images")
    public WordPressImageImportResponse importarImagenes(
            @PathVariable Long id,
            Authentication authentication) {
        return imageImportService.importarImagenes(id, authentication);
    }

    @PostMapping("/propiedades/{propiedadId}/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public WordPressPublishResponse publicarPropiedad(
            @PathVariable UUID propiedadId,
            Authentication authentication) {
        return publishService.publicar(propiedadId, authentication);
    }

    @PostMapping("/propiedades/{propiedadId}/unpublish")
    public WordPressUnpublishResponse despublicarPropiedad(
            @PathVariable UUID propiedadId,
            Authentication authentication) {
        return publishService.despublicar(propiedadId, authentication);
    }
}
