# ADR 0003: El historial guarda cambios, no snapshots

- **Estado:** aceptada
- **Fecha:** 2026-10-26

## Contexto

RNF-ESC-03 pide no guardar información redundante. Pero si solo se guardan los cambios, no se distingue "el precio no cambió" de "no se consultó" o de "el producto desapareció".

## Decisión

- `price_history` recibe una fila solo cuando cambia alguno de `price`, `list_price` o `in_stock`.
- El estado actual vive desnormalizado en `offer`: `current_price`, `list_price` e `in_stock`. Las consultas operativas nunca leen el historial.
- `offer.last_checked_at` (última consulta) y `offer.last_seen_at` (última vez que la tienda lo devolvió) resuelven la ambigüedad.

## Consecuencias

- El historial crece con los cambios reales y no con la frecuencia de consulta.
- Si el volumen lo pide, `price_history` se puede particionar por mes sin tocar el resto del modelo.
- Para graficar un período hay que "extender" el último valor conocido hasta la fecha consultada. Eso es lógica de lectura, no de escritura.
