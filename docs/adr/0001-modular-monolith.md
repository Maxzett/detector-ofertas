# ADR 0001: Modular monolith con límites verificados

- **Estado:** aceptada
- **Fecha:** 2026-10-26

## Contexto

El documento de requerimientos (RNF-ESC-04) pide separar discovery, obtención de datos, normalización, persistencia, evaluación de alertas y notificación, para poder escalar cada parte más adelante. El MVP tiene un solo usuario y pocas tiendas.

## Decisión

Un único deployable de Spring Boot dividido en módulos de negocio, cada uno en su paquete raíz: `catalog`, `ingestion`, `alerts`, `notifications`, `identity`, `web` y `shared`.

- Dentro de cada módulo se aplica ports & adapters: `domain`, `application`, `port` e `infrastructure`.
- Los módulos se comunican por llamadas a la API pública del otro módulo o por eventos de Spring (`OfferChanged`).
- Los límites se verifican en un test con Spring Modulith (`ApplicationModules.verify()`). Si un módulo importa una clase interna de otro, el build falla.

## Consecuencias

- Separación equivalente a la de microservices, sin red, sin deploys múltiples y sin consistencia eventual entre bases.
- `ingestion` es el primer candidato a separarse como worker si el volumen crece. Como ya está aislado y se comunica por eventos, el cambio es de deployment, no de diseño.
- Hay que respetar la disciplina de paquetes: el test de modularidad la hace cumplir.
