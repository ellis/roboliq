import QtQuick 2.0

Item {
    width: plate.zoom
    height: plate.zoom + 3

    property string color

    Rectangle {
        x: 0
        y: 0
        width: parent.width
        height: parent.height - 3
        color: parent.color
    }

    Rectangle {
        visible: app.color == parent.color
        color: "cyan"
        x: 0
        y: plate.zoom
        width: parent.width
        height: 3
    }

    MouseArea {
        anchors.fill: parent
        onPressed: { app.color = parent.color; }
    }
}
