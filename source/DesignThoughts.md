# Design Thoughts

## From protocol to executable script

Date: 2014-02-03

- The protocol lists a sequence of top-level commands.
- Each top-level command may have multiple possible agent/method alternatives.
- Each top-level command is decomposed into either another top-level commands or a set of alternative methods.
- Once the methods are expanded, we have an ordered chain of operators for planning.
- The operators may have pre-requisites, which can be fulfilled at any time before the operator.
- The operators also may have unbound variables.
- So the output of the first processing stage of a protocol is a partial plan, containing a partially ordered list of operators and a partial variable binding map.

