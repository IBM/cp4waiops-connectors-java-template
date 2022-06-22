# cp4waiops-connectors-java-template

Template for java connectors based on Open Liberty

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
          newName: hyc-cp4mcm-team-docker-local.artifactory.swg-devops.com/cp/aiopsedge/java-grpc-connector-template
          newTag: sdk-updates
```

## Development

First [obtain the sdk](#obtain-the-sdk).

To run the liberty server outside of a docker container, navigate to the top directory and run
`mvn liberty:run`. Settings can be provided in the `src/main/liberty/config/bootstrap.properties` or
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
```

After running `mvn liberty:run`, your connector will get the configuration from the gRPC server.

## Setting the authSecret in the `bundlemanifest.yaml`
1. Create a GitHub Authentication Secret
1. Navigate to https://GITHUB_URL/settings/tokens
1. Click Generate New Token
1. Enter a description into the Note field and then click Generate Token at the bottom of the page
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

## Metric Template Walkthrough

The metric template was designed to show how to create a connector that triggers metric Anomalies in the Alert Viewer.

**Steps**
1. Navigate to Data and tool connections in the UI and click Add Connection.
2. Scroll down until you see Java gRPC Connector Template and click Add Connection and then Connect.
3. Fill out the Name field, ensure Enable gathering of connector metrics is toggled to the On position. Use default Metric name or type in a custom name. Select Historical mode, a Start Date (1 month prior to the end date is recommended), and End Date (Current date is recommended to generate a graph without gaps)
Note: To run through this process again choose a different metric name than the one you used before.
4. Click Done.
Note: Optional Steps below
In OpenShift you should see the java-grpc-connector-template pod created wait for the following logs to appear
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
10. Turn the Enable workload with high CPU usage toggle to On to begin generating higher values for your metrics. And switch to Live (if optional step was skipped) and click Save.
11. Navigate to Stories and alerts, then click Alerts.
12. After a few minutes you will see an Alert with resource name `database01.bigblue.com` and with the name of the Metric you specified when creating the connection.
13. Clicking on the Alert will generate a menu in the lower right with more details
14. Uncollapse the Information tab and select Metric anomaly details and click on View expanded chart to see the full timeline.
15. Navigate back to edit the connection and turn off the high CPU usage toggle
16. After a few minutes the generated Metric Alert will be cleared automatically.

**Debugging**
If no alert is generated after waiting for a few minutes, ensure that at no point you toggled Enable workload with high CPU usage to On. 

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
You can run this command after you see `Done sending sample data` in the java-grpc-connector-template pod to confirm sample data was consumed. Once you change the connection to live and query the above again, you will see one new entry every 5 minutes or so. And the values will be between 0 and 1 unless you have turned on Enable workload with high CPU usage, which will create values around 99. 

Running the above curl command can also confirm whether you trained the data properly or not. The results should show `"anomalous":false` for low values and true for high values if you walked through the steps in the proper order. If you see a value in the 90s but with anomalous as false, then try walking through these steps again in order but with a different Metric Name, making sure you don't turn on live data with high CPU before training.

You can find a sample Metric [here](sample-metric.json)

## Generic Topology Walkthrough
The generic topology allows you to create relationships between resources for the topology viewer.


![Resource management](/images/generic-topology01.png)
The sample creates the resources

![Topology](/images/generic-topology02.png)
The resources have relationships

The sample will start up the generic topology processor pod.

To check if the pod is running:
```bash
oc get pods | grep generic-topology-processor
```

To check the pod logs:
```bash
oc logs -f $(oc get pod -l connector.aiops.ibm.com/name=generic-topology-processor -o jsonpath='{.items[0].metadata.name}')
```


The generic topology processor gets topology data from Kafka and inserts it into the Elastic database. In the [bundle's prereqs](bundle-artifacts/prereqs) folder, there are yaml files for configuring and deploying the processor:

```yaml
generictopologyprocessor-deployment.yaml
generictopologyprocessor-service.yaml
generictopologyprocessor-serviceaccount.yaml
generictopologyprocessor-observerrole.yaml
generictopologyprocessor-observerrolebinding.yaml
```

When the generic topology processsor is correctly configured, it will be in a running state and there should be no errors in the logs.

Once it is running, the connector will communicate with the processor via Kafka. The topic is `cp4waiops-cartridge.connector-generic.svc_topology.connector_report`.

- This topic has its write permissions set in the [connector schema](bundle-artifacts/prereqs/connectorschema.yaml) 
- This topic is defined in the [topics file](bundle-artifacts/prereqs/topics.yaml) 
- This topic is set as one that messages are produced to in the `ConnectorTemplate.java`, so the gRPC server knows the communication is on that topic

### Kafka Message Format
Example Kafka message:
```json
{
  "nodes": [
    {
      "name": "Dog Owner",
      "entityTypes": [
        "owner",
        "human"
      ],
      "id": "id1",
      "properties": {
        "prop2": "value2",
        "prop1": "value1"
      },
      "tags": [
        "Sample Topology Data"
      ]
    },
    {
      "name": "Dog",
      "entityTypes": [
        "pet"
      ],
      "id": "id2",
      "properties": {
        "prop2": "value2",
        "prop1": "value1"
      },
      "tags": [
        "Sample Topology Data"
      ]
    }
  ],
  "edges": [
    {
      "destination": "id2",
      "source": "id1",
      "properties": {
        "prop2": "value2",
        "prop1": "value1"
      },
      "relation": "attachedTo"
    }
  ]
}
```

- Each node in `nodes` will show up as a resource
- The `edges` defines the relationships between the nodes
- The `id` is the unique identifier and is later referenced for the edges
- `entityTypes` describes the type of data. There are built in types defined in this [document](https://www.ibm.com/docs/en/cloud-paks/cloud-pak-watson-aiops/3.3.2?topic=reference-entity-types) that will display an icon, otherwise a generic icon will be used
- The `relation` in `edges` are defined in this [document](https://www.ibm.com/docs/en/cloud-paks/cloud-pak-watson-aiops/3.3.2?topic=reference-edge-types). If the type is missing, an error will be thrown in the generic topology processor logs

### Known Limitations
- Only one generic processor pod can be run, as a result only one connector type should use this generic processsor to ensure only one pod is run at a time
- All the data and relationships have to be sent in a single Kafka message. As a result, there are 1 MB size limitations on the Kafka messages. Paging may be implemented in the future

### Cleaning Up the Topology (WARNING: Deletes All Topology Data, Use In Dev Environment Only)
As you insert topology data, the resources can quickly fill up. To clean up the topology resources, you can run the following commands. However, this will delete ALL topology data, so do not run this on a production environment. Use this only in a development environment.

As a prerequsitiie, you have to expose the elastsic route for the service `iaf-system-elasticsearch-es`, in this example, the secure route is created with name `iaf-system-elasticsearch-es-aiops`


```bash
CASSANDRA_USER=admin
CASSANDRA_PASS=`oc get secret aiops-topology-cassandra-auth-secret -o jsonpath='{.data.password}' | base64 -d`

ELASTIC_USER=elasticsearch-admin
ELASTIC_PASS=`oc get secret ibm-cp-watson-aiops-elastic-admin-secret -o jsonpath='{.data.password}' | base64 -d`

ELASTIC_AUTH=`echo -n $ELASTIC_USER:$ELASTIC_PASS | base64 -w 0`

oc scale deployment aiops-topology-layout --replicas=0
oc scale deployment aiops-topology-merge --replicas=0
oc scale deployment aiops-topology-search --replicas=0
oc scale deployment aiops-topology-status --replicas=0
oc scale deployment aiops-topology-topology --replicas=0
oc scale deployment aiops-topology-observer-service --replicas=0
oc scale deployment aiopsedge-instana-topology-integrator --replicas=0
oc scale deployment `oc get deployment | grep ibm-grpc-instana | awk '{print $1;}'` --replicas=0
sleep 30
curl -k  --request DELETE 'https://iaf-system-elasticsearch-es-aiops.apps.beta.cp.fyre.ibm.com/aiops-searchservice_v10' \
--header 'Authorization: Basic '$ELASTIC_AUTH

oc exec -ti aiops-topology-cassandra-0 -- bash -c 'cqlsh --ssl -u $CASSANDRA_USER -p $CASSANDRA_PASS -e "DROP KEYSPACE janusgraph;"'
sleep 2
oc exec -ti aiops-topology-cassandra-0 -- bash -c 'cqlsh --ssl -u $CASSANDRA_USER -p $CASSANDRA_PASS -e "SELECT * FROM system_schema.keyspaces;"'

oc scale deployment aiops-topology-layout --replicas=1
oc scale deployment aiops-topology-merge --replicas=1
oc scale deployment aiops-topology-search --replicas=1
oc scale deployment aiops-topology-status --replicas=1
oc scale deployment aiops-topology-topology --replicas=1
oc scale deployment aiops-topology-observer-service --replicas=1

sleep 300

oc scale deployment aiopsedge-instana-topology-integrator --replicas=1
oc scale deployment `oc get deployment | grep ibm-grpc-instana | awk '{print $1;}'` --replicas=1
```