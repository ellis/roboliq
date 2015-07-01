var _ = require('lodash');

/**
 * queryResults: value returned from llpl.query()
 * predicateName: name of the predicate that was used for the query
 * return: {parameterName1: parameterValues1, ...}
 */
function extractValuesFromQueryResults(queryResults, predicateName) {
  var acc = _.reduce(queryResults, function(acc, x1) {
    var x2 = x1[predicateName];
    _.forEach(x2, function(value, name) {
      if (_.isEmpty(acc[name]))
        acc[name] = [value];
      else
        acc[name].push(value);
    });
    return acc;
  }, {});
  return acc;
}

function getObjectsValue(objects, key) {
    var l = key.split('.');
    while (l.length > 1) {
      objects = objects[l[0]];
      l = _.rest(l);
    }
    return objects[l[0]];
}

module.exports = {
  extractValuesFromQueryResults: extractValuesFromQueryResults,
  getObjectsValue: getObjectsValue
}
