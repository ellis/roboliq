var _ = require('lodash');
var assert = require('assert');
var pipetterUtils = require('./pipetterUtils.js');
var sourceParser = require('../../parsers/sourceParser.js');

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
			var wells = getObjectsValue(data.objects, source+".wells");
			assert(!_.isEmpty(wells));
			item.sourceWell = wells[0];
		}
		else {
			item.sourceWell = source;
		}
	});
}

// Rotate through source wells in order
function sourceMethod2(group, data) {
	var sourceToWellIndex = {};
	_.forEach(group, function (item) {
		var source = item.source;
		var sourceInfo = sourceParser.parse(item.source);
		if (sourceInfo.source) {
			var wells = getObjectsValue(data.objects, source+".wells");
			assert(!_.isEmpty(wells));
			var i = (sourceToWellIndex.hasOwnProperty(source)) ? sourceToWellIndex[source] : 0;
			item.sourceWell = wells[i];
			sourceToWellIndex[source] = (i + 1) % wells.length;
		}
		else {
			item.sourceWell = source;
		}
	});
}

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
	var sourceToItems = _.groupBy(group, 'source');
	_.forEach(sourceToItems, function (items, source) {
		_.forEach(items, function(item) {
			assert(item.source);
			if (_.isArray(item.source)) {
				var wells = item.source;
				assert(!_.isEmpty(wells));
				if (wells.length === 1) {
					item.sourceWell = wells[0];
				}
				else {
					var wellAndVolumes = _.map(wells, function(wellName) {
						var volume = pipetterUtils.getWellVolume(wellName, data, effects);
						return [well, volume.toNumber('ul')];
					});
					// Sort by volume
					math.sort(wellAndVolumes, function(a, b) { return -compare(a[1], b[1])});
					// Pick well with greatest volume
					var wellName = wellAndVolumes[0][0];
					item.sourceWell = wellName;
					// Get effect of pipetting, so that source volumes are changed appropriately
					var effect = pipetterUtils.getEffects_pipette({items: [item]});
					_.merge(effects, effect);
				}
			}
			else {
				item.sourceWell = source;
			}
		});
	});
}

module.exports = {
	sourceMethod1: sourceMethod1,
	sourceMethod2: sourceMethod2,
	sourceMethod3: sourceMethod3
}
