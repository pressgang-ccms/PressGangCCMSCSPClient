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

echo "Previous Version: ${PREV_VERSION}"
echo "New Version: ${VERSION}"
echo "Release: ${RELEASE}"

echo ""

PREV_FILE_NAME=csprocessor-${PREV_VERSION}
echo "Previous File: ${PREV_FILE_NAME}.spec"

FILE_NAME=csprocessor-${VERSION}
echo "New File: ${FILE_NAME}.spec"

echo ""

pushd ${DIR}

cd ${DIR}/rpm/SPECS/

echo "Starting to create the RPM spec file"
NEW_FILE="false"

# Copy the file if it doesn't already exist
if [ ! -f ${FILE_NAME}.spec ]; then

    # Check that the spec file to be copied from exists
    if [ ! -f "${PREV_FILE_NAME}.spec" ]; then
        echo "The previous rpm spec doesn't exist!"
        exit
    fi

    cp "${PREV_FILE_NAME}.spec" "${FILE_NAME}.spec"
    NEW_FILE="true"
fi

# Fix the Release and Version
sed "s/Version: [0-9\.]\+/Version: ${VERSION}/g" -i ${FILE_NAME}.spec
sed "s/Release: [0-9\.]\+/Release: ${RELEASE}/g" -i ${FILE_NAME}.spec

# Add the changelog message if the file is new
if [ ${NEW_FILE} = "true" ]; then
    RPM_DATE=`date '+%a %b %d %Y'`
    sed "s/%changelog/%changelog\n* ${RPM_DATE} NAME - ${VERSION}\n- /g" -i ${FILE_NAME}.spec
fi

# Open the file to add the changelog
nano ${FILE_NAME}.spec

echo "Finished creating the RPM spec file"

cd ${DIR}

echo "Making the RPM Package"

mkdir $FILE_NAME
cp ${DIR}/../target/csprocessor-client-${VERSION}.jar ${FILE_NAME}/csprocessor.jar
cp ${DIR}/../csprocessor.bash ${FILE_NAME}/csprocessor.bash
tar -czf ${DIR}/rpm/SOURCES/${FILE_NAME}.tar.gz ${FILE_NAME}/
rpmbuild --define "_topdir ${DIR}/rpm" -bb ${DIR}/rpm/SPECS/${FILE_NAME}.spec
rm -r ${FILE_NAME}

echo "Finished making the RPM package"

echo "Run the following command to upload to your YUM server, replacing USER and SERVER with proper values"
echo "scp ${DIR}/rpm/RPMS/noarch/${FILE_NAME}-${RELEASE}.noarch.rpm USER@SERVER:/var/www/html/yum/noarch/"
echo Run the following commands on the YUM server
echo createrepo --update --no-database /var/www/html/yum/

popd
