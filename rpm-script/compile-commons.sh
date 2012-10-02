#!/bin/bash

# The projects need a higher version of maven than is available 
# as an RPM in RHEL, so allow an override 
MAVEN_BIN=/opt/apache-maven-3.0.4/bin/mvn

# Get the directory hosting the script. This is important if the script is called from 
# another working directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function error()
{
  echo "An Error occured. Exiting..."
  popd
  exit 1
}

# Move to the git directory
pushd ~/git

# Build the common utilities first
cd ./PressGangCCMSCommonUtilities
${MAVEN_BIN} clean install
if [[ $? != 0 ]]; then
  error
fi

# Build the zanata interface
cd ../PressGangCCMSZanataInterface
${MAVEN_BIN} clean install
if [[ $? != 0 ]]; then
  error
fi

# Build the REST Commons library 
cd ../PressGangCCMSRESTCommon            
${MAVEN_BIN} clean install
if [[ $? != 0 ]]; then
  error
fi

# Build the rest commons
cd ../PressGangCCMSRESTv1Common
${MAVEN_BIN} clean install
if [[ $? != 0 ]]; then
  error
fi 

# Build the content spec commons
cd ../PressGangCCMSContentSpec
${MAVEN_BIN} clean install
if [[ $? != 0 ]]; then
  error
fi

# Move back to the current directory
popd
