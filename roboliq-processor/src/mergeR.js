import mergeWith from 'lodash/mergeWith';

/*
* Recursively merge properties of two objects
*/
function mergeR(obj1, obj2) {
  for (var p in obj2) {
    try {
      // Property in destination object set; update its value.
      if ( obj2[p].constructor==Object ) {
        obj1[p] = MergeRecursive(obj1[p], obj2[p]);

      } else {
        obj1[p] = obj2[p];

      }

    } catch(e) {
      // Property in destination object not set; create it and set its value.
      obj1[p] = obj2[p];

    }
  }

  return obj1;
}

var merge = function() {
    var obj = {},
        i = 0,
        il = arguments.length,
        key;
    for (; i < il; i++) {
        for (key in arguments[i]) {
            if (arguments[i].hasOwnProperty(key)) {
                obj[key] = arguments[i][key];
            }
        }
    }
    return obj;
};

module.exports = {
	mergeR: (objValue, srcValue, key, object, source, stack) => {
		CONTINUE: should probably just make my own function to skip undefined, remove nulls, and replace arrays
		if (isUndefined(srcValue)) {
			return objValue;
		}
		else if (isNull(srcValue)) {
		}
	}
}
