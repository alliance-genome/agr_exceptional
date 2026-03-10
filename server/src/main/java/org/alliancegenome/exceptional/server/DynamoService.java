package org.alliancegenome.exceptional.server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.exceptional.model.ExceptionGroup;
import org.alliancegenome.exceptional.model.ExceptionReport;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

@ApplicationScoped
public class DynamoService {

	@ConfigProperty(name = "exceptional.dynamo.groups-table", defaultValue = "exception_groups")
	String groupsTable;

	@ConfigProperty(name = "exceptional.dynamo.reports-table", defaultValue = "exception_reports")
	String reportsTable;

	@ConfigProperty(name = "exceptional.dynamo.region", defaultValue = "us-east-1")
	String region;

	@ConfigProperty(name = "exceptional.ttl.days", defaultValue = "90")
	int ttlDays;

	private DynamoDbClient dynamo;

	@PostConstruct
	void init() {
		dynamo = DynamoDbClient.builder()
			.region(Region.of(region))
			.httpClient(UrlConnectionHttpClient.builder().build())
			.build();
		ensureTablesExist();
	}

	private void ensureTablesExist() {
		try {
			dynamo.describeTable(DescribeTableRequest.builder().tableName(groupsTable).build());
		} catch (ResourceNotFoundException e) {
			Log.info("Creating table: " + groupsTable);
			dynamo.createTable(CreateTableRequest.builder()
				.tableName(groupsTable)
				.keySchema(KeySchemaElement.builder().attributeName("group_id").keyType(KeyType.HASH).build())
				.attributeDefinitions(
					AttributeDefinition.builder().attributeName("group_id").attributeType(ScalarAttributeType.S).build(),
					AttributeDefinition.builder().attributeName("status").attributeType(ScalarAttributeType.S).build(),
					AttributeDefinition.builder().attributeName("last_seen").attributeType(ScalarAttributeType.S).build()
				)
				.globalSecondaryIndexes(GlobalSecondaryIndex.builder()
					.indexName("status-index")
					.keySchema(
						KeySchemaElement.builder().attributeName("status").keyType(KeyType.HASH).build(),
						KeySchemaElement.builder().attributeName("last_seen").keyType(KeyType.RANGE).build()
					)
					.projection(Projection.builder().projectionType(ProjectionType.ALL).build())
					.build())
				.billingMode(BillingMode.PAY_PER_REQUEST)
				.build());
			try {
				dynamo.updateTimeToLive(UpdateTimeToLiveRequest.builder()
					.tableName(groupsTable)
					.timeToLiveSpecification(TimeToLiveSpecification.builder()
						.attributeName("ttl")
						.enabled(true)
						.build())
					.build());
			} catch (DynamoDbException ex) {
				Log.warn("TTL may already be enabled on " + groupsTable + ": " + ex.getMessage());
			}
		}

		try {
			dynamo.describeTable(DescribeTableRequest.builder().tableName(reportsTable).build());
		} catch (ResourceNotFoundException e) {
			Log.info("Creating table: " + reportsTable);
			dynamo.createTable(CreateTableRequest.builder()
				.tableName(reportsTable)
				.keySchema(
					KeySchemaElement.builder().attributeName("group_id").keyType(KeyType.HASH).build(),
					KeySchemaElement.builder().attributeName("timestamp_report_id").keyType(KeyType.RANGE).build()
				)
				.attributeDefinitions(
					AttributeDefinition.builder().attributeName("group_id").attributeType(ScalarAttributeType.S).build(),
					AttributeDefinition.builder().attributeName("timestamp_report_id").attributeType(ScalarAttributeType.S).build()
				)
				.billingMode(BillingMode.PAY_PER_REQUEST)
				.build());
			try {
				dynamo.updateTimeToLive(UpdateTimeToLiveRequest.builder()
					.tableName(reportsTable)
					.timeToLiveSpecification(TimeToLiveSpecification.builder()
						.attributeName("ttl")
						.enabled(true)
						.build())
					.build());
			} catch (DynamoDbException ex) {
				Log.warn("TTL may already be enabled on " + reportsTable + ": " + ex.getMessage());
			}
		}
	}

	public List<ExceptionGroup> getGroupsByStatus(String status) {
		QueryRequest request = QueryRequest.builder()
			.tableName(groupsTable)
			.indexName("status-index")
			.keyConditionExpression("#s = :status")
			.expressionAttributeNames(Map.of("#s", "status"))
			.expressionAttributeValues(Map.of(":status", AttributeValue.builder().s(status).build()))
			.scanIndexForward(false)
			.build();
		List<ExceptionGroup> groups = new ArrayList<>();
		for (Map<String, AttributeValue> item : dynamo.query(request).items()) {
			groups.add(mapToGroup(item, false));
		}
		return groups;
	}

	public List<ExceptionGroup> getActiveAndResolvedGroupsWithEmbeddings() {
		ScanRequest request = ScanRequest.builder()
			.tableName(groupsTable)
			.filterExpression("#s = :active OR #s = :resolved")
			.expressionAttributeNames(Map.of("#s", "status"))
			.expressionAttributeValues(Map.of(
				":active", AttributeValue.builder().s("active").build(),
				":resolved", AttributeValue.builder().s("resolved").build()
			))
			.build();
		List<ExceptionGroup> groups = new ArrayList<>();
		for (Map<String, AttributeValue> item : dynamo.scan(request).items()) {
			groups.add(mapToGroup(item, true));
		}
		return groups;
	}

	public ExceptionGroup getGroup(String groupId) {
		GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
			.tableName(groupsTable)
			.key(Map.of("group_id", AttributeValue.builder().s(groupId).build()))
			.build());
		if (!response.hasItem() || response.item().isEmpty()) {
			return null;
		}
		return mapToGroup(response.item(), false);
	}

	public void putGroup(ExceptionGroup group) {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("group_id", AttributeValue.builder().s(group.getGroupId()).build());
		item.put("status", AttributeValue.builder().s(group.getStatus()).build());
		item.put("exception_count", AttributeValue.builder().n(String.valueOf(group.getExceptionCount())).build());
		item.put("first_seen", AttributeValue.builder().s(group.getFirstSeen()).build());
		item.put("last_seen", AttributeValue.builder().s(group.getLastSeen()).build());
		item.put("latest_type", AttributeValue.builder().s(group.getLatestType()).build());
		item.put("latest_message", AttributeValue.builder().s(group.getLatestMessage()).build());
		item.put("ttl", AttributeValue.builder().n(String.valueOf(group.getTtl())).build());
		if (group.getEmbedding() != null) {
			item.put("embedding", AttributeValue.builder().b(SdkBytes.fromByteArray(floatsToBytes(group.getEmbedding()))).build());
		}
		if (group.getServices() != null && !group.getServices().isEmpty()) {
			item.put("services", AttributeValue.builder().ss(group.getServices()).build());
		}
		dynamo.putItem(PutItemRequest.builder().tableName(groupsTable).item(item).build());
	}

	public void updateGroupForNewException(ExceptionGroup group) {
		Map<String, AttributeValue> values = new HashMap<>();
		values.put(":last_seen", AttributeValue.builder().s(group.getLastSeen()).build());
		values.put(":exception_count", AttributeValue.builder().n(String.valueOf(group.getExceptionCount())).build());
		values.put(":latest_type", AttributeValue.builder().s(group.getLatestType()).build());
		values.put(":latest_message", AttributeValue.builder().s(group.getLatestMessage()).build());
		values.put(":status", AttributeValue.builder().s(group.getStatus()).build());
		values.put(":ttl", AttributeValue.builder().n(String.valueOf(group.getTtl())).build());
		if (group.getEmbedding() != null) {
			values.put(":embedding", AttributeValue.builder().b(SdkBytes.fromByteArray(floatsToBytes(group.getEmbedding()))).build());
		}

		String updateExpr = "SET last_seen = :last_seen, exception_count = :exception_count, embedding = :embedding, latest_type = :latest_type, latest_message = :latest_message, #s = :status, #t = :ttl";

		Map<String, String> names = new HashMap<>();
		names.put("#s", "status");
		names.put("#t", "ttl");

		if (group.getServices() != null && !group.getServices().isEmpty()) {
			values.put(":services", AttributeValue.builder().ss(group.getServices()).build());
			updateExpr += ", services = :services";
		}

		dynamo.updateItem(UpdateItemRequest.builder()
			.tableName(groupsTable)
			.key(Map.of("group_id", AttributeValue.builder().s(group.getGroupId()).build()))
			.updateExpression(updateExpr)
			.expressionAttributeNames(names)
			.expressionAttributeValues(values)
			.build());
	}

	public void updateGroupStatus(String groupId, String newStatus) {
		dynamo.updateItem(UpdateItemRequest.builder()
			.tableName(groupsTable)
			.key(Map.of("group_id", AttributeValue.builder().s(groupId).build()))
			.updateExpression("SET #s = :status")
			.expressionAttributeNames(Map.of("#s", "status"))
			.expressionAttributeValues(Map.of(":status", AttributeValue.builder().s(newStatus).build()))
			.build());
	}

	public void putReport(String groupId, ExceptionReport report, String reportId) {
		String sk = report.getTimestamp() + "#" + reportId;
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("group_id", AttributeValue.builder().s(groupId).build());
		item.put("timestamp_report_id", AttributeValue.builder().s(sk).build());
		item.put("timestamp", AttributeValue.builder().s(report.getTimestamp()).build());
		item.put("report_id", AttributeValue.builder().s(reportId).build());
		item.put("service", AttributeValue.builder().s(report.getService()).build());
		item.put("host", AttributeValue.builder().s(report.getHost()).build());
		item.put("type", AttributeValue.builder().s(report.getType()).build());
		item.put("message", AttributeValue.builder().s(report.getMessage()).build());
		item.put("stacktrace", AttributeValue.builder().s(report.getStacktrace()).build());
		item.put("ttl", AttributeValue.builder().n(String.valueOf(computeTtl())).build());
		dynamo.putItem(PutItemRequest.builder().tableName(reportsTable).item(item).build());
	}

	public List<ExceptionReport> getReportsForGroup(String groupId, int limit) {
		QueryRequest request = QueryRequest.builder()
			.tableName(reportsTable)
			.keyConditionExpression("group_id = :gid")
			.expressionAttributeValues(Map.of(":gid", AttributeValue.builder().s(groupId).build()))
			.scanIndexForward(false)
			.limit(limit)
			.build();
		List<ExceptionReport> reports = new ArrayList<>();
		for (Map<String, AttributeValue> item : dynamo.query(request).items()) {
			reports.add(mapToReport(item));
		}
		return reports;
	}

	private ExceptionGroup mapToGroup(Map<String, AttributeValue> item, boolean includeEmbedding) {
		ExceptionGroup group = new ExceptionGroup();
		group.setGroupId(item.get("group_id").s());
		group.setStatus(item.get("status").s());
		if (item.containsKey("exception_count")) {
			group.setExceptionCount(Integer.parseInt(item.get("exception_count").n()));
		}
		if (item.containsKey("first_seen")) {
			group.setFirstSeen(item.get("first_seen").s());
		}
		if (item.containsKey("last_seen")) {
			group.setLastSeen(item.get("last_seen").s());
		}
		if (item.containsKey("latest_type")) {
			group.setLatestType(item.get("latest_type").s());
		}
		if (item.containsKey("latest_message")) {
			group.setLatestMessage(item.get("latest_message").s());
		}
		if (item.containsKey("ttl")) {
			group.setTtl(Long.parseLong(item.get("ttl").n()));
		}
		if (item.containsKey("services") && item.get("services").ss() != null) {
			group.setServices(new ArrayList<>(item.get("services").ss()));
		}
		if (includeEmbedding && item.containsKey("embedding")) {
			group.setEmbedding(bytesToFloats(item.get("embedding").b().asByteArray()));
		}
		return group;
	}

	private ExceptionReport mapToReport(Map<String, AttributeValue> item) {
		ExceptionReport report = new ExceptionReport();
		if (item.containsKey("timestamp")) {
			report.setTimestamp(item.get("timestamp").s());
		}
		if (item.containsKey("service")) {
			report.setService(item.get("service").s());
		}
		if (item.containsKey("host")) {
			report.setHost(item.get("host").s());
		}
		if (item.containsKey("type")) {
			report.setType(item.get("type").s());
		}
		if (item.containsKey("message")) {
			report.setMessage(item.get("message").s());
		}
		if (item.containsKey("stacktrace")) {
			report.setStacktrace(item.get("stacktrace").s());
		}
		return report;
	}

	long computeTtl() {
		return Instant.now().plus(Duration.ofDays(ttlDays)).getEpochSecond();
	}

	private static byte[] floatsToBytes(float[] floats) {
		ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		for (float f : floats) {
			buffer.putFloat(f);
		}
		return buffer.array();
	}

	private static float[] bytesToFloats(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		float[] floats = new float[bytes.length / 4];
		for (int i = 0; i < floats.length; i++) {
			floats[i] = buffer.getFloat();
		}
		return floats;
	}
}
