/**
 * Módulo <b>alerts</b>: criterios del usuario, matching y deduplicación de avisos.
 * <p>Escucha {@code OfferChanged}, evalúa las alertas candidatas con {@code AlertMatcher}
 * y mantiene el estado por par (alert, offer).</p>
 */
package dev.marzetti.pricemonitor.alerts;
