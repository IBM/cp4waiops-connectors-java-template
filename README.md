# cp4waiops-connectors-java-template

Template for java connectors based on Open Liberty

## Downloading the SDK <a name="obtain-the-sdk"></a>

Release versions of the sdk can be found [here](https://github.ibm.com/quicksilver/grpc-java-sdk/releases). Make sure
that `connector/pom.xml` uses the correct version. For example, `v1.0.1` maps to `1.0.1` in the `pom.xml` file.

To upgrade to another version of the SDK, modify the value of `CONNECTOR_SDK_VERSION` in the `Makefile`.

1. Set the environment variable GITHUB_TOKEN to a github api key with full repo access.
2. Run `make download-connector-sdk` from the `open-liberty` folder.

## Server Setup <a name="server-setup"></a>

Create a connector schema for the template connector, setup the topics, and then create the configuration.
```sh
oc create -f misc/connector-template-schema.yaml
oc create -f misc/connector-template-topics.yaml
oc create -f misc/example-template-configuration.yaml
```

Retrieve the connector id:
```sh
CONNECTORID=$(oc get connectorconfiguration example-template-connector -o jsonpath={.metadata.uid})
echo $CONNECTORID
```

If authentication has been enabled, retrieve the client id and secret associated with the configuration or construct
one using the example in `misc/example-client-registration.json`.

The server certificate can be found in the `connector-bridge-cert-secret` secret. The value of `ca.crt` needs to be
fetched and base64 decoded (if using the cli).

The client certificate can be found in the `aiopsedge-client-cert` secret. The values of `tls.crt` and `tls.key` need
to be fetched and base64 decoded (if using the cli).

## Docker Container

First [obtain the sdk](#obtain-the-sdk).

To build the docker image, navigate to this folder in a terminal and execute:
```sh
docker build -f container/Dockerfile -t connector-template .
```

To run the docker image, copy the server certs to a folder, gather the connector info, and run it like so:
```sh
docker run \
  -e grpc-bridge.host=connector-bridge-aiopsedge.apps.victor.cp.fyre.ibm.com \
  -e grpc-bridge.port=443 \
  -e grpc-bridge.server-certificate-file=/service-bindings/grpc-bridge/ca.crt \
  -e grpc-bridge.client-certificate-file=/service-bindings/grpc-bridge/tls.crt \
  -e grpc-bridge.client-private-key-file=/service-bindings/grpc-bridge/tls.key \
  -e connector-template.id="f5aa7fa9-92eb-4bec-942c-37eb3e3e9601" \
  -e connector-template.client-id="example-template-connector-1938af98" \
  -e connector-template.client-secret="cryptographically-secure-value" \
  --mount type=bind,source=/path/to/certs,destination=/service-bindings/grpc-bridge -it connector-template
```
Make sure to update the values and certificate path used above!

To see the cloud events being produced and sent, enable <a name="tracing">tracing</a> in `container/server.xml`.
```xml
<server>
  <!-- other settings... -->
  <!-- trace logging for debugging, do NOT ship with this enabled -->
  <logging consoleFormat="simple" consoleSource="message,trace" consoleLogLevel="info" traceFileName="stdout" traceFormat="BASIC" traceSpecification="com.ibm.aiops.connectors.*=all" />
</server>
```

The server.xml can be dynamically changed without restarting open liberty server. In container case, use `vi` to modify `/config/server.xml` in container and save. Similar logs like following from instana connector can be found once it's updated. 

```
[3/11/22, 6:38:37:517 UTC] 00000040 TraceSpecific I   TRAS0018I: The trace state has been changed. The new trace state is *=info:com.ibm.aiops.connectors.*=all:com.ibm.watson.aiops.connectors.instana.*=all:com.ibm.watson.aiops.instana.*=all.
[3/11/22, 6:38:37:524 UTC] 00000025 ConfigRefresh A   CWWKG0017I: The server configuration was successfully updated in 0.680 seconds.
```

## Troubleshooting

Enable [trace logging](#tracing) and look for exceptions.

Common Errors:

`UNAVAILABLE: ssl exception` indicates that server and/or client certificates have not been setup correctly. Refer to
the [setup instructions](#server-setup).

`UNAUTHENTICATED: unable to authenticate client` indicates that credentials were not provided, that they are invalid,
or that the permissions assigned are insufficient. Refer to the [setup instructions](#server-setup).

## Development

First [obtain the sdk](#obtain-the-sdk).

To run the liberty server outside of a docker container, navigate to the `connector` directory and run
`mvn liberty:run`. Settings can be provided in the `connector/source/main/liberty/config/bootstrap.properties` or
`connector/source/main/liberty/config/server.xml` file. Be aware that values set there will not be used in the
docker container!

Execute `make test` from the `open-liberty` folder to run the tests.

## Inputs (actions)

A cloud event of type `com.ibm.watson.aiops.connectors.template.test-requested` with an address to test.

Example:
```yaml
{
    "specversion": "1.0",
    "id": "66451fc8-41c8-4fb2-a92a-ffa7ad523238",
    "source": "template.connectors.aiops.watson.ibm.com/connectortemplate",
    "type": "com.ibm.watson.aiops.connectors.template.test-requested",
    "time": "2021-11-02T04:49:14.5608556Z",
    "address": "https://www.ibm.com",
    "partitionkey": "partition-0",
    "componentname": "connector",
    "topic": "cp4waiops-cartridge.test-actions-1",
    "connectionid": "f5aa7fa9-92eb-4bec-942c-37eb3e3e9601",
}
```

## Outputs

A cloud event of type `com.ibm.watson.aiops.connectors.template.test-completed` or
`com.ibm.watson.aiops.connectors.template.test-failed` depending on whether or not a test succeeds.

Example:
```yaml
{
    "specversion": "1.0",
    "id": "5d2cfafa-76a7-4de2-8471-fb62878dc8f3",
    "source": "template.connectors.aiops.watson.ibm.com/connectortemplate",
    "type": "com.ibm.watson.aiops.connectors.template.test-completed",
    "time": "2021-11-02T04:53:36.543344Z",
    "connectionid": "f5aa7fa9-92eb-4bec-942c-37eb3e3e9601",
    "componentname": "connector",
    "topic": "cp4waiops-cartridge.test-output-1",
    "partitionkey": "https://www.ibm.com",
    "address": "https://www.ibm.com",
    "responsetime": "81",
    "datacontenttype": "text/plain",
    "data": "200"
}
```
