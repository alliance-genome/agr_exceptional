package org.alliancegenome.exceptional.model;

import lombok.Data;

@Data
public class ExceptionReport {

	private String timestamp;
	private String service;
	private String host;
	private String type;
	private String message;
	private String stacktrace;
}
