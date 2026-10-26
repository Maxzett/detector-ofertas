package dev.marzetti.pricemonitor.ingestion;

/** La URL no corresponde a ninguna tienda configurada, o su plataforma todavía no tiene connector. */
public class UnsupportedStoreException extends RuntimeException {

    public UnsupportedStoreException(String message) {
        super(message);
    }
}
