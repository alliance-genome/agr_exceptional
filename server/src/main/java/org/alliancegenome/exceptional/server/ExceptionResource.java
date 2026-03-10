package org.alliancegenome.exceptional.server;

import org.alliancegenome.exceptional.interfaces.ExceptionResourceInterface;
import org.alliancegenome.exceptional.model.ExceptionReport;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;

public class ExceptionResource implements ExceptionResourceInterface {

	@Override
	public Response ingest(ExceptionReport report) {
		// TODO: Phase 2 — parse, buffer, store
		Log.infof("[%s] %s: %s", report.getService(), report.getType(), report.getMessage());
		Log.info(report.getStacktrace());
		return Response.accepted().build();
	}

	@Override
	public Response health() {
		return Response.ok("{\"status\":\"ok\"}").build();
	}
}
