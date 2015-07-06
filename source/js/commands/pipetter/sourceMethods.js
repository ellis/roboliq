- assign source well by group for items without assigned source wells; if multiple syringes need to access the same source, and that source has multiple wells, then possible methods include:
    - pick first one
    - rotate through source wells in order
    - rotate through source wells in order of max volume
    - try a simple geometrical assignment considering whether there are more tips or wells; if that fails, use previous method
    - same as above, but if wells > tips, try starting at first (wells - tips) wells and see which one produces the greatest minimum final volume

// Pick the first well in a source set and ignore the others
function sourceMethod1(items) {
	_.forEach(items, function (item) {
		var source = item.source;
		var sourceInfo = sourceParser.parse(item.source);
		if (sourceInfo.source) {
			var wells = getObjectsValue(data.objects, source+".wells");
			assert(!_.isEmpty(wells));
			item.sourceWell = wells[0];
		}
		else {
			item.sourceWell = source;
		}
	});
}

// Rotate through source wells in order
function sourceMethod2(items) {
	var sourceToWellIndex = {};
	_.forEach(items, function (item) {
		var source = item.source;
		var sourceInfo = sourceParser.parse(item.source);
		if (sourceInfo.source) {
			var wells = getObjectsValue(data.objects, source+".wells");
			assert(!_.isEmpty(wells));
			var i = (sourceToWellIndex.hasOwnProperty(source)) ? sourceToWellIndex[source] : 0;
			item.sourceWell = wells[i];
			sourceToWellIndex[source] = (i + 1) % wells.length;
		}
		else {
			item.sourceWell = source;
		}
	});
}
