package org.alliancegenome.exceptional.server;

import org.alliancegenome.exceptional.model.ExceptionGroup;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/exception/groups")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Exception Groups", description = "Endpoints for browsing and managing exception groups")
public class GroupResource {

	@Inject
	DynamoService dynamoService;

	@Inject
	EmbeddingCache embeddingCache;

	@GET
	@Operation(summary = "List exception groups", description = "Returns all exception groups filtered by status")
	@APIResponse(responseCode = "200", description = "List of exception groups")
	public Response listGroups(
			@Parameter(description = "Filter by group status", example = "active")
			@QueryParam("status") @DefaultValue("active") String status) {
		return Response.ok(dynamoService.getGroupsByStatus(status)).build();
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Get exception group", description = "Returns a single exception group by its ID")
	@APIResponse(responseCode = "200", description = "The exception group")
	@APIResponse(responseCode = "404", description = "Group not found")
	public Response getGroup(
			@Parameter(description = "Exception group ID", required = true)
			@PathParam("id") String groupId) {
		ExceptionGroup group = dynamoService.getGroup(groupId);
		if (group == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(group).build();
	}

	@GET
	@Path("/{id}/reports")
	@Operation(summary = "Get reports for group", description = "Returns exception reports belonging to a group")
	@APIResponse(responseCode = "200", description = "List of exception reports")
	public Response getGroupReports(
			@Parameter(description = "Exception group ID", required = true)
			@PathParam("id") String groupId,
			@Parameter(description = "Maximum number of reports to return", example = "50")
			@QueryParam("limit") @DefaultValue("50") int limit) {
		return Response.ok(dynamoService.getReportsForGroup(groupId, limit)).build();
	}

	@PUT
	@Path("/{id}/resolve")
	@Operation(summary = "Resolve exception group", description = "Sets the group status to resolved")
	@APIResponse(responseCode = "200", description = "Group resolved successfully")
	public Response resolveGroup(
			@Parameter(description = "Exception group ID", required = true)
			@PathParam("id") String groupId) {
		dynamoService.updateGroupStatus(groupId, "resolved");
		return Response.ok().build();
	}

	@PUT
	@Path("/{id}/archive")
	@Operation(summary = "Archive exception group", description = "Sets the group status to archived and removes its embedding from the cache")
	@APIResponse(responseCode = "200", description = "Group archived successfully")
	public Response archiveGroup(
			@Parameter(description = "Exception group ID", required = true)
			@PathParam("id") String groupId) {
		dynamoService.updateGroupStatus(groupId, "archived");
		embeddingCache.remove(groupId);
		return Response.ok().build();
	}

	@PUT
	@Path("/{id}/reopen")
	@Operation(summary = "Reopen exception group", description = "Sets the group status back to active")
	@APIResponse(responseCode = "200", description = "Group reopened successfully")
	public Response reopenGroup(
			@Parameter(description = "Exception group ID", required = true)
			@PathParam("id") String groupId) {
		dynamoService.updateGroupStatus(groupId, "active");
		return Response.ok().build();
	}
}
