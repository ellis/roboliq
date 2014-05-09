import QtQuick 2.1
import QtQuick.Controls 1.1
import QtQuick.Dialogs 1.0
import QtQuick.Layouts 1.1

ApplicationWindow {
    id: app
    title: qsTr("Hello World")
    width: 640
    height: 480

    property string color: "#ff0000"

    menuBar: MenuBar {
        Menu {
            title: qsTr("File")
            MenuItem {
                text: qsTr("Open")
                onTriggered: fileOpenDialog.visible = true;
            }
            MenuItem {
                text: qsTr("Save")
                //onTriggered: backend.saveImage("image.png");
                onTriggered: backend.saveImage("image.png");
            }
            MenuItem {
                text: qsTr("Save Worklist Color Chart 384")
                onTriggered: backend.saveWorklistColorchart384()
            }
            MenuItem {
                text: qsTr("Save Worklist Sepia")
                onTriggered: backend.saveWorklistSepia()
            }
            MenuItem {
                text: qsTr("Save Worklist Color")
                onTriggered: backend.saveWorklistColor3()
            }
            MenuItem {
                text: qsTr("Exit")
                onTriggered: Qt.quit();
            }
        }
        Menu {
            title: qsTr("Robot")
            MenuItem {
                text: qsTr("96 Well Plate")
                onTriggered: backend.setSize96()
            }
            MenuItem {
                text: qsTr("384 Well Plate")
                onTriggered: backend.setSize384()
            }
            MenuItem {
                text: qsTr("Exit")
                onTriggered: Qt.quit();
            }
        }
        Menu {
            title: qsTr("Image")
            MenuItem {
                text: qsTr("Colorize Sepia")
                onTriggered: {
                    backend.colorizeMonochrome();
                    canvas.requestPaint();
                }
            }
            MenuItem {
                text: qsTr("Colorize RYB")
                onTriggered: {
                    backend.colorizeHues3();
                    canvas.requestPaint();
                }
            }
            MenuItem {
                text: qsTr("384 Well Plate")
                onTriggered: backend.setSize384()
            }
            MenuItem {
                text: qsTr("Exit")
                onTriggered: Qt.quit();
            }
        }
    }

    toolBar: ToolBar {
        RowLayout {
            ToolButton {
                text: "white"
                onClicked: app.color = "#ffffff"
            }
            ToolButton {
                text: "black"
                onClicked: app.color = "#000000"
            }
            ToolButton {
                text: "red"
                onClicked: app.color = "#ff0000"
            }
        }
    }

    FileDialog {
        id: fileOpenDialog
        title: "Please choose a file"
        selectExisting: true
        selectMultiple: false
        nameFilters: [ "Image files (*.png *.jpg *.gif)", "All files (*)" ]
        selectedNameFilter: "Image files (*.png *.jpg *.gif)"
        onAccepted: {
            backend.openImage(fileOpenDialog.fileUrl);
            canvas.requestPaint();
            console.log(backend.rowCount)
            plate.width = backend.colCount * plate.zoom
            plate.height = backend.rowCount * plate.zoom
        }
        onRejected: {
        }
    }

    ColumnLayout {
        id: plateLayout
        anchors.fill: parent
        spacing: plate.zoom

        Rectangle {
            id: plate
            color: "white";
            width: backend.colCount * plate.zoom + 2;
            height: backend.rowCount * plate.zoom + 2;
            anchors.centerIn: parent
            border.color: "black"

            property int zoom: 20
            property int xpos
            property int ypos
            property Image image

            Canvas {
                id: canvas
                anchors.fill: parent

                onPaint: {
                    var ctx = getContext('2d')
                    for (var row = 0; row < backend.rowCount; row++) {
                        for (var col = 0; col < backend.colCount; col++) {
                            var x, y;
                            x = col * plate.zoom;
                            y = row * plate.zoom;
                            ctx.fillStyle = backend.getFillStyle(row, col);
                            ctx.fillRect(x, y, plate.zoom, plate.zoom);
                        }
                    }
                }

                MouseArea {
                    id: mouseArea
                    anchors.fill: parent

                    function handleMouse(mouseX, mouseY) {
                        var imageRow, imageCol;
                        imageRow = Math.floor(mouseY / plate.zoom);
                        imageCol = Math.floor(mouseX / plate.zoom);
                        if (imageRow >= 0 && imageRow < backend.rowCount && imageCol >= 0 && imageCol < backend.colCount) {
                            backend.setColor(imageRow, imageCol, app.color)
                            canvas.requestPaint();
                        }
                    }

                    onPressed: {
                        handleMouse(mouseX, mouseY);
                    }
                    onMouseXChanged: {
                        handleMouse(mouseX, mouseY);
                    }
                    onMouseYChanged: {
                        handleMouse(mouseX, mouseY);
                    }
                }
            }
        }

        RowLayout {
            id: colorsLayout
            spacing: 0
            Layout.fillWidth: true
            Layout.preferredHeight: plate.zoom
            Layout.maximumHeight: plate.zoom

            Rectangle {
                Layout.fillWidth: true
                Layout.minimumWidth: plate.zoom
                Layout.preferredWidth: plate.zoom
                Layout.maximumWidth: plate.zoom
                Layout.minimumHeight: plate.zoom
                color: "#ff00ff";
            }

            Rectangle {
                Layout.fillWidth: true
                Layout.minimumWidth: plate.zoom
                Layout.preferredWidth: plate.zoom
                Layout.maximumWidth: plate.zoom
                Layout.minimumHeight: plate.zoom
                color: "plum"
            }
        }
    }
}
