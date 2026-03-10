package org.alliancegenome.exceptional.server;

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

import org.alliancegenome.exceptional.model.ExceptionGroup;

@Path("/exception/groups")
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

	@Inject
	DynamoService dynamoService;

	@Inject
	EmbeddingCache embeddingCache;

	@GET
	public Response listGroups(@QueryParam("status") @DefaultValue("active") String status) {
		return Response.ok(dynamoService.getGroupsByStatus(status)).build();
	}

	@GET
	@Path("/{id}")
	public Response getGroup(@PathParam("id") String groupId) {
		ExceptionGroup group = dynamoService.getGroup(groupId);
		if (group == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		return Response.ok(group).build();
	}

	@GET
	@Path("/{id}/reports")
	public Response getGroupReports(@PathParam("id") String groupId, @QueryParam("limit") @DefaultValue("50") int limit) {
		return Response.ok(dynamoService.getReportsForGroup(groupId, limit)).build();
	}

	@PUT
	@Path("/{id}/resolve")
	public Response resolveGroup(@PathParam("id") String groupId) {
		dynamoService.updateGroupStatus(groupId, "resolved");
		return Response.ok().build();
	}

	@PUT
	@Path("/{id}/archive")
	public Response archiveGroup(@PathParam("id") String groupId) {
		dynamoService.updateGroupStatus(groupId, "archived");
		embeddingCache.remove(groupId);
		return Response.ok().build();
	}

	@PUT
	@Path("/{id}/reopen")
	public Response reopenGroup(@PathParam("id") String groupId) {
		dynamoService.updateGroupStatus(groupId, "active");
		return Response.ok().build();
	}
}
