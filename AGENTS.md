# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Spring Boot 4 / Java 17 backend, a Vue admin frontend, and a Vue/Capacitor mobile app.

- `src/main/java/org/dsb/fundvaluation/`: backend controllers, services, DTOs, models, and config.
- `src/main/resources/`: application config, bundled fund data, and built admin assets.
- `src/test/java/org/dsb/fundvaluation/`: backend unit and MVC tests.
- `frontend/`: Vue 3 admin UI. Its production build writes to `src/main/resources/static/admin/`.
- `mobile/`: Vue 3 mobile client and Capacitor scripts.
- `data/funds/`: local writable fund JSON files for admin CRUD.

## Build, Test, and Development Commands

- `./mvnw test` or `.\mvnw.cmd test`: run backend tests.
- `./mvnw package` or `.\mvnw.cmd package`: build the Spring Boot JAR.
- `cd frontend && npm run dev`: start admin UI with `/api` proxied to `localhost:5000`.
- `cd frontend && npm run build`: build admin assets into the backend static directory.
- `cd mobile && npm run dev`: start the mobile web dev server.
- `cd mobile && npm test`: run mobile Node test suites.
- `cd mobile && npm run build`: build mobile web assets.

On Windows PowerShell, use `npm.cmd` if script execution policy blocks `npm.ps1`.

## Coding Style & Naming Conventions

Use 4-space indentation for Java and 2-space indentation for Vue, JavaScript, JSON, and YAML. Keep Java packages under `org.dsb.fundvaluation`; name services `*Service`, controllers `*Controller`, and tests `*Test`. API JSON uses snake_case via Jackson `SNAKE_CASE`. Prefer explicit DTOs for public API contracts and keep external parsing inside service classes.

## Testing Guidelines

Backend tests use Spring Boot test support and `*Test` classes under `src/test/java`. Add focused tests when changing valuation math, file persistence, refresh behavior, or controller responses. Mobile tests use Node's built-in runner in `mobile/test/*.test.js`. Run relevant backend and frontend/mobile tests before opening a PR.

## Commit & Pull Request Guidelines

Git history uses short summaries; continue with concise, imperative messages such as `Validate fund file names` or `Add mobile snapshot tests`. PRs should include a description, affected areas, verification commands, and screenshots for UI changes. Link issues when applicable and call out config, deployment, or data migration changes.

## Security & Configuration Tips

Do not hardcode secrets or production credentials. Keep writable fund data under `fund.data.dir`, and validate fund codes before file operations. Treat `/api/admin/**` as sensitive: protect it with authentication in production and restrict CORS origins. Redis is required by the app configuration; document host, port, and operational expectations for each environment.

## Change Tracking

Every repository change must update this file. Add one entry to the iteration log with the next iteration number, the iteration date, and a short summary of what changed. Include code, config, documentation, asset, dependency, and generated-output changes.

## Iteration Log

| Iteration | Date | Summary |
| --- | --- | --- |
| 1 | 2026-05-26 | Created the contributor guide and added the required change-tracking rule. |
| 2 | 2026-05-29 | Improved mobile valuation refresh, overseas valuation data normalization, news filtering display, contribution analysis, and touch scrolling behavior. |
| 3 | 2026-05-29 | Fixed mobile overseas valuation white screen by exposing template formatters and hardening polling/data normalization. |
| 4 | 2026-05-29 | Fixed A-share valuation change percentages staying at 0 before NAV refresh by calculating fund change from live quotes. |
| 5 | 2026-06-01 | Added in-app mobile news viewing, fixed overseas cached close valuation parsing, and polished mobile fund/news card styling. |
| 6 | 2026-06-02 | Rechecked overseas valuation, corrected US market session detection, and filled missing cached quote symbols before calculation. |
| 7 | 2026-06-02 | Adjusted ssh-tool deployment operations to redirect remote script output that breaks Windows GBK console encoding. |
| 8 | 2026-06-02 | Added an optional Maven deploy profile that runs ssh-tool after package when explicitly enabled. |
