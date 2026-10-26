package dev.marzetti.pricemonitor.ingestion;

import java.net.URI;

/** La tienda respondió, pero no tiene el producto pedido. */
public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(URI productUrl) {
        super("La tienda no tiene el producto " + productUrl);
    }
}
