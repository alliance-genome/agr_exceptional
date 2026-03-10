# Phase 6 — Enhancements

Goal: Production hardening and quality-of-life features.

## TODO

- [ ] Exception aggregation
  - Group by signature, show first seen / last seen / count
  - Trend over time (spark line or bar chart)
- [ ] Alerting on new exception signatures
  - Notify when a never-before-seen signature appears
  - Webhook, Slack, or email integration
- [ ] Resolved / acknowledged status
  - Mark a signature as resolved
  - Re-open automatically if it recurs
- [ ] Environment context in payload
  - JVM version, OS, env vars, Quarkus profile
  - Git commit hash of the reporting service
- [ ] Quarkus CDI auto-initialization
  - @Startup bean that reads config and initializes ExceptionCatcher
  - Zero-code integration: just add the JAR + set env vars
- [ ] Retention policies
  - S3 lifecycle rules (configurable per environment)
  - Buffer TTL for resolved exceptions
- [ ] Demo mode for local development
  - Generate fake exceptions for UI testing without a real client
- [ ] MCP tool integration
  - Query exceptions from Claude Code (like the logs MCP tool)
