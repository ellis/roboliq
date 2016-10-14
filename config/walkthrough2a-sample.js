module.exports = {
  "roboliq": "v1",
  "objects": {
		"ourlab": {
			"type": "Namespace",
			"mario": {
				"type": "Namespace",
        "controller": {
          "type": "Agent"
        },
				"transporter1": {
					"type": "Transporter",
				},
        "site": {
					"type": "Namespace",
          "P1": { "type": "Site" },
          "P2": { "type": "Site" }
        }
      },
      "model": {
				"type": "Namespace",
				"plateModel_96well": {
					"type": "PlateModel",
					"label": "96 well plate",
					"rows": 8,
					"columns": 12
				}
      }
    }
  },
  "predicates": [
    {"isSiteModel": {"model": "ourlab.mario.model.siteModel_1"}},
    {"stackable": {"below": "ourlab.mario.model.siteModel_1", "above": "ourlab.mario.model.plateModel_96well"}},
    {"siteModel": {"site": "ourlab.mario.site.P1", "siteModel": "ourlab.mario.model.siteModel_1"}},
    {"siteModel": {"site": "ourlab.mario.site.P2", "siteModel": "ourlab.mario.model.siteModel_1"}},
    {"siteCliqueSite": {"siteClique": "ourlab.mario.siteClique1", "site": "ourlab.mario.site.P2"}},
		{"transporter.canAgentEquipmentProgramSites": {
				"agent": "ourlab.mario.controller",
				"equipment": "ourlab.mario.transporter1",
        "program": "Narrow",
				"siteClique": "ourlab.mario.siteClique1"
		}}
  ]
};
