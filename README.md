# Detector de Ofertas

Monitor de precios para tiendas online argentinas. El usuario define alertas (por ejemplo, *"botines Nike gama alta, talle 42, menos de $300.000, al menos 20% de descuento"*), el sistema sigue los productos que cumplen esos criterios en varias tiendas, guarda la evolución de sus precios y avisa por Telegram cuando aparece una oportunidad.

> **Estado:** Etapa 1 de 7 (esqueleto). Ver el [plan de etapas](#plan-de-etapas).

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Flyway |
| Base de datos | PostgreSQL 16 |
| Arquitectura | Modular monolith con límites verificados por Spring Modulith |
| Tests | JUnit, Testcontainers (PostgreSQL real), Spring Modulith |
| Frontend | React + TypeScript (Etapa 6) |
| Notificaciones | Telegram Bot API (Etapa 5) |
| Infra | Docker Compose, GitHub Actions |

## Arquitectura

Un único deployable dividido en módulos de negocio. Solo `ingestion` habla con las tiendas y solo `notifications` habla con Telegram. Los módulos se encadenan por eventos:

```
ingestion ──> catalog ──(OfferChanged)──> alerts ──> notifications ──> Telegram
    │            │
 tiendas     PostgreSQL
```

| Módulo | Responsabilidad |
| --- | --- |
| `catalog` | Tiendas, listings, offers e historial de precios |
| `ingestion` | Discovery, refresh y normalización; connectors por plataforma |
| `alerts` | Criterios del usuario, matching y deduplicación |
| `notifications` | Cola de avisos, reintentos y canales |
| `identity` | Usuarios y autenticación |
| `web` | API REST |

Principio rector: **las alertas son necesidades del usuario; los productos y su historial son conocimiento del sistema.** Borrar o editar una alerta nunca toca el historial.

### Fuentes de datos

Cada tienda corre sobre una plataforma de e-commerce distinta, así que hay un connector por **plataforma**, no por tienda:

| Tienda | Plataforma | Vía de acceso | Estado |
| --- | --- | --- | --- |
| Mega Sports | VTEX | API JSON pública | MVP |
| Digital Sport | Sport 78 (propia) | HTML del servidor | MVP |
| Dexter / Stock Center / Moov | Salesforce Commerce Cloud | JSON del storefront | Siguiente |
| Solo Deportes | Magento 2 | HTML del servidor | Siguiente |
| adidas | SFCC + Next.js | JSON-LD | Parcial |
| Nike | VTEX | — | Descartada (anti-bot) |

El detalle de cada fuente está en [`docs/sources/`](docs/sources/README.md). El proyecto respeta robots.txt y no evade protecciones anti-bot ([ADR 0004](docs/adr/0004-uso-responsable-de-fuentes.md)).

### Decisiones de arquitectura

- [ADR 0001: Modular monolith con límites verificados](docs/adr/0001-modular-monolith.md)
- [ADR 0002: Un connector por plataforma, no por tienda](docs/adr/0002-conectores-por-plataforma.md)
- [ADR 0003: El historial guarda cambios, no snapshots](docs/adr/0003-historial-solo-cambios.md)
- [ADR 0004: Uso responsable de las fuentes](docs/adr/0004-uso-responsable-de-fuentes.md)

## Cómo correrlo

### Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (en Windows, con el backend WSL2)
- JDK 21, solo para correr los tests o la app fuera de Docker. Maven no hace falta: se usa el wrapper (`mvnw`).

### Levantar todo con Docker

```bash
cp .env.example .env      # y editar las credenciales
docker compose up --build
```

Verificar que la app y la base estén arriba:

```bash
curl http://localhost:8080/actuator/health
```

### Tests

Los tests de integración levantan un PostgreSQL real con Testcontainers, así que Docker tiene que estar corriendo.

```bash
cd backend
./mvnw verify        # Linux / macOS / Git Bash
mvnw.cmd verify      # Windows (cmd o PowerShell)
```

### Desarrollo sin Docker Compose

Correr `TestPriceMonitorApplication` (en `src/test/java`) desde el IDE. Levanta la app con un PostgreSQL de Testcontainers, sin configurar nada más.

## Estructura del repositorio

```
.
├── backend/                 # Spring Boot (un paquete por módulo)
│   └── src/main/resources/db/migration/   # Flyway
├── docs/
│   ├── adr/                 # decisiones de arquitectura
│   └── sources/             # fichas de cada tienda evaluada
├── .github/workflows/       # CI
└── docker-compose.yml
```

## Plan de etapas

- [x] **Etapa 0:** evaluación de fuentes ([fichas](docs/sources/README.md))
- [x] **Etapa 1:** esqueleto: módulos, schema, Docker Compose y CI
- [ ] **Etapa 2:** catálogo y connector VTEX
- [ ] **Etapa 3:** alertas y discovery
- [ ] **Etapa 4:** refresh y robustez (rate limit, circuit breaker, observabilidad)
- [ ] **Etapa 5:** evaluación de alertas y notificaciones por Telegram
- [ ] **Etapa 6:** frontend
- [ ] **Etapa 7:** seguridad, deploy y demo
