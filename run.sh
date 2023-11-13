#!/bin/bash

mvn clean package

docker compose up -d --build feline