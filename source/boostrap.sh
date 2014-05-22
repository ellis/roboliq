#!/usr/bin/env sh

set -e

wget http://hdfgroup.org/ftp/HDF5/hdf-java/current/bin/hdf-java-2.10-linux.tar.gz
tar xf hdf-java-2.10-linux.tar.gz
cd hdf-java-2.10
./HDF-JAVA-2.10.0-Linux.sh
rsync -arv HDF-JAVA-2.10.0-Linux/usr/lib/ ../base/lib/
