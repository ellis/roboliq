import QtQuick 2.1
import QtQuick.Controls 1.1
import QtQuick.Dialogs 1.0
import QtQuick.Layouts 1.0

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
                text: qsTr("Save Worklist")
                onTriggered: backend.saveWorklist()
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
                onTriggered: backend.
            }
            MenuItem {
                text: qsTr("Save")
                //onTriggered: backend.saveImage("image.png");
                onTriggered: backend.saveImage("image.png");
            }
            MenuItem {
                text: qsTr("Save Worklist")
                onTriggered: backend.saveWorklist()
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
        }
        onRejected: {
        }
    }

    Rectangle {
        id: plate
        color: "white";
        width: backend.colCount * 10;
        height: backend.rowCount * 10;
        anchors.centerIn: parent
        border.color: "black"

        property int xpos
        property int ypos
        property Image image

        Canvas {
            id: canvas
            anchors.fill: parent

            onPaint: {
                var ctx = getContext('2d')
                for (var row = 0; row < 8; row++) {
                    for (var col = 0; col < 12; col++) {
                        var x, y;
                        x = col * 10;
                        y = row * 10;
                        ctx.fillStyle = backend.getFillStyle(row, col);
                        ctx.fillRect(x, y, 10, 10);
                    }
                }
            }

            MouseArea {
                id: mouseArea
                anchors.fill: parent

                function handleMouse(mouseX, mouseY) {
                    var imageRow, imageCol;
                    imageRow = Math.floor(mouseY / 10);
                    imageCol = Math.floor(mouseX / 10);
                    if (imageRow >= 0 && imageRow < 8 && imageCol >= 0 && imageCol < 12) {
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
}
