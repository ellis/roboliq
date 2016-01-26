import _ from 'lodash';
import commandHelper from '../../commandHelper.js';
import evowareHelper from './evowareHelper.js';

function stripQuotes(s) {
	return (_.startsWith(s, '"') && _.endsWith(s, '"'))
		? s.substring(1, s.length - 1) : s;
}

export function _aspirate(parsed, data) {
	handlePipetterSpirate(parsed, data, "Aspirate");
}

function handlePipetterSpirate(parsed, data, func) {
	if (_.isEmpty(parsed.value.items) return [];

	const tuples = _.map(params.value.items, item => {
		const {labware: labwareName, subject: location} = wellsParser.parseOne(item.well);
		const labware = commandHelper.lookupPath(labwareName, {}, data);
		const labwareModel = commandHelper.lookupPath([labwareName, "model"], {}, data);
		const [row, col] = wellsParser.locationTextToRowCol(location);
		//labwareName <- ResultC.from(wellPosition.labware_?, "incomplete well specification; please also specify the labware")
		//labwareInfo <- getLabwareInfo(objects, labwareName)
		return {item, labwareName, labware, labwareModel, row, col};
	});
	return handlePipetterSpirateDoGroup(parsed, data, func, tuples)

	COPIED FROM elsewhere
	// Find all labware
	var labwareName_l = _(wellName_l).map(function (wellName) {
		//console.log({wellName})
		var i = wellName.indexOf('(');
		return (i >= 0) ? wellName.substr(0, i) : wellName;
	}).uniq().value();
	var labware_l = _.map(labwareName_l, function (name) { return _.merge({name: name}, expect.objectsValue({}, name, data.objects)); });

}

function handlePipetterSpirateDoGroup(
	objects: JsObject,
	program: String,
	func: String,
	tuple_l: List[(PipetterItem, WellNameSingleParsed, LabwareInfo)]
) {
	if (tuple_l.isEmpty) return ResultC.unit(Nil)
	val col = tuple_l.head._2.col
	val labwareInfo = tuple_l.head._3
	// Get all items on the same labware and in the same column
	val tuple_l2 = tuple_l.takeWhile(tuple => tuple._2.col == col && tuple._3 == labwareInfo)
	val (tuple_l3, tipSpacing) = {
		if (tuple_l2.length == 1) {
			tuple_l.take(1) -> 1
		}
		// If there are multiple items, group the ones that are acceptably spaced
		else {
			val syringe0 = tuple_l.head._1.syringe
			val row0 = tuple_l.head._2.row
			val dsyringe = tuple_l(1)._1.syringe - syringe0
			val drow = tuple_l(1)._2.row - row0
			// Syringes and rows should have ascending indexes, and the spacing should be 4 at most
			if (dsyringe <= 0 || drow <= 0 || drow / dsyringe > 4) {
				tuple_l.take(1) -> 1
			}
			else {
				// Take as many items as preserve the initial deltas for syringe and row
				tuple_l2.zipWithIndex.takeWhile({ case (tuple, index) =>
					tuple._2.row == row0 + index * drow && tuple._1.syringe == syringe0 + index * dsyringe
				}).map(_._1) -> drow
			}
		}
	}
	for {
		token_l1 <- handlePipetterSpirateHandleGroup(objects, program, func, tuple_l3, labwareInfo, tipSpacing)
		token_l2 <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l.drop(tuple_l3.size))
	} yield token_l1 ++ token_l2
}

/**
 * [handlePipetterSpirateHandleGroup description]
 * @param  {object} objects - current object state
 * @param  {string} program - name of the evoware liquid class
 * @param  {string} func - "Aspirate" or "Dispense"
 * @param  {array} tuple_l - List[(PipetterItem, WellNameSingleParsed, Any)]
 * @param  {?} labwareInfo - LabwareInfo
 * @param  {integer} tipSpacing - how far apart the tips should be (FIXME: is the base value 1 or 0?)
 * @return {array} array of line info
 */
function handlePipetterSpirateHandleGroup(
	objects,
	program,
	func,
	tuple_l: List[(PipetterItem, WellNameSingleParsed, Any)],
	labwareInfo: LabwareInfo,
	tipSpacing
) {
	// Calculate syringe mask
	val syringe_l = tuple_l.map(_._1.syringe)
	val syringeMask = encodeSyringes(syringe_l)

	val well_l = tuple_l.map(_._2)
	val volume_l = Array.fill(12)("0")

	for {
		labwareModelInfo <- getLabwareModelInfo(objects, labwareInfo.labwareModelName0)
		plateMask <- encodeWells(labwareModelInfo.rowCount, labwareModelInfo.colCount, well_l)
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		_ <- ResultC.foreach(tuple_l) { tuple =>
			for {
				amount <- AmountParser.parse(tuple._1.volume)
				syringe = tuple._1.syringe
				_ <- ResultC.assert(syringe >= 1 && syringe <= 12, `invalid syringe value ${syringe}: must be between 1 and 12`)
				volumeString <- amount match {
					case Amount_Volume(volume) =>
						ResultC.unit(df.format(volume.ul))
					case Amount_Variable(name) =>
						ResultC.unit(name)
					case _ =>
						ResultC.error(s"invalid volume `${tuple._1.volume}`: expected liquid volume or evoware variable name, but got $amount")
				}
			} yield {
				volume_l(syringe - 1) = s""""$volumeString""""
			}
		}
	} yield {
		const l = [
			syringeMask,
			`"${program}"`,
			volume_l.join(","),
			labwareInfo.cngs.gridIndex, labwareInfo.cngs.siteIndex,
			tipSpacing,
			`"${plateMask}"`,
			0,
			0
		];
		const line = `${func}(${l.join(",")});`;

		val siteToNameAndModel_m: Map[CarrierNameGridSiteIndex, (String, String)] = {
			// Don't set labware for the "System" liquid site
			if (labwareInfo.cngs.carrierName == "System") Map()
			else Map(labwareInfo.cngs -> (labwareInfo.siteName, labwareInfo.labwareModelName))
		}

		List(Token(line, JsObject(), siteToNameAndModel_m))
	}
}
