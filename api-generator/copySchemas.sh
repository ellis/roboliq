#!/usr/bin/env bash

rm -rf content/schemas
mkdir -p content/schemas/{commands,evowareConfiguration,evowareCommands}
cd content/schemas/commands &&
ln -s ../../../../roboliq-processor/src/schemas/*.yaml . &&
cd ../evowareConfiguration &&
ln -s ../../../../roboliq-evoware/src/schemas/*.yaml . &&
cd ../evowareCommands &&
ln -s ../../../../roboliq-evoware/src/schemas/commands/*.yaml .
