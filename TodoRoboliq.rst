=================
Programming Tasks
=================

YAML
====

:Started: 2012-04-07

Top priority
------------

Goal: Get the new YAML-based code running to the point that we can run a PCR mix again.

#. ``PcrCmd``: create it and get it to work like the old one
#. ``Processor``: handle selection of new pools from the database for ``PcrCmd``
#. ``PcrCmd``: add master mix functionality
#. create script to run PCRs on invitrogen primers

Done:

* accommodate tubes
* ``Processor``: allow user to specify liquids and find their wells in the database
* ``BsseMain``: save evoware scripts to a file
* ``protocol-003``: create script to dilute invitrogen primers
* ``bsse`` module: get it working again in minimal form
* ``evoware`` module: get to compile and run again, though with fewer commands
* ``Processor``: automatically select bench locations for plates
* ``DispenseCmd`` and ``AspirateCmd``: add events
* parser for RoboEase well specification


General
=======

:Started: 2012-04-07

Soon
----

* ``Processor``: allow for more than one tube to be automatically placed on a rack
* ``VesselContent`` and ``Liquid``: create ``equals`` and ``hashcode`` functions
* ``PipetteScheduler``: improve performance
* ``PipetteScheduler``: create a much simpler version using a combinatorial approach -- worry about correctness, not performance
* ``CmdBean``: autogenerate ``doc`` property
* ``EvowareTranslator``: output ``doc`` properties as comments
* ``ObjBase``: ``builder`` should not be accessible from outside -- should only contain "original" states, not any which come from running commands
* ``PipetteCmd``: properly process all parameters, and use top parameters as defaults for item parameters
* ``Processor``: write events to database file to be read by future scripts
* ``Processor``: restructure as described in the section "Command Processing" in NotesRoboliq.rst
* ``Processor``: allow different levels of protocol evaluation: 1) without specific device or robot assignments, 2) with specifics.
* ``Processor``: don't select plate locations unless we have a specific robot
* ``RoboliqYamlBean``: move ``locations`` property to an Evoware bean
* ``Rack``: create Holder, Vessle, Rack, and other concepts defined in the Glossary

Intermediate
------------

* ``ObjBase``: design a better separation of responsibilities between ``ObjBase``, ``Processor``, ``StateQuery``, and ``StateBuilder``.
  For example, it might be a good idea to create a ``StateBase`` which holds the state information that's currently kept in ``ObjBase``.
* ``RobotQuery``: get rid of ``RobotState`` in favor of ``StateQuery``
* ``Result``: adapt monad to accommodate propogation of warnings too, or create a CmdResult
* ``Result``: use it more in code in order to avoid such things as ``if (messages.hasErrors) ...``
* create better, more structured error/warning messages
* ``evoware`` module: add commands to script to export information and process it
* control program for evoware
* read in evoware export data and write results to database
* ``PipetteScheduler``: produce some form of navigatable log (SVG, HTML, CSS, JavaScript, or just RST) in order to make it possible to follow the choices made
* ``VesselContent``: track the cost of stock substances in the vessel
* Remove ``WellStateWriter`` and any other ``*StateWriter`` classes

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

``evoware`` module
------------------

* move as much code as possible from ``bsse`` module to ``evoware`` module
* ``PipetteDevice``: see what code can be moved in from ``EvowarePipetteDevice``

Take care of someday
--------------------

* ``TipState``: simplify it so that it can only hold a single liquid, though contaminants may accumulate
* consider adding structure to ``roboliq.core`` and perhaps using imports in the package object.
* ``WashCmd``: the generic handling should be improved or removed
* Consider using unicode: ℓ for list, σ for set, mu for map, º like prime in haskell, alpha and beta for "numbering"
* YAML: write a converter between SnakeYAML and scala immutable objects
* See about integrating `Scalaz <http://code.google.com/p/scalaz/>`
* ``Liquid``: consider removing it and just using ``VolumeContent``

Unlikely to ever do
-------------------

* YAML: write or get a parser that matches better with scala?


Questions for Fabian
====================

* Any ideas about how to determine when we're allowed to waste an expensive substance by multipipetting?
* Lets review ``VesselContent`` class to see whether it makes sense
