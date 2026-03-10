# Phase 2 — Server Ingest + Storage

Goal: A Quarkus server that receives exception reports, buffers them in memory, and stores them in S3 for historical querying via Athena.

## TODO

- [ ] `/ingest` endpoint — parse and validate exception JSON
  - Accept POST with Content-Type application/json
  - Validate required fields (timestamp, signature, type, message, stacktrace, service)
  - Return 202 Accepted on success, 400 on bad payload
- [ ] In-memory ring buffer for recent exceptions
  - Thread-safe bounded deque
  - Query by time range, service, exception type
  - Configurable size (env var)
- [ ] S3 storage
  - Write gzip JSON files partitioned by `dt=YYYY-MM-DD/hour=HH/`
  - Batch writes (accumulate N exceptions or T seconds, then flush)
  - S3 client via AWS SDK
- [ ] Athena integration
  - Glue table with partition projection (same pattern as agr_logs)
  - Query historical exceptions by time range, service, type, message search
  - Pagination support
- [ ] REST API endpoints
  - `GET /api/exceptions` — query (routes to buffer or Athena based on time range)
  - `GET /api/stream` — SSE for live tail of new exceptions
  - `GET /api/services` — list services that have reported exceptions
  - `GET /health` — buffer stats, uptime
- [ ] Bearer token auth on `/ingest` (optional, configurable)
- [ ] Configuration via application.properties / env vars
  - S3 bucket, AWS region, Athena database/table/workgroup
  - Buffer size, ingest secret, port
