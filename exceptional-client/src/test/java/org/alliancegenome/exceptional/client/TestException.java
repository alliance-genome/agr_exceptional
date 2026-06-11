package org.alliancegenome.exceptional.client;

public class TestException {

	public static void main(String[] args) {
		ExceptionCatcher.initialize();
		Long l = 0L;
		l = null;
		System.out.println(l.toString());
	}

}
