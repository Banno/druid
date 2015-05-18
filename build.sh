#!/bin/bash

mvn clean package install -DskipTests=true -Dmaven.test.skip=true

cp services/target/druid-*-bin.tar.gz ../druid-docker/base
