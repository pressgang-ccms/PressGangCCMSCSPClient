#!/bin/bash

# Get the directory hosting the script. This is important if the script is called from 
# another working directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ -z $1 ]; then
 echo "No Previous Version specified!"
 exit
fi

if [ -z $2 ]; then
 echo "No New Version specified!"
 exit
fi

PREV_VERSION=$1
VERSION=$2

echo "Previous Version: $PREV_VERSION"
echo "New Version: $VERSION"

FILE_NAME=cspclient-"$VERSION"

pushd ${DIR}
echo "Starting to compile the CSP"

# Compile the CSP
./compile-csp.sh

if [[ $? != 0 ]]; then
    exit 1
fi

echo "Finished compiling the CSP"

cd ${DIR}/rpm/SPECS/

echo "Starting to create the RPM spec file"

cp -i cspclient-"$PREV_VERSION".spec "$FILE_NAME".spec
nano "$FILE_NAME".spec

echo "Finihsed creating the RPM spec file"

cd ${DIR}

echo "Making the RPM Package"

mkdir $FILE_NAME
cp ${DIR}/../target/csprocessor-client-"$VERSION"-SNAPSHOT.jar "$FILE_NAME"/csprocessor.jar
tar -czf ${DIR}/rpm/SOURCES/"$FILE_NAME".tar.gz "$FILE_NAME"/
rpmbuild --define "_topdir ${DIR}/rpm" -bb ${DIR}/rpm/SPECS/"$FILE_NAME".spec
rm -r $FILE_NAME

echo "Finished making the RPM package"

scp ${DIR}/rpm/RPMS/noarch/"$FILE_NAME"-1.noarch.rpm root@csprocessor.cloud.lab.eng.bne.redhat.com:/root/

popd
