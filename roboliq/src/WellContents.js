/**
 * A module of functions for querying and manipulating well contents.
 * @module WellContents
 */

import _ from 'lodash';
var assert = require('assert');
var math = require('mathjs');
import expect from './expectCore.js';
var misc = require('./misc.js');
var wellsParser = require('./parsers/wellsParser.js');

export const emptyVolume = math.unit(0, 'ul');
export const unknownVolume = math.eval('Infinity l');

/**
 * Validate well contents.  Throws an error if they aren't valid.
 *
 * @param  {array} contents - The well contents.
 */
export function checkContents(contents) {
	if (_.isUndefined(contents)) {
		// ok
	}
	else if (!_.isArray(contents)) {
		assert(false, "expected well contents to be represented by an array: "+JSON.stringify(contents));
	}
	else if (contents.length == 0) {
		// ok
	}
	else {
		//console.log(contents)
		var volume = math.eval(contents[0]);
		if (contents.length == 1) {
			// FIXME: remove 'false, ' from here!
			assert.equal(volume.toNumber('l'), 0, "when the contents array has only one element, that element must be 0: "+JSON.stringify(contents));
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
 * Tries to find the contents array for the given syringe.
 *
 * @param {string} syringeName name of the syringe
 * @param {object} data the data object passed to command handlers
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {WellContents} the contents array if found, otherwise null
 *//*
export function getSyringeContents(syringeName, data, effects) {
	//console.log({syringeName})
	const contentsName = `${syringeName}.contents`;
	// Check for well or labware contents in effects object
	if (!_.isEmpty(effects)) {
		if (effects.hasOwnProperty(contentsName))
			return effects[contentsName];
	}

	let contents = misc.findObjectsValue(contentsName, data.objects, effects);
	checkContents(contents);
	return contents;
}*/

/**
 * Tries to find the contents array for the given well.
 *
 * @param {string} wellName name of the well
 * @param {object} data the data object passed to command handlers
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {WellContents} the contents array if found, otherwise null
 */
export function getWellContents(wellName, data, effects) {
	//console.log({wellName})
	var wellInfo = wellsParser.parseOne(wellName);
	assert(wellInfo.wellId, "missing `wellId`: "+JSON.stringify(wellInfo));
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
	if (_.isArray(contents)) {
		expect.try({objectName: wellName}, () => checkContents(contents));
		return contents;
	}

	return [];
}

/**
 * Get the volume of the contents array.
 * @param {array} contents The well contents array
 * @return {object} the mathjs volume if found, otherwise 0ul
 */
export function getVolume(contents) {
	checkContents(contents);
	if (!_.isEmpty(contents)) {
		const volume = math.eval(contents[0]);
		if (math.unit('l').equalBase(volume)) return volume;
	}
	return emptyVolume;
}

/**
 * Check whether the contents are empty.
 * They are empty if the contents are undefined, an empty array,
 * or the array begins with a number that mathjs considers equal to 0.
 *
 * @param {WellContents} contents
 * @return {Boolean} true if the contents are empty
 */
export function isEmpty(contents) {
	const volume = getVolume(contents);
	return math.equal(volume.toNumber('l'), 0);
}

/**
 * Get the volume of the given well.
 * @param {string} wellName name of the well
 * @param {object} data the data object passed to command handlers
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} the mathjs volume if found, otherwise 0ul
 */
export function getWellVolume(wellName, data, effects) {
	var contents = getWellContents(wellName, data, effects);
	if (!_.isEmpty(contents)) {
		var volume = math.eval(contents[0]);
		if (math.unit('l').equalBase(volume)) return volume;
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
export function getContentsAndName(wellName, data, effects) {
	//console.log("getContentsAndName", wellName)
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
		//console.log("contentsName", contentsName, effects[contentsName], _.get(data.objects, contentsName))
		var contents = effects[contentsName] || misc.findObjectsValue(contentsName, data.objects, effects);
		checkContents(contents);
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
	if (_.isArray(contents)) {
		checkContents(contents);
		return [contents, contentsName];
	}

	return [undefined, wellInfo.labware+".contents."+wellInfo.wellId];
}

/**
 * Convert the contents array encoding to a map of substances to amounts
 * @param  {array} contents The well contents array
 * @return {object} map of substance name to the volume or amount of that substance in the well
 */
export function flattenContents(contents) {
	checkContents(contents);
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
		return _.fromPairs([[contents[1], volume]]);
	}
	// If the array has more than two elements, each element after the volume has the same
	// structure as the top array and they represent the mixture originally dispensed in the well.
	else {
		/*const maps = _(contents).tail().map(contents2 => {
			const flattened = flattenContents(contents2);
			//console.log({flattened});
			const x = _.mapValues(flattened, value => math.eval(value))
			//console.log({x});
			return x;
		}).value();*/
		var maps = _.map(_.tail(contents), contents => _.mapValues(flattenContents(contents), value => math.eval(value)));
		//console.log("maps: "+JSON.stringify(maps));
		var merger = function(a, b) { return (_.isUndefined(a)) ? b : math.add(a, b); };
		var mergeArgs = _.flatten([{}, maps, merger]);
		//console.log("mergeArgs: "+mergeArgs);
		var merged = _.mergeWith.apply(_, mergeArgs);
		//console.log("merged: "+JSON.stringify(merged));
		var total = math.eval(contents[0]);
		var subTotal = _.reduce(merged, function(total, n) { return math.add(total, n); }, emptyVolume);
		//console.log("total: "+total);
		//console.log("subTotal: "+subTotal);
		//var factor = math.fraction(total, subTotal);
		try {
			const result = _.mapValues(merged, function(v) {
				//console.log({v, totalNumber: total.toNumber("l")})
				const numerator = math.multiply(v, total.toNumber("l"));
				const result = math.divide(numerator, subTotal.toNumber("l"))
				return result.format({precision: 4});
			});
			return result;
		} catch (e) {
			console.log({total: total.toNumber("l"), subTotal: subTotal.toNumber("l"), factor})
			console.log(JSON.stringify(contents))
			throw e;
		}
	}
}

/**
 * Add source contents to destination contents at the given volume.
 * @param {array} srcContents - current contents of the source well
 * @param {array} dstContents - current contents of the destination well
 * @param {string} volume - a string representing the volume to transfer
 * @return {array} an array whose first element is the new source contents and whose second element is the new destination contents.
 */
export function transferContents(srcContents, dstContents, volume) {
	assert(_.isArray(srcContents));
	checkContents(srcContents);

	if (_.isString(volume))
		volume = math.eval(volume);

	const volumeText = volume.format({precision: 14});

	//console.log({dstContents})
	if (_.isUndefined(dstContents) || _.isEmpty(dstContents) || !_.isArray(dstContents))
		dstContents = [];
	//console.log({dstContents})
	checkContents(dstContents);

	const srcContentsToAppend = [volumeText].concat(_.tail(srcContents));
	let dstContents2;
	// If the destination is empty:
	if (dstContents.length <= 1) {
		dstContents2 = srcContentsToAppend;
	}
	else {
		const dstVolume = math.eval(dstContents[0]);
		const totalVolumeText = math.add(dstVolume, volume).format({precision: 14});
		// If the destination currently only contains one substance:
		if (dstContents.length === 2) {
			dstContents2 = [totalVolumeText, dstContents, srcContentsToAppend];
		}
		// Otherwise add source to destination contents
		else {
			const dstSumOfComponents = math.sum(_.map(_.tail(dstContents), l => math.eval(l[0])));
			if (math.equal(dstVolume, dstSumOfComponents)) {
				dstContents2 = _.flatten([totalVolumeText, _.tail(dstContents), [srcContentsToAppend]]);
			}
			else {
				dstContents2 = _.flatten([totalVolumeText, [dstContents], [srcContentsToAppend]]);
			}
		}
	}
	//console.log("dstContents", dstContents);

	// Decrease volume of source
	const srcVolume0 = math.eval(srcContents[0]);
	const srcVolume1 = math.chain(srcVolume0).subtract(volume).done();
	const srcContents2 = [srcVolume1.format({precision: 14})].concat(_.tail(srcContents));

	checkContents(srcContents2);
	checkContents(dstContents2);

	return [srcContents2, dstContents2];
}
