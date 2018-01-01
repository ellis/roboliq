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
	"plate*": {
		"plateDWP": {
			"storageSite": "ourlab.mario.site.REGRIP",
			"roma*": {
				"ourlab.mario.roma1": {
					"vector*": {
						"Narrow": {
							"site*": [
								"ourlab.mario.site.P2",
								"ourlab.mario.site.P3",
								"ourlab.mario.site.P4",
								"ourlab.mario.site.P5",
								"ourlab.mario.site.P6",
								"ourlab.mario.site.P7",
								"ourlab.mario.site.P8",
								//"ourlab.mario.site.CENTRIFUGE_2",
								//"ourlab.mario.site.CENTRIFUGE_4",
							]
						}
					}
				},
				/*"ourlab.mario.roma2": {
					"vector*": {
						"Narrow": {
							"site*": [
								"ourlab.mario.site.ROBOSEAL",
								//"ourlab.mario.site.CENTRIFUGE_2",
								//"ourlab.mario.site.CENTRIFUGE_4",
							]
						}
					}
				},*/
			}
		}
	}
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
							if (_.isPlainObject(value3)) {
								const value2 = (_.isNumber(key3)) ? key3 + 1 : key3;
								return _.merge({}, x, _.fromPairs([[key2, value2]]), value3);
							}
							else {
								return _.merge({}, x, _.fromPairs([[key2, value3]]));
							}
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
console.log(yaml.stringify(table, 4, 2))

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

	let culturePlateIndex = 0;
	narrow(Map(), table, {groupBy: ["roma", "vector"]}, (scope, data) => {
		const data1 = _.shuffle(data);
		// console.log("A")
		// console.log({scope, data})

		const stepRomaVector = {comment: `Test plate movements for ${scope.get("roma")} with vector ${scope.get("vector")}`};

		narrow(scope, data1, {groupBy: "site"}, (scope, data) => {
			// console.log("C")
			// console.log({scope, data})
			appendStep(stepRomaVector, {
				command: "transporter.movePlate",
				equipment: scope.get("roma"),
				program: scope.get("vector"),
				object: scope.get("plate"),
				destination: scope.get("site")
			});
		});

		appendStep(stepRomaVector, {
			command: "transporter.movePlate",
			equipment: scope.get("roma"),
			program: scope.get("vector"),
			object: scope.get("plate"),
			destination: scope.get("storageSite")
		});

		appendStep(steps, stepRomaVector);
	});

	const protocol = {
		roboliq: "v1",
		objects: {
			plateDWP: {type: "Plate", model: "ourlab.model.plateModel_96_dwp", location: "ourlab.mario.site.REGRIP"},
			//plateNunc: {type: "Plate", model: "ourlab.model.plateModel_96_square_transparent_nunc", location:
		},
		steps
	};

	return protocol;
}

const protocol = test();

// If run from the command line:
if (require.main === module) {
	//console.log(yaml.stringify(protocol, 9, 2));
	console.log(JSON.stringify(protocol, null, '\t'))
}
else {
	module.exports = protocol;
}
