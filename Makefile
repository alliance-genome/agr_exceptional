.PHONY: build mvndeploy cdkdeploy

build:
	mvn package -pl model,server -am -DskipTests

mvndeploy:
	@CURRENT=$$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout); \
	MAJOR=$$(echo $$CURRENT | cut -d. -f1); \
	MINOR=$$(echo $$CURRENT | cut -d. -f2); \
	NEW_VERSION="$$MAJOR.$$((MINOR + 1)).0"; \
	echo "Bumping version: $$CURRENT -> $$NEW_VERSION"; \
	mvn versions:set -DnewVersion=$$NEW_VERSION -DgenerateBackupFiles=false -q && \
	git add pom.xml */pom.xml && \
	git commit -m "release: $$NEW_VERSION" && \
	git tag "$$NEW_VERSION" && \
	mvn --batch-mode -ntp -Dmaven.test.skip=true deploy && \
	git push --follow-tags

cdkdeploy: build
	cd cdk && npx cdk deploy
