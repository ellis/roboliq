var planTaskStatements = [
  // Actions
  {"action": {"description": "fully specified seal command",
    "task": {"sealer.sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site"}},
    "preconditions": [
      {"model": {"labware": "?labware", "model": "?model"}},
      {"sealer.sealPlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site"}},
      {"location": {"labware": "?labware", "site": "?site"}}
    ],
    "deletions": [],
    "additions": [{"plateHasSeal": {"labware": "?labware"}}]
  }},

  // Methods
  {"method": {"description": "method for sealing",
    "task": {"sealer.sealPlate": {"labware": "?labware"}},
    "preconditions": [
      {"model": {"labware": "?labware", "model": "?model"}},
      {"sealer.sealPlate_canAgentEquipmentProgramModelSite": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "model": "?model", "site": "?site"}}
    ],
    "subtasks": {"ordered": [
      {"ensureLocation": {"labware": "?labware", "site": "?site"}},
      {"sealAction": {"agent": "?agent", "equipment": "?equipment", "program": "?program", "labware": "?labware", "model": "?model", "site": "?site"}}
    ]}
  }}
];
