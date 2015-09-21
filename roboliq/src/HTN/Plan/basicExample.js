// This is a rewrite of the basic-example.lisp file included in the distribution of SHOP2.

module.exports = [
   // State
   {"have": {"agent": "sam", "thing": "banjo"}},
   // Tasks
   {"tasks": {"unordered": [{"swap": {"agent": "sam", "thing1": "banjo", "thing2": "kiwi"}}]}},
   // Actions
   {"action": {"description": "pick up the thing",
	       "task": {"pickup": {"agent": "?agent", "thing": "?thing"}},
	       "preconditions": [],
	       "deletions": [],
	       "additions": [{"have": {"agent": "?agent", "thing": "?thing"}}]}},

   {"action": {"description": "drop the thing",
	       "task": {"drop": {"agent": "?agent", "thing": "?thing"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing"}}],
	       "deletions": [{"have": {"agent": "?agent", "thing": "?thing"}}],
	       "additions": []}},
   // Methods
   {"method": {"description": "drop thing1 and pick up thing2",
	       "task": {"swap": {"agent": "?agent",
				 "thing1": "?thing1",
				 "thing2": "?thing2"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing1"}}],
	       "subtasks": {"ordered": [{"drop": {"agent": "?agent", "thing": "?thing1"}},
                                        {"pickup": {"agent": "?agent", "thing": "?thing2"}}]}}},
   {"method": {"description": "drop thing2 and pick up thing1",
	       "task": {"swap": {"agent": "?agent",
				 "thing1": "?thing1",
				 "thing2": "?thing2"}},
	       "preconditions": [{"have": {"agent": "?agent", "thing": "?thing2"}}],
	       "subtasks": {"ordered": [{"drop": {"agent": "?agent", "thing": "?thing2"}},
                                        {"pickup": {"agent": "?agent", "thing": "?thing1"}}]}}},
   // Rules: no rules in this example
];
