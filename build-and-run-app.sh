#!/bin/sh

mvn clean package
java -jar target/springer-nature-test-1.0-SNAPSHOT-fat.jar
