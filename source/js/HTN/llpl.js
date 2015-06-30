/*
 * llpl
 *
 * A Limited Logic Programming Language
 *
 * Warren Sack <wsack@ucsc.edu>
 * September 2010
 *
 * Ellis Whitehead <ellis.whitehead@gmail.com>
 * Lightly adapted for nodejs 2015
 *
 * This is a logic programming interpreter.
 *
 * Sample usage:
 * var llpl = require('./HTN/Logic/llpl.js');
 * var sicpDB = require('./HTN/Logic/sicpDB.js');
 * llpl.initializeDatabase(sicpDB);
 *
 * Here is an example query that can be run from the browser console
 * once the code for the utilities and the interpreter have been loaded
 * and the database has been initialized with the contents of sicpDB.js:
 *
 * llpl.query({"lives-near": {"person1": "?x", "person2": "?y"}});
 *
 */

var utils = require('./utils.js');

////
//// Syntax
////


//
// isDRule(obj): boolean
//
function isDRule(obj) {
  return (utils.isAssertionOfType(obj, '<--'));
}

//
// druleConclusion(obj): object
//
function druleConclusion(obj) {
  var definition = utils.values(obj)[0];
  var newObj = {};
  for (var key in definition) {
    if ((key !== 'or') && (key !== 'and') && (key !== 'not')) {
      newObj[key] = definition[key];
    }
  }
  return (newObj);
}

//
// druleBody(obj): object
//
function druleBody(obj) {
  var definition = utils.values(obj)[0];
  var newObj = {};
  for (key in definition) {
    if ((key === 'or') || (key === 'and') || (key === 'not')) {
      newObj[key] = definition[key];
    }
  }
  return (newObj);
}

//
// isPRule(obj): boolean
//
function isPRule(obj) {
  return (utils.isAssertionOfType(obj, '-->'));
}

//
// pruleAntecendent(obj): object
//
function pruleAntecedent(obj) {
  var definition = utils.values(obj)[0];
  var newObj = {};
  for (var key in definition) {
    if (key !== 'consequents') {
      newObj[key] = definition[key];
    }
  }
  return (newObj);
}

//
// pruleConsequents(obj): object
//
function pruleConsequents(obj) {
  return (obj["-->"]["consequents"]);
}

//
// isEval(obj): boolean
//
function isEval(obj) {
  return (utils.isAssertionOfType(obj, 'eval'));
}

//
// isPrint(obj): boolean
//
function isPrint(obj) {
  return (utils.isClauseTypeWithArgsList(obj, 'print'))
}


////
//// Database
////


// This is a global variable that contains, after initializeDatabase
// has been run, a indexed database of rules and assertions.
var database = {};

//
// initializeDatabase(a): side effects the database of assertions and rules
//
// Given a array of rules and assertions, creates a new object of indexed
// rules and assertions.
//
function initializeDatabase(rsAndAs) {
  var i;
  database["drules"] = {};
  database["prules"] = {};
  database["assertions"] = {};
  var assertionsList = [];
  var isLoaded = false;
  for (i = 0; i < rsAndAs.length; i++) {
    isLoaded = false;
    if (isDRule(rsAndAs[i]) && !isLoaded) {
      indexDRule(rsAndAs[i]);
      isLoaded = true;
    }
    if (isPRule(rsAndAs[i]) && !isLoaded) {
      indexPRule(rsAndAs[i]);
      isLoaded = true;
    }
    if (utils.isAssertion(rsAndAs[i]) && !isLoaded) {
      assertionsList.push(rsAndAs[i]);
      isLoaded = true;
    }
    if (!isLoaded) {
      utils.ppToConsole("Unrecognized form in database: " + rsAndAs[i]);
    }
  }
  for (i = 0; i < assertionsList.length; i++) {
    indexAssertion(assertionsList[i]);
  }
  return (database);
}

//
// indexAssertion(p): side effects database["assertions"]
//
function indexAssertion(p) {
  var bindingsArray = findAssertions(p, {});
  // If nothing like this already exists in the database,
  // then add it to the database and run the prules.
  if (utils.isEmpty(bindingsArray)) {
    var pred = utils.predicateName(p);
    if (database["assertions"][pred] !== undefined) {
      database["assertions"][pred].push(p);
    } else {
      database["assertions"][pred] = [p];
    }
    applyPRules(p);
  }
}

//
// retractAssertion(p): side effects database["assertions"]
//
// Note that assertions containing variables will not be
// retracted.  When an assertion is retracted, all consequents
// of that assertion are also retracted by running reversePRules.
//
function retractAssertion(p) {
  var pred = utils.predicateName(p);
  if (database["assertions"][pred] !== undefined) {
    var numberOfAssertions = database["assertions"][pred].length;
    var result = [];
    for (var i = 0; i < database["assertions"][pred].length; i++) {
      if (!utils.equal(p, database["assertions"][pred][i])) {
        result.push(database["assertions"][pred][i]);
      }
    }
    database["assertions"][pred] = result;
    if (numberOfAssertions > result.length) {
      reversePRules(p);
    }
  }
}

//
// fetchAssertions(queryPattern): array
//
function fetchAssertions(queryPattern) {
  var pred = utils.predicateName(queryPattern);
  if (database["assertions"][pred] !== undefined) {
    return (database["assertions"][pred]);
  }
  return ([]);
}

//
// findAssertions(pattern,bindings): array
//
function findAssertions(pattern, bindings) {
  var a = fetchAssertions(pattern);
  var bindingsArray = [];
  var freshBindings;
  for (var i = 0; i < a.length; i++) {
    freshBindings = utils.copyIt(bindings);
    if (utils.unifyPatterns(a[i], pattern, freshBindings)) {
      bindingsArray.push(freshBindings);
    }
  }
  return (bindingsArray);
}

//
// indexDRule(r): side effects database["drules"]
//
function indexDRule(r) {
  var pred = utils.predicateName(druleConclusion(r));
  if (database["drules"][pred] !== undefined) {
    database["drules"][pred].push(r);
  } else {
    database["drules"][pred] = [r];
  }
}

//
// fetchDRules(queryPattern): array
//
function fetchDRules(queryPattern) {
  var pred = utils.predicateName(queryPattern);
  if (database["drules"][pred] !== undefined) {
    return (database["drules"][pred]);
  } else {
    return ([]);
  }
}

//
// indexPRule(r): side effects database["prules"]
//
function indexPRule(r) {
  var pred = utils.predicateName(pruleAntecedent(r));
  if (database["prules"][pred] !== undefined) {
    database["prules"][pred].push(r);
  } else {
    database["prules"][pred] = [r];
  }
}

//
// fetchPRules(queryPattern): array
//
function fetchPRules(queryPattern) {
  var pred = utils.predicateName(queryPattern);
  if (database["prules"][pred] !== undefined) {
    return (database["prules"][pred]);
  } else {
    return ([]);
  }
}


////
//// Evaluation
////


//
// applyPRule(prule,pattern): array
//
function applyPRule(prule, pattern) {
  var cleanPRule = utils.renameVariables(utils.copyIt(prule));
  var antecedent = pruleAntecedent(cleanPRule);
  var bindings = {};
  if (utils.unifyPatterns(pattern, antecedent, bindings)) {
    var consequents = utils.instantiate(utils.copyIt(pruleConsequents(cleanPRule)),
      bindings);
    for (var i = 0; i < consequents.length; i++) {
      indexAssertion(consequents[i]);
    }
  }
}

//
// applyPRules(pattern): array
//
function applyPRules(pattern) {
  var prules = fetchPRules(pattern);
  for (var i = 0; i < prules.length; i++) {
    applyPRule(prules[i], pattern);
  }
}

//
// reversePRule(prule,pattern): side effects database["assertions"]
//
function reversePRule(prule, pattern) {
  var cleanPRule = utils.renameVariables(utils.copyIt(prule));
  var antecedent = pruleAntecedent(cleanPRule);
  var bindings = {};
  if (utils.unifyPatterns(pattern, antecedent, bindings)) {
    var consequents = utils.instantiate(utils.copyIt(pruleConsequents(cleanPRule)),
      bindings);
    for (var i = 0; i < consequents.length; i++) {
      retractAssertion(consequents[i]);
    }
  }
}

//
// reversePRules(pattern): side effects database["assertions"]
//
function reversePRules(pattern) {
  var prules = fetchPRules(pattern);
  for (var i = 0; i < prules.length; i++) {
    reversePRule(prules[i], pattern);
  }
}

//
// applyDRule(drule,queryPattern,bindings): array
//
function applyDRule(drule, queryPattern, bindings) {
  var cleanDRule = utils.renameVariables(utils.copyIt(drule));
  var conclusion = druleConclusion(cleanDRule);
  if (utils.unifyPatterns(queryPattern, conclusion, bindings)) {
    return (qeval(druleBody(cleanDRule), [bindings]));
  } else {
    return ([]);
  }
}

//
// applyDRules(pattern,bindings): array
//
function applyDRules(pattern, bindings) {
  var drules = fetchDRules(pattern);
  var bindingsArray = [];
  var freshBindings, nextBindingsArray;
  for (var i = 0; i < drules.length; i++) {
    freshBindings = utils.copyIt(bindings);
    nextBindingsArray = applyDRule(drules[i], pattern, freshBindings);
    for (var j = 0; j < nextBindingsArray.length; j++) {
      bindingsArray.push(nextBindingsArray[j]);
    }
  }
  return (bindingsArray);
}

//
// simpleQuery(queryPattern,bindingsArray): array
//
function simpleQuery(queryPattern, bindingsArray) {
  if (utils.isEmpty(queryPattern)) {
    return (bindingsArray);
  }
  var combinedBindingsArray = [];
  var newBindingsArrayFromAssertions, newBindingsArrayFromDRules, i, j;
  for (i = 0; i < bindingsArray.length; i++) {
    newBindingsArrayFromAssertions = findAssertions(queryPattern, bindingsArray[i]);
    for (j = 0; j < newBindingsArrayFromAssertions.length; j++) {
      combinedBindingsArray.push(newBindingsArrayFromAssertions[j]);
    }
    newBindingsArrayFromDRules = applyDRules(queryPattern, bindingsArray[i]);
    for (j = 0; j < newBindingsArrayFromDRules.length; j++) {
      combinedBindingsArray.push(newBindingsArrayFromDRules[j]);
    }
  }
  return (combinedBindingsArray);
}

//
// conjoin(conjunctsArray,bindingsArray): array
//
function conjoin(conjunctsArray, bindingsArray) {
  if (utils.isEmpty(conjunctsArray)) {
    return (bindingsArray);
  }
  var newBindingsArray = qeval(conjunctsArray[0], bindingsArray);
  conjunctsArray.shift();
  return (conjoin(conjunctsArray, newBindingsArray));
}

//
// disjoin(disjunctsArray,bindingsArray): array
//
function disjoin(disjunctsArray, bindingsArray) {
  if (utils.isEmpty(disjunctsArray)) {
    return (bindingsArray);
  }
  var combinedBindingsArray = [];
  var newBindingsArray, freshBindingsArray;
  for (var i = 0; i < disjunctsArray.length; i++) {
    freshBindingsArray = utils.copyIt(bindingsArray);
    newBindingsArray = qeval(disjunctsArray[i], freshBindingsArray);
    for (var j = 0; j < newBindingsArray.length; j++) {
      combinedBindingsArray.push(newBindingsArray[j]);
    }
  }
  return (combinedBindingsArray);
}

//
// negate(nQueryPattern,bindingsArray): array
//
function negate(nQueryPattern, bindingsArray) {
  var query = utils.negation(nQueryPattern);
  var resultantBindingsArray = [];
  var result;
  for (var i = 0; i < bindingsArray.length; i++) {
    result = qeval(query, [bindingsArray[i]]);
    if (utils.isEmpty(result)) {
      resultantBindingsArray.push(bindingsArray[i]);
    }
  }
  return (resultantBindingsArray);
}

//
// eeval(toEvaluate,bindingsArray): bindings array
//
// Handle evaluation of assignment statements, boolean comparisons,
// and arithmetic statements.
//
function eeval(toEvaluate, bindingsArray) {
  expression = toEvaluate["eval"];
  var operator, iexpression, bindings;
  var legalOperators = utils.arrayToSet(["=", "==", "!=", ">", ">=", "<", "<=", "+", "-", "/", "*"]);
  var resultantBindingsArray = [];
  for (var op in expression) {
    operator = op;
  }
  if (!utils.elementOf(operator, legalOperators)) {
    utils.ppToConsole("Error: unrecognized operator in statement or expression: " + utils.pp(operator));
    return ([]);
  }
  for (var i = 0; i < bindingsArray.length; i++) {
    bindings = utils.copyIt(bindingsArray[i]);
    iexpression = utils.instantiate(utils.copyIt(expression), bindings);
    if (op == "=") {
      resultantBindingsArray.push(evalAssignment(iexpression, bindings));
    } else {
      if (evalExpression(iexpression, bindings) !== false) {
        resultantBindingsArray.push(bindings);
      }
    }
  }
  return (resultantBindingsArray);
}

//
// evalAssignment(expression,bindings): bindings
//
function evalAssignment(expression, bindings) {
  var argsList = expression["="];
  var lhs = argsList[0];
  var rhs = argsList[1];
  rhs = evalExpression(rhs, bindings);
  bindings[lhs] = rhs.toString();
  return (bindings);
}

//
// evalExpression(expression,bindings): number or boolean value
//
function evalExpression(expression, bindings) {
  var iexpression = utils.instantiate(utils.copyIt(expression), bindings);
  if (utils.typeOf(iexpression) !== 'object') {
    if ((iexpression === true) || (iexpression === "true")) {
      return (true);
    }
    if ((iexpression === false) || (iexpression === "false")) {
      return (false);
    }
    if (!isNaN(iexpression)) {
      return (parseFloat(iexpression));
    } else {
      utils.ppToConsole("Error: expression should be a number, true, or false: " + utils.pp(iexpression));
      return (iexpression);
    }
  } else {
    var operator, i;
    for (var op in iexpression) {
      operator = op;
    }
    var argValues = [];
    for (i = 0; i < iexpression[operator].length; i++) {
      argValues.push(evalExpression(iexpression[operator][i], bindings));
    }
    if (argValues.length != 2) {
      utils.ppToConsole("Error: only two arguments are allowed in expressions: " + utils.pp(iexpression));
    }
    return (eval(argValues[0] + " " + operator + " " + argValues[1]));
  }
}

//
// printQuery(query,bindingsArray): output message to console
//
function printQuery(query, bindingsArray) {
  var q;
  for (var i = 0; i < bindingsArray.length; i++) {
    q = utils.instantiate(utils.copyIt(query), bindingsArray[i]);
    utils.ppToConsole(q["print"]);
  }
  return (bindingsArray);
}

//
// qeval(query,bindingsArray): array
//
function qeval(query, bindingsArray) {
  if (utils.isConjunction(query)) {
    return (conjoin(utils.conjuncts(query), bindingsArray));
  }
  if (utils.isDisjunction(query)) {
    return (disjoin(utils.disjuncts(query), bindingsArray));
  }
  if (utils.isNegation(query)) {
    return (negate(query, bindingsArray));
  }
  if (isEval(query)) {
    return (eeval(query, bindingsArray));
  }
  if (isPrint(query)) {
    return (printQuery(query, bindingsArray));
  }
  return (simpleQuery(query, bindingsArray));
}

//
// query(q): outputs to console
//
function query(q) {
  var cleanQuery, instance;
  var results = [];
  var bindingsArray = qeval(q, [{}]);
  if (utils.isEmpty(bindingsArray)) {
    utils.ppToConsole("No");
  } else {
    for (var i = 0; i < bindingsArray.length; i++) {
      cleanQuery = utils.copyIt(q);
      instance = utils.instantiate(cleanQuery, bindingsArray[i]);
      utils.ppToConsole('(' + i + ')');
      utils.ppToConsole(instance);
      results.push(instance);
      // utils.ppToConsole(bindingsArray[i]);
    }
  }
  return (results);
}


//
// Return the methods for interacting with the
// database and the rules.
//


module.exports = {
  database: database,
  initializeDatabase: initializeDatabase,
  indexAssertion: indexAssertion,
  retractAssertion: retractAssertion,
  findAssertions: findAssertions,
  fetchAssertions: fetchAssertions,
  conjoin: conjoin,
  qeval: qeval,
  query: query
};
