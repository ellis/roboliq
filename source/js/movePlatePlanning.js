var evowareConfig = [
	//
	// State
	//
	{"isAgent": {"agent": "ourlab.mario.evoware"}},
	{"isTransporter": {"equipment": "ourlab.mario.roma1"}},
	{"isModel": {"model": "ourlab.model1"}},
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
];

taskDefs = [
	//
	// Actions
	//
	{"action": {"description": "print for debugging",
		"task": {"print": {"text": "?text"}},
		"preconditions": [
		],
		"deletions": [],
		"additions": []
	}},
	{"action": {"description": "transport plate from origin to destination",
		"task": {"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?destination", "destinationModel": "?destinationModel"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?destination"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?destination"}},
		],
		"deletions": [{"location": {"labware": "?labware", "site": "?origin"}}],
		"additions": [{"location": {"labware": "?labware", "site": "?destination"}}]
	}},

	//
	// Methods
	//

	// movePlate-null
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"location": {"labware": "?labware", "site": "?destination"}}
		],
		"subtasks": {"ordered": [
			{"print": {"text": "movePlate-null"}},
		]}
	}},

	// movePlate-one
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?destination"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?destination"}},
		],
		"subtasks": {"ordered": [
			{"print": {"text": "movePlate-one"}},
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?destination", "destinationModel": "?destinationModel"}},
		]}
	}},

	// movePlate-two
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent1", "equipment": "?equipment1", "program": "?program1", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent1", "equipment": "?equipment1", "program": "?program1", "model": "?model", "site": "?site2"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent2", "equipment": "?equipment2", "program": "?program2", "model": "?model", "site": "?site2"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent2", "equipment": "?equipment2", "program": "?program2", "model": "?model", "site": "?destination"}},
			{"siteModel": {"site": "?site2", "siteModel": "?site2Model"}},
			{"stackable": {"below": "?site2Model", "above": "?model"}},
			{"siteIsClear": {"site": "?site2"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?site2"}},
			{"movePlate_pathOk": {"siteA": "?site2", "siteB": "?destination"}},
		],
		"subtasks": {"ordered": [
			{"print": {"text": "movePlate-two"}},
			{"movePlateAction": {"agent": "?agent1", "equipment": "?equipment1", "program": "?program1", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?site2", "destinationModel": "?site2Model"}},
			{"movePlateAction": {"agent": "?agent2", "equipment": "?equipment2", "program": "?program2", "labware": "?labware", "model": "?model", "origin": "?site2", "originModel": "?site2Model", "destination": "?destination", "destinationModel": "?destinationModel"}},
		]}
	}},

	// movePlate-a-null
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate-a": {"labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"location": {"labware": "?labware", "site": "?destination"}}
		],
		"subtasks": {"ordered": [
			{"print": {"text": "movePlate-a-null"}},
		]}
	}},

	// movePlate-a-one
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate-a": {"agent": "?agent", "labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"isAgent": {"agent": "?agent"}},
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?destination"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?destination"}},
		],
		"subtasks": {"ordered": [
			{"print": {"text": "movePlate-one agent"}},
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?destination", "destinationModel": "?destinationModel"}},
		]}
	}},

	// movePlate-a-two
	{"method": {"description": "transport plate from origin to destination",
		"task": {"movePlate-a": {"agent": "?agent", "labware": "?labware", "destination": "?destination"}},
		"preconditions": [
			{"model": {"labware": "?labware", "model": "?model"}},
			{"location": {"labware": "?labware", "site": "?origin"}},
			{"siteModel": {"site": "?origin", "siteModel": "?originModel"}},
			{"siteModel": {"site": "?destination", "siteModel": "?destinationModel"}},
			{"stackable": {"below": "?destinationModel", "above": "?model"}},
			{"siteIsClear": {"site": "?destination"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment1", "program": "?program1", "model": "?model", "site": "?origin"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment1", "program": "?program1", "model": "?model", "site": "?site2"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment2", "program": "?program2", "model": "?model", "site": "?site2"}},
			{"movePlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment2", "program": "?program2", "model": "?model", "site": "?destination"}},
			{"siteModel": {"site": "?site2", "siteModel": "?site2Model"}},
			{"stackable": {"below": "?site2Model", "above": "?model"}},
			{"siteIsClear": {"site": "?site2"}},
			{"movePlate_pathOk": {"siteA": "?origin", "siteB": "?site2"}},
			{"movePlate_pathOk": {"siteA": "?site2", "siteB": "?destination"}},
		],
		"subtasks": {"ordered": [
			{"print": {"text": "movePlate-a-two"}},
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment1", "program": "?program1", "labware": "?labware", "model": "?model", "origin": "?origin", "originModel": "?originModel", "destination": "?site2", "destinationModel": "?site2Model"}},
			{"movePlateAction": {"agent": "?agent", "equipment": "?equipment2", "program": "?program2", "labware": "?labware", "model": "?model", "origin": "?site2", "originModel": "?site2Model", "destination": "?destination", "destinationModel": "?destinationModel"}},
		]}
	}},

	//
	// Rules
	//

	// same: Two things are the same if they unify.
	{"<--": {"same": {"thing1": "?thing", "thing2": "?thing"}}},

	// clear: a site is clear if no labware is on it
	{"<--": {"siteIsClear": {"site": "?site"},
		"and": [{"not": {"location": {"labware": "?labware", "site": "?site"}}}]}
	},

	// clear: a site is clear if no labware is on it
	{"<--": {"movePlate_pathOk": {"siteA": "?siteA", "siteB": "?siteB"},
		"and": [
			{"not": {"movePlate_excludePath": {"siteA": "?siteA", "siteB": "?siteB"}}},
			{"not": {"movePlate_excludePath": {"siteA": "?siteB", "siteB": "?siteA"}}},
		]}
	},

];

module.exports = {
  evowareConfig: evowareConfig,
  taskDefs: taskDefs
}
