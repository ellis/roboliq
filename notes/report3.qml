import Qt.labs.presentation 1.0
import QtQuick 1.0

Presentation
{
	width: 800 //1280
	height: 600 //720


	Slide {
		title: "RoboEase - Goals"
		content: [
			"language tailored to liquid handling robots",
			"portable across robots",
			"portable across labs",
			"possible to add new peripheral devices",
		]
	}

	Slide {
		CodeSection {
			text:
"// Liquids\n" +
"val water      = new Liquid(Properties.Water, CleanPolicy.ThoroughNone) at Labwares.reagents50(1)\n" +
"val buffer10x  = new Liquid(Properties.Water, CleanPolicy.Thorough) at Labwares.eppendorfs(A1)\n" +
"val dNTP       = new Liquid(Properties.Water, CleanPolicy.Thorough)\n" +
"val primerF    = new Liquid(Properties.Water, CleanPolicy.Decontaminate, Contaminant.DNA)\n" +
"val primerB    = new Liquid(Properties.Water, CleanPolicy.Decontaminate, Contaminant.DNA)\n" +
"val polymerase = new Liquid(Properties.Glycerol, CleanPolicy.Thorough)\n" +
"\n" +
"// Plates\n" +
"val plateA = new Plate at Sites.cooled2 model LabwareModels.platePcr\n" +
"val plateB = new Plate at Sites.cooled1 model LabwareModels.platePcr\n" +
"\n" +
"// Programs for external devices\n" +
"val pcrMixProgram = new PcrMixProgram {\n" +
"	buffer     = buffer10x initial(10 x)  final(1 x)\n" +
"	dNPT       = dNTP at Labwares.eppendorfs(B1) conc0(2 uM)  conc1(.2 uM)\n" +
"	primerF    at Labwares.eppendorfs(A2) conc0(50 uM) conc1(.5 uM)\n" +
"	primerB    at Labwares.eppendorfs(B2) conc0(50 uM) conc1(.5 uM)\n" +
"	polymerase at Labwares.eppendorfs(C2) conc0(5)     conc1(0.01)\n" +
"	template ...???\n" +
"	volumePerWell = 50 ul\n" +
"	masterMixWells = Labwares.eppendorfs(D2)\n" +
"}\n" +
"val thermocycleProgram = new ThermocycleProgram(0, 2)\n" +
"val centrifugeProgram = new CentrifugeProgram(2000, 15, 9, 20, plateB)\n" +
"\n" +
"// Instructions\n" +
"pcrMixProgram(plateA(B5+2))\n" +
"seal(plateA)\n" +
"thermocycleProgram(plateA)\n" +
"centrifugeProgram(plateA)\n" +
"peel(plateA)\n"
		}
	}

    Slide {
        title: "Slide {} Element"
        content: [
            "Bullet points",
            "Should be short",
            "And to the point",
            " Sub point",
            "  Sub Sub point",
            " Sub point"
        ]

        CodeSection {

            text: "Slide {\n" +
                  "    id: areaSlide\n" +
                  "    title: \"Slide {} Element\"\n" +
                  "    content: [\n" +
                  "              \"Bullet points\",\n" +
                  "              \"Should be short\",\n" +
                  "              \"And to the point\",\n" +
                  "              \" Sub point\",\n" +
                  "              \"  Sub Sub point\",\n" +
                  "              \" Sub point\"\n" +
                  "             ]\n" +
                  "}\n"
        }
    }

    Slide {
        title: "Slide {}, continued"
        Rectangle {
            anchors.fill: parent
            color: "lightGray"
            Text {
                text: "Slide fills this area..."
                anchors.centerIn: parent
            }
        }
    }

    Slide {
        id: fillAreaSlide
        title: "Slide {}, continued"
        content: ["The built-in property \"contentWidth\" can be used to let the bullet points fill a smaller area of the slide..."]

        SequentialAnimation on contentWidth {
            PropertyAction { value: fillAreaSlide.width }
            PauseAnimation { duration: 2500 }
            NumberAnimation { to: fillAreaSlide.width / 2; duration: 5000; easing.type: Easing.InOutCubic }
            running: fillAreaSlide.visible
        }

        Rectangle {
            height: parent.height
            width: parent.contentWidth

            color: "lightGray"
            z: -1
        }
    }

    Slide {
        title: "Slide {}, continued"
        centeredText: "Use the predefined <i><b><code>centeredText</code></b></i> property to put a single block of text at the center of the Slide{}"
    }

    Slide {
        title: "Slide {}, continued"
        centeredText: '<font color="red"><i>Use</i> rich text, <font color="blue">if <b>you</b> like...'
    }




    Slide {
        title: "Font size relative to screen size"
        content: [
            "Which means you don't need to worry about it",
            "Bullet points wraps around on the edges, regardless of how long they are, like this. Even if you should choose to use a very long bullet point (which would distract your audience) it would still look ok'ish",
            "If you run out of height, you're out of luck though..."
        ]
    }



    Slide {
        id: interactiveSlide

        title: "Embed Interactive Content"

        Rectangle {
            id: box
            width: parent.fontSize * 10
            height: width
            color: mouse.pressed ? "lightsteelblue" : "steelblue"

            NumberAnimation on rotation { from: 0; to: 360; duration: 10000; loops: Animation.Infinite; running: visible }

            Text {
                text: "Click Me!"
                anchors.centerIn: parent
            }

            MouseArea {
                id: mouse
                anchors.fill: parent
                drag.target: box
            }
        }
    }


    Slide {
        title: "Features"
        centeredText: 'Hit [esc] to fade out the current page if there are questions from the audience'
    }

    Slide {
        title: "Features"
        centeredText: 'Navigate back and forth using [left] and [right]\n[space] or [click] takes you to the next slide.'
    }


    Slide {
        centeredText: "Now go make our own presentations\n\nEnjoy!"
    }


}
