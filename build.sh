#!/bin/bash

cp pom.xml pom.xml.backup
sed -i 's/-->//g; s/<!--//g' pom.xml
mvn clean package
mv pom.xml.backup pom.xml
