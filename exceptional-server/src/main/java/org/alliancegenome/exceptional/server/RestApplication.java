package org.alliancegenome.exceptional.server;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
@OpenAPIDefinition(
	info = @Info(
		title = "AGR Exceptional API",
		description = "Exception ingestion, grouping, and management service for the Alliance of Genome Resources",
		version = "0.1.0"
	)
)
public class RestApplication extends Application {
}
