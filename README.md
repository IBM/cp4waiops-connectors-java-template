# cp4waiops-connectors-java-template

Template for java connectors based on Open Liberty

## Downloading the SDK <a name="obtain-the-sdk"></a>

Release versions of the sdk can be found [here](https://github.ibm.com/quicksilver/grpc-java-sdk/releases). Make sure
that `connector/pom.xml` uses the correct version. For example, `v1.0.1` maps to `1.0.1` in the `pom.xml` file.

To upgrade to another version of the SDK, modify the value of `CONNECTOR_SDK_VERSION` in the `Makefile`.

1. Set the environment variable GITHUB_TOKEN to a github api key with full repo access.
2. Run `make download-connector-sdk` from the `open-liberty` folder.

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

To run the liberty server outside of a docker container, navigate to the top` directory and run
`mvn liberty:run`. Settings can be provided in the `src/main/liberty/config/bootstrap.properties` or
`src/main/liberty/config/server.xml` file. Be aware that values set there will not be used in the
docker container!

Execute `make test` from the `open-liberty` folder to run the tests.
