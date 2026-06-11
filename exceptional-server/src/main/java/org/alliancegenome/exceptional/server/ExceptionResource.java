package org.alliancegenome.exceptional.server;

import org.alliancegenome.exceptional.interfaces.ExceptionResourceInterface;
import org.alliancegenome.exceptional.model.ExceptionReport;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

public class ExceptionResource implements ExceptionResourceInterface {

	@Inject
	IngestService ingestService;

	@Override
	public Response ingest(ExceptionReport report) {
		try {
			ingestService.ingest(report);
			return Response.accepted().build();
		} catch (Exception e) {
			Log.error("Failed to ingest exception report", e);
			return Response.serverError().entity(e.getMessage()).build();
		}
	}

	@Override
	public Response health() {
		return Response.ok("{\"status\":\"ok\"}").build();
	}
}
