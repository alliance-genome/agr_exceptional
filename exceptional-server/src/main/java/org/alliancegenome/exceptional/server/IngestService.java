package org.alliancegenome.exceptional.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.alliancegenome.exceptional.model.ExceptionGroup;
import org.alliancegenome.exceptional.model.ExceptionReport;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IngestService {

	@Inject
	EmbeddingService embeddingService;

	@Inject
	EmbeddingCache embeddingCache;

	@Inject
	DynamoService dynamoService;

	public void ingest(ExceptionReport report) throws Exception {
		float[] embedding = embeddingService.computeEmbedding(report.getStacktrace());

		Map<String, float[]> allEmbeddings = embeddingCache.getAll();

		String bestGroupId = null;
		double bestSimilarity = -1;
		for (Map.Entry<String, float[]> entry : allEmbeddings.entrySet()) {
			double sim = embeddingService.cosineSimilarity(embedding, entry.getValue());
			if (sim > bestSimilarity) {
				bestSimilarity = sim;
				bestGroupId = entry.getKey();
			}
		}

		String groupId;
		if (bestGroupId != null && bestSimilarity >= embeddingService.getSimilarityThreshold()) {
			groupId = bestGroupId;
			ExceptionGroup group = dynamoService.getGroup(groupId);
			if ("resolved".equals(group.getStatus())) {
				group.setStatus("active");
			}

			float[] oldEmbedding = embeddingCache.get(groupId);
			float[] newAvg = EmbeddingService.updateRunningAverage(oldEmbedding, group.getExceptionCount(), embedding);
			group.setEmbedding(newAvg);
			group.setExceptionCount(group.getExceptionCount() + 1);
			group.setLastSeen(report.getTimestamp());
			group.setLatestType(report.getType());
			group.setLatestMessage(report.getMessage());

			List<String> services = group.getServices() != null ? new ArrayList<>(group.getServices()) : new ArrayList<>();
			if (!services.contains(report.getService())) {
				services.add(report.getService());
				group.setServices(services);
			}

			dynamoService.updateGroupForNewException(group);
			embeddingCache.put(groupId, newAvg);
			Log.infof("Matched exception to existing group %s (similarity=%.4f)", groupId, bestSimilarity);
		} else {
			groupId = UUID.randomUUID().toString();
			ExceptionGroup group = new ExceptionGroup();
			group.setGroupId(groupId);
			group.setStatus("active");
			group.setEmbedding(embedding);
			group.setExceptionCount(1);
			group.setFirstSeen(report.getTimestamp());
			group.setLastSeen(report.getTimestamp());
			group.setLatestType(report.getType());
			group.setLatestMessage(report.getMessage());
			group.setServices(List.of(report.getService()));
			group.setTtl(dynamoService.computeTtl());

			dynamoService.putGroup(group);
			embeddingCache.put(groupId, embedding);
			Log.infof("Created new exception group %s", groupId);
		}

		dynamoService.putReport(groupId, report, UUID.randomUUID().toString());
	}
}
