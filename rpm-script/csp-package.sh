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
if [ ! -z $3 ]; then
	RELEASE=$3
else
	RELEASE="1"
fi

echo "Previous Version: $PREV_VERSION"
echo "New Version: $VERSION"
echo "Release: $RELEASE"

FILE_NAME=cspclient-"$VERSION"

pushd ${DIR}

cd ${DIR}/rpm/SPECS/

echo "Starting to create the RPM spec file"

cp -i cspclient-"$PREV_VERSION".spec "$FILE_NAME".spec
nano "$FILE_NAME".spec

echo "Finished creating the RPM spec file"

cd ${DIR}

echo "Making the RPM Package"

mkdir $FILE_NAME
cp ${DIR}/../target/csprocessor-client-"$VERSION".jar "$FILE_NAME"/csprocessor.jar
tar -czf ${DIR}/rpm/SOURCES/"$FILE_NAME".tar.gz "$FILE_NAME"/
rpmbuild --define "_topdir ${DIR}/rpm" -bb ${DIR}/rpm/SPECS/"$FILE_NAME".spec
rm -r $FILE_NAME

echo "Finished making the RPM package"

scp ${DIR}/rpm/RPMS/noarch/${FILE_NAME}-${RELEASE}.noarch.rpm root@csprocessor.cloud.lab.eng.bne.redhat.com:/root/

popd

echo Run the following commands on the YUM server
echo "cp ~/${FILE_NAME}-${RELEASE}.noarch.rpm /var/www/html/yum/updates/noarch/" 
echo createrepo --update --no-database /var/www/html/yum/updates/
