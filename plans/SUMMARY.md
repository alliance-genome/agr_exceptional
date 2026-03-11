# AGR Exceptional — Phase Summary

## Phase 1 — Client Library (MVP)
**Status: Complete**

Java client JAR that captures uncaught exceptions and sends them to the server via HTTP POST.

- [x] Multi-module Maven project (model, client, server)
- [x] ExceptionCatcher with rescu REST proxy
- [x] ExceptionReport model with Lombok
- [x] Shared ExceptionResourceInterface (Jandex indexed)
- [x] Basic working end-to-end (client → server)
- [x] Async dispatch + dedup (60s window)
- [x] Configurable endpoint via env var (AGR_EXCEPTIONAL_ENDPOINT)
- [x] Shutdown hook to drain pending sends
- [x] Unit tests (5 passing)
- [x] Published to Maven Central (0.1.0)

## Phase 2 — Server Ingest, Grouping + Storage
**Status: Complete**

Server groups exceptions by stacktrace similarity using Bedrock Titan embeddings, stores in DynamoDB.

- [x] Bedrock Titan Text Embeddings V2 integration (EmbeddingService)
- [x] Cosine similarity matching against group average embeddings (configurable threshold)
- [x] In-memory embedding cache with DynamoDB backing (EmbeddingCache)
- [x] DynamoDB tables with auto-creation and waiters (exception_groups, exception_reports)
- [x] Group lifecycle (active / resolved / archived) with auto-reopen
- [x] Group management REST endpoints (list, detail, reports, resolve, archive, reopen)
- [x] 90-day TTL on all records
- [x] AWS profile via AWS_PROFILE env var

## Phase 3 — OpenAPI Annotations + Swagger UI
**Status: Complete**

Server is a pure API with Swagger. UI lives in agr_logs repo.

- [x] MicroProfile OpenAPI dependency in model module
- [x] @Schema annotations on ExceptionGroup and ExceptionReport
- [x] @Operation/@Tag/@APIResponse on ExceptionResourceInterface (ingest, health)
- [x] @Operation/@Tag/@APIResponse/@Parameter on GroupResource (all 6 endpoints)
- [x] @OpenAPIDefinition on RestApplication with API title/description/version
- [x] Swagger UI enabled (quarkus.swagger-ui.always-include=true)

## Phase 4 — Exception Viewer UI
**Status: Complete**

Exception viewer UI in agr_logs repo, calling agr_exceptional API cross-origin.

- [x] CORS enabled on server (dev: all origins, prod: logs.alliancegenome.org)
- [x] Groups list view sorted by count descending, with status filter tabs
- [x] Group detail modal (85vw) with summary, services, hosts, reports
- [x] Expandable stacktraces with inline arrow toggle
- [x] Resolve / archive / reopen actions with confirmation dialogs
- [x] Nav tabs linking between Logs and Exceptions pages
- [x] Flask route serving /exceptions page

## Phase 5 — Deployment
**Status: Complete**

Internal-only Lambda deployed via CDK (Java).

- [x] Quarkus Lambda adapter (quarkus-amazon-lambda-rest → function.zip)
- [x] CDK stack (Lambda, Private API Gateway, VPC endpoint, EventBridge warmup)
- [x] VPC-internal only via Private API Gateway + execute-api VPC endpoint
- [x] Private custom domain name (exceptions.alliancegenome.org) with ACM wildcard cert
- [x] SnapStart enabled (~1s cold start restore, ~6-45ms warm)
- [x] EventBridge warmup rule (every 5 min)
- [x] IAM policies (DynamoDB CRUD + TTL, Bedrock InvokeModel)
- [x] Route53 A record alias in private hosted zone
- [x] GitHub Actions workflow (Maven Central publish on release)
- [x] Makefile targets (build, mvndeploy, cdkdeploy)

## Phase 6 — Integration
**Status: In Progress**

Client library integrated into AGR Java services.

- [x] Published to Maven Central (0.1.0)
- [x] Add to agr_curation (PR [#2532](https://github.com/alliance-genome/agr_curation/pull/2532))
- [x] Add to agr_java_software — indexers + API (PR [#1513](https://github.com/alliance-genome/agr_java_software/pull/1513))
- [x] ExceptionCatcher.report() throughout indexers
- [ ] End-to-end verification with real services (after PRs merge)

## Phase 7 — Enhancements
**Status: Not Started**

Production hardening and quality-of-life.

- [ ] GraalVM native image build (faster cold starts)
- [ ] Automated CDK deploy from GitHub Actions (on release)
- [ ] Alerting (new groups, reopened groups → Slack/SNS)
- [ ] Richer payload (JVM version, git commit hash)
- [ ] Quarkus CDI auto-initialization
- [ ] Trend data per group
- [ ] Demo mode
- [ ] MCP tool integration
