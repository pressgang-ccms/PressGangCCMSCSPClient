#!/bin/bash

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
mvn clean install
if [[ $? != 0 ]]; then
  error
fi

# Build the zanata interface
cd ../PressGangCCMSZanataInterface
mvn clean install
if [[ $? != 0 ]]; then
  error
fi

# Build the REST Commons library 
cd ../PressGangCCMSRESTCommon            
mvn clean install
if [[ $? != 0 ]]; then
  error
fi

# Build the rest commons
cd ../PressGangCCMSRESTv1Common
mvn clean install
if [[ $? != 0 ]]; then
  error
fi 

# Build the content spec commons
cd ../PressGangCCMSContentSpec
mvn clean install
if [[ $? != 0 ]]; then
  error
fi

# Move back to the current directory
popd
