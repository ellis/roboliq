#!/usr/bin/env bash

rm -rf content/schemas
mkdir -p content/schemas/{commands,evowareConfiguration}
cd content/schemas/commands &&
ln -s ../../../../roboliq-processor/src/schemas/*.yaml . &&
cd ../evowareConfiguration &&
ln -s ../../../../roboliq-evoware/src/schemas/*.yaml .
