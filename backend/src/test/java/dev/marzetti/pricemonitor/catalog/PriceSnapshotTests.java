package dev.marzetti.pricemonitor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PriceSnapshotTests {

    private static PriceSnapshot snapshot(String price, String listPrice, boolean inStock) {
        return new PriceSnapshot(
                price == null ? null : new BigDecimal(price),
                listPrice == null ? null : new BigDecimal(listPrice),
                inStock);
    }

    @Test
    void sameAmountWithDifferentScaleIsNotAChange() {
        // El JSON de la tienda trae 44499; la columna NUMERIC(14,2) devuelve 44499.00
        assertThat(snapshot("44499", "62999", true).differsFrom(snapshot("44499.00", "62999.00", true)))
                .isFalse();
    }

    @Test
    void priceChangeIsAChange() {
        assertThat(snapshot("39999", "62999", true).differsFrom(snapshot("44499", "62999", true))).isTrue();
    }

    @Test
    void listPriceChangeIsAChange() {
        assertThat(snapshot("44499", "59999", true).differsFrom(snapshot("44499", "62999", true))).isTrue();
    }

    @Test
    void stockChangeIsAChange() {
        assertThat(snapshot("44499", "62999", false).differsFrom(snapshot("44499", "62999", true))).isTrue();
    }

    @Test
    void appearingOrDisappearingPriceIsAChange() {
        assertThat(snapshot(null, null, false).differsFrom(snapshot("44499", null, false))).isTrue();
        assertThat(snapshot(null, null, false).differsFrom(snapshot(null, null, false))).isFalse();
    }

    @Test
    void anythingDiffersFromNoPreviousState() {
        assertThat(snapshot("44499", "62999", true).differsFrom(null)).isTrue();
    }

    @Test
    void computesAdvertisedDiscount() {
        // Caso real de Mega Sports: Kappa Termoli FG, $44.499 con lista de $62.999
        assertThat(snapshot("44499", "62999", true).advertisedDiscountPct())
                .contains(new BigDecimal("29.37"));
    }

    @Test
    void noDiscountWhenListPriceIsMissingOrNotHigher() {
        assertThat(snapshot("44499", null, true).advertisedDiscountPct()).isEmpty();
        assertThat(snapshot("44499", "44499", true).advertisedDiscountPct()).isEmpty();
        assertThat(snapshot("44499", "40000", true).advertisedDiscountPct()).isEmpty();
    }

    @Test
    void rejectsNegativePrices() {
        assertThatThrownBy(() -> snapshot("-1", null, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
