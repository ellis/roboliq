function makeBlocksWorld(utils) {
  
  var world = 
    [
     
   ////
   //// initial state of the world
   ////
   /*
   {"blocks": utils.listToAssertion(["a","b","c"])},
   {"on": {"above": "b", "below": "table"}},
   {"on": {"above": "a", "below": "table"}},
   {"on": {"above": "c", "below": "a"}},
   */
   {"blocks": utils.listToAssertion(["a","b","c","d","e","f","g","h","i","j","k","l","m"])},
   {"on": {"above": "a", "below": "m"}},
   {"on": {"above": "b", "below": "table"}},
   {"on": {"above": "c", "below": "a"}},
   {"on": {"above": "d", "below": "c"}},
   {"on": {"above": "e", "below": "b"}},
   {"on": {"above": "f", "below": "table"}},
   {"on": {"above": "g", "below": "table"}},
   {"on": {"above": "h", "below": "g"}},
   {"on": {"above": "i", "below": "h"}},
   {"on": {"above": "j", "below": "e"}},
   {"on": {"above": "k", "below": "j"}},
   {"on": {"above": "l", "below": "k"}},
   {"on": {"above": "m", "below": "table"}},
   
   ////
   //// Goals
   ////
   /*
   {"goals": utils.listToAssertion([{"goal": {"on": {"above": "a", "below": "b"}}},
			            {"goal": {"on": {"above": "b", "below": "c"}}},
			            {"goal": {"on": {"above": "c", "below": "table"}}}])},
   */
   {"goals": utils.listToAssertion([{"goal": {"on": {"above": "a", "below": "table"}}},
				    {"goal": {"on": {"above": "b", "below": "a"}}},
				    {"goal": {"on": {"above": "c", "below": "b"}}},
				    {"goal": {"on": {"above": "d", "below": "c"}}},
				    {"goal": {"on": {"above": "e", "below": "d"}}},
				    {"goal": {"on": {"above": "f", "below": "table"}}},
				    {"goal": {"on": {"above": "g", "below": "f"}}},
				    {"goal": {"on": {"above": "h", "below": "g"}}},
				    {"goal": {"on": {"above": "i", "below": "h"}}},
				    {"goal": {"on": {"above": "j", "below": "i"}}},
				    {"goal": {"on": {"above": "k", "below": "table"}}},
				    {"goal": {"on": {"above": "l", "below": "k"}}},
				    {"goal": {"on": {"above": "m", "below": "l"}}}])},


   
   ////
   //// Tasks
   ////
   {"tasks": {"ordered": [{"solveGoals": {"goals": "?goals"}}]}},


   ////
   //// Actions
   ////
  
   //
   // move 
   //
   // Primary action in the blocks world: move a block from one place to another.
   // This version has no preconditions because they are accounted for in the
   // methods and the subtask orderings of the methods: nothing is moved before
   // it is clear and its destination is clear.
   //
   {"action": {"description": "move a block",
	       "task": {"move": {"block": "?block", "from": "?from", "to": "?to"}},
	       "preconditions": [],
	       "additions": [{"on": {"above": "?block", "below": "?to"}}],
	       "deletions": [{"on": {"above": "?block", "below": "?from"}}]}},

   //
   // assert
   //
   // Book-keeping action for making assertions about the state of the tasks being
   // executed by the planner.
   //
   {"action": {"description": "add an assertion into the database",
	       "task": {"assert": {"assertion": "?a"}},
	       "preconditions": [],
	       "deletions": [],
	       "additions": ["?a"]}},

   ////
   //// Methods
   ////

   //
   // solveGoals
   //
   // Top-level method
   //
   // unstackAll: put all of the blocks onto the table;
   // addAssertions: assert the goals into the database;
   // findWhichDoNotMove: consult the goals, see which blocks should stay on the table;
   // stackAll: iterate through the goals, solve all goals to stack blocks on the table,
   //           then solve all goals that entail stacking blocks on top of the blocks on
   //           the table, etc.
   //
   {"method": {"description": "solve all stated goals for all declared blocks",
	       "task": {"solveGoals": {"goals": "?goals"}},
	       "preconditions": [{"goals": "?goals"},
                                 {"blocks": "?blocks"},
                                 //{"print": ["?blocks"]},
				 ],
	       "subtasks": {"ordered": [{"unstackAll": "?blocks"},
                                        {"addAssertions": "?goals"},
                                        {"findWhichDoNotMove": "?blocks"},
                                        {"stackAll": "?goals"}]}}},

   //
   // addAssertions
   //
   // Add a list of assertions into the database.
   //
   // addAssertions / base case / add the final assertion into the database
   //
   {"method": {"description": "given a list of assertions, assert them into the database",
	       "task": {"addAssertions": {"cons": {"first": "?assertion", "rest": "nil"}}},
	       "preconditions": [],
	       "subtasks": [{"assert": {"assertion": "?assertion"}}]}},
   //
   // addAssertions / recursive case / add the first asertions into the database and recur with the rest
   //
   {"method": {"description": "given a list of assertions, assert them into the database",
	       "task": {"addAssertions": {"cons": {"first": "?assertion", "rest": "?rest"}}},
	       "preconditions": [{"not": {"same": {"thing1": "?rest", "thing2": "nil"}}}],
	       "subtasks": [{"assert": {"assertion": "?assertion"}},
                            {"addAssertions": "?rest"}]}},

   // 
   // unstackAll
   //
   // Move all of the blocks to the table.
   //
   // unstackAll / base case 1 / the final block is already on the table
   //
   {"method": {"description": "unstack the blocks",
	       "task": {"unstackAll": {"cons": {"first": "?block", "rest": "nil"}}},
	       "preconditions": [{"on": {"above": "?block", "below": "table"}},
                                 // {"print": ["base case 1: already on the table", "?block"]}
				 ],
	       "subtasks": []}},
   //
   // unstackAll / base case 2 / the final block is not on the table
   //
   {"method": {"description": "unstack the blocks",
	       "task": {"unstackAll": {"cons": {"first": "?block", "rest": "nil"}}},
	       "preconditions": [{"on": {"above": "?block", "below": "?from"}},
                                 {"not": {"same": {"thing1": "?from", "thing2": "table"}}},
                                 // {"print": ["base case 2: not on the table", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"move": {"block": "?block", "from": "?from", "to": "table"}}]}}},
   //
   // unstackAll / recursive case 1 / the first block is on the table
   //
   {"method": {"description": "unstack the blocks",
	       "task": {"unstackAll": {"cons": {"first": "?block", "rest": "?rest"}}},
	       "preconditions": [{"on": {"above": "?block", "below": "table"}},
                                 {"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 // {"print": ["recursive case 1: on the table already", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"unstackAll": "?rest"}]}}},
   //
   // unstackAll / recursive case 2 / the first block is clear, but not on the table, so move it
   //
   {"method": {"description": "unstack the blocks",
	       "task": {"unstackAll": {"cons": {"first": "?block", "rest": "?rest"}}},
	       "preconditions": [{"clear": {"block": "?block"}},
                                 {"on": {"above": "?block", "below": "?from"}},
                                 {"not": {"same": {"thing1": "?from", "thing2": "table"}}},
                                 {"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 // {"print": ["recursive case 2: clear but not on the table", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"move": {"block": "?block", "from": "?from", "to": "table"}},
                                        {"unstackAll": "?rest"}]}}},
   //
   // unstackAll / recursive case 3 / the first block is not clear and not on the table, 
   //                                 so append it to the end of the list
   //
   {"method": {"description": "unstack the blocks",
	       "task": {"unstackAll": {"cons": {"first": "?block", "rest": "?rest"}}},
	       "preconditions": [{"not": {"clear": {"block": "?block"}}},
                                 {"on": {"above": "?block", "below": "?from"}},
                                 {"not": {"same": {"thing1": "?from", "thing2": "table"}}},
                                 {"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 {"append": {"list1": "?rest",
					     "list2": {"cons": {"first": "?block", "rest": "nil"}}, 
					     "result": "?rotatedList"}},
                                 // {"print": ["recursive case 3: not clear yet and not on the table", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"unstackAll": "?rotatedList"}]}}},

   //
   // findWhichDoNotMove
   //
   // Find those blocks which do not need to be moved.  A block does not need
   // to move if there is a goal stating it should be on the table, because
   // a method that runs before this one, unstackAll, puts all of the
   // blocks on the table.  Those in their final positions are marked with 
   // a doNotMove assertion.
   //
   // findWhichDoNotMove / base case 1 / the remaining block should not move
   //
   {"method": {"description": "find those blocks which do not need to be moved",
	       "task": {"findWhichDoNotMove": {"cons": {"first": "?block", "rest": "nil"}}},
	       "preconditions": [{"goal": {"on": {"above": "?block", "below": "table"}}},
                                 // {"print": ["base case 1: remaining block should not move", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"assert": {"assertion": {"doNotMove": {"block": "?block"}}}}]}}},
   //
   // findWhichDoNotMove / base case 2 / the remaining block needs to move
   //
   {"method": {"description": "find those blocks which do not need to be moved",
	       "task": {"findWhichDoNotMove": {"cons": {"first": "?block", "rest": "nil"}}},
	       "preconditions": [{"not": {"goal": {"on": {"above": "?block", "below": "table"}}}},
                                 // {"print": ["base case 2: remaining block can move", "?block"]}
				 ],
	       "subtasks": []}},
   //
   // findWhichDoNotMove / recursive case 1 / the first block should not move
   //
   {"method": {"description": "find those blocks which do not need to be moved",
	       "task": {"findWhichDoNotMove": {"cons": {"first": "?block", "rest": "?rest"}}},
	       "preconditions": [{"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 {"goal": {"on": {"above": "?block", "below": "table"}}},
                                 // {"print": ["recursive case 1: first block should not move", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"assert": {"assertion": {"doNotMove": {"block": "?block"}}}},
                                        {"findWhichDoNotMove": "?rest"}]}}},
   //
   // findWhichDoNotMove / recursive case 2 / the first block needs to move
   //
   {"method": {"description": "find those blocks which do not need to be moved",
	       "task": {"findWhichDoNotMove": {"cons": {"first": "?block", "rest": "?rest"}}},
	       "preconditions": [{"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 {"not": {"goal": {"on": {"above": "?block", "below": "table"}}}},
                                 // {"print": ["recursive case 2: first block can move", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"findWhichDoNotMove": "?rest"}]}}},

   //
   // stackAll
   //
   // Iterate through the list of goals, achieve each goal that is ready; if a goal is
   // not ready, append it to the end of the list and continue working through the list
   // of goals.
   //
   // stackAll / base case 1 / the final block is already where it is suppose to be
   //
   {"method": {"description": "given a list of goals, achieve them one at a time",
	       "task": {"stackAll": {"cons": {"first": {"goal": {"on": {"above": "?block", "below": "?to"}}}, 
					      "rest": "nil"}}},
	       "preconditions": [{"on": {"above": "?block", "below": "?to"}},
                                 // {"print": ["base case 1: final block is where it should be", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"assert": {"assertion": {"doNotMove": {"block": "?block"}}}}]}}},
   //
   // stackAll / base case 2 / the final block needs to be moved
   //
   {"method": {"description": "given a list of goals, achieve them one at a time",
	       "task": {"stackAll": {"cons": {"first": {"goal": {"on": {"above": "?block", "below": "?to"}}}, 
					      "rest": "nil"}}},
	       "preconditions": [{"on": {"above": "?block", "below": "?currentPosition"}},
                                 {"not": {"same": {"thing1": "?to", "thing2": "?currentPosition"}}},
                                 // {"print": ["base case 2: final block needs to be moved", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"move": {"block": "?block", "from": "?currentPosition", "to": "?to"}},
                                        {"assert": {"assertion": {"doNotMove": {"block": "?block"}}}}]}}},
   //
   // stackAll / recursive case 1 / the first goal is ready and already solved
   //
   {"method": {"description": "given a list of goals, achieve them one at a time",
	       "task": {"stackAll": {"cons": {"first": {"goal": {"on": {"above": "?block", "below": "?to"}}}, 
					      "rest": "?rest"}}},
	       "preconditions": [{"isReady": {"goal": {"on": {"above": "?block", "below": "?to"}}}},
                                 {"on": {"above": "?block", "below": "?to"}},
                                 {"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 // {"print": ["recursive case 1: first goal is ready and solved", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"assert": {"assertion": {"doNotMove": {"block": "?block"}}}},
                                        {"stackAll": "?rest"}]}}},
   //
   // stackAll / recursive case 2 / the first goal is ready, but unsolved, and so can be solved with a move
   //
   {"method": {"description": "given a list of goals, achieve them one at a time",
	       "task": {"stackAll": {"cons": {"first": {"goal": {"on": {"above": "?block", "below": "?to"}}}, 
					      "rest": "?rest"}}},
	       "preconditions": [{"isReady": {"goal": {"on": {"above": "?block", "below": "?to"}}}},
                                 {"on": {"above": "?block", "below": "?currentPosition"}},
                                 {"not": {"same": {"thing1": "?to", "thing2": "?currentPosition"}}},
                                 {"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 // {"print": ["recursive case 2: first goal is ready but unsolved", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"move": {"block": "?block", "from": "?currentPosition", "to": "?to"}},
                                        {"assert": {"assertion": {"doNotMove": {"block": "?block"}}}},
                                        {"stackAll": "?rest"}]}}},
   //
   // stackAll / recursive case 3 / the first goal is not ready and so will have to wait; it is appended to the end
   //
   {"method": {"description": "given a list of goals, achieve them one at a time",
	       "task": {"stackAll": {"cons": {"first": {"goal": {"on": {"above": "?block", "below": "?to"}}}, 
	                                      "rest": "?rest"}}},
	       "preconditions": [{"not": {"isReady": {"goal": {"on": {"above": "?block", "below": "?to"}}}}},
                                 {"not": {"same": {"thing1": "?rest", "thing2": "nil"}}},
                                 {"append": {"list1": "?rest", 
					     "list2": {"cons": {"first": {"goal": {"on": {"above": "?block", 
											  "below": "?to"}}},
								"rest": "nil"}},
					     "result": "?rotatedList"}},
                                 // {"print": ["recursive case 3: first goal not ready", "?block"]}
				 ],
	       "subtasks": {"ordered": [{"stackAll": "?rotatedList"}]}}},


   ////
   //// Rules
   ////

   //
   // same
   //
   // Two things are the same if they unify.
   //
   {"<--": {"same": {"thing1": "?thing", "thing2": "?thing"}}},

   //
   // append
   //
   // Append two lists together
   //
   {"<--": {"append": {"list1": "nil", "list2": "?result", "result": "?result"}}},
   {"<--": {"append": {"list1": {"cons": {"first": "?headOfList1",
					  "rest": "?restOfList1"}},
		       "list2": "?list2",
		       "result": {"cons": {"first": "?headOfList1",
					   "rest": "?restOfResult"}}},
	    "and": [{"append": {"list1": "?restOfList1", "list2": "?list2", 
				"result": "?restOfResult"}}]}},

   //
   // clear 
   //
   // A block is clear if there is not another block on it.
   //
   {"<--": {"clear": {"block": "?block"},
	    "and": [{"not": {"on": {"above": "?otherBlock", "below": "?block"}}}]}},
   //
   // The table is always clear.
   //
   {"<--": {"clear": {"block": "table"}}},

   //
   // doNotMove
   //
   // The table should never be moved.
   //
   {"<--": {"doNotMove": {"block": "table"}}},
    
   //
   // isReady
   //
   // A goal is ready to be solved if it entails moving a block to the
   // table and the block is clear.
   //
   {"<--": {"isReady": {"goal": {"on": {"above": "?above", "below": "table"}}},
	    "and": [{"clear": {"block": "?above"}}]}},
   // 
   // A goal is ready to be solved if both the origin and destination of the
   // block are clear, and if the destination does not have to be moved.
   //
   {"<--": {"isReady": {"goal": {"on": {"above": "?above", "below": "?below"}}},
	    "and": [{"clear": {"block": "?above"}},
                    {"clear": {"block": "?below"}},
                    {"doNotMove": {"block": "?below"}}]}},
  
     ];

  return({world : world});

}
