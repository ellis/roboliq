var objectToLogicConverters = {
  "Plate": function(name, object) {
    return {value: [
      {"isLabware": {"labware": name}},
      {"isPlate": {"labware": name}},
      {"model": {"labware": name, "model": object.model}},
      {"location": {"labware": name, "site": object.location}}
    ]};
  },
  "PlateModel": function(name, object) {
    return {value: [{"isModel": {"model": name}}]};
  },
  "Site": function(name, object) {
    return {value: [{"isSite": {"model": name}}]};
  },
  "Transporter": function(name, object) {
    return {value: [{"isTransporter": {"equipment": name}}]};
  },
};

module.exports = {
  objectToLogicConverters: objectToLogicConverters
};
