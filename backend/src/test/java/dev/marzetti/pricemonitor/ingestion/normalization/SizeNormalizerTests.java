package dev.marzetti.pricemonitor.ingestion.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SizeNormalizerTests {

    @Test
    void keepsArgentineSizes() {
        assertThat(SizeNormalizer.toCanonical("42", "AR")).isEqualTo("42");
        assertThat(SizeNormalizer.toCanonical(" 39.5 ", "ar")).isEqualTo("39.5");
    }

    @Test
    void normalizesDecimalSeparatorAndTrailingZero() {
        assertThat(SizeNormalizer.toCanonical("39,5", "AR")).isEqualTo("39.5");
        assertThat(SizeNormalizer.toCanonical("42.0", "AR")).isEqualTo("42");
    }

    @Test
    void doesNotGuessOtherScales() {
        assertThat(SizeNormalizer.toCanonical("9", "US")).isNull();
        assertThat(SizeNormalizer.toCanonical("42", null)).isNull();
        assertThat(SizeNormalizer.toCanonical("", "AR")).isNull();
    }
}
