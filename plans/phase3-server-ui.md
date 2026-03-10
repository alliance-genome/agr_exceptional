# Phase 3 — Server UI

Goal: A web viewer for browsing and searching exceptions, served from the Quarkus app.

## TODO

- [ ] Static HTML/CSS/JS served from Quarkus (META-INF/resources or static route)
- [ ] Live tail via SSE
  - New exceptions appear in real-time
  - Auto-scroll with pause on user scroll
- [ ] Filtering
  - By service name (dropdown populated from /api/services)
  - By exception type
  - By time range (preset: 5m, 15m, 1h, 6h, 24h, 7d)
  - By message text (search box)
- [ ] Exception detail view
  - Full stacktrace display
  - Metadata (service, host, timestamp, signature)
- [ ] Exception grouping by signature
  - Show occurrence count, first seen, last seen
  - Click to expand individual occurrences
- [ ] Responsive layout
  - Similar style to agr_logs viewer (dark theme, scanlines optional)
