#!/bin/bash

# The projects need a higher version of maven than is available 
# as an RPM in RHEL, so allow an override 
if [ -z "$MAVEN_BIN" ] && [ -z "$MAVEN_HOME" ]; then
	MAVEN_BIN=/opt/apache-maven-3.0.4/bin/mvn
elif [ -z "$MAVEN_BIN" ]; then
	MAVEN_BIN="$MAVEN_HOME"/bin/mvn
fi

# Get the directory hosting the script. This is important if the script is called from 
# another working directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function error()
{
  echo "An Error occured("$1"). Exiting..."
  popd
  exit $1
}

pushd ${DIR}
# Build the common libraries first
./compile-commons.sh
ERROR_CODE=$?
if [[ $ERROR_CODE != 0 ]]; then
  error $ERROR_CODE
fi

# Move to the git directory
pushd ~/git

# Build the csp components and client
cd ./csprocessor
${MAVEN_BIN} clean package -DskipTests
ERROR_CODE=$?
if [[ $ERROR_CODE != 0 ]]; then
  error $ERROR_CODE
fi

# Move back to the current directory
popd
popd
