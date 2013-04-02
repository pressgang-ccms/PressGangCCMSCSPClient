#!/bin/bash

# The projects need a higher version of maven than is available 
# as an RPM in RHEL, so allow an override 
if [ -z "$MAVEN_BIN" ] && [ -z "$MAVEN_HOME" ]; then
	MAVEN_BIN=/usr/bin/mvn
elif [ -z "$MAVEN_BIN" ]; then
	MAVEN_BIN="$MAVEN_HOME"/bin/mvn
fi

# Get the directory hosting the script. This is important if the script is called from 
# another working directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function error()
{
  echo "An Error occurred("$1"). Exiting..."
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

# Build the processor
cd ./PressGangCCMSContentSpecProcessor
${MAVEN_BIN} clean install
ERROR_CODE=$?
if [[ $ERROR_CODE != 0 ]]; then
  error $ERROR_CODE
fi

# Build the builder
cd ../PressGangCCMSBuilder
${MAVEN_BIN} clean install
ERROR_CODE=$?
if [[ $ERROR_CODE != 0 ]]; then
  error $ERROR_CODE
fi

# Build the client
cd ../PressGangCCMSCSPClient
${MAVEN_BIN} clean package
ERROR_CODE=$?
if [[ $ERROR_CODE != 0 ]]; then
  error $ERROR_CODE
fi

# Move back to the current directory
popd
popd
