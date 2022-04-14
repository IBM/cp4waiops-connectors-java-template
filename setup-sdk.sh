#!/bin/bash

set -euo pipefail

if [ -z "$GITHUB_TOKEN" ]; then echo "GITHUB_TOKEN not set"; exit 1; fi
if [ -z "$1" ]; then echo "version not provided as first parameter"; exit 1; fi

OWNER="quicksilver"
REPO="grpc-java-sdk"
TAG="$1"
ARTIFACT="repo.tgz"
LIST_URL="https://github.ibm.com/api/v3/repos/${OWNER}/${REPO}/releases/tags/${TAG}"

# get url for artifact with name==$artifact
asset_url=$(curl -H "Authorization: token ${GITHUB_TOKEN}" "${LIST_URL}" | jq ".assets[] | select(.name==\"${ARTIFACT}\") | .url" | sed 's/\"//g')

rm -f repo.tgz
curl -LJO -H "Authorization: token ${GITHUB_TOKEN}" -H 'Accept: application/octet-stream' "${asset_url}"

rm -rf repo
tar xfz repo.tgz
rm -f repo.tgz

echo "sdk downloaded to repo directory"
