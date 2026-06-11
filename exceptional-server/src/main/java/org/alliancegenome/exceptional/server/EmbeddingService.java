package org.alliancegenome.exceptional.server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@ApplicationScoped
public class EmbeddingService {

	@ConfigProperty(name = "exceptional.bedrock.model-id", defaultValue = "amazon.titan-embed-text-v2:0")
	String modelId;

	@ConfigProperty(name = "exceptional.bedrock.region", defaultValue = "us-east-1")
	String region;

	@ConfigProperty(name = "exceptional.similarity.threshold", defaultValue = "0.85")
	double similarityThreshold;

	private BedrockRuntimeClient bedrockClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	void init() {
		bedrockClient = BedrockRuntimeClient.builder()
			.region(Region.of(region))
			.httpClientBuilder(UrlConnectionHttpClient.builder())
			.build();
	}

	public float[] computeEmbedding(String text) throws Exception {
		String requestBody = objectMapper.writeValueAsString(new LinkedHashMap<String, Object>() {{
			put("inputText", text);
			put("dimensions", 1024);
			put("normalize", true);
		}});

		InvokeModelRequest request = InvokeModelRequest.builder()
			.modelId(modelId)
			.contentType("application/json")
			.body(SdkBytes.fromUtf8String(requestBody))
			.build();

		InvokeModelResponse response = bedrockClient.invokeModel(request);
		JsonNode root = objectMapper.readTree(response.body().asUtf8String());
		JsonNode embeddingNode = root.get("embedding");

		float[] embedding = new float[embeddingNode.size()];
		for (int i = 0; i < embeddingNode.size(); i++) {
			embedding[i] = (float) embeddingNode.get(i).doubleValue();
		}
		return embedding;
	}

	public double cosineSimilarity(float[] a, float[] b) {
		double dot = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
		}
		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	public double getSimilarityThreshold() {
		return similarityThreshold;
	}

	public static float[] updateRunningAverage(float[] oldAvg, int oldCount, float[] newEmbedding) {
		float[] result = new float[oldAvg.length];
		for (int i = 0; i < oldAvg.length; i++) {
			result[i] = (oldAvg[i] * oldCount + newEmbedding[i]) / (oldCount + 1);
		}
		return result;
	}

	public static byte[] floatsToBytes(float[] floats) {
		ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		for (float f : floats) {
			buffer.putFloat(f);
		}
		return buffer.array();
	}

	public static float[] bytesToFloats(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		float[] floats = new float[bytes.length / 4];
		for (int i = 0; i < floats.length; i++) {
			floats[i] = buffer.getFloat();
		}
		return floats;
	}
}
