# ADR 0004: Uso responsable de las fuentes; no se evaden protecciones

- **Estado:** aceptada
- **Fecha:** 2026-10-26

## Contexto

En la Etapa 0, nike.com.ar respondió JSON a un navegador real, pero **HTTP 403** a un cliente HTTP común (Cloudflare). adidas.com.ar tiene un endpoint de stock por talle que su robots.txt prohíbe explícitamente.

## Decisión

- No se evaden protecciones anti-bot (CAPTCHA, desafíos de Cloudflare, chequeos de IP, login). Si una tienda bloquea, se descarta como fuente directa.
- Se respeta robots.txt: no se consultan rutas que prohíbe, aunque técnicamente respondan.
- Las requests llevan un `User-Agent` propio que identifica al proyecto, con 3 a 5 segundos entre requests a la misma tienda y sin paralelismo por tienda.
- Solo se consulta lo que alguna alerta necesita: el modelo `TRACKED`/`DORMANT` acota el volumen.
- Es un proyecto personal y educativo: no se redistribuyen los datos recolectados.

## Consecuencias

- nike.com.ar no es fuente directa. Los productos Nike se siguen a través de revendedores (Dexter, Digital Sport, Mega Sports).
- adidas.com.ar aporta precio pero no stock por talle.
- Cada decisión queda documentada en `docs/sources/<tienda>.md`, con la fecha de verificación.
