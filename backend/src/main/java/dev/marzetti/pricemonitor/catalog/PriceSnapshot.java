package dev.marzetti.pricemonitor.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Lo que se compara para decidir si una offer cambió: precio actual, precio de lista y stock.
 * Un cambio en cualquiera de los tres genera una fila en {@code price_history} (ADR 0003).
 *
 * @param price     precio de venta; {@code null} si la tienda no lo informa
 * @param listPrice precio de lista ("antes"); {@code null} si la tienda no lo informa
 * @param inStock   si hay stock de esta variante
 */
public record PriceSnapshot(BigDecimal price, BigDecimal listPrice, boolean inStock) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public PriceSnapshot {
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo: " + price);
        }
        if (listPrice != null && listPrice.signum() < 0) {
            throw new IllegalArgumentException("El precio de lista no puede ser negativo: " + listPrice);
        }
    }

    /**
     * Compara por valor numérico y no con {@code equals}: para {@link BigDecimal},
     * {@code 44499} y {@code 44499.00} son distintos con {@code equals}, pero es el mismo precio
     * (uno viene del JSON de la tienda y el otro de una columna NUMERIC(14,2)).
     */
    public boolean differsFrom(PriceSnapshot other) {
        if (other == null) {
            return true;
        }
        return inStock != other.inStock
                || !sameAmount(price, other.price)
                || !sameAmount(listPrice, other.listPrice);
    }

    /**
     * Descuento que anuncia la tienda (precio de lista contra precio actual), en porcentaje
     * con 2 decimales. Vacío si falta alguno de los dos precios o si no hay descuento.
     * Ojo: el precio de lista lo define la tienda y puede estar inflado (ver A5 del análisis).
     */
    public Optional<BigDecimal> advertisedDiscountPct() {
        if (price == null || listPrice == null || listPrice.compareTo(price) <= 0) {
            return Optional.empty();
        }
        return Optional.of(listPrice.subtract(price)
                .multiply(HUNDRED)
                .divide(listPrice, 2, RoundingMode.HALF_UP));
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }
}
