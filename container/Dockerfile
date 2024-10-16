## Build application
# Note: currently the gradle verification is failing due to these test cases, The workaround
# in the local case is to build the gradle project and then copy the war, instead of having
# the dockerfile build the war
# Reference: https://github.com/gradle/gradle/issues/19228
# 
# > Dependency verification failed for configuration ':classpath'
# 2 artifacts failed verification:
#    - junit-bom-5.9.2.module (org.junit:junit-bom:5.9.2) from repository MavenRepo
#    - junit-bom-5.9.3.module (org.junit:junit-bom:5.9.3) from repository MavenRepo
# FROM gradle:7.6-jdk-focal as builder
# WORKDIR /build
# 
# COPY . /build/connector
# RUN cd /build/connector && gradle build

## Final image
FROM --platform=linux/amd64 icr.io/appcafe/websphere-liberty:24.0.0.6-kernel-java11-openj9-ubi
# ARG VERBOSE=true

USER 0
RUN dnf install -y procps-ng && dnf clean all
USER 1001

COPY --chown=1001:0 container/import-certs.sh /opt/import-certs.sh
COPY --chown=1001:0 container/jvm.options /config/

# Add Liberty server configuration including all necessary features
COPY --chown=1001:0  container/server.xml /config/

RUN features.sh
COPY --chown=1001:0  build/libs/*.war /config/apps/

## The following are examples of security related artifacts you may want to mount to the image at runtime.
# /etc/x509/certs/tls.crt
# /etc/x509/certs/tls.key
# example argument: --mount type=bind,source=/path/to/certs,destination=/etc/x509/certs,readonly

## The following are examples of security flags you may want to turn on
# ENV SEC_TLS_TRUSTDEFAULTCERTS=true
# ENV SEC_IMPORT_K8S_CERTS=true

RUN configure.sh