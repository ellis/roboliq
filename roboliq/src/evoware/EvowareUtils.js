import _ from 'lodash';
import fs from 'fs';
import iconv from 'iconv-lite';

/**
 * Encode an integer as an ASCII character.
 * Evoware uses this to generate a string representing a list of wells or sites.
 * @param  {number} n - integer to encode as a character
 * @return {string} a single-character string that represents the number
 */
function encode(n) {
	return String.fromCharCode("0".charCodeAt(0) + n);
}

/**
 * Decode a character to an integer.
 */
function decode(c) {
	return c.charCodeAt(0) - "0".charCodeAt(0);
}

/**
 * Convert the number to hex.  Number should be between 0 and 15.
 * @param  {integer} n - number between 0 and 15
 * @return {char}
 */
function hex(n) {
	return n.toString(16).toUpperCase()[0];
}

/**
 * Takes an encoding of indexes on a 2D surface (as found in the file Carrier.cfg)
 * and
 * @param  {string} encoded - an encoded list of indexes
 * @return {array} tuple of [rows on surface, columns on surface, selected indexes on surface]
 */
function parseEncodedIndexes(encoded) {
	const col_n = decode(encoded.charAt(1));
	const row_n = decode(encoded.charAt(3));
	const s = encoded.substring(4);
	const indexes = _.map(s, (c, c_i) => {
		const n = decode(c);
		//const bit_l = (0 to 7).flatMap(bit_i => if ((n & (1 << bit_i)) > 0) Some(bit_i) else None)
		const bit_l = _.remove(_.range(0, 7).map(bit_i => ((n & (1 << bit_i)) > 0) ? bit_i : undefined), _.isUndefined);
		return bit_l.map(bit_i => c_i * 7 + bit_i);
	})
	return [col_n, row_n, indexes];
}


/**
 * Split an evoware carrier line into its components.
 * The first component is an integer that identifies the type of line.
 * The remaining components are returned as a list of string.
 *
 * @param  {string} line - a text line from Evoware's carrier file
 * @return {array} Returns a pair [kind, items], where kind is an integer
 *   identifying the type of line, and items is a string array of the remaining
 *   components of the line.
 */
function splitSemicolons(line) {
	const l = line.split(";");
	const kind = parseInt(l[0]);
	return [kind, _.tail(l)];
}

class EvowareSemicolonFile {
	constructor(filename, skip) {
		const raw = fs.readFileSync(filename);
		const filedata = iconv.decode(raw, "ISO-8859-1");
		this.lines = filedata.split("\n");
		//console.log("lines:\n"+lines)
		//console.log(lines.length);
		this.lineIndex = skip;
	}

	next() {
		if (this.lineIndex >= this.lines.length)
			return undefined;
		const line = this.lines[this.lineIndex];
		this.lineIndex++;
		return line;
	}

	nextSplit() {
		const line = this.next();
		if (_.isUndefined(line))
			return undefined;
		const result = splitSemicolons(line);
		return result;
	}

	hasNext() {
		return (this.lineIndex < this.lines.length);
	}

	peekAhead(skip) {
		const i = this.lineIndex + skip;
		if (i >= this.lines.length)
			return undefined;
		const line = this.lines[i];
		return line;
	}

	skip(n) {
		this.lineIndex += n;
	}

	take(n) {
		const l = this.lines.slice(this.lineIndex, this.lineIndex + n);
		this.lineIndex += n;
		return l;
	}
}

const exports = {
	EvowareSemicolonFile,
	decode,
	encode,
	hex,
	parseEncodedIndexes,
	splitSemicolons
};

export default exports;
