REGISTRY ?= docker.io/sghung
TAG ?= latest

IMAGE := $(REGISTRY)/cp/java-grpc-connector-template:$(TAG)

ifeq ($(shell uname -s),Darwin)
	# gnu-sed, can be installed using homebrew
	SED_EXE := gsed
else
	SED_EXE := sed
endif

podman-login:
	podman login $(REGISTRY) -u "$$REGISTRY_USERNAME" -p "$$REGISTRY_PASSWORD"

podman-build:
	chmod ug+x container/import-certs.sh
	podman build -f container/Dockerfile -t $(IMAGE) .

podman-push:
	podman push $(IMAGE)

.PHONY: format
format:
	mvn formatter:format

.PHONY: lint-check
lint-check:
	mvn pmd:check formatter:validate -Dpmd.printFailingErrors=true

.PHONY: test
test:
	mvn test
