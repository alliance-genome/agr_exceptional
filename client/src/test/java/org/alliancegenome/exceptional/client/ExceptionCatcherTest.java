package org.alliancegenome.exceptional.client;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

public class ExceptionCatcherTest {

	@After
	public void tearDown() {
		ExceptionCatcher.resetForTesting();
	}

	@Test
	public void testReportSendsToServer() {
		ExceptionCatcher.initialize("http://localhost:9999", "test-service");
		ExceptionCatcher.report(new RuntimeException("test error"));
		assertEquals(1, ExceptionCatcher.sentCount.get());
	}

	@Test
	public void testDedupSuppressesDuplicates() {
		ExceptionCatcher.initialize("http://localhost:9999", "test-service");

		RuntimeException ex = new RuntimeException("same error");
		ExceptionCatcher.report(ex);
		ExceptionCatcher.report(ex);
		ExceptionCatcher.report(ex);

		assertEquals(1, ExceptionCatcher.sentCount.get());
	}

	@Test
	public void testDifferentExceptionsNotDeduped() {
		ExceptionCatcher.initialize("http://localhost:9999", "test-service");

		ExceptionCatcher.report(new RuntimeException("error 1"));
		ExceptionCatcher.report(new RuntimeException("error 2"));

		assertEquals(2, ExceptionCatcher.sentCount.get());
	}

	@Test
	public void testNoEndpointFallsBackToStderr() {
		System.clearProperty("agr.exceptional.endpoint");
		System.clearProperty("agr.exceptional.service");

		ExceptionCatcher.initialize();
		ExceptionCatcher.report(new RuntimeException("test"));

		assertEquals(0, ExceptionCatcher.sentCount.get());
	}

	@Test
	public void testSystemPropertyFallback() {
		System.setProperty("agr.exceptional.endpoint", "http://localhost:9999");
		System.setProperty("agr.exceptional.service", "prop-service");
		try {
			ExceptionCatcher.initialize();
			ExceptionCatcher.report(new RuntimeException("test"));
			assertEquals(1, ExceptionCatcher.sentCount.get());
		} finally {
			System.clearProperty("agr.exceptional.endpoint");
			System.clearProperty("agr.exceptional.service");
		}
	}
}
