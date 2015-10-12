/**
 * A module of helper functions for the pipetter commands.
 * @module commands/pipetter/pipetterUtils
 */

var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var misc = require('../../misc.js');
var wellsParser = require('../../parsers/wellsParser.js');

var emptyVolume = math.unit(0, 'ul');
var unknownVolume = emptyVolume; // math.unit(math.NaN, 'l');

function checkContents(contents) {
	if (_.isUndefined(contents) || contents.length == 0) {
		// ok
	}
	else {
		var volume = math.eval(contents[0]);
		if (contents.length == 1) {
			assert(false, volume.toNumber('l') === 0, "when the contents array has only one element, that element must be 0: "+JSON.stringify(contents));
		}
		else if (contents.length == 2) {
			assert(_.isString(contents[1]), "second element of contents should be a string: "+JSON.stringify(contents));
		}
		else {
			for (var i = 1; i < contents.length; i++) {
				//try {
					checkContents(contents[i]);
				//} catch (e) {
				//
				//}
			}
		}
	}
}

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
	checkContents(contents);
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
	// If contents is an array, then we have the correct contents;
	// Otherwise, we have a map of well contents, but no entry for the current well yet
	if (_.isArray(contents))
		return [contents, contentsName];

	return [null, wellInfo.labware+".contents."+wellInfo.wellId];
}

/**
 * Convert the contents array encoding to a map of substances to amounts
 * @param  {array} contents The well contents array
 * @return {object} map of substance name to the volume or amount of that substance in the well
 */
function flattenContents(contents) {
	//console.log("flattenContents:", contents);
	assert(_.isArray(contents));
	// The first element always holds the volume in the well.
	// If the array has exactly one element, the volume should be 0l.
	if (contents.length <= 1) {
		return {};
	}
	// If the array has exactly two elements, the second element is the name of the substance.
	else if (contents.length == 2) {
		assert(_.isString(contents[1]), "second element of contents should be a string: "+JSON.stringify(contents));
		var volume = math.eval(contents[0]).format({precision: 14});
		return _.zipObject([[contents[1], volume]]);
	}
	// If the array has more than two elements, each element after the volume has the same
	// structure as the top array and they represent the mixture originally dispensed in the well.
	else {
		var maps = _.map(_.rest(contents), function(contents) { return _.mapValues(flattenContents(contents), function(value) { return math.eval(value); }); });
		//console.log("maps: "+JSON.stringify(maps));
		var merger = function(a, b) { return (_.isUndefined(a)) ? b : math.add(a, b); };
		var mergeArgs = _.flatten([{}, maps, merger]);
		//console.log("mergeArgs: "+mergeArgs);
		var merged = _.merge.apply(_, mergeArgs);
		//console.log("merged: "+JSON.stringify(merged));
		var total = math.eval(contents[0]);
		var subTotal = _.reduce(merged, function(total, n) { return math.add(total, n); }, emptyVolume);
		//console.log("total: "+total);
		//console.log("subTotal: "+subTotal);
		var factor = math.divide(total.toNumber("l"), subTotal.toNumber("l"));
		return _.mapValues(merged, function(v) { return math.multiply(v, factor).format({precision: 14}); });
	}
}

/**
 * Get an object representing the effects of pipetting.
 * @param {object} params The parameters for the pipetter._pipette command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} The effects caused by the pipetting command.
 */
function getEffects_pipette(params, data, effects) {
	var effects2 = (effects) ? _.cloneDeep(effects) : {};
	var effectsNew = {}

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
		var volumeText = volume.format({precision: 14});

		var pair = getContentsAndName(item.source, data, effects2);
		//console.log("src contents", item.source, pair[0])
		var srcContents0 = (pair[0]) ? pair[0] : ["Infinity l", item.source];
		//console.log("srcContents0", srcContents0);
		var srcContents = _.cloneDeep(srcContents0);
		var srcContentsName = pair[1];

		pair = getContentsAndName(item.destination, data, effects2);
		//console.log("dst contents", item.destination, pair[0])
		var dstContentsName = pair[1];
		var dstContents0 = (pair[0]) ? pair[0] : ["0 l"];
		var dstContents;
		// If the destination is empty:
		if (!pair[0] || pair[0].length <= 1) {
			dstContents = [volumeText].concat(_.rest(srcContents0));
		}
		// Otherwise add source to destination contents
		else {
			var dstVolume = math.eval(dstContents0[0]);
			var totalVolumeText = math.add(dstVolume, volume).format({precision: 14});
			dstContents = [totalVolumeText, dstContents0, [volumeText].concat(_.rest(srcContents0))];
		}
		//console.log("dstContents", dstContents);

		// Decrease volume of source
		var srcVolume0 = math.eval(srcContents[0]);
		var srcVolume1 = math.chain(srcVolume0).subtract(volume).done();
		srcContents[0] = srcVolume1.format({precision: 14});

		// Update content effects
		effects2[srcContentsName] = srcContents;
		effects2[dstContentsName] = dstContents;
		effectsNew[srcContentsName] = srcContents;
		effectsNew[dstContentsName] = dstContents;

		// Update __WELLS__ effects for source
		//console.log("a", srcContentsName);
		var nameWELL = "__WELLS__."+srcContentsName;
		//console.log("nameWELL:", nameWELL)
		var x = misc.findObjectsValue(nameWELL, data.objects, effects2);
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
		effectsNew[nameWELL] = x;
		//console.log();
	});

	return effects2;
}

module.exports = {
	getContentsAndName: getContentsAndName,
	getEffects_pipette: getEffects_pipette,
	getWellContents: getWellContents,
	getWellVolume: getWellVolume,
	flattenContents: flattenContents,
	emptyVolume: emptyVolume,
	unknownVolume: emptyVolume,
}
