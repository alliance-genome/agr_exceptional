# Phase 4 — Deployment

Goal: Running the server as an internal-only AWS Lambda, entire stack deployed via CDK.

## Design

### Lambda (VPC-internal)

- Quarkus deployed as Lambda via `quarkus-amazon-lambda-http`
- Lambda runs inside AGR VPC (private subnets)
- No public access — only reachable by other AGR services within the VPC
- Function URL or internal ALB for invocation (no public API Gateway)
- Consider Quarkus native image to minimize cold start (~200-300ms vs ~2-3s JVM)
- CloudWatch EventBridge rule pings health endpoint every 5 minutes to keep Lambda warm

### CDK (Infrastructure as Code)

- CDK project lives in this repo under `cdk/` (Java)
- Single stack deploys: Lambda, DynamoDB tables, IAM, EventBridge rule, VPC config
- Environment-aware (stage/prod) via CDK context or parameters

### Cost

- Lambda: effectively free at ~100 requests/day
- DynamoDB: pay-per-request, pennies/month
- Bedrock: pennies/month
- EventBridge: free tier covers the scheduled pings

## TODO

- [ ] Add `quarkus-amazon-lambda-http` dependency to server
- [ ] Native image build (optional but recommended for cold start)
  - GraalVM native compilation
  - Test all reflection/serialization works in native mode
- [ ] CDK project (`cdk/` directory)
  - Lambda function in VPC private subnets
  - VPC endpoints for DynamoDB and Bedrock (so Lambda doesn't need NAT gateway)
  - Internal-only access (no public endpoint)
  - DynamoDB tables
    - exception_groups: GSI on status, TTL enabled, pay-per-request
    - exception_reports: TTL enabled, pay-per-request
  - IAM role for Lambda
    - DynamoDB read/write (scoped to tables)
    - Bedrock InvokeModel (scoped to Titan model)
    - CloudWatch Logs
  - EventBridge rule: ping health endpoint every 5 minutes
  - CloudWatch alarms (Lambda errors, duration)
  - Security group: allow inbound only from AGR VPC CIDR
- [ ] Environment support
  - Stage and prod stacks from same CDK app
  - Per-environment DynamoDB table names
  - Reference existing AGR VPC/subnets
- [ ] Stable internal endpoint for client library
  - Lambda function URL (IAM auth) or internal ALB
  - Discoverable by AGR services (Cloud Map, SSM parameter, or env var)
- [ ] GitHub Actions workflow
  - Build Quarkus (native or JVM), package
  - CDK diff on PR
  - CDK deploy on push to main
