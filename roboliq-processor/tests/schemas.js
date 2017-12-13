/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

import _ from 'lodash';
import fs from 'fs';
import path from 'path';
import yaml from 'yamljs';

const schemas = {};
const dir = __dirname+"/../src/schemas/";
const files = fs.readdirSync(dir);
_.forEach(files, file => {
	// console.log({file})
	if (path.extname(file) === ".yaml") {
		_.merge(schemas, yaml.load(path.join(dir, file)));
	}
});

const extras = {
	"EvowareRobot": {
		"properties": {
			"type": {
				"enum": [
					"EvowareRobot"
				]
			}
		},
		"required": [
			"type"
		]
	},
	"EvowareWashProgram": {
		"properties": {
			"type": {
				"enum": [
					"EvowareWashProgram"
				]
			}
		},
		"required": [
			"type"
		]
	},
	"equipment.close|ourlab.mario.evoware|ourlab.mario.centrifuge": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			}
		},
		"required": [
			"agent",
			"equipment"
		]
	},
	"equipment.open|ourlab.mario.evoware|ourlab.mario.centrifuge": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			}
		},
		"required": [
			"agent",
			"equipment"
		]
	},
	"equipment.openSite|ourlab.mario.evoware|ourlab.mario.centrifuge": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"site": {
				"description": "Site identifier",
				"type": "Site"
			}
		},
		"required": [
			"agent",
			"equipment",
			"site"
		]
	},
	"equipment.run|ourlab.mario.evoware|ourlab.mario.centrifuge": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"program": {
				"description": "Program for centrifuging",
				"type": "object",
				"properties": {
					"rpm": {
						"type": "number",
						"default": 3000
					},
					"duration": {
						"type": "Duration",
						"default": "30 s"
					},
					"spinUpTime": {
						"type": "Duration",
						"default": "9 s"
					},
					"spinDownTime": {
						"type": "Duration",
						"default": "9 s"
					},
					"temperature": {
						"type": "number",
						"default": 25
					}
				}
			}
		},
		"required": [
			"program"
		]
	},
	"equipment.run|ourlab.mario.evoware|ourlab.mario.sealer": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"program": {
				"description": "Program identifier for sealing",
				"type": "string"
			}
		},
		"required": [
			"agent",
			"equipment",
			"program"
		]
	},
	"equipment.close|ourlab.mario.evoware|ourlab.mario.reader": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			}
		},
		"required": [
			"agent",
			"equipment"
		]
	},
	"equipment.open|ourlab.mario.evoware|ourlab.mario.reader": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			}
		},
		"required": [
			"agent",
			"equipment"
		]
	},
	"equipment.openSite|ourlab.mario.evoware|ourlab.mario.reader": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"site": {
				"description": "Site identifier",
				"type": "Site"
			}
		},
		"required": [
			"agent",
			"equipment",
			"site"
		]
	},
	"equipment.run|ourlab.mario.evoware|ourlab.mario.reader": {
		"description": "Run our Infinit M200 reader using either programFile or programData",
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"programFile": {
				"description": "Program filename",
				"type": "File"
			},
			"programData": {
				"description": "Program data"
			},
			"outputFile": {
				"description": "Filename for measured output",
				"type": "string"
			}
		},
		"required": [
			"outputFile"
		]
	},
	"equipment.run|ourlab.mario.evoware|ourlab.mario.shaker": {
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"program": {
				"description": "Program identifier for shaking",
				"type": "string"
			}
		}
	},
	"pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha": {
		"description": "Clean the pipetter tips.",
		"properties": {
			"agent": {
				"description": "Agent identifier",
				"type": "Agent"
			},
			"equipment": {
				"description": "Equipment identifier",
				"type": "Equipment"
			},
			"program": {
				"description": "Program identifier",
				"type": "string"
			},
			"items": {
				"description": "List of which syringes to clean at which intensity",
				"type": "array",
				"items": {
					"type": "object",
					"properties": {
						"syringe": {
							"description": "Syringe identifier",
							"type": "Syringe"
						},
						"intensity": {
							"description": "Intensity of the cleaning",
							"type": "pipetter.CleaningIntensity"
						}
					},
					"required": [
						"syringe",
						"intensity"
					]
				}
			}
		},
		"required": [
			"agent",
			"equipment",
			"items"
		]
	}
};

_.merge(schemas, extras);
// console.log({schemas})

export default schemas;
