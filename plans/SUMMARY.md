# AGR Exceptional — Phase Summary

## Phase 1 — Client Library (MVP)
**Status: In Progress**

Java client JAR that captures uncaught exceptions and sends them to the server via HTTP POST.

- [x] Multi-module Maven project (model, client, server)
- [x] ExceptionCatcher with rescu REST proxy
- [x] ExceptionReport model with Lombok
- [x] Shared ExceptionResourceInterface (Jandex indexed)
- [x] Basic working end-to-end (client → server)
- [ ] Async dispatch + rate limiting / dedup
- [ ] Configurable endpoint via env var
- [ ] Unit tests
- [ ] Publish to Maven Central

## Phase 2 — Server Ingest, Grouping + Storage
**Status: Not Started**

Server groups exceptions by stacktrace similarity using Bedrock Titan embeddings, stores in DynamoDB.

- [ ] Bedrock Titan Text Embeddings V2 integration
- [ ] Cosine similarity matching against group average embeddings
- [ ] DynamoDB tables (exception_groups, exception_reports)
- [ ] Group lifecycle (active / resolved / archived)
- [ ] Group management REST endpoints
- [ ] 90-day TTL on all records

## Phase 3 — API Documentation + UI in agr_logs
**Status: Not Started**

Server is a pure API with Swagger. UI lives in agr_logs repo.

- [ ] OpenAPI annotations on all endpoints
- [ ] Swagger UI in dev/stage
- [ ] Groups list view in agr_logs UI
- [ ] Group detail + exception drill-down
- [ ] Resolve / archive / reopen actions

## Phase 4 — Deployment
**Status: Not Started**

Internal-only Lambda deployed via CDK (Java).

- [ ] Quarkus Lambda adapter
- [ ] CDK stack (Lambda, DynamoDB, IAM, EventBridge warmup)
- [ ] VPC-internal only, no public access
- [ ] Native image build (optional)
- [ ] GitHub Actions CI/CD

## Phase 5 — Integration
**Status: Not Started**

Client library integrated into AGR Java services.

- [ ] Publish to Maven Central (0.0.x versioning)
- [ ] Add to agr_curation, agr_java_software, agr_api
- [ ] End-to-end verification with real services
- [ ] ExceptionCatcher.report() for key caught exceptions

## Phase 6 — Enhancements
**Status: Not Started**

Production hardening and quality-of-life.

- [ ] Alerting (new groups, reopened groups → Slack/SNS)
- [ ] Richer payload (JVM version, git commit hash)
- [ ] Quarkus CDI auto-initialization
- [ ] Trend data per group
- [ ] Demo mode
- [ ] MCP tool integration
