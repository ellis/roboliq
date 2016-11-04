import mergeWith from 'lodash/mergeWith';

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
