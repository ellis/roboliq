* Install Java JDK 7
* Install sbt
* You probably also want to install the Scala IDE for eclipse

# Get the JHDF5 libraries

From within the `source` directory:

    wget https://wiki-bsse.ethz.ch/download/attachments/26609237/cisd-jhdf5-13.06.2-r29633.zip
	unzip cisd-jhdf5*.zip
    mkdir -p base/lib
    cp cisd-jhdf5/lib/batteries_included/cisd-jhdf5-batteries_included_lin_win_mac.jar base/lib

# Windows

Need to put a copy of sbt-launch.jar in the `source` directory.
To run sbt, I used the standard `cmd` console, changed to the `source` directory, and ran the `sbt` batch file.