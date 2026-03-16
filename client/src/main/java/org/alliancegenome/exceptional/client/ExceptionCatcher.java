package org.alliancegenome.exceptional.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.alliancegenome.exceptional.interfaces.ExceptionResourceInterface;
import org.alliancegenome.exceptional.model.ExceptionReport;

import si.mazi.rescu.RestProxyFactory;

public class ExceptionCatcher {

	private static volatile boolean initialized = false;
	private static String serviceName;
	private static ExceptionResourceInterface api;
	private static ExecutorService sender;
	private static final ConcurrentHashMap<Integer, Long> recentHashes = new ConcurrentHashMap<>();

	private static final long DEDUP_WINDOW_MS = 60_000;
	private static final int MAX_DEDUP_ENTRIES = 1000;

	static final AtomicInteger sentCount = new AtomicInteger();

	public static synchronized void initialize(String serverEndpoint, String service) {
		if (initialized) return;
		
		serviceName = service;
		api = RestProxyFactory.createProxy(ExceptionResourceInterface.class, serverEndpoint + "/api");
		sender = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "agr-exceptional-sender");
			t.setDaemon(true);
			return t;
		});
		
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> send(e));
		Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
			send(e);
		});

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			sender.shutdown();
			try {
				sender.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {}
		}, "agr-exceptional-shutdown"));

		initialized = true;
	}

	public static synchronized void initialize() {
		if (initialized) return;
		
		String endpoint = System.getenv("AGR_EXCEPTIONAL_ENDPOINT");
		if (endpoint == null) {
			endpoint = System.getProperty("agr.exceptional.endpoint");
		}
		String service = System.getenv("AGR_EXCEPTIONAL_SERVICE");
		if (service == null) {
			service = System.getProperty("agr.exceptional.service");
		}

		if (endpoint == null) {
			System.err.println("[agr_exceptional] No endpoint configured, exceptions will log to stderr");
			serviceName = service;
			initialized = true;
			return;
		}

		initialize(endpoint, service != null ? service : "unknown");
	}

	public static void report(Throwable t) {
		send(t);
	}

	private static void send(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		String stacktrace = sw.toString();
		int hash = stacktrace.hashCode();
		long now = System.currentTimeMillis();
		Long lastSent = recentHashes.get(hash);
		if (lastSent != null && (now - lastSent) < DEDUP_WINDOW_MS) {
			return;
		}
		recentHashes.put(hash, now);

		if (recentHashes.size() > MAX_DEDUP_ENTRIES) {
			recentHashes.entrySet().removeIf(e -> (now - e.getValue()) > DEDUP_WINDOW_MS);
		}

		if (api == null || sender == null) {
			System.err.println("[agr_exceptional] Not initialized, logging to stderr:");
			t.printStackTrace(System.err);
			return;
		} else {
			t.printStackTrace(System.err);
		}

		ExceptionReport report = buildReport(t, stacktrace);
		sentCount.incrementAndGet();
		sender.submit(() -> {
			try {
				api.ingest(report);
			} catch (Exception e) {
				System.err.println("[agr_exceptional] Failed to send: " + e.getMessage());
				e.printStackTrace(System.err);
				t.printStackTrace(System.err);
			}
		});
	}

	private static ExceptionReport buildReport(Throwable t, String stacktrace) {
		String hostname = "";
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (Exception ignored) {}

		ExceptionReport report = new ExceptionReport();
		report.setTimestamp(Instant.now().toString());
		report.setService(serviceName != null ? serviceName : "unknown");
		report.setHost(hostname);
		report.setType(t.getClass().getName());
		report.setMessage(t.getMessage() != null ? t.getMessage() : "");
		report.setStacktrace(stacktrace);
		return report;
	}

	static void resetForTesting() {
		initialized = false;
		serviceName = null;
		api = null;
		if (sender != null) {
			sender.shutdownNow();
			sender = null;
		}
		recentHashes.clear();
		sentCount.set(0);
	}
}
