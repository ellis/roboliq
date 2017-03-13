#!/usr/bin/env bash

# This deletes all the roboliq modules in the node_modules directories,
# because I don't know how to synchronize the compilations propertly with
# npm.  Fix this later.  Ellis, 2017-03-13

rm -rf node_modules/roboliq*
rm -rf roboliq-processor/node_modules/roboliq*
rm -rf roboliq-evoware/node_modules/roboliq*
npm i
