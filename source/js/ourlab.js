module.exports = {
  objects: {
    "ourlab": {
      "type": "Namespace",
      "mario": {
        "type": "Namespace",
        "evoware": {
          "type": "EvowareRobot"
        },
        "roma1": {
          "type": "Transporter",
          "evowareRoma": 0
        },
        "P2": {
          "type": "Site",
          "evowareCarrier": "MP 2Pos H+P Shake",
          "evowareGrid": 10,
          "evowareSite": 2
        },
        "P3": {
          "type": "Site",
          "evowareCarrier": "MP 2Pos H+P Shake",
          "evowareGrid": 10,
          "evowareSite": 4
        }
      },
      "model1": {
        "type": "PlateModel",
        "evowareName": "Ellis Nunc F96 MicroWell"
      }
    },
  },

  logic: [
    {"isAgent": {"agent": "ourlab.mario.evoware"}},
  	{"isTransporter": {"equipment": "ourlab.mario.roma1"}},
  	{"isModel": {"model": "ourlab.model1"}},
  	{"isSite": {"site": "ourlab.mario.P2"}},
  	{"isSite": {"site": "ourlab.mario.P3"}},
  	{"isSite": {"site": "ourlab.mario.SEALER"}},
  	{"isSiteModel": {"model": "ourlab.model1"}},
  	{"siteModel": {"site": "ourlab.mario.P2", "siteModel": "ourlab.model1"}},
  	{"siteModel": {"site": "ourlab.mario.P3", "siteModel": "ourlab.model1"}},
  	{"siteModel": {"site": "ourlab.mario.SEALER", "siteModel": "ourlab.model1"}},
  	{"stackable": {"below": "ourlab.model1", "above": "ourlab.model1"}},
  	{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.roma1", "program": "Narrow", "model": "ourlab.model1", "site": "ourlab.mario.P2"}},
  	{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.roma1", "program": "Narrow", "model": "ourlab.model1", "site": "ourlab.mario.P3"}},
  	{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "ourlab.mario.evoware", "equipment": "ourlab.mario.roma1", "program": "Narrow", "model": "ourlab.model1", "site": "ourlab.mario.SEALER"}},
  	{"movePlate_excludePath": {"siteA": "ourlab.mario.P3", "siteB": "ourlab.mario.SEALER"}},
  ]
}
