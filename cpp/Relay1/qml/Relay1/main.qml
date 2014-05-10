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
            title: qsTr("Plate")
            MenuItem {
                text: qsTr("96 Well Plate")
                onTriggered: backend.setSize96()
            }
            MenuItem {
                text: qsTr("384 Well Plate")
                onTriggered: backend.setSize384()
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
            Rectangle {
                width: 20
                height: 20
                color: "red"
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

    Item {
        id: plateHolder
        anchors.centerIn: parent
        width: backend.colCount * plate.zoom + 2;
        height: (backend.rowCount + 2) * plate.zoom + 2;

        Rectangle {
            id: plate
            color: "white";
            x: 0
            y: 0
            width: backend.colCount * plate.zoom + 3;
            height: backend.rowCount * plate.zoom + 3;
            border.color: "black"

            property int zoom: 20
            property int xpos
            property int ypos
            property Image image

            Canvas {
                id: canvas
                x: 1
                y: 1
                width: backend.colCount * plate.zoom
                height: backend.rowCount * plate.zoom

                onPaint: {
                    var ctx = getContext('2d')
                    for (var row = 0; row < backend.rowCount; row++) {
                        for (var col = 0; col < backend.colCount; col++) {
                            var x, y;
                            x = col * plate.zoom;
                            y = row * plate.zoom;
                            ctx.fillStyle = backend.getFillStyle(row, col);
                            ctx.s
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

        Row {
            x: 0
            y: (backend.rowCount + 1) * plate.zoom + 2
            width: backend.colCount * plate.zoom + 2
            height: plate.zoom
            spacing: 0

            ColorRect { color: "#ffffff" }
            ColorRect { color: "#dc1424" }
            ColorRect { color: "#e7254a" }
            ColorRect { color: "#ff4e6b" }
            ColorRect { color: "#ff7990" }
            ColorRect { color: "#f8db17" }
            ColorRect { color: "#f1f24f" }
            ColorRect { color: "#f1e869" }
            ColorRect { color: "#fffa73" }
            ColorRect { color: "#0067a2" }
            ColorRect { color: "#059cc2" }
            ColorRect { color: "#2dc6d4" }
            ColorRect { color: "#58c9cb" }
            ColorRect { color: "#000000" }
            ColorRect { color: "#5a5958" }
            ColorRect { color: "#797774" }
            ColorRect { color: "#a0a0a0" }
            ColorRect { color: "#f3441e" }
            ColorRect { color: "#ff8507" }
            ColorRect { color: "#f3441e" }
            ColorRect { color: "#cf6625" }
            ColorRect { color: "#886fa6" }
            ColorRect { color: "#0fb022" }
            ColorRect { color: "#02b26e" }
            ColorRect { color: "#59c99b" }
        }
    }
}
