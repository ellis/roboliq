import _ from 'lodash';
import Immutable, {Map, fromJS} from 'immutable';
import yaml from 'yamljs';

const conditions = {
  "aspirationLocation": "?",
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
      "shakerLocation": "SHAKER1"
    },
		"resealPlate": {
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
      "shakerLocation": "SHAKER2"
    }
  },
  "dilutionLocation": "?",
  "interval": "12 hours",
  "media": "media1",
  "mediaVolume": "80ul",
  "strain": "strain1",
  "strainVolume": "20ul"
};

function descend(scope0, conditions, propertyName, fn) {
	function doit(scope, elem) {
		const elemFlat = _.omitBy(elem, (x, name) => _.endsWith(name, "*"));
		const scope2 = scope.merge(fromJS(elemFlat));
		//console.log("scope: "+JSON.stringify(scope));
		fn(scope2, elem);
	}

	if (propertyName) {
		const propertyNameStar = propertyName+"*";
		_.forEach(conditions[propertyNameStar], (elem, key) => {
			if (_.isNumber(key)) {
				key = key + 1;
			}
			const scope = scope0.set(propertyName, key);
			doit(scope, elem);
		});
	}
	else {
		doit(scope0, conditions);
	}
}

function map(scope0, conditions, propertyName, fn) {
	let result = [];
	function doit(scope, elem) {
		result.push(fn(scope, elem));
	}
	descend(scope0, conditions, propertyName, doit);
	return result;
}

function flatMap(scope0, conditions, propertyName, fn) {
	const result = map(scope0, conditions, propertyName, fn);
	return _.flatten(result);
}

function appendStep(steps, step) {
	//console.log({steps, size: _.size(steps), keys: _.keys(steps)})
	const substeps = _.pickBy(steps, (x, key) => /^[0-9]+$/.test(key));
	steps[_.size(substeps)+1] = step;
	return step;
}

descend(Map(), conditions, null, (scope, elem) => {
	const steps = {};
	const step1 = appendStep(steps, {
		description: "prepare plate"
	});

	/*appendStep(step, {
		command: "timer.start",
		equipment: "ourlab.mario.timer1"
	});*/

	let culturePlateIndex = 0;
	descend(scope, elem, "culturePlate", (scope, elem) => {
		const step = {};

		//console.log(`1: move plate ${scope.get("culturePlate")} to ${scope.get("aspirationLocation")}`);
		appendStep(step, {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("aspirationLocation")
		});

		appendStep(step, {
			command: "pipette.pipette",
			sources: scope.get("strain"),
			mixtures: map(scope, elem, "cultureReplicate", (scope, elem) => {
				return {
					destination: scope.get("cultureWell"),
					syringe: scope.get("syringe"),
					sources: [
						{source: scope.get("strain"), volume: scope.get("strainVolume")},
						{source: scope.get("media"), volume: scope.get("mediaVolume")}
					]
				};
			}),
			destinationLabware: scope.get("culturePlate")
		});

		appendStep(step, {
			command: "sealer.sealPlate",
			object: scope.get("culturePlate")
		});

		appendStep(step, {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("shakerLocation")
		});

		appendStep(step, {
			command: "incubator.start", program: {speed: 100, temperature: "37 degC"}
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

	culturePlateIndex = 0;
	descend(scope, elem, "culturePlate", (scope, elem) => {
		const step = {};
		descend(scope, elem, "cultureReplicate", (scope, elem) => {
			descend(scope, elem, "measurement", (scope, elem) => {
				appendStep(step, {
					command: "transporter.movePlate",
					object: scope.get("dilutionPlate"),
					destination: scope.get("dilutionLocation")
				});
				appendStep(step, {
					command: "incubator.open",
					site: scope.get("shakerLocation")
				});

/*
command: transporter.movePlate
object: $plate
destination: $aspirationLocation
4:
command: pipetter.pipette
sources: $wells
destinations: $dilutionWells
volume: ?
5:
command: system.if
variable: $reseal
steps:
	1:
		command: sealer.sealPlate
		object: $plate
5:
command: transporter.movePlate
object: $plate
destination: $shakerLocation
6:
command: incubator.start
equipment: ?
program:
	temperature: ?
	speed: ?
7:
command: pipetter.dilutionSeries
?:
command: absorbanceReader.measurePlate
program:
	wells: $dilutionWells
programTemplate: ./dm00.mdfx.template

 */
				// dilution*
			});
		});
	});

	console.log(yaml.stringify(steps, 4, 2));
});

/*let scope = _.cloneDeep(conditions);
_.forEach(conditions["culturePlate*"], (elem, key) => {
	scope["culturePlate"] = key;
	const scopeFlat = _.omitBy(scope, (x, name) => _.endsWith(name, "*"));
	console.log("scope:"+JSON.stringify(scopeFlat));
	console.log(`1: prepare plate ${key}`);
	_.forEach(elem["cultureReplicate*"], (elem, key) => {
		scope["cultureReplicate"] = key;
		const scopeFlat2 = _.omitBy(scope, (x, name) => _.endsWith(name, "*"));
		console.log("scope:"+JSON.stringify(scopeFlat2));
	})
})
*/
