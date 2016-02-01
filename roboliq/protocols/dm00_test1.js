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
              "dilution*": [
                {
                  "dilutionFactor": 1,
                  "dilutionWell": "A01"
                },
                {
                  "dilutionFactor": 2,
                  "dilutionWell": "A02"
                },
                {
                  "dilutionFactor": 4,
                  "dilutionWell": "A03"
                },
                {
                  "dilutionFactor": 8,
                  "dilutionWell": "A04"
                },
                {
                  "dilutionFactor": 16,
                  "dilutionWell": "A05"
                }
              ],
              "dilutionPlate": "dilutionPlate1"
            },
            {
              "dilution*": [
                {
                  "dilutionFactor": 1,
                  "dilutionWell": "A07"
                },
                {
                  "dilutionFactor": 2,
                  "dilutionWell": "A08"
                },
                {
                  "dilutionFactor": 4,
                  "dilutionWell": "A09"
                },
                {
                  "dilutionFactor": 8,
                  "dilutionWell": "A10"
                },
                {
                  "dilutionFactor": 16,
                  "dilutionWell": "A11"
                }
              ],
              "dilutionPlate": "dilutionPlate1"
            }
          ],
          "syringe": "ourlab.luigi.liha.syringe.1"
        },
        {
          "cultureWell": "B01"
        }
      ],
      "reseal": false,
      "shakerLocation": "SHAKER1"
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

descend(Map(), conditions, null, (scope, elem) => {
	descend(scope, elem, "culturePlate", (scope, elem) => {
		//console.log(`1: move plate ${scope.get("culturePlate")} to ${scope.get("aspirationLocation")}`);
		console.log(yaml.dump({1: {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("aspirationLocation")
		}}));

		console.log(yaml.dump({2: {
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
		}}));

		console.log(yaml.dump({3: {
			command: "sealer.sealPlate",
			object: scope.get("culturePlate")
		}}));

		console.log(yaml.dump({4: {
			command: "transporter.movePlate", object: scope.get("culturePlate"), destination: scope.get("shakerLocation")
		}}));
	});
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
