import _ from 'lodash';
import fs from 'fs';
import moment from 'moment';
import {XmlDocument} from 'xmldoc';

/**
 * Read fluorescence or absorbance data from Infinite M200 XML file
 * @param  {string} filename - Filename of XML file to loa
 * @return {array} array of objects representing a data table of the measurements.
 */
export default function processXml(filename) {
	const content = fs.readFileSync(filename);
	const doc = new XmlDocument(content);
	//const o = XML.parseFileSync(filename);

	const timeOfMeasurement = moment(doc.attr.Date);
	const section = doc.childNamed("Section");
	const timeStart = moment(section.attr.Time_Start);
	const timeEnd = moment(section.attr.Time_End);
	const datas = section.childrenNamed("Data");
	// Find the average delta-time between measurements
	const duration = timeEnd.diff(timeStart); // Different between start and end in milliseconds
	const dt = (datas.length > 1) ? duration / (datas.length - 1) : 0;
	// console.log({timeOfMeasurement, datas});
	const table = _.flatMap(datas, (data, index) => {
		const wells = data.childrenNamed("Well");
		const cycle = parseInt(data.attr.Cycle);
		return _.flatMap(wells, well => {
			const time = timeStart.add(dt * index, "milliseconds").toISOString();
			const pos = well.attr.Pos;
			const [row, col] = locationTextToRowCol(pos);
			const wellName = locationRowColToText(row, col);
			//const well = pos; // FIXME: getWellName(row, col, plateName)
			const type = well.attr.Type;
			if (type === "Single") {
				const value = Number(well.lastChild.val);
				const entry = {time, well: wellName, cycle, value};
				// console.log(entry);
				return entry;
			}
			else if (type === "Scan") {
				const scans = well.childrenNamed("Scan");
				return scans.map(scan => {
					const wavelength = Number(scan.attr.WL);
					const value0 = scan.val;
					const value = (value0 === "OVER") ? null : Number(value0);
					const entry = {time, well: wellName, cycle, wavelength, value};
					return entry;
				});
			}
		});
	});

	// console.log(table);
	return {date: timeOfMeasurement, table};
}

// FIXME: these are copied from wellsParser.js
function locationTextToRowCol(location) {
	var row = location.charCodeAt(0) - "A".charCodeAt(0) + 1;
	var col = parseInt(location.substr(1));
	return [row, col];
}
function locationRowColToText(row, col) {
	var colText = col.toString();
	if (colText.length == 1) colText = "0"+colText;
	return String.fromCharCode("A".charCodeAt(0) + row - 1) + colText;
}


// processXml("/Users/ellisw/repo/bsse-lab/tania.201411/inst/extdata/20141108--tania13_ph/excitation485/tania13_ph--20141111_112409.xml")

/*
loadInfiniteM200Xml = function(filename, plateName = "", ...) {
  root = xmlRoot(xmlTreeParse(filename))
  timeOfMeasurement = xmlGetAttr(root, "Date")
  timeOfMeasurement = as.POSIXct(timeOfMeasurement, "%Y-%m-%dT%H:%M:%S", tz="UTC")
  data_l = root[["Section"]]["Data", all=T]
  l = lapply(data_l, function(dataNode) {
    well_l = dataNode["Well", all=T]
    cycle = as.numeric(xmlGetAttr(dataNode, "Cycle"))
    l3 = lapply(well_l, function(wellNode) {
      pos = xmlGetAttr(wellNode, "Pos")
      row = substr(pos, 1, 1)
      col = as.numeric(substr(pos, 2, 20))
      well = getWellName(row, col, plateName)

      type = xmlGetAttr(wellNode, "Type")
      if (type == "Single") {
        childCount = length(wellNode)
        singleNode = wellNode[[childCount]] # Get the last measurement
        value = as.numeric(xmlValue(singleNode))
        data.frame(time=timeOfMeasurement, row=row, col=col, well=well, cycle=cycle, value=value, ...)
      }
      else if (type == "Scan") {
        scan_l = wellNode["Scan", all=T]
        dfScan = do.call(rbind, lapply(scan_l, function(scanNode) {
          wavelength = xmlGetAttr(scanNode, "WL")
          value = as.numeric(xmlValue(scanNode))
          data.frame(time=timeOfMeasurement, row=row, col=col, well=well, wavelength=wavelength, value=value, ...)
        }))
        rownames(dfScan) = NULL
        dfScan
      }
    })
    do.call(rbind, l3)
  })
  dfMeasurement = do.call(rbind, l)
  rownames(dfMeasurement) = NULL
  dfMeasurement$well = as.character(dfMeasurement$well)
  dfMeasurement$row = as.integer(dfMeasurement$row)
  dfMeasurement$col = as.integer(dfMeasurement$col)
  dfMeasurement
}
*/
