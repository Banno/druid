#!/bin/bash

find . -type f -name pom.xml -print0 | xargs -0 sed -i '' 's|<version>0.7.1.1</version>|<version>0.7.1.1-hadoop-2.0.0-cdh4.4.0</version>|g'
