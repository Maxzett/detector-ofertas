package dev.marzetti.pricemonitor.ingestion.normalization;

import java.util.Locale;

/**
 * Convierte el talle de una tienda a la escala canónica del sistema (AR).
 *
 * <p>Por ahora solo resuelve tiendas que ya publican en escala AR. Las conversiones
 * US/EU/UK → AR necesitan una tabla por marca (un 9 US de Nike no es igual a un 9 US de adidas)
 * y quedan para cuando se sume una tienda que las necesite.</p>
 */
public final class SizeNormalizer {

    private SizeNormalizer() {
    }

    /**
     * @param raw   talle como lo muestra la tienda
     * @param scale escala de la tienda ({@code store.config.sizeScale})
     * @return talle canónico, o {@code null} si no se puede convertir
     */
    public static String toCanonical(String raw, String scale) {
        if (raw == null || raw.isBlank() || scale == null) {
            return null;
        }
        if (!"AR".equals(scale.toUpperCase(Locale.ROOT))) {
            return null;
        }
        String normalized = raw.trim().replace(',', '.');
        if (normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }
}
