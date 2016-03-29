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
	let current = undefined;

	function tryAdd(item, debug = false) {
		//console.log("A "+JSON.stringify(item));
		// Make sure we still have syringes available
		if (current.syringesAvailable.length == 0) { if (debug) console.log("syringesAvailable.length == 0"); return false; }
		// Make sure all items in the group use the same program
		if (current.program !== item.program) { if (debug) console.log({currentProgram: current.program, itemProgram: item.program}); return false; }
		// Make sure source was not previously a destination in this group
		// const source = item.source || item.well;
		// const destination = item.destination || item.well;
		if (_.some(current.group, item2 => (item.source || item.well) === (item2.destination || item2.well))) { if (debug) console.log({currentGroup: current.group, item}); return false; }
		// Make sure syringe was not already used (only relevant want syringe is manually specified)
		if (item.syringe) {
			if (current.syringesUsed.hasOwnProperty(item.syringe)) { if (debug) console.log({syringesUsed: current.syringesUsed, itemSyringe: item.syringe}); return false; }
			current.syringesUsed[item.syringe] = true;
		}
		//console.log("B");

		assert(item.tipModel);

		// If tipModelToSyringes was provided
		if (!_.isEmpty(tipModelToSyringes)) {
			//console.log("C");
			assert(tipModelToSyringes.hasOwnProperty(item.tipModel), `tipModelToSyringes must contain an entry for "${item.tipModel}"`);
			const syringesPossible = tipModelToSyringes[item.tipModel];
			//console.log({syringesPossible, syringesAvailable})
			assert(!_.isEmpty(syringesPossible));
			// Try to find a possible syringe that's still available
			const l = _.intersection(syringesPossible, current.syringesAvailable);
			if (_.isEmpty(l)) { if (debug) console.log({syringesPossible, syringesAvailable: current.syringesAvailable}); return false; }
			//console.log("D");
			// Remove an arbitrary syringe from the list of available syringes
			current.syringesAvailable = _.without(current.syringesAvailable, l[0]);
		}
		else {
			//console.log("E");
			// Remove an arbitrary syringe from the list of available syringes
			current.syringesAvailable.splice(0, 1);
		}

		current.group.push(item);
		current.layer = item.layer;

		return true;
	}

	const groups = [];
	while (!_.isEmpty(items)) {
		// If we need to start a new group:
		if (_.isUndefined(current)) {
			const item = items.shift();
			current = {
				group: [],
				syringesAvailable: _.clone(syringes),
				syringesUsed: {},
				program: item.program,
				layer: item.layer
			};
			const added = tryAdd(item);
			assert(added, `couldn't add item to empty group!: ${JSON.stringify(item)}`);
			groups.push(current.group);
		}
		// Else, we will try to add an item to the current group
		else {
			const nextIndexes = [0];
			// First check next item that's in the same layer as the last item in group
			if (!_.isUndefined(current.layer)) {
				const j = _.findIndex(items, item => {
					//console.log({a: current.layer, b: item.layer, c: _.isEqual(current.layer, item.layer), d: current.layer === item.layer});
					return _.isEqual(current.layer, item.layer);
				});
				//console.log({layer: current.layer, j})
				if (j > 0) {
					nextIndexes.unshift(j);
				}
			}

			// Try to add one of the items in nextIndexes
			let added = false;
			for (let k = 0; k < nextIndexes.length; k++) {
				const j = nextIndexes[k];
				const item = items[j];
				if (tryAdd(item)) {
					// Remove the j-th element
					items.splice(j, 1);
					added = true;
					break;
				}
			}

			// If no item could be added, signal that a new group should be started by undefining `current`
			if (!added) {
				current = undefined;
			}
		}
	}

	return groups;
}

module.exports = {
	groupingMethod1,
	groupingMethod2,
	groupingMethod3,
}
