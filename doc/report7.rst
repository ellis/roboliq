Liquid Handling Robot
---------------------

Progress Report and Current Questions

Ellis Whitehead

2012-04-04

Progress Since November
-----------------------

* visit to Weizmann
* PCR experiments
* database-centered approach

Database-Centered Approach
--------------------------

* when at Weizmann: scripts usually only have 1 command, maybe up to 3
* for their use caes, the complexity lies in the data to supply to those commands
* use same representation for both data and commands
* YAML - human-readable exchange format for data and programming objects
* output of final AST (inspection, modification)

!!! ADD EXAMPLES

YAML
----

* represents data structures
* like XML but more human-friendly and much easier to convert to internal program data
* can be used for database data, configuration, representing AST
* allows for easier development of a UI, but still allows for use of text-editor

Next Steps
----------

* run primer experiements (Fabian and Markus Uhr)
* 1-year meeting at Weizmann
* more database work
* openBIS database
* automated control of Evoware software
* feedback loops
* portability via import and export?

Import/Export Portability
-------------------------

* For a given set of commands, export all relevant object data along with the commands
* During import, we need to remove data which is specific to the other lab and doesn't fit ours
* How to merge primer data from one lab to another, given conflicting IDs?

Current Questions
-----------------

* biologists to test the robot before I go to Weizmann?
* what do you think of the YAML approach?
* feedback approach?  Feedback on my intended approach

Feedback
--------

History is kept as a list of events rather than a cumulative state
Allows for better analysis of what went on when trying to debug a failed experiment

Run through loops until a conditional branch is encountered
Compile those commands for Evoware
Run that script and wait until execution is finished

* Evoware's capabilities
* Comments in scripts
* Call external program after each step to update database

How to update values when readings are uncertain?

How to evaluate resource usage under uncertain program flow?

...

.. footer::
  Ellis Whitehead, 2012-04-14
