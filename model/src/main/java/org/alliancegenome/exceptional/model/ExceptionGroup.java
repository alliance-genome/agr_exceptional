package org.alliancegenome.exceptional.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
@Schema(name = "ExceptionGroup", description = "A group of similar exceptions clustered by embedding similarity")
public class ExceptionGroup {

	@Schema(description = "Unique identifier for this exception group")
	private String groupId;
	@Schema(description = "Current status: active, resolved, or archived")
	private String status;
	@JsonIgnore
	private float[] embedding;
	@Schema(description = "Total number of exceptions in this group")
	private int exceptionCount;
	@Schema(description = "ISO-8601 timestamp of the first exception in this group")
	private String firstSeen;
	@Schema(description = "ISO-8601 timestamp of the most recent exception in this group")
	private String lastSeen;
	@Schema(description = "List of service names that have reported exceptions in this group")
	private List<String> services;
	@Schema(description = "Exception type from the most recent report")
	private String latestType;
	@Schema(description = "Exception message from the most recent report")
	private String latestMessage;
	@Schema(description = "Time-to-live in epoch seconds for DynamoDB expiration")
	private long ttl;
}
