# cp4waiops-connectors-java-template

Template for java connectors based on Open Liberty

For a development quickstart with an example, please look at [SampleSetup.md](SampleSetup.md)


## Server Setup <a name="server-setup"></a>

Ensure that the `bundlemanifest.yaml` file has been updated to point at the correct git repository and branch. In an
OpenShift project where the Cloud Pak for Watson AI Ops has been installed, apply the `bundlemanifest.yaml` file. After
verifying the status on the related BundleManifest and GitApp CRs, create a connection in the Data and Tool connections UI.

If the connection is deployed locally to the cluster, the image will need to be updated to point at the development image.
If the connection is configured for microedge deployment, the script can be modified to use the development image.

NOTE:

The image can be overriden on the bundlemanifest components. For example:
```yaml
components:
  - name: connector
    path: /bundle-artifacts/connector
    type: kustomize
    kustomization:
      images:
        - name: java-grpc-connector-template
          newName: hyc-cp4mcm-team-docker-local.artifactory.swg-devops.com/aiopsedge/java-grpc-connector-template
          newTag: sdk-updates
```

## Development

To begin, install gradle.
Then run
```bash
gradle wrapper
```

To run the liberty server outside of a docker container, navigate to the top directory and run
`./gradlew libertyRun -Druntime=wlp -DruntimeVersion=24.0.0.6`. Settings can be provided in the `src/main/liberty/config/bootstrap.properties` or
`src/main/liberty/config/server.xml` file. Be aware that values set there will not be used in the
docker container!

Execute `make test` in the root directory to run the tests.

Execute `make docker-build` in the root directory to build your image. MModify the `Makfile` to set the environment variables.

### Setting `bootstrap.properties` values

Get the following properties from the secret `connector-bridge-connection-info`:
```
grpc-bridge.host=connector-bridge-<NAMESPACE>.apps.<CLUSTER URL>
grpc-bridge.port=443
grpc-bridge.server-certificate-file="<PATH>/ca.crt"
grpc-bridge.client-certificate-file="<PATH>/tls.crt"
grpc-bridge.client-private-key-file="<PATH>/tls.key"
```

The certificates should be saved locally.

Get the following information from the secret `connector-local-orchestrator`:
```
grpc-bridge.client-id="<NAMESPACE>-local-orchestrator"
grpc-bridge.client-secret="<SECRET>"
```

Get the UUID of the `ConnectorConfiguration` instance that is created when you make a connection
```
connector-template.id="<UUID>"
grpc-bridge.id="<UUID>"
```

After running `./gradlew libertyRun -Druntime=wlp -DruntimeVersion=24.0.0.6`, your connector will get the configuration from the gRPC server.

You will see a message like:
```log
[2024-04-19, 13:13:20:196 EDT] 00000024 FeatureManage A   CWWKF0011I: The defaultServer server is ready to run a smarter planet. The defaultServer server started in 4.606 seconds.
[2024-04-19, 13:13:20:881 EDT] 00000050 ConnectorCred I   client id and secret provided
[2024-04-19, 13:13:45:405 EDT] 0000004d KafkaHelper   I   setIsLocalDeployment: false
[2024-04-19, 13:13:45:405 EDT] 0000004d StandardConne I   Number of consume topics=0
[2024-04-19, 13:13:50:408 EDT] 0000004d StandardConne I   starting run thread
[2024-04-19, 13:13:50:577 EDT] 00000054 ConnectorTemp I   cpu usage: -1.0
[2024-04-19, 13:13:50:607 EDT] 0000004e GRPCCloudEven I   starting produce stream: channel=cp4waiops-cartridge.analyticsorchestrator.metrics.itsm.raw
```

## Setting the authSecret in the `bundlemanifest.yaml`
1. Create a GitHub Authentication Secret
1. Navigate to https://GITHUB_URL/settings/tokens
1. Click Generate New Token
1. Enter a description into the Note field, select repo scope, and then click Generate Token at the bottom of the page
1. Copy the newly generated access token
1. Create a Kubernetes secret containing your GitHub credentials
1. `oc create secret generic test-utilities-github-token --from-literal=username=<GitHub Username> --from-literal=password=<GitHub access token>`

## Using your own image
If the repo image requires authentication, and if the secret: `ibm-aiops-pull-secret` doesn't exist in OpenShift
1. Create the secret: `ibm-aiops-pull-secret`  
1. Then add your image server credentials to the `ibm-aiops-pull-secret` secret.  

If building manually or using another build system, you can build and push the image with this script:
```
IMAGE=YOUR_IMAGE_LOCATION
docker build -f container/Dockerfile -t $IMAGE .
docker push $IMAGE
```
Inside both files `/bundle-artifacts/connector/kustomization.yaml` and `/bundle-artifacts/prereqs/kustomization.yaml`. In the `images` section: change `PLACEHOLDER_REGISTRY_ADDRESS` in `newName` to your image registry location. Also inside both files change `newTag` from `latest` to whatever your images tag is.

## Troubleshooting Errors
See the [Connector Troubleshooting](https://github.com/IBM/cp4waiops-connectors/blob/main/ConnectorTroubleshooting.md) document for resolving common problems.

## Metric Template Walkthrough and Development

The metric template was designed to show how to create a connector that triggers metric Anomalies in the Alert Viewer.

**Note:** The metric values are dictated by the CPU Usage toggle and assigned to the Metric name specified in the UI. The Enable workload with high CPU usage toggle is provided in this sample ConnectorSchema template in order to easily manipulate the data values for demonstration purposes. This toggle should not be used in your ConnectorSchema, as you should collect and transmit real data.

**Steps**
1. Navigate to Data and tool connections in the UI and click Add Connection.
2. Scroll down until you see Java gRPC Connector Template and click Add Connection and then Connect.
3. Fill out the Name field, ensure Enable gathering of connector metrics is toggled to the On position. Use default Metric name or type in a custom name. Select Historical mode, a Start Date (1 month prior to the end date is recommended), and End Date (Current date is recommended to generate a graph without gaps)

**Note:** To run through this process again choose a different metric name that was not used previously.

4. Click Done.

**Note:** Optional Step below to ensure there is no gap in the metric data timeline 

In OpenShift you should see the java-grpc-connector-template pod created, wait for the following logs to appear
```
5/16/22, 20:44:59:288 UTC] 00000035 ConnectorTemp I Generating sample historical data with metric name: Usage%
[5/16/22, 20:45:01:280 UTC] 00000035 ConnectorTemp I Done sending sample data
```
Edit your new connection from historical to live and ensure Enable workload with high CPU usage is still turned Off so your training data is not contaminated.

5. Navigate to AI model management.
6. Find Metric anomaly detection and click Set up training, Next, Next, Done
7. Click the three dots on the Metric anomaly detection card and select Start training.
8. Wait for training to complete.
9. Navigate back to Manage Data and Tool Connections and select Java gRPC Connector Template then the click on the name of the connection you created earlier to edit.
10. Turn the Enable workload with high CPU usage toggle to On to begin generating higher values for your metrics. And switch to Live (if optional step was skipped) and click Save, a new metric value will be generated and sent every 5 minutes.
11. Navigate to Stories and alerts, then click Alerts.
12. After a few minutes you will see an Alert with resource name `database01.bigblue.com` and with the name of the Metric you specified when creating the connection.
13. Clicking on the Alert will generate a menu in the lower right with more details
14. Uncollapse the Information tab and select Metric anomaly details and click on View expanded chart to see the full timeline.
15. Navigate back to edit the connection and turn off the high CPU usage toggle
16. After the next 5 minute interval the generated Metric Alert will be cleared automatically, refresh the page to see the alert disappear.

**Debugging**

If no alert is generated after waiting for a few minutes, ensure that at no point before training the data that you toggled Enable workload with high CPU usage to On. 

`oc login` and manually confirm data has been consumed by Metric Manager with the following commands

```bash
export ROUTE=$(oc get route | grep ibm-nginx-svc | awk '{print $2}')
PASS=$(oc get secret admin-user-details -o jsonpath='{.data.initial_admin_password}' | base64 -d)
export TOKEN=$(curl -k -X POST https://$ROUTE/icp4d-api/v1/authorize -H 'Content-Type: application/json' -d "{\"username\": \"admin\",\"password\": \"$PASS\"}" | jq .token | sed 's/\"//g')
```

Query the Metrics API and replace <METRICNAME> with the one you used when creating your connection.
```bash
curl -v "https://${ROUTE}/aiops/api/app/metric-api/v1/metrics?resource=database01.bigblue.com&group=CPU&metric=<METRICNAME>" --header "Authorization: Bearer ${TOKEN}" --header 'X-TenantID: cfd95b7e-3bc7-4006-a4a8-a73a79c71255' --insecure
```
You can run this command after you see `Done sending sample data` in the java-grpc-connector-template pod to confirm the historic sample data was consumed. Once you change the connection to live and query the above again, you will see one new entry every 5 minutes or so. And the values will be between 0 and 1 unless you have turned on Enable workload with high CPU usage, which will create values around 99. 

Running the above curl command can also confirm whether you trained the data properly or not. The results should show `"anomalous":false` for low values and true for high values if you walked through the steps in the proper order. If you see a value in the 90s but with anomalous as false, then try walking through these steps again in order but with a different Metric Name, making sure you don't turn on live data with high CPU before training.

**Development**

- You can find a sample Metric with the proper JSON format [here](sample-metric.json) 
- Send cloud events to the proper kafka topic: `cp4waiops-cartridge.analyticsorchestrator.metrics.itsm.raw`
- Cloud events must have a payload that complies with Kafka's max payload size of 1mb
- The tenant must be sent with the payload. Currently the only tenant supported in AIOps is `cfd95b7e-3bc7-4006-a4a8-a73a79c71255`
- Further MM documentation that was used to develop this template can be found [here](https://github.ibm.com/katamari/architecture/blob/master/feature-specs/metrics-anomaly/Testing.md)

## Performance Recommendations
If your connector has high loads or your cluster has high loads of data, there are two problems that can occur.

1. Retry for status setting status
   Your connector's status could be incorrect if the connector bridge was not able to get the status correctly if the cluster is overloaded.

   The API `emitStatus(ConnectorStatus.Phase.Running, StatusTTL);` returns a boolean value. If it is `false`, you should try to send again on a delay and ensure the return value is `true`.

   Your connector should also send the status within a 5 minute interval, even if the status did not change. If a status is not emit to the connector bridge for too long, the server will mark the connection as `Unknown`.

2. Query the SDK queues for preventing Out Of Memory errors
   The connector bridge and connector prevent data loss by retrying cloud events. The connector SDK will retry the cloud event when `emitCloudEvent` is called until the event has been verified.

   There are two queues that keep track of these cloud events before they are removed.

   First, is the cloud event queue, which can be queried via `ConnectorBase`'s `getEventQueueSize()`. This queue tracks events that go into `emitCloudEvent` that has not been sent to the connector bridge yet. Once it attempts a send, the cloud event goes into the unverified queue, that can be queried via `ConnectorBase`'s `getUnverifiedCount()`. You can limit the collection of data of both these queues to prevent Out Of Memory errors. For example, in the snippet below, I monitor the unverified count and stop collecting data when the queue gets to 20. I'll keep waiting until it lowers again. If Kafka is down, the verification doesn't happen, so the unverified queue can grow indefinitely until Kafka is back up again and the connector bridge can communicate with the connector again. You can choose to limit based on the `getEventQueueSize()` too, which is not shown below. If you throttle by the unverified count, it is sufficient as long as you stop emitting cloud events and you save enough space for the event queue to get populated a bit.

  ```java
  for (int i=0;i<40;i++){
      logger.log(Level.INFO, "Queue size: " + this.getEventQueueSize());
      int unverifiedCount = this.getUnverifiedCount();
      logger.log(Level.INFO, "Unverified count: " + this.getUnverifiedCount());

      if (unverifiedCount <= 20){
          logger.log(Level.INFO, "Emitting event since unverifiedCount: " + unverifiedCount);
          // This will push into Kafka, but the ce_type is purposely wrong so it doens't get processed by the Flink
          emitCloudEvent(SNOW_TOPIC_PROBLEMS, null, createEvent(0L, "com.ibm.sdlc.snow.problem.discovered.fake", fileStr, new URI("http://example.org")));
      }
  }
  ```

### Maintenance
- When Liberty updates a supported version, the Dockerfile needs to be updated [container/Dockerfile](container/Dockerfile)
- When Maven updates, the Dockerfile needs to be updated [container/Dockerfile](container/Dockerfile) needs to have the binary updated

### Known Limitations
- Only one generic processor pod can be run, as a result only one connector type should use this generic processsor to ensure only one pod is run at a time
- All the data and relationships have to be sent in a single Kafka message. As a result, there are 1 MB size limitations on the Kafka messages. Paging may be implemented in the future

## Dependency verification
For enhanced security, the build has been changed from Maven to Gradle. The [verification-metadata.xml](gradle/verification-metadata.xml) was generated.

Adding new dependencies, you can call:
```
./gradlew --write-verification-metadata sha256 -Druntime=wlp -DruntimeVersion=24.0.0.6 --no-configuration-cache
```

For depedency verification failures, please look at the reference: https://docs.gradle.org/current/userguide/dependency_verification.html

## Building the Image
Due to a conflict between the dependency verification and the Dockerfile (see https://github.com/gradle/gradle/issues/19228), the workaround is to rely on building via gradle first and the Dockerfile adds the resulting war.

To build:
```
gradle wrapper
gradle build
make podman-login
make podman-build
make podman-push

```

If you hit a security issue, look at the dependency verification