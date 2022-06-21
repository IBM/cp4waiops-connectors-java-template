## TODO: Add license

## Build application
FROM registry.access.redhat.com/ubi8/ubi-minimal:latest as builder
WORKDIR /build

RUN microdnf update && \
    microdnf install -y procps-ng java-11-openjdk tar && \
    microdnf clean all

RUN curl -s -o /tmp/mvn.tar.gz https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz && \
    mkdir /tmp/mvn && \
    tar xfz /tmp/mvn.tar.gz -C /tmp/mvn && \
    mv /tmp/mvn/apache-maven-3.8.6 /usr/local/maven

ENV PATH="/usr/local/maven/bin:${PATH}"
ENV MVN_HOME=/usr/local/maven

# Download and cache dependencies beforehand
COPY pom.xml /build/connector/
RUN cd /build/connector && mvn dependency:go-offline -B

COPY src /build/connector/src
RUN cd /build/connector && rm -r src/main/liberty && mvn install

## Final image
FROM icr.io/appcafe/websphere-liberty:22.0.0.3-kernel-java11-openj9-ubi

USER 0
RUN dnf install -y procps-ng && dnf clean all
USER 1001

COPY --chown=1001:0 container/import-certs.sh /opt/import-certs.sh
COPY --chown=1001:0 container/jvm.options /config/

# Add Liberty server configuration including all necessary features
COPY --chown=1001:0  container/server.xml /config/
RUN configure.sh

COPY --from=builder --chown=1001:0  /build/connector/target/java-grpc-connector-template.war /config/apps/

## The following are examples of security related artifacts you may want to mount to the image at runtime.
# /etc/x509/certs/tls.crt
# /etc/x509/certs/tls.key
# example argument: --mount type=bind,source=/path/to/certs,destination=/etc/x509/certs,readonly

## The following are examples of security flags you may want to turn on
# ENV SEC_TLS_TRUSTDEFAULTCERTS=true
# ENV SEC_IMPORT_K8S_CERTS=true

RUN configure.sh
