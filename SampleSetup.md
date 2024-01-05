# Sample Setup Goal
A complete end to end scenario to get a custom connector running in CP4AIOps

Completed code can be found via: https://github.com/sghung/cp4waiops-connectors-java-template

# Prerequisites
- Podman (https://podman.io/docs/installation)
- CPAIOps installed
- Docker image repository

# Start
1. Fork the repo: https://github.com/IBM/cp4waiops-connectors-java-template. For example, the new repo I forked is in: https://github.com/sghung/cp4waiops-connectors-java-template
1. Start podman:
    ```bash
    podman machine start
    ```
1. Login to Docker, for example:
    ```bash
    docker login docker.io/sghung
    ```
1. Build the image by calling the following command from the root of the project directory. For the tag, use the Docker image location you had previously logged into
    ```
    podman build -f container/Dockerfile -t docker.io/sghung/sample-java-template:latest .
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
        repo: 'https://github.com/sghung/cp4waiops-connectors-java-template'
        branch: main
        authSecret:
        name: test-utilities-github-token
        components:
        - name: deployment
            path: /bundle-artifacts/prereqs
            type: kustomize
    instanced:
        repo: 'https://github.com/sghung/cp4waiops-connectors-java-template'
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
   [2/2] COMMIT docker.io/sghung/sample-java-template:latest
    --> 9ee0cd654153
    Successfully tagged docker.io/sghung/sample-java-template:latest
    9ee0cd654153939823c8e5a896e17c33e4b5c81d827ce44a64c88b52169d10f8
   ```

   Next, push the image via the command:
   ```
   podman push docker.io/sghung/sample-java-template:latest
   ```
1. Update the image addresses in the Bundlemanifest files. First open [/bundle-artifacts/prereqs/kustomization.yaml](/bundle-artifacts/prereqs/kustomization.yaml). I replace:
   ```yaml
   newName: PLACEHOLDER_REGISTRY_ADDRESS/cp/aiopsedge/java-grpc-connector-template
   newTag: latest
   ```
   
   with
   ```yaml
    newName: docker.io/sghung/sample-java-template
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