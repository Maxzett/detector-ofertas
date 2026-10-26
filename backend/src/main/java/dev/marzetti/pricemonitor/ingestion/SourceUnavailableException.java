package dev.marzetti.pricemonitor.ingestion;

/** La tienda no respondió o respondió con un error (timeout, 5xx, contenido inesperado). */
public class SourceUnavailableException extends RuntimeException {

    public SourceUnavailableException(String message) {
        super(message);
    }

    public SourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
