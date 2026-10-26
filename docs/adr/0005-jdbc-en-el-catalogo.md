# ADR 0005: JDBC explícito para las escrituras del catálogo

- **Estado:** aceptada
- **Fecha:** 2026-10-26

## Contexto

Las escrituras del catálogo tienen una forma particular:

- Son upserts por clave natural (`(store_id, external_id)` para el listing y `(store_id, external_sku_id)` para la offer), y se repiten en cada consulta a una tienda.
- Cada observación tiene que leer el estado actual de la offer, compararlo y decidir si escribe historial. Todo dentro de una transacción, con la fila bloqueada para que dos procesos no evalúen el mismo cambio.
- Usan funciones específicas de PostgreSQL: `ON CONFLICT ... DO UPDATE ... RETURNING`, `FOR UPDATE` y operadores sobre JSONB.

## Decisión

El módulo `catalog` accede a la base con `JdbcClient` y SQL explícito, en repositorios del subpaquete `infrastructure`. JPA queda disponible para módulos con CRUD clásico, como `alerts` en la Etapa 3.

## Consecuencias

- Cada escritura es un statement predecible: un upsert sin la ventana de carrera entre "buscar" e "insertar", y sin el `SELECT` previo que haría un `merge` de JPA.
- No hay mapeo de JSONB por Hibernate: los valores de `store.config` se extraen en SQL (`config ->> 'sizeScale'`).
- A cambio, el mapeo fila → objeto es manual y el SQL no se valida al compilar. Lo cubren los tests de integración contra PostgreSQL real (Testcontainers).
