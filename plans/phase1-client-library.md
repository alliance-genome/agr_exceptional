# Phase 1 — Client Library (MVP)

Goal: A working JAR that captures uncaught exceptions and sends them to the server via HTTP POST.

## Done

- [x] Project structure (multi-module Maven, Java 21)
- [x] ExceptionCatcher — initialize with endpoint + service name, sets uncaught exception handler
- [x] HTTP POST to server `/ingest` endpoint using Java HttpClient
- [x] `report(Throwable)` for manually reporting caught exceptions
- [x] Build verification (mvn compile passes)

## Design Decisions

- No client-side fingerprinting/signature — the server handles exception grouping via textual similarity
- Client is a dumb sender: timestamp, service, host, type, message, stacktrace
- Zero external dependencies (uses only java.net.http.HttpClient)
- Three Maven modules under groupId `org.alliancegenome.exceptional`:
  - `model` — shared ExceptionReport model (client + server depend on this)
  - `client` — client library JAR (what services depend on)
  - `server` — Quarkus server

## TODO

- [ ] Finalize JSON payload format
  - timestamp, service, host, type, message, stacktrace
  - Consider adding: thread name, JVM version, environment label
- [ ] Async dispatch — don't block the throwing thread
  - Currently uses `sendAsync` but no queue/executor management
  - Add a bounded queue + single background sender thread
- [ ] Rate limiting / dedup
  - Track recent exception messages, suppress duplicates within a time window
  - Prevent flooding the server with the same exception in a loop
- [ ] Configurable endpoint
  - Support env var (`AGR_EXCEPTIONAL_ENDPOINT`) and system property
  - Fallback to stderr if endpoint not configured
- [ ] Unit tests
  - JSON payload format
  - Rate limiting / dedup logic
- [ ] Maven build produces a clean JAR with no transitive dependencies
- [ ] Publish to Maven Central
  - Group ID: `org.alliancegenome.exceptional`
  - Sonatype OSSRH account + namespace verification for `org.alliancegenome.exceptional`
  - POM metadata: name, description, url, license, scm, developers
  - maven-source-plugin (attach sources JAR)
  - maven-javadoc-plugin (attach javadoc JAR)
  - maven-gpg-plugin (sign artifacts)
  - nexus-staging-maven-plugin (deploy to OSSRH → release to Central)
  - GitHub Actions workflow: build, sign, publish on tag/release
