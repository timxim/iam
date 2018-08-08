#!/bin/bash
# Freely adapted from https://stackoverflow.com/questions/26082444/how-to-work-around-travis-cis-4mb-output-limit
set -e

export PING_SLEEP=30s
export BUILD_OUTPUT=travis/travis-build.out

dump_output() {
   echo Tailing the last 1000 lines of output:
   tail -1000 $BUILD_OUTPUT
}

error_handler() {
  echo ERROR: An error was encountered with the build.
  dump_output
  exit 1
}

trap 'error_handler' ERR

bash -c "while true; do echo \$(date) - building ...; sleep $PING_SLEEP; done" &
PING_LOOP_PID=$!

mvn clean package >> $BUILD_OUTPUT 2>&1
echo "IAM H2 build & test completed succesfully"
mysql -uroot -e "CREATE DATABASE iam; GRANT ALL PRIVILEGES on iam.* to 'iam'@'%' identified by 'pwd';"
echo "--> MySQL build & test" >> $BUILD_OUTPUT
IAM_DB_HOST=127.0.0.1 mvn -Dspring.profiles.active=mysql-test test >> $BUILD_OUTPUT 2>&1

echo "IAM MySQL tests completed successfully"
tail -100 $BUILD_OUTPUT

## Run SonarCloud analysis

echo "--> SonarCloud" >> $BUILD_OUTPUT
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar >> $BUILD_OUTPUT 2>&1
echo "SonarCloud analysis completed succesfully"

kill ${PING_LOOP_PID}
