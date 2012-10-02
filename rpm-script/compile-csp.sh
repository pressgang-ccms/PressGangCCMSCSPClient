#!/bin/bash

function error()
{
  echo "An Error occured("$1"). Exiting..."
  popd
  exit $1
}

pushd ~/
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
mvn clean package
ERROR_CODE=$?
if [[ $ERROR_CODE != 0 ]]; then
  error $ERROR_CODE
fi

# Move back to the current directory
popd
popd
