# Phase 4 — Deployment

Goal: Running the server in AGR infrastructure on ECS Fargate.

## TODO

- [ ] Dockerfile for Quarkus server
  - Multi-stage build (Maven build + JRE runtime)
  - Or native image if feasible
- [ ] CloudFormation template (similar to agr_logs)
  - ECS cluster + Fargate service
  - Internal ALB for viewer/API access
  - S3 bucket for exception storage (with lifecycle policy)
  - S3 bucket for Athena query results (1-day retention)
  - Glue database + table with partition projection
  - Athena workgroup with scan cutoff
  - IAM roles (S3 write, Athena query, Glue metadata)
  - CloudWatch alarms (target health, error logs)
- [ ] Service discovery (Cloud Map or DNS)
  - Client library needs a stable endpoint to reach the server
- [ ] Health check configuration
  - ALB health check on `/health`
- [ ] GitHub Actions workflow
  - Build, push to ECR, deploy to ECS on push to main
