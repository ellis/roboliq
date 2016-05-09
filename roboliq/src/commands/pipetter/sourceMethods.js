var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
import commandHelper from '../../commandHelper.js';
var expect = require('../../expect.js');
var pipetterUtils = require('./pipetterUtils.js');
var WellContents = require('../../WellContents.js');

/*
- assign source well by group for items without assigned source wells; if multiple syringes need to access the same source, and that source has multiple wells, then possible methods include:
    - pick first one
    - rotate through source wells in order
    - rotate through source wells in order of max volume
    - try a simple geometrical assignment considering whether there are more tips or wells; if that fails, use previous method
    - same as above, but if wells > tips, try starting at first (wells - tips) wells and see which one produces the greatest minimum final volume
*/

// Pick the first well in a source set and ignore the others
function sourceMethod1(group, data) {
	_.forEach(group, function (item) {
		var source = item.source;
		var sourceInfo = sourceParser.parse(item.source);
		if (sourceInfo.source) {
			var wells = expect.objectsValue({}, source+".wells", data.objects);
			assert(!_.isEmpty(wells));
			item.sourceWell = wells[0];
		}
		else {
			item.sourceWell = source;
		}
	});
}

// Rotate through source wells in order
/*function sourceMethod2(group, data) {
	var sourceToWellIndex = {};
	_.forEach(group, function (item) {
		var source = item.source;
		var sourceInfo = sourceParser.parse(item.source);
		if (sourceInfo.source) {
			var wells = getObjectsValue(source+".wells", data.objects);
			assert(!_.isEmpty(wells));
			var i = (sourceToWellIndex.hasOwnProperty(source)) ? sourceToWellIndex[source] : 0;
			item.sourceWell = wells[i];
			sourceToWellIndex[source] = (i + 1) % wells.length;
		}
		else {
			item.sourceWell = source;
		}
	});
}*/

/**
 * Rotate through source wells in order of max volume.
 * The 'sourceWell' property of each item in 'group' will be set.
 *
 * @param  {array} group   Array of pipetting items that are grouped together
 * @param  {object} data    Data passed to the commandHandler
 * @param  {object} effects (Optional) Map from variable to effects
 */
function sourceMethod3(group, data, effects) {
	// Make our own copy of the the effects object
	var effects = (effects) ? _.cloneDeep(effects) : {};

	// Consider each source in the group separately
	var sourceToItems = _.groupBy(_.filter(group, x => x.source), 'source');
	//console.log("sourceToItems:\n"+JSON.stringify(sourceToItems, null, '  '));
	for (const items of _.values(sourceToItems)) {
		console.log("sourceMethod3", items)
		assert(items[0].source);
		var wells = _.clone(items[0].source);
		assert(!_.isEmpty(wells));
		for (const item of items) {
			//console.log("wells: ", wells);
			if (_.isArray(wells)) {
				if (wells.length === 1) {
					item.source = wells[0];
				}
				else {
					var wellAndVolumes = _.map(wells, function(wellName) {
						var volume = WellContents.getWellVolume(wellName, data, effects);
						return {wellName, volume: volume.toNumber('ul')};
					});
					// Sort by volume
					//console.log({wellAndVolumes})
					math.sort(wellAndVolumes, function(a, b) { return -math.compare(a.volume, b.volume)});
					// Pick well with greatest volume
					var wellName = wellAndVolumes[0].wellName;
					item.source = wellName;
					//console.log("well chosen:", wellName);
					// Move the chosen well to the back of the array
					_.pull(wells, wellName);
					wells.push(wellName);

					const params = {items: [_.clone(item)]};
					params.items[0].volume = item.volume.format({precision: 14});
					const schema = {
						properties: {
							items: {
								description: "Data about what should be pipetted where", "type": "array",
								items: {type: "pipetter._PipetteItem"}
							}
						},
						required: ["items"]
					};
					//console.log("param of items:")
					//console.log(JSON.stringify(params, null, '\t'))
					const parsed = commandHelper.parseParams(params, data, schema);
					//console.log("parsed:");
					//console.log(JSON.stringify(parsed, null, '\t'))

					// Get effect of pipetting, so that source volumes are changed appropriately
					var effects2 = pipetterUtils.getEffects_pipette(parsed, data, effects);
					_.merge(effects, effects2);
					//console.log("effects2", effects2)
					//console.log("effects", effects)
				}
			}
			else {
				//item.source = item.source;
			}
		}
	}
}

module.exports = {
	sourceMethod1,
	//sourceMethod2: sourceMethod2,
	sourceMethod3
}
