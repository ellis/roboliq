---
title: Evoware Configuration
layout: commands.html
lookupCollection: evowareConfiguration
---

The `EvowareConfigSpec` enables you to configure your Evoware robot for Roboliq.
You will need to create a JavaScript file in which you define an
`EvowareConfigSpec` object, and then have it converted to the Roboliq protocol format.  Your config file will have the following structure:

```javascript
const evowareConfigSpec = {
  // Lab name and robot name
  namespace: "YOUR LAB ID",
  name: "YOUR ROBOT ID",
  // Compiler settings
  config: {
    TEMPDIR: "TEMPORARY DIRECTORY FOR MEASUREMENT FILES",
    ROBOLIQ: "COMMAND TO CALL ROBOLIQ'S RUNTIME",
    BROWSER: "PATH TO WEB BROWSER"
  },
  // Bench sites on the robot
  sites: {
    MYSITE1: {evowareCarrier: "CARRIER ID", evowareGrid: MYGRID, evowareSite: MYSITE},
    ...
  },
  // Labware models
  models: {
    MYPLATEMODEL1: {type: "PlateModel", rows: 8, columns: 12, evowareName: "EVOWARE LABWARE NAME"},
    ...
  },
  // List of which sites and labware models can be used together
  siteModelCompatibilities: [
    {
      sites: ["MYSITE1", ...],
      models: ["MYPLATEMODEL1", ...]
    },
    ...
  ],
  // List of the robot's equipment
  equipment: {
    MYEQUIPMENT1: {
      module: "EQUIPMENT1.js",
      params: {
        ...
      }
    }
  },
  // List of which lid types can be stacked on which labware models
  lidStacking: [
    {
      lids: ["lidModel_standard"],
      models: ["MYPLATEMODEL1"]
    }
  ],
  // List of the robot's robotic arms
  romas: [
    {
      description: "roma1",
      // List of sites this ROMA can safely access with which vectors
      safeVectorCliques: [
				{ vector: "Narrow", clique: ["MYSITE1", ...] },
				...
			]
    },
    ...
  ],
  // Liquid handing arm
  liha: {
    // Available tip models
    tipModels: {
      MYTIPMODEL1000: {programCode: "1000", min: "3ul", max: "950ul", canHandleSeal: false, canHandleCells: true},
      ...
    },
    // List of LIHA syringes (e.g. 8 entries if it has 8 syringes)
    syringes: [
      { tipModelPermanent: "MYTIPMODEL1000" },
      ...
    ],
    // Sites that the LIHA can access
    sites: ["MYSITE1", ...],
    // Specifications for how to wash the tips
    washPrograms: {
      // For Example: Specification for flushing the tips with `programCode == 1000`
      flush_1000: { ... },
      ...
    }
  },
  // Additional user-defined command handlers
  commandHandlers: {
    "MYCOMMAND1": function(params, parsed, data) { ... },
    ...
  },
  // Optional functions to choose among planning alternatives
  planAlternativeChoosers: {
    // For Example: when the `shaker.shakePlate` command has
    // multiple shakers available, you might want to use
    // the one name `MYEQUIPMENT1`
    "shaker.canAgentEquipmentSite": (alternatives) => {
			const l = alternatives.filter(x => x.equipment.endsWith("MYEQUIPMENT1"));
			if (l.length > 0)
				return l[0];
		}
  }
};

const EvowareConfigSpec = require('roboliq-evoware/dist/EvowareConfigSpec.js');
module.exports = EvowareConfigSpec.makeProtocol(evowareConfigSpec);
```

Details about the `EvowareConfigSpec` structure can be found below.
