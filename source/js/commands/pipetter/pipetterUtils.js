var _ = require('lodash');

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

	var contents = misc.getObjectsValue(data.objects, wellContentsName));
	if (!_.isEmpty(contents)) return contents;

	contents = misc.getObjectsValue(data.objects, labwareContentsName));
	return contents;
}

module.exports = {
	getWellContents: getWellContents
}
