package org.alliancegenome.exceptional.client;

public class TestException {

	public static void main(String[] args) {
		ExceptionCatcher.initialize("http://localhost:8080/api", "test-service");

		Long l = 0L;
		
		l = null;
		
		System.out.println(l.toString());
		
	}

}
