package org.alliancegenome.exceptional.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class ExceptionGroup {

	private String groupId;
	private String status;
	@JsonIgnore
	private float[] embedding;
	private int exceptionCount;
	private String firstSeen;
	private String lastSeen;
	private List<String> services;
	private String latestType;
	private String latestMessage;
	private long ttl;
}
