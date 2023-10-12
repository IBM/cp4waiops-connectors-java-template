REGISTRY ?= docker.io/sghung
TAG ?= latest

DOCKER_IMAGE := $(REGISTRY)/cp/aiopsedge/java-grpc-connector-template:$(TAG)

ifeq ($(shell uname -s),Darwin)
	# gnu-sed, can be installed using homebrew
	SED_EXE := gsed
else
	SED_EXE := sed
endif

podman-login:
	podman login $(REGISTRY) -u "$$DOCKER_USERNAME" -p "$$DOCKER_PASSWORD"

podman-build:
	chmod ug+x container/import-certs.sh
	podman build -f container/Dockerfile -t $(DOCKER_IMAGE) .

podman-push:
	podman push $(DOCKER_IMAGE)

.PHONY: format
format:
	mvn formatter:format

.PHONY: lint-check
lint-check:
	mvn pmd:check formatter:validate -Dpmd.printFailingErrors=true

.PHONY: test
test:
	mvn test
