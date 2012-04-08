.. role:: done

Programming Tasks
=================

YAML: Started 2012-04-07
------------------------

Top priority
~~~~~~~~~~~~

Goal: Get the new YAML-based code running to the point that we can run a PCR mix again.

#. ``evoware`` module: get to compile again, though with fewer commands
#. ``Processor``: intelligently select and track ``location`` separately from ``PlateState``/``TubeState``
#. ``PcrCmd``: create it and get it to work like the old one
#. ``Processor``: handle selection of new pools from the database for ``PcrCmd``
#. ``PcrCmd``: add master mix functionality

Done:

* ``DispenseCmd`` and ``AspirateCmd``: add events
* parser for RoboEase well specification

Soon
~~~~

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

Take care of someday
--------------------

* ``TipState``: simplify it so that it can only hold a single liquid, though contaminants may accumulate
