#!/usr/bin/env bash

if [ -z "$1" ]; then
    echo "No version specified"
    exit 1
fi

mvn versions:set -DnewVersion=$1 || exit 1

rm -rf pom.xml.versionsBackup
for f in * ; do
  if [ -d "$f" ]; then
    rm -rf "$f"/pom.xml.versionsBackup
  fi
done
