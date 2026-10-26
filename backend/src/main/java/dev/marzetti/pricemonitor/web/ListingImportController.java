package dev.marzetti.pricemonitor.web;

import dev.marzetti.pricemonitor.ingestion.ImportResult;
import dev.marzetti.pricemonitor.ingestion.ListingImporter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno para importar un producto por URL. Sirve para cargar datos y probar
 * connectors a mano; el monitoreo automático llega con los jobs (Etapas 3 y 4).
 * Sin autenticación hasta la Etapa 7: no exponer públicamente.
 */
@RestController
@RequestMapping("/api/internal/listings")
class ListingImportController {

    record ImportRequest(@NotNull URI url) {
    }

    private final ListingImporter importer;

    ListingImportController(ListingImporter importer) {
        this.importer = importer;
    }

    @PostMapping("/import")
    ImportResult importListing(@Valid @RequestBody ImportRequest request) {
        return importer.importByUrl(request.url());
    }
}
