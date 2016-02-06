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
	//console.log({items, syringes, tipModelToSyringes})
	const groups = [];
	while (!_.isEmpty(items)) {
		const program = items[0].program;
		let syringesAvailable = _.clone(syringes);
		const group = _.takeWhile(items, function (item) {
			//console.log("A "+JSON.stringify(item));
			// Make sure we still have syringes available
			if (syringesAvailable.length == 0) return false;
			// Make sure all items in the group use the same program
			if (item.program !== program) return false;
			//console.log("B");

			assert(item.tipModel);

			// If tipModelToSyringes was provided
			if (!_.isEmpty(tipModelToSyringes)) {
				//console.log("C");
				assert(tipModelToSyringes.hasOwnProperty(item.tipModel));
				var syringesPossible = tipModelToSyringes[item.tipModel];
				//console.log({syringesPossible, syringesAvailable})
				assert(!_.isEmpty(syringesPossible));
				// Try to find a possible syringe that's still available
				var l = _.intersection(syringesPossible, syringesAvailable);
				if (_.isEmpty(l)) return false;
				//console.log("D");
				// Remove an arbitrary syringe from the list of available syringes
				syringesAvailable = _.without(syringesAvailable, l[0]);
			}
			else {
				//console.log("E");
				// Remove an arbitrary syringe from the list of available syringes
				syringesAvailable.splice(0, 1);
			}

			return true;
		});
		assert(group.length > 0);
		items = _.drop(items, group.length);
		groups.push(group);
	}

	return groups;
}

/**
 * Groups are built until no more syringes would be available based on the item's tipModel (but syringe doesn't need to be assigned yet).
 * It tries to group by `layer` by putting as many items from the same layer into the group before moving onto the next item.
 * Breaks are forced on:
 * * program changes
 * * when a previous dispense well is used as a source well
 *
 * NOTE: if 'tipModelToSyringes' is supplied, this algorithm will not work predictably if the sets of syringes partially overlap with each other (complete overlap is fine).
 *
 * @param {array} items Array of pipetting items.
 * @param {array} syringes Array of integers representing the available syringe indexes
 * @param {object} tipModelToSyringes An optional map from tipModel to syringes that can be used with the given tipModel.  If the map contains syringes that aren't listed in the 'syringes' array, they won't be used.
 * @return {array} An array of groups of items; each group is a sublist of items from the original array.
 */
function groupingMethod3(items, syringes, tipModelToSyringes) {
	//console.log({items, syringes, tipModelToSyringes})

	if (_.isEmpty(items))
		return [];

	assert(syringes.length > 0)

	// Make a mutable copy of items, which we'll be splicing and shifting
	items = _.clone(items);

	// While items list isn't empty:
	//
	// If group is empty:
	//  Create a group with first item
	//  Set dispenseWells = {}
	//  Set syringesAvailable = all
	//  Set program to item.program
	//  Set layer to item.layer
	//  If layer:
	//   nextItems = [next item with same layer, next item in items list]
	//  Else:
	//   nextItems = [next item in items list]
	// Else: (group isn't empty)
	//  nextItems = [next item in items list]
	//
	// For all items in nextItems:
	//  if the item can be added to the group:
	//   add item to group
	//   remote item from items list
	//   break
	//
	// If no item was added, create a new empty group
	CONTINUE
	const groups = [];
	let group = [];
	let item = items.unshift();
	while (!_.isEmpty(items)) {
		const program = items[0].program;
		let syringesAvailable = _.clone(syringes);
		const group = [items.shift()];
		let cont = (items.length > 0);
		while (cont) {
			const item = items[0];
			//console.log("A "+JSON.stringify(item));
			// Make sure we still have syringes available
			if (syringesAvailable.length == 0) return false;
			// Make sure all items in the group use the same program
			if (item.program !== program) return false;

			if (!_.isUndefined(item.layer)) {
				const j = _.findIndex(items, item => _.isEqual(layer, item.layer));
				if (j > 0)
			}

		}
		const group = _.takeWhile(items, function (item) {
			//console.log("B");

			assert(item.tipModel);

			// If tipModelToSyringes was provided
			if (!_.isEmpty(tipModelToSyringes)) {
				//console.log("C");
				assert(tipModelToSyringes.hasOwnProperty(item.tipModel));
				var syringesPossible = tipModelToSyringes[item.tipModel];
				//console.log({syringesPossible, syringesAvailable})
				assert(!_.isEmpty(syringesPossible));
				// Try to find a possible syringe that's still available
				var l = _.intersection(syringesPossible, syringesAvailable);
				if (_.isEmpty(l)) return false;
				//console.log("D");
				// Remove an arbitrary syringe from the list of available syringes
				syringesAvailable = _.without(syringesAvailable, l[0]);
			}
			else {
				//console.log("E");
				// Remove an arbitrary syringe from the list of available syringes
				syringesAvailable.splice(0, 1);
			}

			return true;
		});
		assert(group.length > 0);
		items = _.drop(items, group.length);
		groups.push(group);
	}

	return groups;
}

module.exports = {
	groupingMethod1,
	groupingMethod2,
	groupingMethod3,
}
