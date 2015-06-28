/*
 * listRules
 *
 * This set of rules defines basic methods on lists represented as cons objects
 * (member, append, and remove, etc.) in the limited logic programming language.
 *
 * Warren Sack <wsack@ucsc.edu>
 * October 2010
 *
 * Ellis Whitehead <ellis.whitehead@gmail.com>
 * Lightly adapted for nodejs 2015
 */

module.exports = 
  [
   // same
   {"same": {"thing1": "?thing", "thing2": "?thing"}},
   // member
   {"<--": {"member": {"target": "?target", "list": {"cons": {"first": "?target",
							      "rest": "?restOfList"}}}}},
   {"<--": {"member": {"target": "?target", "list": {"cons": {"first": "?firstOfList",
							      "rest": "?restOfList"}}},
	    "and": [{"member": {"target": "?target", "list": "?restOfList"}}]}},
   // append
   {"<--": {"append": {"list1": "nil", "list2": "?result", "result": "?result"}}},
   {"<--": {"append": {"list1": {"cons": {"first": "?headOfList1",
					  "rest": "?restOfList1"}},
		       "list2": "?list2",
		       "result": {"cons": {"first": "?headOfList1",
					   "rest": "?restOfResult"}}},
	    "and": [{"append": {"list1": "?restOfList1", "list2": "?list2",
				"result": "?restOfResult"}}]}},
   // remove
   {"<--": {"remove": {"target": "?target",
		       "list": {"cons": {"first": "?target", "rest": "?result"}},
		       "result": "?result"}}},
   {"<--": {"remove": {"target": "?target",
		       "list": {"cons": {"first": "?headOfList", "rest": "?restOfList"}},
		       "result": {"cons": {"first": "?headOfList", "rest": "?restOfResult"}}},
	    "and": [{"remove": {"target": "?target", "list": "?restOfList", "result": "?restOfResult"}}]}},
   // removeAll
   {"<--": {"removeAll": {"target": "?target", "list": "nil", "result": "nil"}}},
   {"<--": {"removeAll": {"target": "?target",
			  "list": {"cons": {"first": "?target", "rest": "?restOfList"}},
			  "result": "?restOfResult"},
	    "and": [{"removeAll": {"target": "?target", "list": "?restOfList", "result": "?restOfResult"}}]}},
   {"<--": {"removeAll": {"target": "?target",
			  "list": {"cons": {"first": "?headOfList", "rest": "?restOfList"}},
			  "result": {"cons": {"first": "?headOfList", "rest": "?restOfResult"}}},
	    "and": [{"not": {"same": {"thing1": "?target", "thing2": "?headOfList"}}},
                    {"removeAll": {"target": "?target", "list": "?restOfList", "result": "?restOfResult"}}]}},
   // findAll
   {"<--": {"findAll": {"target": "?target", "list": "nil", "result": "nil"}}},
   {"<--": {"findAll": {"target": "?target",
			"list": {"cons": {"first": "?headOfList", "rest": "?restOfList"}},
			"result": {"cons": {"first": "?headOfList", "rest": "?restOfResult"}}},
	    "and": [{"same": {"thing1": "?target", "thing2": "?headOfList"}},
                    {"findAll": {"target": "?target", "list": "?restOfList", "result": "?restOfResult"}}]}},
   {"<--": {"findAll": {"target": "?target",
			"list": {"cons": {"first": "?headOfList", "rest": "?restOfList"}},
			"result": "?restOfResult"},
	    "and": [{"not": {"same": {"thing1": "?target", "thing2": "?headOfList"}}},
                    {"findAll": {"target": "?target", "list": "?restOfList", "result": "?restOfResult"}}]}},
   ];
