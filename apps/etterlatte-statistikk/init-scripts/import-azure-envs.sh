#!/bin/env sh

DIR=/var/run/secrets/nais.io/azuread

echo "Attempting to export Azure AD from $DIR if it exists"

if test -d $DIR;
then
    for FILE in `ls $DIR`
    do
       KEY="AZURE_`echo $FILE | tr '[:lower:]' '[:upper:]'`"
       echo "- exporting $KEY"
       export $KEY=`cat $DIR/$FILE`
    done
fi
