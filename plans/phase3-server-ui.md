# Phase 3 — API Documentation + UI in agr_logs

Goal: Server is a pure API with Swagger/OpenAPI. UI lives in the agr_logs repo.

## Server (this repo)

- [ ] OpenAPI/Swagger enabled (already have quarkus-smallrye-openapi)
- [ ] All group and report endpoints documented with OpenAPI annotations
  - Descriptions, response codes, example payloads
- [ ] Swagger UI accessible at `/swagger-ui` in dev/stage
- [ ] Disable Swagger UI in production (serve API only)

## UI (agr_logs repo)

- [ ] New section/tab in the agr_logs viewer for exceptions
- [ ] Groups list view
  - Default: show active groups
  - Filter by status (active/resolved/archived)
  - Filter by service
  - Filter by time range
  - Columns: exception type, latest message, count, first seen, last seen, service
- [ ] Group detail view
  - Click group to see list of individual exceptions
  - Each shows timestamp, host, service, message
  - Click exception to see full stacktrace
- [ ] Group actions
  - Resolve button (mark as taken care of)
  - Archive button (permanently dismiss)
  - Reopen button (on resolved groups)
- [ ] API integration
  - Calls agr_exceptional server endpoints
  - Configurable server URL
