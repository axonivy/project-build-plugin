#!/bin/bash
set -e

newVersion=${1}

mvn --batch-mode -f pom.xml versions:set-property versions:commit -Dproperty=ivy.api -DnewVersion=${newVersion}
