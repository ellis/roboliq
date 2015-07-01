//var _ = require('lodash');

var predicates = [
  //
  // Rules
  //

  // same: Two things are the same if they unify.
  {"<--": {"same": {"thing1": "?thing", "thing2": "?thing"}}},

  // clear: a site is clear if no labware is on it
  {"<--": {"siteIsClear": {"site": "?site"},
    "and": [{"not": {"location": {"labware": "?labware", "site": "?site"}}}]}
  },
];

var objectToPredicateConverters = {
  "Plate": function(name, object) {
    var value = [
      {"isLabware": {"labware": name}},
      {"isPlate": {"labware": name}},
      {"model": {"labware": name, "model": object.model}},
      {"location": {"labware": name, "site": object.location}}
    ];
    if (object.sealed)
      value.push({"plateIsSealed": {"labware": name}});
    return {value: value};
  },
  "PlateModel": function(name, object) {
    return {value: [{"isModel": {"model": name}}]};
  },
  "Site": function(name, object) {
    return {value: [{"isSite": {"model": name}}]};
  },
};

var planHandlers = {
  "trace": function() { return []; }
};

module.exports = {
  predicates: predicates,
  objectToPredicateConverters: objectToPredicateConverters,
  planHandlers: planHandlers
};
