#!/bin/bash
# this scripts builds and runs containers which perform the test of a maven based download
NET_NAME=axoniy-mvn-downloadtest 
cd "$(dirname "$(realpath "$0")")";
docker network create $NET_NAME
docker build -t axonivy-mvn-downloadtest-testrepo -f Dockerfile-mvnrepo .
docker build -t axonivy-mvn-downloadtest-mvnbuild -f Dockerfile-mvnbuild ../
docker rm -f axonivy-mvn-downloadtest-testrepo
docker rm -f axonivy-mvn-downloadtest-mvnbuild
docker run -d --net="$NET_NAME" --name axonivy-mvn-downloadtest-testrepo axonivy-mvn-downloadtest-testrepo
docker run -ti --rm --net="$NET_NAME" --name axonivy-mvn-downloadtest-mvnbuild axonivy-mvn-downloadtest-mvnbuild
