package dev.marzetti.pricemonitor.ingestion;

/**
 * Resultado de importar un producto por URL.
 *
 * @param offersSeen    variantes (talles) encontradas
 * @param offersCreated variantes nuevas
 * @param offersChanged variantes cuyo precio o stock cambió desde la última consulta
 */
public record ImportResult(
        String storeCode,
        String externalId,
        String title,
        int offersSeen,
        int offersCreated,
        int offersChanged) {
}
