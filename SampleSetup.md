# Sample Setup Goal
A complete end to end scenario to get a custom connector running in CP4AIOps

Completed code can be found via: https://github.com/requestbowlers/cp4waiops-connectors-java-template

# Prerequisites
- Podman (https://podman.io/docs/installation)
- CPAIOps installed
- Docker image repository

# Development Environment Setup
1. Fork the repo: https://github.com/IBM/cp4waiops-connectors-java-template. For example, the new repo I forked is in: https://github.com/requestbowlers/cp4waiops-connectors-java-template
1. Start podman:
    ```bash
    podman machine start
    ```
1. Login to Docker, for example:
    ```bash
    docker login requestbowlers
    ```
1. Build the image by calling the following command from the root of the project directory. For the tag, use the Docker image location you had previously logged into
    ```
    podman build -f container/Dockerfile -t requestbowlers/sample-java-template:latest .
    ```
1. While the image is building (it can take several minutes), the template code requires some modifications for it to run. Begin by updating the GitHub location. Open [bundlemanifest.yaml](bundlemanifest.yaml)
1. Update the `repo` and `branch` to match your own location. In this example, I will modify the file to be:
   ```yaml
   apiVersion: connectors.aiops.ibm.com/v1beta1
    kind: BundleManifest
    metadata:
    name: java-grpc-connector-template
    spec:
    prereqs:
        repo: 'https://github.com/requestbowlers/cp4waiops-connectors-java-template'
        branch: main
        authSecret:
        name: test-utilities-github-token
        components:
        - name: deployment
            path: /bundle-artifacts/prereqs
            type: kustomize
    instanced:
        repo: 'https://github.com/requestbowlers/cp4waiops-connectors-java-template'
        branch: main
        authSecret:
        name: test-utilities-github-token
        components:
        - name: connector
            path: /bundle-artifacts/connector
            type: kustomize
   ```

   Later in CPAIOps, this repository will be retrieved using the secret `test-utilities-github-token`. You will need to create that secret in CPAIOps with your Git token's credentials. If you'd like to rename this secret to something else, you can do that here too.

   In CPAIOps, this repository will be loaded and the directories [/bundle-artifacts/prereqs](/bundle-artifacts/prereqs) and [/bundle-artifacts/connector](/bundle-artifacts/connector) will have the yaml files deployed.

   As part of the deployment, the image that is being built will be defined here.
1. If your image was successfully built, you'll see a message like:
   ```bash
   [2/2] COMMIT requestbowlers/sample-java-template:latest
    --> 9ee0cd654153
    Successfully tagged requestbowlers/sample-java-template:latest
    9ee0cd654153939823c8e5a896e17c33e4b5c81d827ce44a64c88b52169d10f8
   ```

   Next, push the image via the command:
   ```
   podman push requestbowlers/sample-java-template:latest
   ```
1. Update the image addresses in the Bundlemanifest files. First open [/bundle-artifacts/prereqs/kustomization.yaml](/bundle-artifacts/prereqs/kustomization.yaml). I replace:
   ```yaml
   newName: PLACEHOLDER_REGISTRY_ADDRESS/cp/aiopsedge/java-grpc-connector-template
   newTag: latest
   ```
   
   with
   ```yaml
    newName: requestbowlers/sample-java-template
    newTag: latest
   ```

   If your tag is not `latest`, update `newTag` as needed

1. Another image that needs to be updated is the generic topology image. To find this image, login with the OpenShift CLI. `cp.icr.io/cp/cp4waiops/generic-topology-processor` needs the proper tag from the install. I get that tag or digest via the call:

    ```bash
    oc describe ClusterServiceVersion | grep generic-topology-processor 
    ```

    I update my [/bundle-artifacts/prereqs/kustomization.yaml](/bundle-artifacts/prereqs/kustomization.yaml) from:

    ```yaml
    - name: generic-topology-processor
    newName: cp.icr.io/cp/cp4waiops/generic-topology-processor
    digest: REPLACE_WITH_DIGEST_FROM_INSTALL
    ```

    to:

    ```yaml
    - name: generic-topology-processor
    newName: cp.icr.io/cp/cp4waiops/generic-topology-processor
    newTag: v4.1.1-20230716.2205-62247c872
    ```

1. Commit the changes into GitHub into the `main` branch so the `bundlemanfiest.yaml` will pickup the changes
1. Next, prepare the OpenShift cluster to pull from the GitHub repository. Ensure you generate a GitHub token that can read from your code repository

    ```bash
    oc create secret generic test-utilities-github-token --from-literal=username=<GitHub Username> --from-literal=password=<GitHub access token>
    ```

1. In the OpenShift Console, you can create an image pull secret. Create the name as `ibm-aiops-pull-secret` and put in your Docker information:
![Secret](image/../images/image-pull-secret.png)

1. Now to deploy the BundleManifest
    ```bash
    oc apply -f bundlemanifest.yaml
    ```

1. Check if the connector was successfully configured (this may take a few seconds):
    ```bash
    oc get BundleManifest | grep java-grpc-connector-template
    java-grpc-connector-template    Configured
    ```

1. Restart the `connections-ui` pod forcefully, or you can wait 5-10 minutes for it to auto refresh. Don't kill this pod if other people are using CP4AIOps:

    ```bash
    oc get pods | grep connections-ui
    aiops-connections-ui-57dc845f75-zls5c                             1/1     Running                  0               40h

    oc delete pod aiops-connections-ui-57dc845f75-zls5c
    ```

1. In the CP4AIOps UI, you will now see an integration for Java gRPC Connector Template
    ![integration](images/java-grpc-integration.png)

# Development with Maven + Generating Images
So how do you use maven in all of this?

You can make modifications to the connector. For example, you can modify [ConnectorTemplate.java](src/main/java/com/ibm/aiops/connectors/template/ConnectorTemplate.java). You can also modify the test cases to add new unit tests.

To verify the build and tests work, you would run:
```bash
mvn install
```

To build an updated image, you would do (replace with your own image repository):
```bash
podman build -f container/Dockerfile -t requestbowlers/sample-java-template:latest .
podman push requestbowlers/sample-java-template:latest
```

Once the image is pushed to your repository, then you can restart the pod:
```bash
oc get pods | grep java-grpc-connector-template
java-grpc-connector-template-49445af7-ef2a-4692-b564-636a354bgq   1/1     Running                  0              12m
oc delete pod java-grpc-connector-template-49445af7-ef2a-4692-b564-636a354bgq
```

The new pod that starts up will pull the latest image.

# Development with Maven + Local deployment
Create a new integration for the java template. This time, ensure this deployment option is selected:

![integration](images/remote.png)

Download the script:
![integration](images/script.png)

For local development, which means you can update your code without generating an image, you need to work with a remote connection. This remote connection means that CP4AIOps is waiting for the pod to be started in a remote instance (such as your local development laptop).

Create a folder called `certs` in the root directory.

In [src/main/liberty/config/bootstrap.properties](src/main/liberty/config/bootstrap.properties), this file defines the properties used to connect to the gRPC server.

For example (I hid the host):
```
# These settings are developer setting, they will not be used in the produced docker file

# From the secret connector-bridge-connection-info
grpc-bridge.host=HOSTHIDDEN
grpc-bridge.port=443
# When run locally, this file is copied to <PROJECT ROOT>/target/liberty/wlp/usr/servers/defaultServer
grpc-bridge.server-certificate-file="../../../../../../certs/ca.crt"
grpc-bridge.client-certificate-file="../../../../../../certs/tls.crt"
grpc-bridge.client-private-key-file="../../../../../../certs/tls.key"

# From the secret connector-local-orchestrator
grpc-bridge.client-id="782cb888-b565-4e0b-be17-5d08ad16d680"
grpc-bridge.client-secret="qO6ljMDvAUVtCe6IamSKmTj2Sr3Im6SYYqdSFeDW2XEoQazWEFKtKj5WA8fN"

# The UUID of the Service Now ConnectorConfiguration
connector-template.id="782cb888-b565-4e0b-be17-5d08ad16d680"

com.ibm.ws.logging.console.format=simple#json
com.ibm.ws.logging.console.source=message,trace
com.ibm.ws.logging.console.log.level=info
com.ibm.ws.logging.trace.file.name=stdout
com.ibm.ws.logging.trace.format=BASIC
com.ibm.ws.logging.trace.specification="*=warning:com.ibm.aiops.connectors.*=all"
```

I create a `certs` folder, I looked at the script and copied the following properties:
- Copy `caCertificate` property into `certs/ca.crt` (everything starting from `-----BEGIN CERTIFICATE-----` and ending with `-----END CERTIFICATE-----`)
- Copy `tlscrt` property into `certs/tls.crt` (everything starting from `-----BEGIN CERTIFICATE-----` and ending with `-----END CERTIFICATE-----`)
- Copy `tlskey` property into `certs/tls.key` (everything starting from `-----BEGIN RSA PRIVATE KEY-----` and ending with `-----END RSA PRIVATE KEY-----`)
- Copy `host` into the property `grpc-bridge.host` 
- Copy `clientID` into the property `grpc-bridge.client-id` and `connector-template.id`
- Copy `clientSecret` into the property `grpc-bridge.client-secret`

Now, I got to the root of the project and run:
```
mvn liberty:run
```

This is how we do development on these connectors, since you don't need to generate an image each time. You can make changes, then do `mvn liberty:run` to pickup the new changes. To terminate it, you can end the process. If it's stuck running, you can do `mvn liberty:stop`. 