FROM maven:3.5.2-jdk-8

RUN addgroup --gid 1000 build && adduser --uid 1000 --gid 1000 --disabled-password --gecos "" build
RUN apt-get -y update && apt-get -y install gnupg2
