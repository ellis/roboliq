import _ from 'lodash';
import assert from 'assert';
import Immutable, {Map, fromJS} from 'immutable';
import yaml from 'yamljs';

const tree = {
	"pipettingLocation": "?",
	"culturePlate*": {
		"puncturePlate": {
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
					"syringe": "ourlab.luigi.liha.syringe.1"
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
					"syringe": "ourlab.luigi.liha.syringe.2"
				}
			],
			"reseal": false,
			"incubatorLocation": "SHAKER1"
		},
		"resealPlate": {
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
					"syringe": "ourlab.luigi.liha.syringe.1"
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
					"syringe": "ourlab.luigi.liha.syringe.2"
				}
			],
			"reseal": true,
			"incubatorLocation": "SHAKER2"
		}
	},
	"dilutionLocation": "?",
	"interval": "12 hours",
	"media": "media1",
	"mediaVolume": "80ul",
	"strain": "strain1",
	"strainVolume": "20ul",
	"sampleVolume": "5ul"
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
			command: "pipette.pipetteMixtures",
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
			destinationLabware: scope.get("culturePlate")
		});

		appendStep(step, {
			command: "sealer.sealPlate",
			object: scope.get("culturePlate")
		});

		appendStep(step, {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("incubatorLocation")
		});

		appendStep(step, {
			command: "incubator.start", program: "incubatorProgram"
		});

		// Start 12h times for the first plate only
		if (culturePlateIndex === 0) {
			appendStep(step, {
				command: "timer.start",
				equipment: "ourlab.mario.timer2"
			});
		}

		appendStep(step1, {
			command: "timer.doAndWait",
			equipment: "ourlab.mario.timer1",
			duration: "15 minutes",
			steps: step
		});

		culturePlateIndex++;
	});

	appendStep(step1, {
		command: "timer.wait",
		equipment: "ourlab.mario.timer2",
		till: "12 hours"
	});

	narrow(Map(), table, {groupBy: "measurement"}, (scope, data) => {
		const measurementStep = {};

		//console.log({culturePlate: scope.get("culturePlate")});
		narrow(scope, data, {groupBy: "culturePlate"}, (scope, data) => {
			console.log({reseal1: scope.get("reseal")});
			const step = {};

			appendStep(step, {
				command: "transporter.movePlate",
				object: scope.get("dilutionPlate"),
				destination: scope.get("dilutionLocation")
			});
			appendStep(step, {
				command: "incubator.open",
				site: scope.get("incubatorLocation")
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
				volume: scope.get("sampleVolume"),
				sourceLabware: scope.get("culturePlate"),
				destinationLabware: scope.get("dilutionPlate")
			});
			appendStep(step, {
				command: "system.if",
				test: scope.get("reseal"),
				then: {
					1: {command: "sealer.sealPlate", object: scope.get("culturePlate")}
				}
			});
			appendStep(step, {
				command: "transporter.movePlate",
				object: scope.get("culturePlate"),
				destination: scope.get("incubatorLocation")
			});
			appendStep(step, {
				command: "incubator.start", program: "incubatorProgram"
			});
			appendStep(step, {
				command: "pipetter.dilutionSeries",
				diluent: "water",
				items: mapConditions(scope, data, {groupBy: "cultureWell"}, 0, (scope) => {
					return {
						destination: scope.get("dilutionWell"),
						dilutionFactor: scope.get("dilutionFactor"),
						syringe: scope.get("syringe")
					};
				})
			});
			appendStep(step, {
				command: "absorbanceReader.measurePlate",
				object: scope.get("dilutionPlate"),
				program: {
					wells: mapConditions(scope, data, {select: "dilutionWell"}, 1, (scope) => scope.get("dilutionWell"))
				},
				programTemplate: "./dm00.mdfx.template"
			});

			appendStep(measurementStep, {
				description: `Measurement ${scope.get("measurement")} on ${scope.get("culturePlate")}`,
				command: "timer.doAndWait",
				equipment: "ourlab.mario.timer1",
				duration: "15 minutes",
				steps: step
			});
		});

		appendStep(steps, {
			description: `Measurement ${scope.get("measurement")}`,
			command: "timer.doAndWait",
			equipment: "ourlab.mario.timer1",
			duration: "12 hours",
			steps: measurementStep
		});
	});
	console.log(yaml.stringify(steps, 5, 2));
}

test();
