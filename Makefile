.PHONY: build mvndeploy cdkdeploy

build:
	mvn package -pl model,server -am -DskipTests

mvndeploy:
	mvn --batch-mode -ntp -Dmaven.test.skip=true deploy

cdkdeploy: build
	cd cdk && npx cdk deploy
