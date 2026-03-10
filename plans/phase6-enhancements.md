# Phase 6 — Enhancements

Goal: Production hardening and quality-of-life features.

## TODO

- [ ] Alerting on new exception groups
  - Notify when a never-before-seen group is created
  - Slack webhook or SNS integration
- [ ] Alerting on resolved group reopened
  - Notify when a "fixed" exception comes back
- [ ] Environment context in payload
  - JVM version, OS, Quarkus profile
  - Git commit hash of the reporting service
  - Additional metadata map for custom fields
- [ ] Quarkus CDI auto-initialization
  - @Startup bean that reads config and initializes ExceptionCatcher
  - Zero-code integration: just add the JAR + set env vars
- [ ] Trend data on groups
  - Track exception count per hour/day
  - Expose via API for UI spark lines or charts
- [ ] Demo mode for local development
  - Server generates fake exception groups for UI testing
- [ ] MCP tool integration
  - Query exception groups from Claude Code (like the logs MCP tool)
  - View active groups, drill into reports
