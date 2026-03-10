package org.alliancegenome.exceptional.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "ExceptionReport", description = "An individual exception report submitted by a service")
public class ExceptionReport {

	@Schema(description = "ISO-8601 timestamp when the exception occurred")
	private String timestamp;
	@Schema(description = "Name of the service that reported the exception")
	private String service;
	@Schema(description = "Hostname of the machine where the exception occurred")
	private String host;
	@Schema(description = "Fully qualified exception class name")
	private String type;
	@Schema(description = "Exception message text")
	private String message;
	@Schema(description = "Full stack trace of the exception")
	private String stacktrace;
}
