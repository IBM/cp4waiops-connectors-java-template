## TODO: Add license

## Build application
FROM registry.access.redhat.com/ubi8/openjdk-11:latest as builder
WORKDIR /build

# Download and cache dependencies beforehand
COPY --chown=jboss:jboss pom.xml /build/connector/
RUN cd /build/connector && mvn dependency:go-offline -B

COPY --chown=jboss:jboss . /build/connector
RUN cd /build/connector && rm -r src/main/liberty && mvn install

## Final image
FROM --platform=linux/amd64 icr.io/appcafe/websphere-liberty:23.0.0.12-kernel-java11-openj9-ubi
# ARG VERBOSE=true

USER 0
RUN dnf install -y procps-ng && dnf clean all
USER 1001

COPY --chown=1001:0 container/import-certs.sh /opt/import-certs.sh
COPY --chown=1001:0 container/jvm.options /config/

# Add Liberty server configuration including all necessary features
COPY --chown=1001:0  container/server.xml /config/

RUN features.sh
COPY --from=builder --chown=1001:0  /build/connector/target/java-grpc-connector-template.war /config/apps/

## The following are examples of security related artifacts you may want to mount to the image at runtime.
# /etc/x509/certs/tls.crt
# /etc/x509/certs/tls.key
# example argument: --mount type=bind,source=/path/to/certs,destination=/etc/x509/certs,readonly

## The following are examples of security flags you may want to turn on
# ENV SEC_TLS_TRUSTDEFAULTCERTS=true
# ENV SEC_IMPORT_K8S_CERTS=true

RUN configure.sh