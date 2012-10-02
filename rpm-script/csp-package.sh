#!/bin/bash

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

pushd ~
echo "Starting to compile the CSP"

# Compile the CSP
./compile-csp.sh

if [[ $? != 0 ]]; then
    exit 1
fi

echo "Finished compiling the CSP"

cd ~/rpm/SPECS/

echo "Starting to create the RPM spec file"

cp -i cspclient-"$PREV_VERSION".spec "$FILE_NAME".spec
nano "$FILE_NAME".spec

echo "Finihsed creating the RPM spec file"

cd ~/

echo "Making the RPM Package"

mkdir $FILE_NAME
cp ~/git/csprocessor/client/target/csprocessor-client-"$VERSION"-SNAPSHOT.jar "$FILE_NAME"/csprocessor.jar
tar -czf ~/rpm/SOURCES/"$FILE_NAME".tar.gz "$FILE_NAME"/
rpmbuild -bb ~/rpm/SPECS/"$FILE_NAME".spec
rm -r $FILE_NAME

echo "Finished making the RPM package"

scp rpm/RPMS/noarch/"$FILE_NAME"-1.noarch.rpm root@csprocessor.cloud.lab.eng.bne.redhat.com:/root/

popd
