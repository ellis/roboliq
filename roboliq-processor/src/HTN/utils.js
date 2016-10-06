/*
 * utils
 *
 * Warren Sack <wsack@ucsc.edu>
 * November 2010
 *
 * Ellis Whitehead <ellis.whitehead@gmail.com>
 * Lightly adapted for nodejs 2015
 *
 */

////
//// Objects
////


//
// keys(obj): array
//
function keys(obj) {
  var result = [];
  var key;
  for (key in obj) {
    result.push(key);
  }
  return (result);
}

//
// values(obj): array
//
function values(obj) {
  var result = [];
  var key;
  for (key in obj) {
    result.push(obj[key]);
  }
  return (result);
}

//
// typeOf(value): string
//
// Douglas Crockford's definition:
// http://javascript.crockford.com/remedial.html
//
function typeOf(value) {
  var s = typeof value;
  if (s === 'object') {
    if (value) {
      if (value instanceof Array) {
        s = 'array';
      }
    } else {
      s = 'null';
    }
  }
  return s;
}

//
// copyIt(e): typeOf(e)
//
function copyIt(e) {
  var typeOfE = typeOf(e);
  if ((e === null) || (e === undefined) || (typeOfE === 'boolean') || (typeOfE === 'number')) {
    return (e);
  }
  if (typeOfE === 'string') {
    return (e.slice());
  }
  if (typeOfE === 'array') {
    return (copyArray(e));
  }
  if (typeOfE === 'object') {
    return (copyObject(e));
  } else {
    throw {
      name: 'syntax_error',
      message: "Unknown element: " + e
    }
  }
}

//
// copyObject(obj): object
//
function copyObject(obj) {
  var newObj = {};
  var key;
  for (key in obj) {
    newObj[key] = copyIt(obj[key]);
  }
  return (newObj);
}

//
// copyArray(a): array
//
function copyArray(a) {
  var newArray = [];
  var i;
  for (i = 0; i < a.length; i++) {
    newArray.push(copyIt(a[i]));
  }
  return (newArray);
}

//
// ppToBrowser(string): print print to current browser window
//
function ppToBrowser(str) {
  var formatted = pp(str);
  formatted = formatted.replace(/ /g, "&nbsp;");
  formatted = formatted.replace(/\n/g, "<br>");
  document.write(formatted);
}

//
// ppToConsole(string): print print to current browser console
//
function ppToConsole(str) {
  console.log(pp(str));
}

//
// pp(thing): string
//
function pp(thing) {
  return (ppThing(thing, ""));
}

//
// ppThing(thing,indent): string
//
function ppThing(thing, indent) {
  var result = "";
  if (typeOf(thing) === 'object') {
    result = result.concat(indent + "{" + "\n");
    result = result.concat(ppObject(thing, indent + "   "));
    result = result.concat(indent + "}" + "\n");
  }
  if (typeOf(thing) === 'array') {
    result = result.concat(indent + "[" + "\n");
    result = result.concat(ppArray(thing, indent + "   "));
    result = result.concat(indent + "]" + "\n");
  }
  if ((typeOf(thing) !== 'object') && (typeOf(thing) !== 'array')) {
    result = result.concat(indent + "\"" + thing + "\"" + "\n");
  }
  return (result);
}

//
// ppObject: string
//
function ppObject(obj, indent) {
  var result = "";
  var ks = keys(obj);
  var str, separator;
  var close = "";
  for (var i = 0; i < ks.length; i++) {
    str = indent + "\"" + ks[i] + "\": ";
    if (i < (ks.length - 1)) {
      separator = ",";
    } else {
      separator = "";
    }
    if ((typeOf(obj[ks[i]]) !== 'object') && (typeOf(obj[ks[i]]) !== 'array')) {
      result = result.concat(str + " \"" + obj[ks[i]] + "\"" + separator + "\n");
    } else {
      if (typeOf(obj[ks[i]]) === 'object') {
        str += "{";
        close = "}";
        result = result.concat(str + "\n");
        result = result.concat(ppObject(obj[ks[i]], indent + "   "));
      }
      if (typeOf(obj[ks[i]]) === 'array') {
        str += "[";
        close = "]";
        result = result.concat(str + "\n");
        result = result.concat(ppArray(obj[ks[i]], indent + "   "));
      }
      result = result.concat(indent + close + separator + "\n");
    }
  }
  return (result);
}

//
// ppArray: string
//
function ppArray(a, indent) {
  var result = "";
  var str, separator, i;
  var onOneLine = true;
  for (i = 0; i < a.length; i++) {
    if ((typeOf(a[i]) === 'object') || (typeOf(a[i]) === 'array')) {
      onOneLine = false;
    }
  }
  if (onOneLine) {
    str = indent;
    for (i = 0; i < a.length; i++) {
      if (i < (a.length - 1)) {
        separator = ", ";
      } else {
        separator = "";
      }
      str += "\"" + a[i] + "\"" + separator;
    }
    result = result.concat(str + "\n");
  } else {
    for (i = 0; i < a.length; i++) {
      if (i < (a.length - 1)) {
        separator = ",";
      } else {
        separator = "";
      }
      if ((typeOf(a[i]) !== 'object') && (typeOf(a[i]) !== 'array')) {
        result = result.concat(indent + "\"" + a[i] + "\"" + separator + "\n");
      }
      if (typeOf(a[i]) === 'object') {
        str = indent + "{";
        close = "}";
        result = result.concat(str + "\n");
        result = result.concat(ppObject(a[i], indent + "   "));
      }
      if (typeOf(a[i]) === 'array') {
        str = indent + "[";
        close = "]";
        result = result.concat(str + "\n");
        result = result.concat(ppArray(a[i], indent + "   "));
      }
      result = result.concat(indent + close + separator + "\n");
    }
  }
  return (result);
}


////
//// First-order logic syntax (without quantifiers)
////


//
// isVariable(str): boolean
//
// Pattern variable names are strings that begin with a question mark ('?')
//
function isVariable(str) {
  if ((typeOf(str) === 'string') && (str.search(/\?/) === 0)) {
    return (true);
  }
  return (false);
}

//
// isAnonymousVariable(str): boolean
//
// Anonymous variables look like this: ?_
//
function isAnonymousVariable(str) {
  if ((typeOf(str) === 'string') && (str.search(/\?/) === 0) && (str.search(/\_/) === 1) && (str.length === 2)) {
    return (true);
  }
  return (false);
}

//
// currentVariableID: counter, integer, used to distinction
// one instance of a variable from subsequent instances.
//
var currentVariableID = 0;

//
// makeNewVariableID(): string and side effects currentVariableID
//
function makeNewVariableID() {
  currentVariableID += 1;
  return (currentVariableID);
}

//
// makeNewVariable(variable,id): string
//
function makeNewVariable(variable, id) {
  return (variable + '_' + id);
}

//
// renameVars(x,id): variable or array or object
//
function renameVars(x, id) {
  if ((isEmpty(x)) || (x === '') || (x === null) || (isAnonymousVariable(x))) {
    return (x);
  }
  if (typeOf(x) === 'object') {
    for (var key in x) {
      x[key] = renameVars(x[key], id);
    }
    return (x);
  }
  if (typeOf(x) === 'array') {
    var xRenamed = [];
    for (var i = 0; i < x.length; i++) {
      xRenamed.push(renameVars(x[i], id));
    }
    return (xRenamed);
  }
  if (isVariable(x)) {
    x = makeNewVariable(x, id);
    return (x);
  }
  return (x);
}

//
// renameVariables(x): variable or array or object
//
function renameVariables(x) {
  return (renameVars(x, makeNewVariableID()));
}

//
// isAssertion(obj): boolean
//
// Assertions are objects with one key (the name of the predicate) and
// one value (a object containing argument bindings)
//
function isAssertion(obj) {
  if (typeOf(obj) !== 'object') {
    return (false);
  }
  var ks = keys(obj);
  var vs = values(obj);
  if ((ks.length === 1) && (typeOf(vs[0]) === 'object')) {
    return (true);
  }
  return (false);
}

//
// isAssertionOfType(obj,type): boolean
//
function isAssertionOfType(obj, type) {
  if (typeOf(obj) !== 'object') {
    return (false);
  }
  var ks = keys(obj);
  var vs = values(obj);
  if ((ks.length === 1) && (ks[0] === type) && (typeOf(vs[0]) === 'object')) {
    return (true);
  }
  return (false);
}

//
// predicateName(obj): string
//
function predicateName(obj) {
  var ks = keys(obj);
  return (ks[0]);
}

//
// argsList(obj): array
//
function argsList(obj) {
  var list = values(obj);
  return (list[0]);
}

//
// predicateArguments(obj): object
//
function predicateArguments(obj) {
  return (argsList(obj));
}

//
// isClauseTypeWithArgsList(obj,clauseType): boolean
//
function isClauseTypeWithArgsList(obj, clauseType) {
  if (utils.typeOf(obj) !== 'object') {
    return (false);
  }
  var ks = utils.keys(obj);
  var vs = utils.values(obj);
  if ((ks.length === 1) && (ks[0] === clauseType) && (utils.typeOf(vs[0]) === 'array')) {
    return (true);
  }
  return (false);
}

//
// isDisjunction(obj): boolean
//
function isDisjunction(obj) {
  return (isClauseTypeWithArgsList(obj, 'or'));
}

//
// disjuncts(obj): array
//
function disjuncts(obj) {
  return (argsList(obj));
}

//
// isConjunction(obj): boolean
//
function isConjunction(obj) {
  return (isClauseTypeWithArgsList(obj, 'and'));
}

//
// conjuncts(obj): array
//
function conjuncts(obj) {
  return (argsList(obj));
}

//
// isNegation(obj): boolean
//
function isNegation(obj) {
  return (isAssertionOfType(obj, 'not'));
}

//
// negation(obj): object
//
function negation(obj) {
  return (argsList(obj));
}

//
// listToAssertion(array): object
//
// Destructively converts an array, a, into an assertion of this structure:
// {"cons": {"first": a[0], "rest": {"cons": {"first": a[1], "rest": ...
//
function listToAssertion(a) {
  if (a.length == 0) {
    return ("nil");
  } else {
    return ({
      "cons": {
        "first": a.shift(),
        "rest": listToAssertion(a)
      }
    });
  }
}

//
// assertionToList(object): array
//
// Creates an array ([a[0],a[1],...]) given an assertion of this structure:
// {"cons": {"first": a[0], "rest": {"cons": {"first": a[1], "rest": ...
//
function assertionToList(obj) {
  var l = [];
  if (obj == "nil") {
    return (l);
  } else {
    l.push(obj.cons.first);
    return (l.concat(assertionToList(obj.cons.rest)));
  }
}



////
//// Unification of objects in first-order logical form
////


//
// occursIn(variable,pattern): boolean
//
function occursIn(variable, pattern) {
  if (isVariable(pattern) && (variable === pattern)) {
    return (true);
  }
  if (typeOf(pattern) === 'object') {
    if (pattern[variable] !== undefined) {
      return (true);
    }
    for (var key in pattern) {
      if (occursIn(variable, pattern[key])) {
        return (true);
      }
    }
  }
  if (typeOf(pattern) === 'array') {
    for (var i = 0; i < pattern.length; i++) {
      if (occursIn(variable, pattern[i])) {
        return (true);
      }
    }
  }
  return (false);
}

//
// unify(pattern1,pattern2): boolean
//
function unify(pattern1, pattern2) {
  var bindings = {};
  if (unifyPatterns(pattern1, pattern2, bindings)) {
    removeNonVariableBindings(bindings);
    return (true);
  }
  return (false);
}

//
// removeNonVariableBindings(bindings): boolean
//
function removeNonVariableBindings(bindings) {
  var result = false;
  for (var key in bindings) {
    if (!(isVariable(key))) {
      result = true;
      delete bindings[key];
    }
  }
  return (result);
}

//
// unifyVariable(x,y,bindings): boolean
//
function unifyVariable(variable, pattern, bindings) {
  var variableBinding;
  if (isAnonymousVariable(variable) || isAnonymousVariable(pattern)) {
    return (true);
  }
  if (isVariable(pattern) && (variable === pattern)) {
    return (true);
  }
  if (bindings[variable] !== undefined) {
    variableBinding = bindings[variable];
  } else {
    variableBinding = false;
  }
  if (variableBinding) {
    return (unifyPatterns(variableBinding, pattern, bindings));
  }
  if (occursIn(variable, pattern)) {
    return (false);
  } else {
    bindings[variable] = pattern;
    return (true);
  }
}

//
// unifyPairs(obj1,obj2,bindings): boolean
//
// Accept two objects containing variable-value bindings and try to unify them.
//
function unifyPairs(obj1, obj2, bindings) {
  //console.log("unifyPairs: ", obj1, obj2)
  var sharedKeys = [];
  var key, dpair, bpair;
  for (key in obj1) {
    if (obj2[key] !== undefined) {
      if (!(unifyPatterns(obj1[key], obj2[key], bindings))) {
        return (false);
      } else {
        sharedKeys.push(key);
      }
    }
  }
  for (var i = 0; i < sharedKeys.length; i++) {
    key = sharedKeys[i];
    if (bindings[key] !== undefined) {
      dpair = {};
      dpair[key] = obj1[key];
      bpair = {};
      bpair[key] = bindings[key];
      delete bindings[key];
      if (!(unifyPairs(dpair, bpair, bindings))) {
        return (false);
      } else {
        bindings[key] = bpair[key];
      }
    }
  }
  for (key in obj2) {
    if (obj1[key] === undefined) {
      if (bindings[key] !== undefined) {
        dpair = {};
        dpair[key] = obj2[key];
        bpair = {};
        bpair[key] = bindings[key];
        delete bindings[key];
        if (!(unifyPairs(dpair, bpair, bindings))) {
          return (false);
        } else {
          bindings[key] = bpair[key];
        }
      } else {
        bindings[key] = obj2[key];
      }
    }
  }
  return (true);
}

//
// unifyPatterns(pattern1,pattern2,bindings): boolean
//
function unifyPatterns(pattern1, pattern2, bindings) {
  //console.log("a")
  if (isVariable(pattern1)) {
    return (unifyVariable(pattern1, pattern2, bindings));
  }
  //console.log("b")
  if (isVariable(pattern2)) {
    return (unifyVariable(pattern2, pattern1, bindings));
  }
  //console.log("c")
  if ((typeOf(pattern1) === 'string') && (typeOf(pattern2) === 'string')) {
    if (pattern1 == pattern2) {
      return (true);
    } else {
      //console.log("failed at ", pattern1, pattern2)
      return (false);
    }
  }
  //console.log("d")
  if ((typeOf(pattern1) === 'array') && (typeOf(pattern2) === 'array')) {
    if (pattern1.length === pattern2.length) {
      var result = true;
      for (var i = 0; i < pattern1.length; i++) {
        if (result) {
          result = unifyPatterns(pattern1[i], pattern2[i], bindings);
        }
      }
      return (result);
    } else {
        //console.log("failed at ", pattern1, pattern2)
      return (false);
    }
  }
  //console.log("e")
  if (isAssertion(pattern1) && isAssertion(pattern2)) {
    if (predicateName(pattern1) == predicateName(pattern2)) {
      return (unifyPairs(predicateArguments(pattern1), predicateArguments(pattern2), bindings));
    } else {
        //console.log("failed at ", pattern1, pattern2)
      return (false);
    }
  }
  //console.log("f")
  if ((typeOf(pattern1) === 'object') && (typeOf(pattern2) === 'object')) {
    return (unifyPairs(pattern1, pattern2, bindings));
  } else {
    //console.log("failed at ", pattern1, pattern2)
    return (false);
  }
}

//
// containsVariable(pattern): boolean
//
function containsVariable(pattern) {
  if (isVariable(pattern)) {
    return (true);
  }
  if (typeOf(pattern) === 'object') {
    for (var key in pattern) {
      if (containsVariable(pattern[key])) {
        return (true);
      }
    }
  }
  if (typeOf(pattern) === 'array') {
    for (var i = 0; i < pattern.length; i++) {
      if (containsVariable(pattern[i])) {
        return (true);
      }
    }
  }
  return (false);
}

//
// equal(thing1,thing2): boolean
//
// Thing1 is equal to thing2, if they unify with one another, but
// the unification does not produce any variable bindings.  Thus,
// things (arrays, objects, etc.) that contain variables cannot be
// equal.  But, for example, objects that are not necessarily ===,
// but that have the same structure and the same constants (strings,
// numbers, etc.) in the same positions, are equal.
//
function equal(thing1, thing2) {
  if (containsVariable(thing1) || containsVariable(thing2)) {
    return (false);
  }
  if (unifyPatterns(thing1, thing2, {})) {
    return (true);
  } else {
    return (false);
  }
}

//
// instantiate(pattern): object or variable or string or array
//
// Note that the input object, pattern, is destructively modified by replacing
// any variables with bindings found in the bindings parameter.  The object
// output is the destructively modified input object/pattern.
//
function instantiate(pattern, bindings) {
  if (isVariable(pattern)) {
    if (bindings[pattern] !== undefined) {
      return (instantiate(bindings[pattern], bindings));
    } else {
      return (pattern);
    }
  }
  if (typeOf(pattern) === 'array') {
    var newPattern = [];
    for (var i = 0; i < pattern.length; i++) {
      newPattern.push(instantiate(pattern[i], bindings));
    }
    return (newPattern);
  }
  if (typeOf(pattern) === 'object') {
    for (var key in pattern) {
      pattern[key] = instantiate(pattern[key], bindings);
    }
    return (pattern);
  } else {
    return (pattern);
  }
}

//
// necessarilyUnify(pattern,pattern,bindings): boolean
//
// Given two patterns and a set of bindings, determine if they both
// instantiate to equivalent forms that contain no variables; or, if
// they unify but contain identical unbound variables in identical
// positions (i.e., if the can be unified without creating any additions
// to the bindings).
//
function necessarilyUnify(pat1, pat2, bindings) {
  var freshBindings = copyIt(bindings);
  var i1 = instantiate(copyIt(pat1), freshBindings);
  var i2 = instantiate(copyIt(pat2), freshBindings);
  var uBindings = {};
  var result = unifyPatterns(i1, i2, uBindings);
  if (result && isEmpty(uBindings)) {
    return (true);
  } else {
    return (false);
  }
}


////
//// Sets
////


//
// arrayToSet(array): object
//
function arrayToSet(a) {
  var s = {};
  for (var i = 0; i < a.length; i++) {
    if (s[a[i]] === undefined) {
      s[a[i]] = true;
    }
  }
  return (s);
}

//
// setToArray(object): array
//
function setToArray(s) {
  return (keys(s));
}


//
// setUnion(object,object): object
//
function setUnion(s1, s2) {
  var a = keys(s1);
  a = a.concat(keys(s2));
  return (arrayToSet(a));
}

//
// setIntersection(object,object): object
//
function setIntersection(s1, s2) {
  var intersection = [];
  for (var key in s1) {
    if (s2[key] !== undefined) {
      intersection.push(key);
    }
  }
  return (arrayToSet(intersection));
}

//
// setDifference(object,object): object
//
function setDifference(s1, s2) {
  var difference = [];
  for (var key in s1) {
    if (s2[key] === undefined) {
      difference.push(key);
    }
  }
  return (arrayToSet(difference));
}

//
// elementOf(element,object): boolean
//
function elementOf(e, s) {
  if (s[e] !== undefined) {
    return (true);
  } else {
    return (false);
  }
}

//
// isEmpty(x): boolean
//
function isEmpty(x) {
  if (typeOf(x) === 'array') {
    if (x.length <= 0) {
      return (true);
    } else {
      return (false);
    }
  }
  if (typeOf(x) === 'object') {
    var ks = keys(x);
    if (ks.length <= 0) {
      return (true);
    } else {
      return (false);
    }
  }
  return (false);
}

////
//// Arrays
////

//
// removeElement(e,a): array
//
function removeElement(e, a) {
  var result = [];
  for (var i = 0; i < a.length; i++) {
    if (a[i] != e) {
      result.push(a[i]);
    }
  }
  return (result);
}

// Sort (Quicksort for arrays)
// The built-in sort function in JavaScript in Chrome seems to cache the
// predicate it is used with if it is called with a predicate in its
// parameter.  This seems to be a bug in the implementation of sort.
// Thus, here is an implementation of quickSort to use until that
// bug is fixed.

Array.prototype.swap = function(a, b) {
  var tmp = this[a];
  this[a] = this[b];
  this[b] = tmp;
}

function partition(array, predicate, begin, end, pivot) {
  var piv = array[pivot];
  array.swap(pivot, end - 1);
  var store = begin;
  var comparison;
  for (var ix = begin; ix < end - 1; ix++) {
    comparison = predicate(array[ix], piv);
    if ((comparison == -1) || (comparison == 0)) {
      array.swap(store, ix);
      store++;
    }
  }
  array.swap(end - 1, store);
  return (store);
}

function qsort(array, predicate, begin, end) {
  if ((end - 1) > begin) {
    var pivot = begin + Math.floor(Math.random() * (end - begin));
    pivot = partition(array, predicate, begin, end, pivot);
    qsort(array, predicate, begin, pivot);
    qsort(array, predicate, pivot + 1, end);
  }
}

function quickSort(array, predicate) {
  qsort(array, predicate, 0, array.length);
  return (array);
}


////
//// Search
////

function makeSearch() {

  //
  // searchStates: object
  //
  // This is an object containing all search states indexed by
  // their respectives IDs
  //
  var searchStates = {};

  //
  // currentSearchStateID: counter, integer, used to distinction
  // one instance of a search state from subsequent instances.
  //
  var currentSearchStateID = 0;

  //
  // makeNewSearchStateID(): string and side effects currentSearchStateID
  //
  function makeNewSearchStateID() {
    currentSearchStateID += 1;
    return (currentSearchStateID);
  }

  //
  // makeNewSearchState(): searchState
  //
  function makeNewSearchState(object) {
    var sstate = copyIt(object);
    delete sstate["successors"];
    sstate["id"] = makeNewSearchStateID();
    sstate["success"] = false;
    searchStates[sstate["id"]] = sstate;
    return (sstate);
  }

  //
  // expandSearchState(sstate,expander): array of sstates
  //
  // Given a search state and a function, expander, apply the
  // function to generate a set of successor search states if
  // successors do not yet exist; otherwise, return the existing
  // successor states.
  //
  function expandSearchState(sstate, expander) {
    var newSStates, successors;
    if (sstate["depth"] == undefined) {
      sstate["depth"] = 0;
    }
    if (sstate["successors"] == undefined) {
      sstate["successors"] = [];
      successors = [];
      newSStates = expander(sstate);
      for (var i = 0; i < newSStates.length; i++) {
        newSStates[i] = makeNewSearchState(newSStates[i]);
        newSStates[i]["depth"] = sstate["depth"] + 1;
        sstate["successors"].push(newSStates[i]["id"]);
        successors.push(newSStates[i]);
        newSStates[i]["predecessor"] = sstate["id"];
      }
    }
    return (successors);
  }

  //
  // scoreSearchState(sstate,scorer): number
  //
  // Given a search state and a function, scorer, apply the
  // function to assign the state a numerical score.
  //
  function scoreSearchState(sstate, scorer) {
    sstate["score"] = scorer(sstate);
  }

  //
  // depthFirstScorer(sstate): number
  //
  function depthFirstScorer(sstate) {
    return (0 - sstate["depth"]);
  }

  //
  // breadthFirstScorer(sstate): number
  //
  function breadthFirstScorer(sstate) {
    return (sstate["depth"]);
  }

  //
  // bySearchStateScore(a,b): {-1,0,1}
  //
  function bySearchStateScore(a, b) {
    var result = 0;
    if (a["score"] < b["score"]) {
      result = -1;
    }
    if (a["score"] > b["score"]) {
      result = 1;
    }
    return (result);
  }

  //
  // searchSuccess(sstate,isSuccess): boolean
  //
  //
  // Given a search state and a function, isSuccess, apply the
  // function to determine if the state is a final, successful
  // state for the search.
  //
  function searchSuccess(sstate, isSuccess) {
    if (isSuccess(sstate)) {
      sstate["success"] = true;
    }
    return (sstate["success"]);
  }

  //
  // allUnexpandedStates(): array of search states
  //
  function allUnexpandedStates() {
    var unexpanded = [];
    for (var id in searchStates) {
      if ((searchStates[id]["successors"] == undefined) && !searchStates[id]["success"]) {
        unexpanded.push(searchStates[id]);
      }
    }
    return (unexpanded);
  }

  //
  // search(scorer,expander,isSuccess): search state
  //
  function search(scorer, expander, isSuccess) {
    //console.log("\nsearch");
    var queue = allUnexpandedStates();
    if (isEmpty(queue)) {
      return ({});
    } else {
      //console.log("queue ("+queue.length+"):")//"\n"+JSON.stringify(queue, null, '  '))
      for (var i = 0; i < queue.length; i++) {
        //console.log(JSON.stringify(queue[i].currentTask));
        scoreSearchState(queue[i], scorer);
      }
      queue.sort(bySearchStateScore);
      /*
        var scores = "";
        for ( var j = 0; j < queue.length; j++ ) {
          scores = scores + "for state " + queue[j]["id"] + " score = " + queue[j]["score"];
        }
        //console.log("calling SEARCH where the number of unexpanded states = " + queue.length + "\n" + scores);
        //console.log("queue[0].agenda: "+JSON.stringify(queue[0].agenda))
        //var response = prompt("continue?","yes");
        //if ( response == "no" ) { return(queue[0]); }
      //*/
      if (searchSuccess(queue[0], isSuccess)) {
        return (queue[0]);
      } else {
        expandSearchState(queue[0], expander);
        return (search(scorer, expander, isSuccess));
      }
    }
  }

  return ({
    search: search,
    searchStates: searchStates,
    ss: searchStates,
    makeNewSearchState: makeNewSearchState,
    expandSearchState: expandSearchState,
    scoreSearchState: scoreSearchState,
    searchSuccess: searchSuccess,
    allUnexpandedStates: allUnexpandedStates,
    depthFirstScorer: depthFirstScorer,
    breadthFirstScorer: breadthFirstScorer
  });
} // end of makeSearch

//
// Make public a selection of the utility methods.
//
var utils = {
  typeOf: typeOf,
  keys: keys,
  values: values,
  copyIt: copyIt,
  ppToConsole: ppToConsole,
  ppToBrowser: ppToBrowser,
  pp: pp,
  ppObject: ppObject,
  ppArray: ppArray,
  isVariable: isVariable,
  isAnonymousVariable: isAnonymousVariable,
  renameVariables: renameVariables,
  isAssertion: isAssertion,
  isAssertionOfType: isAssertionOfType,
  isClauseTypeWithArgsList: isClauseTypeWithArgsList,
  argsList: argsList,
  predicateName: predicateName,
  predicateArguments: predicateArguments,
  isDisjunction: isDisjunction,
  disjuncts: disjuncts,
  isConjunction: isConjunction,
  conjuncts: conjuncts,
  isNegation: isNegation,
  negation: negation,
  occursIn: occursIn,
  listToAssertion: listToAssertion,
  assertionToList: assertionToList,
  unify: unify,
  unifyVariable: unifyVariable,
  unifyPairs: unifyPairs,
  unifyPatterns: unifyPatterns,
  containsVariable: containsVariable,
  equal: equal,
  instantiate: instantiate,
  necessarilyUnify: necessarilyUnify,
  arrayToSet: arrayToSet,
  setToArray: setToArray,
  setUnion: setUnion,
  setIntersection: setIntersection,
  setDifference: setDifference,
  elementOf: elementOf,
  isEmpty: isEmpty,
  removeElement: removeElement,
  quickSort: quickSort,
  makeSearch: makeSearch
};

module.exports = utils;
