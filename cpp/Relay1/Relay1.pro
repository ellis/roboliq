# Add more folders to ship with the application, here
folder_01.source = qml/Relay1
folder_01.target = qml
DEPLOYMENTFOLDERS = folder_01

# Additional import path used to resolve QML modules in Creator's code model
QML_IMPORT_PATH =

SOURCES += main.cpp disphelper.c

HEADERS += disphelper.h

LIBS += -lole32 -loleaut32 -luuid

# Installation path
# target.path =

# Please do not modify the following two lines. Required for deployment.
include(qtquick2controlsapplicationviewer/qtquick2controlsapplicationviewer.pri)
qtcAddDeployment()
