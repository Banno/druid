#!/bin/bash

mvn clean package install -DskipTests=true -Dmaven.test.skip=true
