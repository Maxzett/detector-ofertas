package dev.marzetti.pricemonitor.ingestion.port;

/** Capacidades que puede ofrecer una fuente (§11 del documento de requerimientos). */
public enum ConnectorCapability {
    /** Buscar productos por texto o categoría. */
    SEARCH,
    /** Traer un producto por su URL o id. */
    FETCH_BY_URL,
    /** Informa el precio de lista además del precio actual. */
    LIST_PRICE,
    /** Informa stock por variante (talle). */
    STOCK_PER_VARIANT
}
