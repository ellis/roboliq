.. role:: done

=================
Programming Tasks
=================

YAML
====

:Started: 2012-04-07

Top priority
------------

Goal: Get the new YAML-based code running to the point that we can run a PCR mix again.

#. ``evoware`` module: get to compile again, though with fewer commands
#. ``Processor``: intelligently select and track ``location`` separately from ``PlateState``/``TubeState``
#. ``WashCmd``: figure out how to handle this to call our BSSE scripts
#. ``PcrCmd``: create it and get it to work like the old one
#. ``Processor``: handle selection of new pools from the database for ``PcrCmd``
#. ``PcrCmd``: add master mix functionality
#. accommodate tubes

Done:

* ``DispenseCmd`` and ``AspirateCmd``: add events
* parser for RoboEase well specification


General
=======

:Started: 2012-04-07

Soon
----

* ``ObjBase``: ``builder`` should not be accessible from outside -- should only contain "original" states, not any which come from running commands
* ``PipetteCmd``: properly process all parameters, and use top parameters as defaults for item parameters
* ``Processor``: write events to database file to be read by future scripts
* ``Processor``: restructure as described in the section "Command Processing" in NotesRoboliq.rst
* autogenerate ``doc`` property
* evoware: output ``doc`` comments

Intermediate
------------

* ``Result``: adapt monad to accommodate propogation of warnings too, or create a CmdResult
* ``Result``: use it more in code in order to avoid such things as ``if (messages.hasErrors) ...``
* create better, more structured error/warning messages
* ``evoware`` module: add commands to script to export information and process it
* control program for evoware
* read in evoware export data and write results to database
* ``Processor``: allow different levels of protocol evaluation: 1) without specific device or robot assignments, 2) with specifics.

Location handling
-----------------

Develop an intelligent method for determining the location of plates and tubes.
In our case, tubes are easy, because each type of tube has only one rack where it can be placed.
Various plate locations, however, accept a number of different plate models.
In addition, we want to take constraints and preferences into consideration too.
So develop an appropriate algorithm for choosing locations given a set of constraints and preferences.

Then we also need to make the algorithm dynamic, so that it can accommodate both
1) changing constraits over time and
2) the case where there are more plates than locations, and so locations need to be switched during execution

Pre- and post- handling for commands
------------------------------------

* somehow add pre- and post- commands for ensuring valid conditions for the main command
* somehow add pre- and post- conditions for testing whether things are what we think they are

Take care of someday
--------------------

* ``TipState``: simplify it so that it can only hold a single liquid, though contaminants may accumulate
* consider adding structure to ``roboliq.core`` and perhaps using imports in the package object.
