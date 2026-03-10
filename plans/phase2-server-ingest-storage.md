# Phase 2 — Server Ingest, Grouping + Storage

Goal: Server receives exceptions, computes embeddings via AWS Bedrock Titan, groups by cosine similarity, stores in DynamoDB with 90-day TTL.

## Design

### Ingest Flow

1. Exception arrives at `/ingest`
2. Compute embedding of `stacktrace` via Bedrock Titan Text Embeddings V2 (1,024-dim vector)
3. Load active + resolved group embeddings from DynamoDB
4. Compute cosine similarity between exception embedding and each group's average embedding
5. Best match above threshold → assign to group, update group average embedding
6. No match → create new group, exception embedding becomes group average
7. If match is a resolved group → reopen it to active
8. If match is an archived group → ignore match, create new group

### Group Lifecycle

- **Active** — receiving new exceptions, visible in UI
- **Resolved** — marked by user as "taken care of". New matching exception reopens to active
- **Archived** — user explicitly archives. New matching exceptions create a new group

### Embeddings (AWS Bedrock Titan)

- Model: Amazon Titan Text Embeddings V2
- Output: 1,024-dimension float vector
- Input limit: 8,192 tokens (more than enough for any stacktrace)
- Cost: ~$0.00002 per 1,000 input tokens (~$0.000004 per exception)
- At 100 exceptions/day: effectively free
- Similarity: cosine distance between embedding vectors
- Group average: mean of all member exception embeddings (running average)

### DynamoDB Tables

**exception_groups**
- PK: `group_id` (UUID)
- status: `active` | `resolved` | `archived`
- embedding: list of 1,024 floats (group average embedding)
- exception_count: number of exceptions in this group
- first_seen: timestamp of earliest exception
- last_seen: timestamp of most recent exception
- service: originating service (or list if multiple)
- latest_type: exception class name
- latest_message: most recent exception message
- ttl: epoch seconds (90 days from last_seen)
- GSI on `status` for querying active/resolved groups

**exception_reports**
- PK: `group_id`, SK: `timestamp#report_id`
- report_id: UUID
- timestamp, service, host, type, message, stacktrace
- ttl: epoch seconds (90 days from timestamp)

### Running Average Embedding

When a new exception joins a group:
```
new_avg = (old_avg * old_count + new_embedding) / (old_count + 1)
```
This keeps the group embedding representative as more exceptions accumulate.

## TODO

- [ ] DynamoDB table setup
  - exception_groups table with GSI on status
  - exception_reports table
  - TTL enabled on both tables (90 days)
- [ ] Bedrock integration
  - AWS SDK Bedrock Runtime dependency
  - Titan Text Embeddings V2 client
  - Compute embedding for incoming stacktrace
  - Cosine similarity function
- [ ] Ingest flow
  - `/ingest` endpoint validates ExceptionReport
  - Compute embedding via Bedrock
  - Load active + resolved group embeddings from DynamoDB
  - Find best matching group (cosine similarity above threshold)
  - Handle resolved match (reopen) vs archived match (new group)
  - Assign to existing group or create new group
  - Update group stats (count, last_seen, embedding, ttl)
  - Store exception report in exception_reports table
- [ ] Group management endpoints (add to ExceptionResourceInterface)
  - `GET /api/exception/groups` — list groups (filter by status)
  - `GET /api/exception/groups/{id}` — group detail
  - `GET /api/exception/groups/{id}/reports` — list exceptions in a group
  - `PUT /api/exception/groups/{id}/resolve` — mark as resolved
  - `PUT /api/exception/groups/{id}/archive` — mark as archived
  - `PUT /api/exception/groups/{id}/reopen` — reopen a resolved group
- [ ] Configuration via env vars
  - DynamoDB table names, AWS region
  - Similarity threshold (default TBD, e.g. 0.85)
  - TTL duration (default 90 days)
  - Bedrock model ID
- [ ] Update model module
  - ExceptionGroup model class
  - Add group endpoints to ExceptionResourceInterface
- [ ] AWS dependencies in server pom
  - DynamoDB SDK
  - Bedrock Runtime SDK
