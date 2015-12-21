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
	var effects2 = (effects) ? _.cloneDeep(effects) : {};
	var effectsNew = {}

	console.log(JSON.stringify(parsed));
	for (const item of parsed.items.value) {
		console.log(JSON.stringify(item));

		/*CONTINUE
		let [srcContents0, srcContentsName] = WellContents.getContentsAndName(item.source.value, data, effects2);
		if (_.isEmpty(srcContents0))
			srcContents0 = ["Infinity l", item.source.value];
		//console.log("srcContents0", srcContents0, srcContentsName);

		const [dstContents0, dstContentsName] = WellContents.getContentsAndName(item.destination.value, data, effects2);
		//console.log("dst contents", dstContents0, dstContentsName);

		const [srcContents1, dstContents1] = WellContents.transferContents(srcContents0, dstContents0, item.volume.value);
		//console.log({srcContents1, dstContents1});

		// Update content effects
		effects2[srcContentsName] = srcContents1;
		effects2[dstContentsName] = dstContents1;
		effectsNew[srcContentsName] = srcContents1;
		effectsNew[dstContentsName] = dstContents1;
		//console.log()

		const volume = item.volume.value;

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
			effects2[nameWELL] = well1;
			effectsNew[nameWELL] = well1;
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
			effects2[nameWELL] = well1;
			effectsNew[nameWELL] = well1;
		}
		*/
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
	_.forEach(parsed.items.value, item => {
		console.log(JSON.stringify(item));

		// Get initial contents of the source well
		let [srcContents0, srcContentsName] = WellContents.getContentsAndName(item.source.value, data, effects2);
		if (_.isEmpty(srcContents0))
			srcContents0 = ["Infinity l", item.source.value];
		//console.log("srcContents0", srcContents0, srcContentsName);

		// Get initial contents of the syringe
		const syringeContents0 = item.syringe.value.contents || [];
		// Get initial contents of the destination well
		const [dstContents0, dstContentsName] = WellContents.getContentsAndName(item.destination.value, data, effects2);
		//console.log("dst contents", dstContents0, dstContentsName);

		// Final contents of source and destination wells
		const [srcContents1, dstContents1] = WellContents.transferContents(srcContents0, dstContents0, item.volume.value);
		//console.log({srcContents1, dstContents1});

		// Contaminate the syringe with source contents
		// FIXME: Contaminate the syringe with destination contents if there is wet contact, i.e., use dstContents1 instead of srcContents0 for flattenContents()
		const contaminantsName = `${item.syringe.objectName}.contaminants`;
		const contaminants0 = item.syringe.value.contaminants || [];
		const contaminants1 = _.keys(WellContents.flattenContents(srcContents0));
		const contaminants2 = _.uniq(contaminants0.concat(contaminants1));
		effects2[contaminantsName] = contaminants2;
		effectsNew[contaminantsName] = contaminants2;

		// Update content effects
		effects2[srcContentsName] = srcContents1;
		effects2[dstContentsName] = dstContents1;
		effectsNew[srcContentsName] = srcContents1;
		effectsNew[dstContentsName] = dstContents1;
		//console.log()

		const volume = item.volume.value;

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
			effects2[nameWELL] = well1;
			effectsNew[nameWELL] = well1;
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
			effects2[nameWELL] = well1;
			effectsNew[nameWELL] = well1;
		}
	});

	//console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}
