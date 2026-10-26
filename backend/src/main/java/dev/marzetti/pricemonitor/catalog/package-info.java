/**
 * Módulo <b>catalog</b>: tiendas, listings, offers e historial de precios.
 *
 * <p>Es el conocimiento persistente del sistema. {@link dev.marzetti.pricemonitor.catalog.OfferUpdateService}
 * aplica la regla "solo se guardan cambios" en {@code price_history} y publica
 * {@link dev.marzetti.pricemonitor.catalog.OfferChanged}.</p>
 *
 * <p><b>API pública:</b> los tipos de este paquete raíz. El subpaquete {@code infrastructure}
 * (acceso a datos con JDBC) es interno y Spring Modulith impide usarlo desde otros módulos.</p>
 */
package dev.marzetti.pricemonitor.catalog;
