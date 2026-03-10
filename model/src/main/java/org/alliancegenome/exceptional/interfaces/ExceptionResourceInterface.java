package org.alliancegenome.exceptional.interfaces;

import org.alliancegenome.exceptional.model.ExceptionReport;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/exception")
@Tag(name = "Exception Ingestion", description = "Endpoints for submitting exception reports and checking service health")
public interface ExceptionResourceInterface {

	@POST
	@Path("/ingest")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Ingest an exception report", description = "Accepts an exception report, generates an embedding, and groups it with similar exceptions")
	@RequestBody(content = @Content(schema = @Schema(implementation = ExceptionReport.class)))
	@APIResponse(responseCode = "200", description = "Exception report ingested and grouped successfully")
	@APIResponse(responseCode = "500", description = "Internal server error during ingestion")
	Response ingest(ExceptionReport report);

	@GET
	@Path("/health")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Health check", description = "Returns the health status of the exception service")
	@APIResponse(responseCode = "200", description = "Service is healthy")
	Response health();
}
