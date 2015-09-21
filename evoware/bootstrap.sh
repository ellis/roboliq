#!/usr/bin/env sh

set -e

wget 'https://wiki-bsse.ethz.ch/download/attachments/26609237/cisd-jhdf5-13.06.2-r29633.zip?version=1&modificationDate=1376120226957&api=v2'
unzip cisd-jhdf5-13.06.2-r29633.zip
mkdir -p base/lib
cp cisd-jhdf5/lib/batteries_included/cisd-jhdf5-batteries_included_lin_win_mac.jar base/lib

#wget http://hdfgroup.org/ftp/HDF5/hdf-java/current/bin/hdf-java-2.10-linux.tar.gz
#tar xf hdf-java-2.10-linux.tar.gz
#cd hdf-java-2.10
#./HDF-JAVA-2.10.0-Linux.sh
#rsync -arv HDF-JAVA-2.10.0-Linux/usr/lib/ ../base/lib/
