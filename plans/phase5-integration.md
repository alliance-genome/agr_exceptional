# Phase 5 — Integration

Goal: Client library integrated into AGR Java services, exceptions flowing end-to-end.

## TODO

- [ ] Publish client JAR to AGR Maven repo (or GitHub Packages)
- [ ] Add dependency to agr_curation
  - Initialize in application startup (Quarkus @Startup bean)
  - Configure endpoint and service name ("agr_curation")
- [ ] Add dependency to agr_java_software
  - agr_indexer — initialize on indexer startup
  - agr_variant_indexer — initialize on indexer startup
  - Configure service names per indexer
- [ ] Add dependency to agr_api (if applicable)
- [ ] Configure endpoint per environment
  - Internal ALB DNS or Cloud Map service name
  - Env var: AGR_EXCEPTIONAL_ENDPOINT
- [ ] End-to-end verification
  - Trigger a test exception in each service
  - Verify it appears in the server buffer and UI
  - Verify it persists to S3 and is queryable via Athena
- [ ] Add `ExceptionCatcher.report()` calls for key caught exceptions
  - Bulk load failures in curation
  - Indexer document failures
