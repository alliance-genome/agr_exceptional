# AGR Exceptional

Exception capture and reporting system for Alliance of Genome Resources services.

A Java client library that captures exceptions from AGR services and sends them to a central server. The server groups exceptions by stacktrace similarity using AWS Bedrock embeddings and stores them in DynamoDB. Exceptions are automatically grouped, can be marked as resolved or archived, and expire after 90 days.

## Modules

| Module | Description |
|--------|-------------|
| `model` | Shared interfaces and models (ExceptionReport, ExceptionResourceInterface) |
| `client` | Java client library — drop into any AGR service to capture and report exceptions |
| `server` | Quarkus REST server — receives exceptions, computes embeddings, manages groups |
| `cdk` | AWS CDK stack — Lambda, Private API Gateway, VPC endpoint, Route53 |

## Quick Start

### Build

```bash
make
```

### Run the server locally

```bash
cd server
mvn quarkus:dev
```

Server starts on `http://localhost:8080`. Swagger UI available at `http://localhost:8080/swagger-ui`.

### Deploy

```bash
make cdkdeploy
```

Deploys as a Lambda behind a Private API Gateway, accessible within the VPC at `https://exceptions.alliancegenome.org/api`.

### Use the client

Add the dependency:

```xml
<dependency>
    <groupId>org.alliancegenome.exceptional</groupId>
    <artifactId>client</artifactId>
    <version>0.1.0</version>
</dependency>
```

Initialize in your application:

```java
ExceptionCatcher.initialize("https://exceptions.alliancegenome.org", "my-service");
```

Uncaught exceptions are automatically captured. For caught exceptions:

```java
try {
    // ...
} catch (Exception e) {
    ExceptionCatcher.report(e);
}
```

## Architecture

```
AGR Java Services                     AGR Exceptional Server (Lambda)
┌──────────────────┐                 ┌──────────────────────────────┐
│ agr_curation     │──HTTP POST──┐   │  Quarkus REST API            │
│ agr_indexer      │──HTTP POST──┤   │  ├─ /ingest                  │
│ agr_api          │──HTTP POST──┼──▶│  ├─ Bedrock Titan Embeddings │
│ any Java service │──HTTP POST──┘   │  ├─ Cosine similarity match  │
└──────────────────┘                 │  ├─ DynamoDB (groups/reports) │
   (client library)                  │  └─ Group management API     │
                                     └──────────────────────────────┘
                                                  │
                                     agr_logs UI (viewer)
```

## Phased Implementation Plan

- [Phase Summary](plans/SUMMARY.md) — overall progress tracker
- [Phase 1 — Client Library](plans/phase1-client-library.md) — Java client JAR (in progress)
- [Phase 2 — Server Ingest + Grouping](plans/phase2-server-ingest-storage.md) — Bedrock embeddings, DynamoDB, group lifecycle
- [Phase 3 — API Docs + UI](plans/phase3-server-ui.md) — Swagger on server, exception viewer in agr_logs
- [Phase 4 — Deployment](plans/phase4-deployment.md) — Lambda via CDK, VPC-internal
- [Phase 5 — Integration](plans/phase5-integration.md) — roll out to AGR services
- [Phase 6 — Enhancements](plans/phase6-enhancements.md) — alerting, auto-init, MCP integration
