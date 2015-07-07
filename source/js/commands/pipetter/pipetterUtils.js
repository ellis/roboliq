var _ = require('lodash');
var math = require('mathjs');

var emptyVolume = math.eval('0ul');

/**
 * Tries to find the contents array for the given well.
 *
 * @param {string} wellName name of the well
 * @param {object} data the data object passed to command handlers
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {array} the contents array if found, otherwise null
 */
function getWellContents(wellName, data, effects) {
	var wellInfo = sourceParser.parse(wellName);
	assert(wellInfo.wellId);
	var labwareContentsName = srcInfo.labware+".contents";
	var wellContentsName = srcInfo.labware+".contents."+srcInfo.wellId;
	// Check for well or labware contents in effects object
	if (!_.isEmpty(effects)) {
		if (effects.hasOwnProperty(wellContentsName))
			return effects(wellContentsName);
		if (effects.hasOwnProperty(labwareContentsName))
			return effects(labwareContentsName);
	}

	var contents = misc.getObjectsValue(data.objects, wellContentsName);
	if (!_.isEmpty(contents)) return contents;

	contents = misc.getObjectsValue(data.objects, labwareContentsName);
	return contents;
}

/**
 * Get the volume of the given well.
 * @param {string} wellName name of the well
 * @param {object} data the data object passed to command handlers
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {array} the volume if found, otherwise 0ul
 */
function getWellVolume(wellName, data, effects) {
	var contents = getWellContents(wellName, data, effects);
	if (!_.isEmpty(contents)) {
		var volume = math.eval(contents[0]);
		if (volume.unit.name === 'l') return volume;
	}
	return emptyVolume;
}

/**
 * Get an object representing the effects of pipetting.
 * @param {object} params The parameters for the pipetter.instruction.pipette command.
 * @param {object} data The data object passed to command handlers.
 * @return {object} The effects caused by the pipetting command.
 */
function getEffects_pipette(params, data) {
	var effects = {};

	//console.log(JSON.stringify(params));
	_.forEach(params.items, function(item) {
		//console.log(JSON.stringify(item));
		var volume = math.eval(item.volume);
		var srcInfo = sourceParser.parse(item.source);
		assert(srcInfo.wellId);
		var dstInfo = sourceParser.parse(item.destination);
		assert(dstInfo.wellId);
		var srcContentsName = srcInfo.labware+".contents."+srcInfo.wellId;
		var dstContentsName = dstInfo.labware+".contents."+dstInfo.wellId;
		var srcContents = effects[srcContentsName] || _.cloneDeep(misc.getObjectsValue(data.objects, srcContentsName)) || ["0ul", item.source];
		//console.log("srcContents", srcContents)
		if (!_.isEmpty(srcContents)) {
			// Get destination contents before the command
			var dstContents = effects[dstContentsName] || _.cloneDeep(misc.getObjectsValue(data.objects, dstContentsName));
			if (_.isEmpty(dstContents)) {
				dstContents = ["0ul"];
			}
			//console.log("dstContents", dstContents)
			var dstVolume = math.eval(dstContents[0]);
			// Increase total well volume
			dstContents[0] = math.chain(dstVolume).add(volume).done().format({precision: 14});
			// Create new content element to add to the contents list
			var newContents = (srcContents.length === 2 && _.isString(srcContents[1]))
				? srcContents[1]
				: srcContents;
			//console.log("newContents", newContents)
			// Augment contents list
			dstContents.push(newContents);
			//console.log("dstContents2", dstContents)

			// Decrease volume of source
			var srcVolume = math.eval(srcContents[0]);
			srcContents[0] = math.chain(srcVolume).subtract(volume).done().format({precision: 14});

			// Update effects
			effects[srcContentsName] = srcContents;
			effects[dstContentsName] = dstContents;
		}
	});

	return effects;
}

module.exports = {
	getEffects_pipette: getEffects_pipette,
	getWellContents: getWellContents,
	getWellVolume: getWellVolume,
	emptyVolume: emptyVolume
}
