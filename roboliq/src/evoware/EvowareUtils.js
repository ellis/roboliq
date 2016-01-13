import _ from 'lodash';

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

const exports = {
	decode,
	encode,
	hex,
	parseEncodedIndexes
};

export default exports;