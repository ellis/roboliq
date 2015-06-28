/*
 *
 * planBlocksDisplay
 *
 * Warren Sack <wsack@ucsc.edu>
 * November 2010
 *
 * Accept a set of states from the search tree resulting from planning
 * how to move the blocks.  Extract and order the interesting information
 * from those search states.
 *
 */

var discourseMethods = 
  [

   ////
   //// Tasks
   ////
   {"tasks": {"ordered": [{"makeDiscourse": {"discourse": "?discourse"}}]}},


   ////
   //// Actions
   ////


   //
   // makeDiscourse
   //
   // Top-level action
   //
   {"action": {"description": "extract and order the interesting information from the given planning states",
	       "task": {"makeDiscourse": {"discourse": "?discourse"}},
	       "preconditions": [{"state": {"id": "?id", 
					    "success": "true",
					    "tasks": "?allTasks",
					    "predecessor": "?predecessor"}},
                                 {"state": {"id": "1", 
					    "assertions": {"blocks": {"cons": {"first": {"blocks": "?blocks"}}},
							   "on": "?ons"}}},
                                 {"findAll": {"target": {"move": {"block": "?_", "from": "?_", "to": "?_"}}, 
					      "list": "?allTasks", 
					      "result": "?moveTasks"}},
                                 {"append": {"list1": "?blocks", "list2": "?ons", "result": "?blocksAndPositions"}},
                                 {"append": {"list1": "?blocksAndPositions", "list2": "?moveTasks", "result": "?discourse"}}],
	       "deletions": [],
	       "additions": []}},
   
   ////
   //// Rules
   ////

   // same
   {"same": {"thing1": "?thing", "thing2": "?thing"}},
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
   // append
   {"<--": {"append": {"list1": "nil", "list2": "?result", "result": "?result"}}},
   {"<--": {"append": {"list1": {"cons": {"first": "?headOfList1",
					  "rest": "?restOfList1"}},
		       "list2": "?list2",
		       "result": {"cons": {"first": "?headOfList1",
					   "rest": "?restOfResult"}}},
	    "and": [{"append": {"list1": "?restOfList1", "list2": "?list2", 
				"result": "?restOfResult"}}]}},
   ];
