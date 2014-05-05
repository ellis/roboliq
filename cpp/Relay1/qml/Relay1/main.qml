import QtQuick 2.1
import QtQuick.Controls 1.0

ApplicationWindow {
    title: qsTr("Hello World")
    width: 640
    height: 480

    menuBar: MenuBar {
        Menu {
            title: qsTr("File")
            MenuItem {
                text: qsTr("Exit")
                onTriggered: Qt.quit();
            }
        }
    }

    Rectangle {
        id: plate
        color: "white";
        width: 120;
        height: 90;
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
                ctx.fillStyle = "black"
                var x = Math.floor(plate.xpos / 10) * 10;
                var y = Math.floor(plate.ypos / 10) * 10;
                ctx.fillRect(x, y, 10, 10)
            }

            MouseArea {
                id: mouseArea
                anchors.fill: parent
                onPressed: {
                    plate.xpos = mouseX
                    plate.ypos = mouseY
                    canvas.requestPaint()
                }
                onMouseXChanged: {
                    plate.xpos = mouseX
                    plate.ypos = mouseY
                    canvas.requestPaint()
                }
                onMouseYChanged: {
                    plate.xpos = mouseX
                    plate.ypos = mouseY
                    canvas.requestPaint()
                }
            }
        }
    }
}
