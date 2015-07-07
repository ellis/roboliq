/*
- break the items into groups that should be handled simultaneously, possible methods include:
    - each item is its own group
    - groups are built until no more syringes would be available based on the item's tipModel (but syringe doesn't need to be assigned yet)
    - groups are built (with limited look-ahead) where alternatives are investigated when a group splits over two columns
    - have a fixed assignment between wells and syringes (i.e. row n = tip (n % 4)) for the sake of managing differences between tips
*/

var _ = require('lodash');
var assert = require('assert');

/**
 * Place each item into its own group.
 *
 * @param {array} items Array of pipetting items.
 * @return {array} An array of groups of items; each group is a sublist of items from the original array.
 */
function groupingMethod1(items) {
	return _.map(items, function(item) { return [item]; });
}

/**
 * Groups are built until no more syringes would be available based on the item's tipModel (but syringe doesn't need to be assigned yet).
 * Also break groups on program changes.
 * TODO: break group if a previous dispense well is used as a source well.
 *
 * NOTE: if 'tipModelToSyringes' is supplied, this algorithm will not work predictably if the sets of syringes partially overlap with each other (complete overlap is fine).
 *
 * @param {array} items Array of pipetting items.
 * @param {array} syringes Array of integers representing the available syringe indexes
 * @param {object} tipModelToSyringes An optional map from tipModel to syringes that can be used with the given tipModel.  If the map contains syringes that aren't listed in the 'syringes' array, they won't be used.
 * @return {array} An array of groups of items; each group is a sublist of items from the original array.
 */
function groupingMethod2(items, syringes, tipModelToSyringes) {
	var groups = [];
	while (!_.isEmpty(items)) {
		var program = items[0].program;
		var syringesAvailable = _.clone(syringes);
		var group = _.takeWhile(items, function (item) {
			// Make sure we still have syringes available
			if (syringesAvailable.length == 0) return false;
			// Make sure all items in the group use the same program
			if (item.program !== program) return false;

			assert(item.tipModel);

			// If tipModelToSyringes was provided
			if (!_.isEmpty(tipModelToSyringes)) {
				assert(tipModelToSyringes.hasOwnProperty(item.tipModel));
				var syringesPossible = tipModelToSyringes[item.tipModel];
				assert(!_.isEmpty(syringesPossible));
				// Try to find a possible syringe that's still available
				var l = _.intersection(syringesPossible, syringesAvailable);
				if (_.isEmpty(l)) return false;
				// Remove an arbitrary syringe from the list of available syringes
				syringesAvailable = _.without(syringesAvailable, l[0]);
			}
			else {
				// Remove an arbitrary syringe from the list of available syringes
				syringesAvailable.splice(0, 1);
			}
		});
		items = _.drop(items, group.length);
		groups.push(group);
	}

	return groups;
}

module.exports = {
	groupingMethod1: groupingMethod1,
	groupingMethod2: groupingMethod2,
}
