const _ = require('lodash');
const assert = require('assert');
const Immutable = require('immutable');
const {Map, fromJS} = Immutable;
const yaml = require('yamljs');

// Site and labware:
// P1 (not used)
// P2 "dilutionPlate" pipetting
// P3 "culturePlate" pipetting
// P4 stillPlate (initial, incubator)
// P5 shakePlate (initial, incubator)
// P6 dilutionPlate1 (storage)
// P7 dilutionPlate2 (storage)

const tree = {
	"pipettingLocation": "ourlab.mario.site.P3",
	"culturePlate*": {
		"stillPlate": {
			"cultureReplicate*": [
				{
					"cultureWell": "A01",
					"measurement*": [
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "A01" }, { "dilutionFactor": 2, "dilutionWell": "A02" }, { "dilutionFactor": 4, "dilutionWell": "A03" }, { "dilutionFactor": 8, "dilutionWell": "A04" }, { "dilutionFactor": 16, "dilutionWell": "A05" } ],
							"dilutionPlate": "dilutionPlate1"
						},
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "A07" }, { "dilutionFactor": 2, "dilutionWell": "A08" }, { "dilutionFactor": 4, "dilutionWell": "A09" }, { "dilutionFactor": 8, "dilutionWell": "A10" }, { "dilutionFactor": 16, "dilutionWell": "A11" } ],
							"dilutionPlate": "dilutionPlate1"
						}
					],
					"syringe": "ourlab.mario.liha.syringe.1"
				},
				{
					"cultureWell": "B01",
					"measurement*": [
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "B01" }, { "dilutionFactor": 2, "dilutionWell": "B02" }, { "dilutionFactor": 4, "dilutionWell": "B03" }, { "dilutionFactor": 8, "dilutionWell": "B04" }, { "dilutionFactor": 16, "dilutionWell": "B05" } ],
							"dilutionPlate": "dilutionPlate1"
						},
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "B07" }, { "dilutionFactor": 2, "dilutionWell": "B08" }, { "dilutionFactor": 4, "dilutionWell": "B09" }, { "dilutionFactor": 8, "dilutionWell": "B10" }, { "dilutionFactor": 16, "dilutionWell": "B11" } ],
							"dilutionPlate": "dilutionPlate1"
						}
					],
					"syringe": "ourlab.mario.liha.syringe.2"
				}
			],
			"shake": false,
			"incubatorLocation": "ourlab.mario.site.P4"
		},
		"shakePlate": {
			"cultureReplicate*": [
				{
					"cultureWell": "A01",
					"measurement*": [
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "A01" }, { "dilutionFactor": 2, "dilutionWell": "A02" }, { "dilutionFactor": 4, "dilutionWell": "A03" }, { "dilutionFactor": 8, "dilutionWell": "A04" }, { "dilutionFactor": 16, "dilutionWell": "A05" } ],
							"dilutionPlate": "dilutionPlate2"
						},
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "A07" }, { "dilutionFactor": 2, "dilutionWell": "A08" }, { "dilutionFactor": 4, "dilutionWell": "A09" }, { "dilutionFactor": 8, "dilutionWell": "A10" }, { "dilutionFactor": 16, "dilutionWell": "A11" } ],
							"dilutionPlate": "dilutionPlate2"
						}
					],
					"syringe": "ourlab.mario.liha.syringe.1"
				},
				{
					"cultureWell": "B01",
					"measurement*": [
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "B01" }, { "dilutionFactor": 2, "dilutionWell": "B02" }, { "dilutionFactor": 4, "dilutionWell": "B03" }, { "dilutionFactor": 8, "dilutionWell": "B04" }, { "dilutionFactor": 16, "dilutionWell": "B05" } ],
							"dilutionPlate": "dilutionPlate2"
						},
						{
							"dilution*": [ { "dilutionFactor": 1, "dilutionWell": "B07" }, { "dilutionFactor": 2, "dilutionWell": "B08" }, { "dilutionFactor": 4, "dilutionWell": "B09" }, { "dilutionFactor": 8, "dilutionWell": "B10" }, { "dilutionFactor": 16, "dilutionWell": "B11" } ],
							"dilutionPlate": "dilutionPlate2"
						}
					],
					"syringe": "ourlab.mario.liha.syringe.2"
				}
			],
			"shake": true,
			"incubatorLocation": "ourlab.mario.site.P5"
		}
	},
	"dilutionLocation": "ourlab.mario.site.P2",
	"interval": "12 hours",
	"media": "media1",
	"mediaVolume": "800ul",
	"strain": "strain1",
	"strainVolume": "800ul",
	"sampleVolume": "200ul"
};

// Consider: select, groupBy, orderBy, unique

function flatten(input, depth = -1) {
	assert(_.isArray(input));
	let flatter = input;
	let again = true;
	while (again && depth != 0) {
		//console.log({depth})
		again = false;
		flatter = _.flatMap(flatter, (row) => {
			//console.log({row})
			assert(_.isPlainObject(row));
			let rows = [{}];
			_.forEach(row, (value, key) => {
				//console.log({key, value})
				if (depth != 0 && _.endsWith(key, "*")) {
					again = true;
					const key2 = key.substring(0, key.length - 1);
					// For each entry in value, make a copy of every row in rows with the properties of the entry
					rows = _.flatMap(rows, x => {
						return _.map(value, (value3, key3) => {
							//console.log({key3, value3})
							const value2 = (_.isNumber(key3)) ? key3 + 1 : key3;
							assert(_.isPlainObject(value3));
							return _.merge({}, x, _.fromPairs([[key2, value2]]), value3);
						});
					});
				}
				else {
					_.forEach(rows, row => { row[key] = value; });
					/*if (key === "reseal") {
						console.log("reseal: "+value)
						console.log(rows)
					}*/
				}
			});
			//console.log({rows})
			return rows;
		});
		if (depth > 0)
			depth--;
	}

	return flatter;
}

function query(table, q) {
	let table2 = _.clone(table);
	if (q.select) {
		table2 = _.map(table2, x => _.pick(x, q.select));
	}

	if (q.where) {
		_.forEach(q.where, (value, key) => {
			table2 = _.filter(table, row => _.isEqual(row[key], value));
		});
	}

	if (q.uniqueBy) {
		const groupKeys = (_.isArray(q.uniqueBy)) ? q.uniqueBy : [q.uniqueBy];
		const groups = _.map(_.groupBy(table2, row => _.map(groupKeys, key => row[key])), _.identity);
		//console.log({groupsLength: groups.length})
		table2 = _.flatMap(groups, group => {
			const first = group[0];
			// Find the properties that are the same for all items in the group
			const uniqueKeys = [];
			_.forEach(first, (value, key) => {
				const isUnique = _.every(group, row => _.isEqual(row[key], value));
				if (isUnique) {
					uniqueKeys.push(key);
				}
			});
			return _.pick(first, uniqueKeys);
		});
	}
	else if (q.unique) {
		table2 = _.uniqWith(table2, _.isEqual);
	}

	if (q.groupBy) {
		const groupKeys = (_.isArray(q.groupBy)) ? q.groupBy : [q.groupBy];
		table2 = _.map(_.groupBy(table2, row => _.map(groupKeys, key => row[key])), _.identity);
	}
	else {
		table2 = [table2];
	}

	return table2;
}

const table = flatten([tree]);
//console.log(yaml.stringify(table, 4, 2))

let x;
//x = query(table, {select: "culturePlate"});
//x = query(table, {select: "culturePlate", groupBy: "culturePlate"});
//x = query(table, {select: "culturePlate", unique: true, groupBy: "culturePlate"});
//x = query(table, {select: ["culturePlate", "syringe", "cultureWell"], unique: true, groupBy: "culturePlate"});
//x = query(table, {select: ["culturePlate", "syringe", "cultureWell", "strain", "strainVolume", "media", "mediaVolume"], unique: true, groupBy: "culturePlate"});
//x = query(table, {uniqueBy: ["culturePlate", "cultureWell"]});
//x = query(table, {where: {dilutionFactor: 1}});
//console.log(yaml.stringify(x, 4, 2))

function appendStep(steps, step) {
	//console.log({steps, size: _.size(steps), keys: _.keys(steps)})
	const substeps = _.pickBy(steps, (x, key) => /^[0-9]+$/.test(key));
	steps[_.size(substeps)+1] = step;
	return step;
}

function narrow(scope, data, q, fn) {
	const groups = _.isPlainObject(q) ? query(data, q) : [data];
	_.forEach(groups, group => {
		if (group.length > 0) {
			const first = _.head(group);
			// Find the properties that are the same for all items in the group
			const uniqueKeys = [];
			_.forEach(first, (value, key) => {
				const isUnique = _.every(group, row => _.isEqual(row[key], value));
				if (isUnique) {
					uniqueKeys.push(key);
				}
				// FIXME: for debug only
				else {
					//if (key == "reseal") console.log(`not unique ${key}: ${value}, ${_.map(group, x => x[key]).join(",")}`)
				}
				// ENDFIX
			});
			//console.log({uniqueKeys: uniqueKeys.join(",")})
			let scope2 = scope;
			// Add those properties to the scope
			_.forEach(uniqueKeys, key => {
				scope2 = scope2.set(key, fromJS(first[key]));
			});
			fn(scope2, group);
		}
	});
}

function mapConditions(scope, data, q, flatten = 1, fn) {
	let result = [];
	narrow(scope, data, q, (scope2, data2) => {
		const l = _.map(data2, row => {
			const scope3 = scope2.merge(fromJS(row));
			return fn(scope3);
		});
		result.push(l);
	});
	for (let i = 0; i < flatten; i++) {
		result = _.flatten(result);
	}
	return result;
}

function mapConditionGroups(scope, data, q, flatten = 1, fn) {
	let result = [];
	narrow(scope, data, q, (scope2, data2) => {
		//console.log({scope2, data2})
		//process.exit(-1);
		result.push(fn(scope2, data2));
	});
	for (let i = 0; i < flatten; i++) {
		result = _.flatten(result);
	}
	return result;
}

function test() {
	const steps = {};
	const step1 = appendStep(steps, {
		description: "prepare plate"
	});

	/*appendStep(step, {
		command: "timer.start",
		equipment: "ourlab.mario.timer1"
	});*/


	let culturePlateIndex = 0;
	narrow(Map(), table, {groupBy: "culturePlate"}, (scope, data) => {
		//console.log({culturePlate: scope.get("culturePlate")});
		//console.log({scope, data})
		const step = {};

		//console.log(`1: move plate ${scope.get("culturePlate")} to ${scope.get("aspirationLocation")}`);
		appendStep(step, {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("pipettingLocation")
		});

		appendStep(step, {
			command: "pipetter.pipetteMixtures",
			//consider a "narrowBy" or "focusOn" or "restrictBy" field that is kind of equivalent to groupBy + take 1 + flatten
			mixtures: mapConditions(scope, data, {uniqueBy: "cultureWell"}, 1, (scope) => {
				return {
					destination: scope.get("cultureWell"),
					syringe: scope.get("syringe"),
					sources: [
						{source: scope.get("strain"), volume: scope.get("strainVolume")},
						{source: scope.get("media"), volume: scope.get("mediaVolume")}
					]
				}
			}),
			destinationLabware: scope.get("culturePlate"),
			clean: "light"
		});

		/*appendStep(step, {
			command: "sealer.sealPlate",
			object: scope.get("culturePlate")
		});*/

		appendStep(step, {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("incubatorLocation")
		});

		// Start 12h timer for the first plate only
		if (culturePlateIndex === 0) {
			appendStep(step, {
				command: "timer.start",
				equipment: "ourlab.mario.timer2"
			});
		}

		appendStep(step1, {
			command: "timer.doAndWait",
			equipment: "ourlab.mario.timer1",
			duration: "3 minutes",
			steps: step
		});

		culturePlateIndex++;
	});

	appendStep(step1, {
		command: "timer.wait",
		equipment: "ourlab.mario.timer2",
		till: "6 minutes",
		stop: true
	});

	narrow(Map(), table, {groupBy: "measurement"}, (scope, data) => {
		const measurementStep = {};

		//console.log({culturePlate: scope.get("culturePlate")});
		narrow(scope, data, {groupBy: "culturePlate"}, (scope, data) => {
			//console.log({reseal1: scope.get("reseal")});
			const step = {};

			appendStep(step, {
				command: "transporter.movePlate",
				object: scope.get("dilutionPlate"),
				destination: scope.get("dilutionLocation")
			});
			appendStep(step, {
				command: "transporter.movePlate",
				object: scope.get("culturePlate"),
				destination: scope.get("pipettingLocation")
			});
			appendStep(step, {
				command: "pipetter.pipette",
				items: mapConditions(scope, data, {where: {dilutionFactor: 1}}, 1, (scope) => {
					return {
						source: scope.get("cultureWell"),
						destination: scope.get("dilutionWell"),
						syringe: scope.get("syringe")
					}
				}),
				volumes: scope.get("sampleVolume"),
				sourceLabware: scope.get("culturePlate"),
				destinationLabware: scope.get("dilutionPlate"),
				cleanBegin: "none",
				clean: "light"
			});
			appendStep(step, {
				command: "system.if",
				test: scope.get("shake"),
				then: {
					1: {command: "shaker.shakePlate", object: scope.get("culturePlate"), program: {duration: "1 minute"}}
				}
			});
			appendStep(step, {
				command: "transporter.movePlate",
				object: scope.get("culturePlate"),
				destination: scope.get("incubatorLocation")
			});
			appendStep(step, {
				command: "pipetter.pipetteDilutionSeries",
				diluent: "water",
				destinationLabware: scope.get("dilutionPlate"),
				items: mapConditionGroups(scope, data, {groupBy: "cultureWell"}, 0, (scope, data) => {
					//console.log({scope2, data2})
					return {
						destinations: mapConditions(scope, data, {}, 1, (scope) => scope.get("dilutionWell")),
						syringe: scope.get("syringe")
					};
				}),
				cleanBegin: "none",
				clean: "light"
			});
			/*appendStep(step, {
				command: "absorbanceReader.measurePlate",
				object: scope.get("dilutionPlate"),
				program: {
					wells: mapConditions(scope, data, {select: "dilutionWell"}, 1, (scope) => scope.get("dilutionWell"))
				},
				programTemplate: "./dm00.mdfx.template"
			});*/

			appendStep(measurementStep, {
				description: `Measurement ${scope.get("measurement")} on ${scope.get("culturePlate")}`,
				command: "timer.doAndWait",
				equipment: "ourlab.mario.timer2",
				duration: "3 minutes",
				steps: {
					1: {
						command: "transporter.doThenRestoreLocation",
						objects: [scope.get("dilutionPlate")],
						steps: step
					}
				}
			});
		});

		appendStep(steps, {
			description: `Measurement ${scope.get("measurement")}`,
			command: "timer.doAndWait",
			equipment: "ourlab.mario.timer1",
			duration: "6 minutes",
			steps: measurementStep
		});
	});

	const protocol = {
		roboliq: "v1",
		objects: {
			water: { type: "Variable", value: "ourlab.mario.systemLiquid" },
			strain1: {type: "Liquid", wells: "trough1(C01 down to F01)"},
			media1: {type: "Liquid", wells: "trough2(C01 down to F01)"},
			trough1: {type: "Plate", model: "ourlab.model.troughModel_100ml", location: "ourlab.mario.site.R5", contents: ["Infinity l", "strain1"]},
			trough2: {type: "Plate", model: "ourlab.model.troughModel_100ml", location: "ourlab.mario.site.R6", contents: ["Infinity l", "media1"]},
			stillPlate: {type: "Plate", model: "ourlab.model.plateModel_96_dwp", location: "ourlab.mario.site.P4"},
			shakePlate: {type: "Plate", model: "ourlab.model.plateModel_96_dwp", location: "ourlab.mario.site.P5"},
			dilutionPlate1: {type: "Plate", model: "ourlab.model.plateModel_96_square_transparent_nunc", location: "ourlab.mario.site.P6"},
			dilutionPlate2: {type: "Plate", model: "ourlab.model.plateModel_96_square_transparent_nunc", location: "ourlab.mario.site.P7"},
		},
		steps
	};

	return protocol;
}

const protocol = test();

// If run from the command line:
if (require.main === module) {
	console.log(yaml.stringify(protocol, 9, 2));
}
else {
	module.exports = protocol;
}
