/*
 * sicpDB
 *
 * Rules and assertions for the database of the limited
 * logic programming language.  The syntax is JSON.
 *
 * This database is an translation and adaptation of the example
 * used in chapter 4 of Sussman and Abelson's book
 * "The Structure and Interpretation of Computer Programs."
 * At the bottom of the file are the addition of a few extra
 * rules that demonstrate the extra facilities of the
 * limited logic programming language.
 *
 * Warren Sack <wsack@ucsc.edu>
 * September 2010
 *
 * Ellis Whitehead <ellis.whitehead@gmail.com>
 * Lightly adapted for nodejs 2015
 */


//
// sicpDB
//
// This set of rules and assertions is largely taken from chapter 4 of Sussman and Abelson's
// book "The Structure and Interpretation of Computer Programs."
//
module.exports =
  [
   {"address": {"name": "Ben Bitdiddle", "city": "Slummerville", "street": "Ridge Road", "number": "10"}},
   {"job": {"name": "Ben Bitdiddle", "title":["computer", "wizard"]}},
   {"salary": {"name": "Ben Bitdiddle", "amount": "60000"}},

   {"address": {"name": "Alyssa P. Hacker", "city": "Cambridge", "street": "Mass Ave", "number": "78"}},
   {"job": {"name":  "Alyssa P. Hacker", "title": ["computer", "programmer"]}},
   {"salary": {"name":  "Alyssa P. Hacker", "amount": "40000"}},
   {"supervisor": {"name":  "Alyssa P. Hacker", "boss": "Ben Bitdiddle"}},

   {"address": {"name": "Cy D. Fect", "city": "Cambridge", "street": "Ames Street", "number": "3"}},
   {"job": {"name": "Cy D. Fect", "title": ["computer", "programmer"]}},
   {"salary": {"name": "Cy D. Fect", "amount": "35000"}},
   {"supervisor": {"name": "Cy D. Fect", "boss": "Ben Bitdiddle"}},

   {"address": {"name": "Lem E. Tweakit", "city": "Boston", "street": "Bay State Road", "number": "22"}},
   {"job": {"name": "Lem E. Tweakit", "title": ["computer", "technician"]}},
   {"salary": {"name": "Lem E. Tweakit", "amount": "25000"}},
   {"supervisor": {"name": "Lem E. Tweakit", "boss": "Ben Bitdiddle"}},

   {"address": {"name": "Louis Reasoner", "city": "Slumerville", "street": "Pine Tree Road", "number": "80"}},
   {"job": {"name": "Louis Reasoner", "title": ["computer", "programmer", "trainee"]}},
   {"salary": {"name": "Louis Reasoner", "amount": "30000"}},
   {"supervisor": {"name": "Louis Reasoner", "boss": "Alyssa P. Hacker"}},

   {"supervisor": {"name": "Ben Bitdiddle", "boss": "Oliver Warbucks"}},

   {"address": {"name": "Oliver Warbucks", "city": "Swellesley", "street": "Top Heap Road"}},
   {"job": {"name": "Oliver Warbucks", "title": ["administration", "big", "wheel"]}},
   {"salary": {"name": "Oliver Warbucks", "amount": "150000"}},

   {"address": {"name": "Eben Scrooge", "city": "Weston", "street": "Shady Lane", "number": "10"}},
   {"job":  {"name": "Eben Scrooge", "title": ["accounting", "chief", "accountant"]}},
   {"salary":  {"name": "Eben Scrooge", "amount": "75000"}},
   {"supervisor": {"name": "Eben Scrooge", "boss": "Oliver Warbucks"}},

   {"address": {"name": "Robert Cratchet", "city": "Allston", "street": "N Harvard Street", "number": "16"}},
   {"job":  {"name": "Robert Cratchet", "title": ["accounting", "scrivener"]}},
   {"salary": {"name": "Robert Cratchet", "amount": "18000"}},
   {"supervisor": {"name": "Robert Cratchet", "boss": "Eben Scrooge"}},

   {"address": {"name": "DeWitt Aull", "city": "Slumerville", "street": "Onion Square", "number": "5"}},
   {"job": {"name": "DeWitt Aull", "title": ["administration", "secretary"]}},
   {"salary": {"name": "DeWitt Aull", "amount": "25000"}},
   {"supervisor": {"name": "DeWitt Aull", "boss": "Oliver Warbucks"}},

   {"can-do-job": {"title1": ["computer", "wizard"], "title2": ["computer", "programmer"]}},
   {"can-do-job": {"title1": ["computer", "wizard"], "title2": ["computer", "technician"]}},
   {"can-do-job": {"title1": ["computer", "programmer"], "title2": ["computer", "programmer", "trainee"]}},
   {"can-do-job": {"title1": ["administration", "secretary"], "title2": ["administration", "big", "wheel"]}},

   {"<--": {"lives-near": {"person1": "?person1", "person2": "?person2"},
	    "and": [{"address": {"name": "?person1", "city": "?city"}},
                    {"address": {"name": "?person2", "city": "?city"}},
                    {"not": {"same": {"entity1": "?person1", "entity2": "?person2"}}}]}},

   {"<--": {"same": {"entity1": "?x", "entity2": "?x"}}},

   {"<--": {"wheel": {"person": "?person"},
	    "and": [{"supervisor": {"name": "?middle-manager", "boss": "?person"}},
                    {"supervisor":  {"name": "?someone", "boss": "?middle-manager"}}]}},


   {"<--": {"outranked-by": {"staff-person": "?staff-person", "boss": "?boss"},
	    "or": [{"supervisor": {"name": "?staff-person", "boss": "?boss"}},
		   {"and": [{"supervisor": {"name": "?staff-person", "boss": "?middle-manager"}},
                            {"outranked-by": {"staff-person": "?middle-manager", "boss": "?boss"}}]}]}},

   // Some extra rules to demonstrate...
   // a deduction rule with the evaluation of an expression;
   // a deduction rule with the evaluation of an assignment statement;
   // a deduction rule with a print statement; and,
   // two examples of production rules.  Note that the production
   // rules are mutually recursive but do not put the interpreter
   // into an infinite loop.

   {"<--": {"rich": {"name": "?name"},
	    "and": [{"salary": {"name": "?name", "amount": "?dollars"}},
                    {"print": ["This is the current salary information:",
                               {"salary": {"name": "?name", "amount": "?dollars"}}]},
                    {"eval": {">=": [{"+": ["?dollars","20000"]},"60000"]}},
                    {"eval": {"=": ["?millionDollars","1000000"]}},
                    {"print": ["But, this is what ","?name"," would like to make :$","?millionDollars"]}]}},

   {"-->": {"salary": {"name": "?name", "amount": "60000"},
	    "consequents": [{"rich": {"name": "?name"}}]}},

   {"-->": {"rich": {"name": "?name"},
	    "consequents": [{"salary": {"name": "?name", "amount": "60000"}},
                            {"brilliant": {"name": "?name"}}]}}


   ]; // end of sicpDB
