# 1.2-SNAPSHOT (2023-07-26)
- Updated the way Java 11 was being pulled in. There was a breaking change that caused the currently JDK to throw the error `The exception: Exception in thread "main" java.lang.Error: java.io.FileNotFoundException: /usr/lib/jvm/java-11-openjdk-11.0.20.0.8-2.el8.x86_64/lib/tzdb.dat (No such file or directory) ` during build time. To fix this problem, the base image `registry.access.redhat.com/ubi8/openjdk-11:latest` is used, which also has its own Maven    

# 1.1-SNAPSHOT (2023-07-05)
- Update `connectors-sdk` to `1.4.18`
- In 4.1.x CP4WAIOPs releases, a newly added connector has a conflicting topic `topology-connector-report`. As a result, this topic is no longer in the `prereqs` for the topic. As a result, `topics.yaml` is commented out and comments are added as to how this file could be used
- Updated Liberty and Maven version
- Added `grpc-bridge.grpc-client-keep-alive-time-seconds=60` support into Liberty's `META-INF`. This property fixes some of the UI status timeout issues
- Changed `apiAdaptor` to `grpc`
- Added `deploymentType: ["local", "remote"]` to schema. Without this entry, the connector will not show up in the UI
- Enabled more verbose logging to help with development. A comment is added to disable this level of logging when in production