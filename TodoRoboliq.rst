.. role:: done

Programming Tasks
=================

YAML: Started 2012-04-07
------------------------

Top priority
~~~~~~~~~~~~

Goal: Get the new YAML-based code running to the point that we can run a PCR mix again.

#. ``DispenseCmd`` and ``AspirateCmd``: add events
#. ``PcrCmd``: create it and get it to work like the old one
#. ``Processor``: handle selection of new pools from the database for ``PcrCmd``
#. ``evoware`` module: get to compile again, though with fewer commands
#. ``Processor``: intelligently select and track ``location`` separately from ``PlateState``/``TubeState``
#. ``PcrCmd``: add master mix functionality

Done:

* parser for RoboEase well specification

Soon
~~~~

* ``PipetteCmd``: properly process all parameters, and use top parameters as defaults for item parameters
* ``Processor``: write events to database file to be read by future scripts
* autogenerate `doc` property
* evoware: output `doc` comments

Intermediate
------------

* ``evoware`` module: add commands to script to export information and process it
* control program for evoware
* read in evoware export data and write results to database

Take care of someday
--------------------

* ``TipState``: simplify it so that it can only hold a single liquid, though contaminants may accumulate
