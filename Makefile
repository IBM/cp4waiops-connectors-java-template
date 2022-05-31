REGISTRY ?= hyc-cp4mcm-team-docker-local.artifactory.swg-devops.com
TAG ?= latest

DOCKER_IMAGE := $(REGISTRY)/cp/aiopsedge/java-grpc-connector-template:$(TAG)

ifeq ($(shell uname -s),Darwin)
	# gnu-sed, can be installed using homebrew
	SED_EXE := gsed
else
	SED_EXE := sed
endif

docker-login:
	docker login $(REGISTRY) -u "$$DOCKER_USERNAME" -p "$$DOCKER_PASSWORD"

docker-build:
	chmod ug+x container/import-certs.sh
	docker build -f container/Dockerfile -t $(DOCKER_IMAGE) .

docker-push:
	docker push $(DOCKER_IMAGE)

.PHONY: format
format:
	mvn formatter:format

.PHONY: lint-check
lint-check:
	mvn pmd:check formatter:validate -Dpmd.printFailingErrors=true

.PHONY: test
test:
	mvn test
