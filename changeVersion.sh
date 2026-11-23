#!/usr/bin/env bash

if [ -z "$1" ]; then
    echo "No version specified"
    exit 1
fi

mvn versions:set -DnewVersion="$1" -DgenerateBackupPoms=false
