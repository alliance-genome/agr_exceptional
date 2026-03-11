package org.alliancegenome.exceptional.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.CfnBasePathMappingV2;
import software.amazon.awscdk.services.apigateway.CfnDomainNameAccessAssociation;
import software.amazon.awscdk.services.apigateway.CfnDomainNameV2;
import software.amazon.awscdk.services.apigateway.CorsOptions;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InterfaceVpcEndpoint;
import software.amazon.awscdk.services.ec2.InterfaceVpcEndpointService;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.RuleTargetInput;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.AnyPrincipal;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SnapStartConf;
import software.amazon.awscdk.services.lambda.Version;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.InterfaceVpcEndpointTarget;
import software.constructs.Construct;

public class ExceptionalStack extends Stack {

	public ExceptionalStack(Construct scope, String id, StackProps props) {
		super(scope, id, props);

		// VPC lookup
		IVpc vpc = Vpc.fromLookup(this, "Vpc", VpcLookupOptions.builder()
			.vpcId("vpc-55522232")
			.build());

		// Security groups
		ISecurityGroup defaultSg = SecurityGroup.fromSecurityGroupId(this, "DefaultSg", "sg-21ac675b");
		ISecurityGroup httpSg = SecurityGroup.fromSecurityGroupId(this, "HttpSg", "sg-0415cab61ab6b45c5");

		// Subnet selection - private subnets with NAT
		SubnetSelection privateSubnets = SubnetSelection.builder()
			.subnetType(SubnetType.PRIVATE_WITH_EGRESS)
			.build();

		// VPC endpoint for execute-api (required for Private API Gateway)
		InterfaceVpcEndpoint apiEndpoint = InterfaceVpcEndpoint.Builder.create(this, "ApiVpcEndpoint")
			.vpc(vpc)
			.service(new InterfaceVpcEndpointService("com.amazonaws.us-east-1.execute-api"))
			.subnets(privateSubnets)
			.securityGroups(List.of(defaultSg))
			.privateDnsEnabled(true)
			.build();

		// Lambda function
		Function fn = Function.Builder.create(this, "ExceptionalFn")
			.runtime(Runtime.JAVA_21)
			.handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
			.code(Code.fromAsset("../server/target/function.zip"))
			.memorySize(1024)
			.timeout(Duration.seconds(30))
			.vpc(vpc)
			.vpcSubnets(privateSubnets)
			.securityGroups(List.of(defaultSg, httpSg))
			.environment(Map.of(
				"QUARKUS_PROFILE", "prod",
				"JAVA_TOOL_OPTIONS", "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
			))
			.snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
			.build();

		// Published version + alias (required for SnapStart)
		Version version = fn.getCurrentVersion();
		Alias alias = Alias.Builder.create(this, "LiveAlias")
			.aliasName("live")
			.version(version)
			.build();

		// IAM policies - DynamoDB
		fn.addToRolePolicy(PolicyStatement.Builder.create()
			.actions(List.of(
				"dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem",
				"dynamodb:DeleteItem", "dynamodb:Query", "dynamodb:Scan",
				"dynamodb:BatchWriteItem", "dynamodb:BatchGetItem", "dynamodb:DescribeTable"
			))
			.resources(List.of(
				"arn:aws:dynamodb:us-east-1:100225593120:table/exception_groups",
				"arn:aws:dynamodb:us-east-1:100225593120:table/exception_groups/*",
				"arn:aws:dynamodb:us-east-1:100225593120:table/exception_reports",
				"arn:aws:dynamodb:us-east-1:100225593120:table/exception_reports/*"
			))
			.build());

		fn.addToRolePolicy(PolicyStatement.Builder.create()
			.actions(List.of("dynamodb:UpdateTimeToLive"))
			.resources(List.of(
				"arn:aws:dynamodb:us-east-1:100225593120:table/exception_groups",
				"arn:aws:dynamodb:us-east-1:100225593120:table/exception_reports"
			))
			.build());

		// IAM policy - Bedrock
		fn.addToRolePolicy(PolicyStatement.Builder.create()
			.actions(List.of("bedrock:InvokeModel"))
			.resources(List.of("arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-embed-text-v2:0"))
			.build());

		// Private REST API Gateway
		RestApi api = RestApi.Builder.create(this, "ExceptionalApi")
			.restApiName("exceptional-api")
			.endpointConfiguration(EndpointConfiguration.builder()
				.types(List.of(EndpointType.PRIVATE))
				.vpcEndpoints(List.of(apiEndpoint))
				.build())
			.policy(PolicyDocument.Builder.create()
				.statements(List.of(
					PolicyStatement.Builder.create()
						.principals(List.of(new AnyPrincipal()))
						.actions(List.of("execute-api:Invoke"))
						.resources(List.of("execute-api:/*"))
						.effect(Effect.ALLOW)
						.conditions(Map.of("StringEquals", Map.of(
							"aws:sourceVpce", apiEndpoint.getVpcEndpointId()
						)))
						.build()
				))
				.build())
			.deployOptions(StageOptions.builder()
				.stageName("prod")
				.build())
			.defaultCorsPreflightOptions(CorsOptions.builder()
				.allowOrigins(List.of("https://logs.alliancegenome.org", "http://localhost:5000", "http://localhost:5001", "http://localhost:3000", "http://localhost:8080"))
				.allowMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"))
				.allowHeaders(List.of("Content-Type", "Authorization"))
				.build())
			.build();

		// Lambda integration
		LambdaIntegration lambdaIntegration = LambdaIntegration.Builder.create(alias).build();

		// Proxy resource {proxy+} with ANY method
		api.getRoot().addResource("{proxy+}").addMethod("ANY", lambdaIntegration);

		// Root resource GET
		api.getRoot().addMethod("GET", lambdaIntegration);

		// Private custom domain name with ACM certificate
		CfnDomainNameV2 customDomain = CfnDomainNameV2.Builder.create(this, "CustomDomain")
			.domainName("exceptions.alliancegenome.org")
			.certificateArn("arn:aws:acm:us-east-1:100225593120:certificate/047a56a2-09dd-4857-9f28-32d23650d4da")
			.endpointConfiguration(CfnDomainNameV2.EndpointConfigurationProperty.builder()
				.types(List.of("PRIVATE"))
				.build())
			.securityPolicy("TLS_1_2")
			.policy(Map.of(
				"Version", "2012-10-17",
				"Statement", List.of(Map.of(
					"Effect", "Allow",
					"Principal", "*",
					"Action", "execute-api:Invoke",
					"Resource", "*",
					"Condition", Map.of("StringEquals", Map.of(
						"aws:SourceVpce", apiEndpoint.getVpcEndpointId()
					))
				))
			))
			.build();

		// Associate domain with VPC endpoint
		CfnDomainNameAccessAssociation.Builder.create(this, "DomainAccess")
			.domainNameArn(customDomain.getAttrDomainNameArn())
			.accessAssociationSourceType("VPCE")
			.accessAssociationSource(apiEndpoint.getVpcEndpointId())
			.build();

		// Map domain to API prod stage
		CfnBasePathMappingV2.Builder.create(this, "DomainMapping")
			.domainNameArn(customDomain.getAttrDomainNameArn())
			.restApiId(api.getRestApiId())
			.stage("prod")
			.build();

		// EventBridge warmup rule
		Rule warmupRule = Rule.Builder.create(this, "WarmupRule")
			.schedule(Schedule.rate(Duration.minutes(5)))
			.build();

		warmupRule.addTarget(LambdaFunction.Builder.create(alias)
			.event(RuleTargetInput.fromObject(Map.of(
				"httpMethod", "GET",
				"path", "/api/exception/health",
				"requestContext", Map.of(
					"resourcePath", "/api/exception/health",
					"httpMethod", "GET"
				)
			)))
			.build());

		// Route53 A record - private zone
		var hostedZone = HostedZone.fromHostedZoneAttributes(this, "Zone",
			HostedZoneAttributes.builder()
				.hostedZoneId("Z007692222A6W93AZVSPD")
				.zoneName("alliancegenome.org")
				.build());

		ARecord.Builder.create(this, "DnsRecord")
			.zone(hostedZone)
			.recordName("exceptions.alliancegenome.org")
			.target(RecordTarget.fromAlias(new InterfaceVpcEndpointTarget(apiEndpoint)))
			.build();

		// Route53 A record - public zone
		var publicZone = HostedZone.fromHostedZoneAttributes(this, "PublicZone",
			HostedZoneAttributes.builder()
				.hostedZoneId("Z3IZ3D6V94JEC2")
				.zoneName("alliancegenome.org")
				.build());

		ARecord.Builder.create(this, "PublicDnsRecord")
			.zone(publicZone)
			.recordName("exceptions.alliancegenome.org")
			.target(RecordTarget.fromAlias(new InterfaceVpcEndpointTarget(apiEndpoint)))
			.build();
	}
}
