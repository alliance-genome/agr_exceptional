package org.alliancegenome.exceptional.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.logging.Log;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.alliancegenome.exceptional.model.ExceptionGroup;

@ApplicationScoped
public class EmbeddingCache {

	@Inject
	Instance<DynamoService> dynamoService;

	private final ConcurrentHashMap<String, float[]> cache = new ConcurrentHashMap<>();

	@PostConstruct
	void init() {
		warmCache();
	}

	public void warmCache() {
		List<ExceptionGroup> groups = dynamoService.get().getActiveAndResolvedGroupsWithEmbeddings();
		for (ExceptionGroup group : groups) {
			if (group.getEmbedding() != null) {
				cache.put(group.getGroupId(), group.getEmbedding());
			}
		}
		Log.infof("EmbeddingCache warmed with %d groups", cache.size());
	}

	public float[] get(String groupId) {
		return cache.get(groupId);
	}

	public Map<String, float[]> getAll() {
		return new HashMap<>(cache);
	}

	public void put(String groupId, float[] embedding) {
		cache.put(groupId, embedding);
	}

	public void remove(String groupId) {
		cache.remove(groupId);
	}
}
