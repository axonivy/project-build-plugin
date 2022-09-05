FROM maven:3.8.6-eclipse-temurin-17

RUN addgroup --gid 1000 build && adduser --uid 1000 --gid 1000 --disabled-password --gecos "" build
RUN apt-get -y update && apt-get -y install gnupg2 && apt-get install git -y
