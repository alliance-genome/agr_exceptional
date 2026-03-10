package org.alliancegenome.exceptional.interfaces;

import org.alliancegenome.exceptional.model.ExceptionReport;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/exception")
public interface ExceptionResourceInterface {

	@POST
	@Path("/ingest")
	@Consumes(MediaType.APPLICATION_JSON)
	Response ingest(ExceptionReport report);

	@GET
	@Path("/health")
	@Produces(MediaType.APPLICATION_JSON)
	Response health();
}
