package org.alliancegenome.exceptional.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkApp {
	public static void main(String[] args) {
		App app = new App();
		new ExceptionalStack(app, "ExceptionalStack", StackProps.builder()
			.env(Environment.builder()
				.account("100225593120")
				.region("us-east-1")
				.build())
			.build());
		app.synth();
	}
}
