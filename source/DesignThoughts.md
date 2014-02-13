# Design Thoughts

## From protocol to executable script

Date: 2014-02-03

- The protocol lists a sequence of top-level commands.
- Each top-level command may have multiple possible agent/method alternatives.
- Each top-level command is decomposed into either another top-level commands or a set of alternative methods.
- The user is given the choice of which of the alternative methods to use (defaulting to the first option)
- This gives us an ordered chain of operators for planning.
- The operators may have pre-requisites, which can be fulfilled at any time before the operator.
- The operators also may have unbound variables.
- So the output of the first processing stage of a protocol is a partial plan, containing a partially ordered list of operators and a partial variable binding map.

## Placing plates on the table

Date: 2014-02-13

Complications:
- Some commands may require pipetting to more plates than there are pipetting positions, so plates will need to be moved around during the command.
- Sometimes a plate shouldn't be put on the table until the latest possible time
- Sometimes a plate should be removed from the table as soon as possible
