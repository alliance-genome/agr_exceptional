# Phase 5 — Integration

Goal: Client library integrated into AGR Java services, exceptions flowing end-to-end.

## TODO

- [ ] Publish client + model JARs to Maven Central
  - Sonatype OSSRH setup (from phase 1)
  - First release version (e.g. 0.0.1)
- [ ] Add dependency to agr_curation
  - `org.alliancegenome.exceptional:client`
  - Initialize in application startup (Quarkus @Startup bean)
  - Service name: "agr_curation"
- [ ] Add dependency to agr_java_software
  - agr_indexer — initialize on indexer startup, service name: "agr_indexer"
  - agr_variant_indexer — initialize on startup, service name: "agr_variant_indexer"
- [ ] Add dependency to agr_api (if applicable)
- [ ] Configure endpoint per environment
  - Internal Lambda URL or ALB DNS
  - Env var: `AGR_EXCEPTIONAL_ENDPOINT`
  - Set via ECS task definition / environment config
- [ ] End-to-end verification
  - Trigger a test exception in each service
  - Verify it arrives at the server and creates a group in DynamoDB
  - Verify it appears in the agr_logs UI exceptions tab
  - Verify grouping works (send same exception twice, confirm single group)
- [ ] Add `ExceptionCatcher.report()` calls for key caught exceptions
  - Bulk load failures in curation
  - Indexer document failures
  - API request handling errors
