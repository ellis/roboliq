import _ from 'lodash';

function createFactsLine(equipment, variableName, value = "") {
	const l = [
		`"${equipment}"`,
		`"${variableName}"`,
		`"${value}"`,
		`"0"`,
		`""`
	];
	return `FACTS(${l.join(",")});`;
}

module.exports = {
	createFactsLine
};
