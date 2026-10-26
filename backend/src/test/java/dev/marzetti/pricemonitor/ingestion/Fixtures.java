package dev.marzetti.pricemonitor.ingestion;

import dev.marzetti.pricemonitor.catalog.StoreInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Utilidades para tests de connectors. */
public final class Fixtures {

    /**
     * Respuesta real de la API de catálogo de Mega Sports (26/10/2026), recortada a los campos
     * que usa el sistema. Producto: Kappa Termoli FG, 7 talles, 5 con stock.
     */
    public static final String MEGASPORTS_TERMOLI = "fixtures/vtex/megasports-product-termoli.json";

    private Fixtures() {
    }

    public static String read(String classpathLocation) {
        try (InputStream in = Fixtures.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (in == null) {
                throw new IllegalArgumentException("No existe el fixture " + classpathLocation);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static StoreInfo vtexStore(String code, String baseUrl) {
        return new StoreInfo(1L, code, code, URI.create(baseUrl), "VTEX", true, "AR",
                Duration.ofSeconds(3), Duration.ofHours(1));
    }
}
