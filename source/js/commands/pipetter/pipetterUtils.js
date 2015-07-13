/**
 * A module of helper functions for the pipetter commands.
 * @module commands/pipetter/pipetterUtils
 */

var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var misc = require('../../misc.js');
var wellsParser = require('../../parsers/wellsParser.js');

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
	var wellInfo = wellsParser.parseOne(wellName);
	assert(wellInfo.wellId);
	var labwareContentsName = wellInfo.labware+".contents";
	var wellContentsName = wellInfo.labware+".contents."+wellInfo.wellId;
	// Check for well or labware contents in effects object
	if (!_.isEmpty(effects)) {
		if (effects.hasOwnProperty(wellContentsName))
			return effects[wellContentsName];
		if (effects.hasOwnProperty(labwareContentsName))
			return effects[labwareContentsName];
	}

	var contents = misc.findObjectsValue(wellContentsName, data.objects, effects);
	if (!_.isEmpty(contents)) return contents;

	contents = misc.findObjectsValue(labwareContentsName, data.objects, effects);
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
 * @param {string} wellName fully qualified object name of the well
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects The effects object for effects which have taken place during the command handler and aren't in the data object
* @return {array} [content, contentName], where content will be null if not found
 */
function getContentsAndName(wellName, data, effects) {
	if (!effects) effects = {};

	//var i = wellName.indexOf('(');
	//var wellId = if (i >= 0) {}
	var wellInfo = wellsParser.parseOne(wellName);
	var labwareName;
	//console.log("wellInfo", wellInfo);
	if (wellInfo.source) {
		labwareName = wellInfo.source;
	}
	else {
		assert(wellInfo.wellId);
		labwareName = wellInfo.labware;
		// Check for contents of well
		var contentsName = labwareName+".contents."+wellInfo.wellId;
		var contents = effects[contentsName] || misc.findObjectsValue(contentsName, data.objects, effects);
		if (contents)
			return [contents, contentsName];
	}

	// Check for contents of labware
	//console.log("labwareName", labwareName);
	var contentsName = labwareName+".contents";
	//console.log("contentsName", contentsName)
	var contents = effects[contentsName] || misc.findObjectsValue(contentsName, data.objects, effects);
	if (contents)
		return [contents, contentsName];

	return [null, wellInfo.labware+".contents."+wellInfo.wellId];
}

function flattenContents(contents) {

}

/**
 * Get an object representing the effects of pipetting.
 * @param {object} params The parameters for the pipetter.instruction.pipette command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
* @return {object} The effects caused by the pipetting command.
 */
function getEffects_pipette(params, data, effects) {
	var effects2 = {};

	/*__WELLS__:
		plate1.contents.A01:
			isSource: true
			contentsInitial:
				water: 0ul
			volumeAdded: XXX
			volumeRemoved: 60ul
			contentsFinal:
				water: -60ul
				*/
	//console.log(JSON.stringify(params));
	_.forEach(params.items, function(item) {
		//console.log(JSON.stringify(item));
		var volume = math.eval(item.volume);

		var pair = getContentsAndName(item.source, data, effects);
		var srcContents0 = (pair[0]) ? pair[0] : ["0ul", item.source];
		var srcContentsName = pair[1];

		pair = getContentsAndName(item.destination, data, effects);
		var dstContents = (pair[0]) ? pair[0] : ["0ul"];
		var dstContentsName = pair[1];

		//console.log("dstContents", dstContents)
		var dstVolume = math.eval(dstContents[0]);
		// Increase total well volume
		dstContents[0] = math.chain(dstVolume).add(volume).done().format({precision: 14});
		// Create new content element to add to the contents list

		var srcContents = _.cloneDeep(srcContents0);
		var newContents = (srcContents.length === 2 && _.isString(srcContents[1]))
			? srcContents[1]
			: srcContents;
		//console.log("newContents", newContents)
		// Augment contents list
		dstContents.push(newContents);
		//console.log("dstContents2", dstContents)

		// Decrease volume of source
		var srcVolume0 = math.eval(srcContents[0]);
		var srcVolume1 = math.chain(srcVolume0).subtract(volume).done();
		srcContents[0] = srcVolume1.format({precision: 14});

		// Update content effects
		effects2[srcContentsName] = srcContents;
		effects2[dstContentsName] = dstContents;

		// Update __WELLS__ effects for source
		//console.log("a", srcContentsName);
		var nameWELL = "__WELLS__."+srcContentsName;
		//console.log("nameWELL:", nameWELL)
		var x = misc.findObjectsValue(nameWELL, data.objects, effects);
		x = (x) ? _.cloneDeep(x) : {};
		if (_.isEmpty(x)) {
			x.isSource = true;
			x.volumeMin = srcContents0[0];
			x.volumeMax = srcContents0[0];
		}
		//console.log("max:", x.volumeMax, srcVolume1.toString())
		x.volumeMax = math.max(math.eval(x.volumeMax), srcVolume1).format({precision: 14});
		x.volumeMin = math.min(math.eval(x.volumeMin), srcVolume1).format({precision: 14});
		x.volumeRemoved = (x.volumeRemoved)
			? math.chain(math.eval(x.volumeRemoved)).add(volume).done().format({precision: 14})
			: volume.format({precision: 14});
		//console.log("x:\n"+JSON.stringify(x, null, '  '));
		effects2[nameWELL] = x;
	});

	return effects2;
}

module.exports = {
	getContentsAndName: getContentsAndName,
	getEffects_pipette: getEffects_pipette,
	getWellContents: getWellContents,
	getWellVolume: getWellVolume,
	emptyVolume: emptyVolume
}