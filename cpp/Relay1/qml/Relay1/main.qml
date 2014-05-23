import QtQuick 2.1
import QtQuick.Controls 1.1
import QtQuick.Dialogs 1.0
import QtQuick.Layouts 1.1

ApplicationWindow {
    id: app
    title: qsTr("Roboterprinter")
    width: 640
    height: 480

    property string color: "#dc1424"

    menuBar: MenuBar {
        Menu {
            title: qsTr("File")
            MenuItem {
                text: qsTr("Open...")
                onTriggered: fileOpenDialog.visible = true;
            }
            MenuItem {
                text: qsTr("Save As...")
                onTriggered: fileSaveAsDialog.visible = true;
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
                text: "Save"
                action: backend.saveWorklistColor3()
            }

            Rectangle {
                width: 20
                height: 20
                color: app.color
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

    FileDialog {
        id: fileSaveAsDialog
        title: "Please enter a filename"
        selectExisting: false
        selectMultiple: false
        nameFilters: [ "Image files (*.png)", "All files (*)" ]
        selectedNameFilter: "Image files (*.png)"
        onAccepted: {
            backend.saveImage(this.fileUrl);
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

            ColorRect { color: "#ff1424" }
            ColorRect { color: "#ff254a" }
            ColorRect { color: "#ff4e6b" }
            ColorRect { color: "#ff7990" }
            ColorRect { color: "#ffe536" } // yellow
            ColorRect { color: "#fff24f" }
            ColorRect { color: "#fff569" }
            ColorRect { color: "#fff792" } // yellow/8
            ColorRect { color: "#006ffb" } // blue
            ColorRect { color: "#1c8aff" } // blue/2
            ColorRect { color: "#5facf9" } // blue/4
            ColorRect { color: "#76caf9" } // blue/8
            ColorRect { color: "#000000" } // black
            ColorRect { color: "#5a5958" }
            ColorRect { color: "#797774" }
            ColorRect { color: "#a0a0a0" } // black/8
            ColorRect { color: "#cd5c0a" } // brown
            ColorRect { color: "#d06d06" } // brown/2
            ColorRect { color: "#ff5507" } // orange
            ColorRect { color: "#ff8507" } // orange/2
            ColorRect { color: "#886fa6" }
            ColorRect { color: "#15ae27" } // green
            ColorRect { color: "#4ece5d" } // green/2
            ColorRect { color: "#6fe37c" } // green/4
            ColorRect { color: "#ffffff" }
        }
    }
}
