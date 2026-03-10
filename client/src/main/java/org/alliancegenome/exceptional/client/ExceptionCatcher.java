package org.alliancegenome.exceptional.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.Instant;

import org.alliancegenome.exceptional.interfaces.ExceptionResourceInterface;
import org.alliancegenome.exceptional.model.ExceptionReport;

import si.mazi.rescu.RestProxyFactory;

public class ExceptionCatcher {

    private static volatile boolean initialized = false;
    private static String serviceName;
    private static ExceptionResourceInterface api;

    public static synchronized void initialize(String serverEndpoint, String service) {
        if (initialized) return;

        serviceName = service;
        api = RestProxyFactory.createProxy(ExceptionResourceInterface.class, serverEndpoint);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> send(e));
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> send(e));

        initialized = true;
    }

    public static void report(Throwable t) {
        send(t);
    }

    private static void send(Throwable t) {
        if (api == null) {
            System.err.println("[agr_exceptional] Not initialized, logging to stderr:");
            t.printStackTrace(System.err);
            return;
        }
        try {
            ExceptionReport report = buildReport(t);
            api.ingest(report);
        } catch (Exception e) {
            System.err.println("[agr_exceptional] Failed to send: " + e.getMessage());
            e.printStackTrace(System.err);
            t.printStackTrace(System.err);
        }
    }

    private static ExceptionReport buildReport(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
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
        report.setStacktrace(sw.toString());
        return report;
    }
}
