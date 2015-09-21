# Design Thoughts

## From protocol to executable script

Date: 2014-02-03, 14

- The protocol lists a sequence of top-level tasks.
- Each task decomposes into a list of tasks and sets of methods.
- For each set of methods, the user chooses one.
- A method is a way to accomplish a task which doesn't require the user to make anymore choices.
- This gives us an ordered chain of methods.
- The methods decompose into an ordered chain of partially instantiated operators for planning.
- The operators may have two kinds of pre-requisites: those which can be fulfilled at any time before the operator, and those which should be fulfilled after the preceding operator.
- The output of the first processing stage of a protocol is a partial plan, containing a partially ordered list of operators and a partial variable binding map.
- The user may take the opportunity to set additional variable bindings and place additional ordering constraints.

I think we could then use FF planning, where we would intelligently select which actions to run first.
For example, we would try to run all user actions before robot actions at the beginning of the script,
then after that we would try to group user actions together.

## Placing labware on the table

Date: 2014-02-13

Complications:
- We want to involve the user as little as possible -- preferably just at the beginning.
- Sometimes it's best for the user to put the plates on their initial positions, and sometimes it's best to use the hotels and have the robot do it.
- Some commands may require pipetting to more plates than there are pipetting positions, so plates will need to be moved around during the command.
- Sometimes a plate shouldn't be put on the table until the latest possible time
- Sometimes a plate should be removed from the table as soon as possible

## Planning methods

- lifted backwards search for partial order planning in plan space: seems ideal for planning the tasks, but apparently slow.  Can we give it sufficiently small plans to be practical?
- graph plan: also lets us parallelize, but slower than FF.  Requires a propositional representation.
- FF: fastest, but no parallelization?  Probably possible somehow.  Requires a propositional representation.
