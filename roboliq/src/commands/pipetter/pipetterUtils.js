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
 * Get an object representing the effects of pipetting.
 * @param {object} params The parameters for the pipetter._pipette command.
 * @param {object} data The data object passed to command handlers.
 * @param {object} effects an optional effects object for effects which have taken place during the command handler and aren't in the data object
 * @return {object} The effects caused by the pipetting command.
 */
export function getEffects_pipette(params, data, effects) {
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

		let [srcContents0, srcContentsName] = WellContents.getContentsAndName(item.source, data, effects2);
		if (_.isEmpty(srcContents0))
			srcContents0 = ["Infinity l", item.source];
		console.log("srcContents0", srcContents0, srcContentsName);

		const [dstContents0, dstContentsName] = WellContents.getContentsAndName(item.destination, data, effects2);
		console.log("dst contents", dstContents0, dstContentsName);

		const [srcContents1, dstContents1] = WellContents.transferContents(srcContents0, dstContents0, item.volume);
		console.log({srcContents1, dstContents1});

		// Update content effects
		effects2[srcContentsName] = srcContents1;
		effects2[dstContentsName] = dstContents1;
		effectsNew[srcContentsName] = srcContents1;
		effectsNew[dstContentsName] = dstContents1;
		//console.log()

		const volume = math.eval(item.volume);

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
			const well1 = _.merge(well0, {
				volumeMax: math.max(math.eval(well0.volumeMax), volume1).format({precision: 14}),
				volumeMin: math.min(math.eval(well0.volumeMin), volume1).format({precision: 14}),
				volumeRemoved: (well0.volumeRemoved)
					? math.chain(math.eval(well0.volumeRemoved)).add(volume).done().format({precision: 14})
					: volume.format({precision: 14})
			});
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

	console.log("effectsNew:\n"+JSON.stringify(effectsNew));

	return effectsNew;
}
