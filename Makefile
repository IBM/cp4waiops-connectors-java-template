TAG ?= latest

CONNECTOR_SDK_VERSION := 1.3.3
DOCKER_IMAGE := hyc-cp4mcm-team-docker-local.artifactory.swg-devops.com/cp/aiopsedge/java-grpc-connector-template:$(TAG)

ifeq ($(shell uname -s),Darwin)
	# gnu-sed, can be installed using homebrew
	SED_EXE := gsed
else
	SED_EXE := sed
endif

docker-build:
	docker build -f container/Dockerfile -t $(DOCKER_IMAGE) .

docker-push:
	docker push $(DOCKER_IMAGE)

.PHONY: download-connector-sdk
download-connector-sdk:
	$(SED_EXE) -i -e "s|.*CONNECTOR_SDK_VERSION.*|      <version>$(CONNECTOR_SDK_VERSION)</version> <!-- CONNECTOR_SDK_VERSION -->|" pom.xml
	./setup-sdk.sh "v$(CONNECTOR_SDK_VERSION)"

.PHONY: test
test:
	mvn test
