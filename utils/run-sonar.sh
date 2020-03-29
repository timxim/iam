#!/bin/bash
set -e

SONAR_PROJECT_KEY=${SONAR_PROJECT_KEY:-indigo-iam_iam}
SONAR_ORGANIZATION=${SONAR_ORGANIZATION:-indigo-iam}
SONAR_HOST_URL=${SONAR_HOST_URL:-https://sonarcloud.io}

if [ -z ${SONAR_AUTH_TOKEN} ]; then
  echo "Please set the SONAR_AUTH_TOKEN env variable with the sonar auth token"
  exit 1
fi

GIT_BRANCH_NAME=$(echo ${BRANCH_NAME-$(git rev-parse --abbrev-ref HEAD)})

mvn \
  -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
  -Dsonar.organization=${SONAR_ORGANIZATION} \
  -Dsonar.host.url=${SONAR_HOST_URL} \
  -Dsonar.login=${SONAR_AUTH_TOKEN} \
  -Dsonar.branch.name=${GIT_BRANCH_NAME} \
  clean package sonar:sonar
