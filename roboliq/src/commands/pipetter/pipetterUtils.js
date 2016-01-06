/**
 * A module of helper functions for the pipetter commands.
 * @module commands/pipetter/pipetterUtils
 */

var _ = require('lodash');
var assert = require('assert');
var math = require('mathjs');
var misc = require('../../misc.js');
var wellsParser = require('../../parsers/wellsParser.js');
import * as WellContents from '../../WellContents.js';

/**
 * Get an object representing the effects of aspirating.
 * @param {object} params The parameters for the pipetter._aspirate command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} The effects caused by the pipetting command.
 */
export function getEffects_aspirate(parsed, data, effects) {
	const effects2 = (effects) ? _.cloneDeep(effects) : {};
	const effectsNew = {};

	function addEffect(name, obj) {
		effects2[name] = obj;
		effectsNew[name] = obj;
	}

	//console.log(JSON.stringify(parsed, null, '\t'));
	for (const index in parsed.value.items) {
		const item = parsed.value.items[index];
		//console.log(JSON.stringify(item));

		// Get initial contents of the source well
		let [srcContents0, srcContentsName] = WellContents.getContentsAndName(item.source, data, effects2);
		if (_.isEmpty(srcContents0))
			srcContents0 = ["Infinity l", item.source];
		//console.log("srcContents0", srcContents0, srcContentsName);

		// Get initial contents of the syringe
		const syringeContents0 = item.syringe.contents || [];

		// Final contents of source well and syringe
		const [srcContents1, syringeContents1] = WellContents.transferContents(srcContents0, syringeContents0, item.volume);
		//console.log({srcContents1, syringeContents1});

		// Get list of syringe contaminants
		const contaminants0 = item.syringe.contaminants || [];
		const contaminants1 = _.keys(WellContents.flattenContents(syringeContents1));
		const syringeName = parsed.objectName[`items.${index}.syringe`];
		if (!_.isEqual(contaminants0, contaminants1))
			addEffect(`${syringeName}.contaminants`, contaminants1);
		// Update content effects
		addEffect(srcContentsName, srcContents1);
		addEffect(`${syringeName}.contents`, syringeContents1);
		// Remove cleaned property
		if (!_.isUndefined(item.syringe.cleaned))
			addEffect(`${syringeName}.cleaned`, null);

		const volume = item.volume;

		// Update __WELLS__ effects for source
		if (true) {
			const volume1 = math.eval(srcContents1[0]);
			const nameWELL = "__WELLS__."+srcContentsName;
			//console.log("nameWELL:", nameWELL)
			const well0 = misc.findObjectsValue(nameWELL, data.objects, effects2) || {
				isSource: true,
				volumeMin: srcContents0[0],
				volumeMax: srcContents0[0]
			};
			const well1 = _.merge({}, well0, {
				volumeMax: math.max(math.eval(well0.volumeMax), volume1).format({precision: 14}),
				volumeMin: math.min(math.eval(well0.volumeMin), volume1).format({precision: 14}),
				volumeRemoved: (well0.volumeRemoved)
					? math.chain(math.eval(well0.volumeRemoved)).add(volume).done().format({precision: 14})
					: volume.format({precision: 14})
			});
			//console.log({well0, well1});
			//console.log("x:\n"+JSON.stringify(x, null, '  '));
			addEffect(nameWELL, well1);
		}
	}

	//console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}

/**
 * Get an object representing the effects of pipetting.
 * @param {object} params The parameters for the pipetter._pipette command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} The effects caused by the pipetting command.
 */
export function getEffects_pipette(parsed, data, effects) {
	const effects2 = (effects) ? _.cloneDeep(effects) : {};
	const effectsNew = {};

	function addEffect(name, obj) {
		effects2[name] = obj;
		effectsNew[name] = obj;
	}

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
	//console.log("getEffects_pipette:")
	//console.log(JSON.stringify(parsed, null, '\t'));
	for (const index in parsed.value.items) {
		const item = parsed.value.items[index];
		//console.log(JSON.stringify(item));

		// Get initial contents of the source well
		let [srcContents0, srcContentsName] = WellContents.getContentsAndName(item.source, data, effects2);
		if (_.isEmpty(srcContents0))
			srcContents0 = ["Infinity l", item.source];
		//console.log("srcContents0", srcContents0, srcContentsName);

		// Get initial contents of the syringe
		const syringeContents0 = item.syringe.contents || [];
		// Get initial contents of the destination well
		const [dstContents0, dstContentsName] = WellContents.getContentsAndName(item.destination, data, effects2);
		//console.log("dst contents", dstContents0, dstContentsName);

		// Final contents of source and destination wells
		const [srcContents1, dstContents1] = WellContents.transferContents(srcContents0, dstContents0, item.volume);
		//console.log({srcContents1, dstContents1});

		// Contaminate the syringe with source contents
		// FIXME: Contaminate the syringe with destination contents if there is wet contact, i.e., use dstContents1 instead of srcContents0 for flattenContents()
		const contaminants0 = item.syringe.contaminants || [];
		const contaminants1 = _.keys(WellContents.flattenContents(srcContents0));
		const contaminants2 = _.uniq(contaminants0.concat(contaminants1));
		const syringeName = parsed.objectName[`items.${index}.syringe`];
		addEffect(`${syringeName}.contaminants`, contaminants2);
		// Remove contents property
		// FIXME: this isn't quite right -- should handle the case of multi-aspriating
		if (!_.isUndefined(item.syringe.contents))
			addEffect(`${syringeName}.contents`, null);
		// Remove cleaned property
		if (!_.isUndefined(item.syringe.cleaned))
			addEffect(`${syringeName}.cleaned`, null);

		// Update content effects
		addEffect(srcContentsName, srcContents1);
		addEffect(dstContentsName, dstContents1);
		//console.log()

		const volume = item.volume;

		// Update __WELLS__ effects for source
		if (true) {
			const volume1 = math.eval(srcContents1[0]);
			const nameWELL = "__WELLS__."+srcContentsName;
			//console.log("nameWELL:", nameWELL)
			const well0 = misc.findObjectsValue(nameWELL, data.objects, effects2) || {
				isSource: true,
				volumeMin: srcContents0[0],
				volumeMax: srcContents0[0]
			};
			const well1 = _.merge({}, well0, {
				volumeMax: math.max(math.eval(well0.volumeMax), volume1).format({precision: 14}),
				volumeMin: math.min(math.eval(well0.volumeMin), volume1).format({precision: 14}),
				volumeRemoved: (well0.volumeRemoved)
					? math.chain(math.eval(well0.volumeRemoved)).add(volume).done().format({precision: 14})
					: volume.format({precision: 14})
			});
			//console.log({well0, well1});
			//console.log("x:\n"+JSON.stringify(x, null, '  '));
			addEffect(nameWELL, well1);
		}

		// Update __WELLS__ effects for destination
		if (true) {
			const volume1 = math.eval(dstContents1[0]);
			const nameWELL = "__WELLS__."+dstContentsName;
			//console.log("nameWELL:", nameWELL)
			const well0 = misc.findObjectsValue(nameWELL, data.objects, effects2) || {
				isSource: false,
				volumeMin: '0 l',
				volumeMax: '0 l'
			};
			const well1 = _.merge(well0, {
				volumeMax: math.max(math.eval(well0.volumeMax), volume1).format({precision: 14}),
				volumeMin: math.min(math.eval(well0.volumeMin), volume1).format({precision: 14}),
				volumeAdded: (well0.volumeAdded)
					? math.chain(math.eval(well0.volumeAdded)).add(volume).done().format({precision: 14})
					: volume.format({precision: 14})
			});
			//console.log("x:\n"+JSON.stringify(x, null, '  '));
			addEffect(nameWELL, well1);
		}
	}

	//console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}
