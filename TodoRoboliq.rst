scala> com.github.rjeschke.txtmark.Processor.process("2^2^ = 4")
res2: java.lang.String = 
"<p>2^2^ = 4</p>
"


* Pipette into single well (check for bleed)
* Dilution in one row (check for bleed and linearity)

1:10000 is the highest concentration
That can be further diluted 1:10000

will use a buffer as solvent, diluted perhaps 1:20
Excite at 460, Read out at ~540, with the monochormator

Use upper reader
Reader:
Black plate: Nunclon 96 Flat White



=================
Programming Tasks
=================

:Started: 2012-05-18

* doc: add a Markdown docstring for multi-line docs
* doc: for VesselContent, create single-line and multi-line doc strings too
* doc: for VesselContent, multi-line doc strings should show concentrations
* doc: when listing liquids or wells in the short doc, list a maximum of 3 -- if there are more show first, elipsis, last
* doc: PipetteCmd: for long doc, make a table of src, dest, vol
* doc: wells to string: add ellipsis function
* doc: give liquids display names (e.g., dntp => dNTP)
* debug why final well volume in is 29.997 instead of 30
* doc: round numbers to 3 significant digits
* Create simpler pipetting methods
* Make master mix
* Get whole PCR command working again

Done:

* doc: when we have multiple volumes, don't list volume at all for short doc
* doc: add fields for single-line and multi-line doc strings
* doc: wells to string: a sequence of wells from the same plate should be in PLATE(...)


Declarative Liquid Preparation
==============================

:Started: 2012-05-24

I'd like to have a declarative approach to preparing liquids.
The user specified the final liquid, and roboliq figures out everything else.

liquid: FlorA(1/10000)+BufferA(1/20)
volume: 50ul
well: P2(F01)


YAML
====

:Started: 2012-04-07

Top priority
------------

Goal: Get the new YAML-based code running to the point that we can run a PCR mix again.

* ``PipetteCmd``: implement more robust (and less "intelligent") pipetting approaches
* ``PcrCmd``: create it and get it to work like the old one
* ``PcrCmd``: make sure wells are mixed
* ``PcrCmd``: seal plate
* ``PcrCmd``: transfer plate to thermocycler
* ``PcrCmd``: run thermocycler
* ``Processor``: handle selection of new pools from the database for ``PcrCmd``
* ``PcrCmd``: add master mix functionality
* create script to run PCRs on invitrogen primers

General
=======

:Started: 2012-04-07

Soon
----

* ``Processor``: allow for more than one tube to be automatically placed on a rack
* ``VesselContent`` and ``Liquid``: create ``equals`` and ``hashcode`` functions
* ``VesselContent``: name ``liquid`` by solute conc and solvent percentage (leave out water unless it's the only content) (e.g. "salt(5μmol)+oil(5%)")
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
* ``Bean`` and ``CmdBean``: refactor classes so that Bean has CmdBean's toString but no _id, and add a class BeanWithId
* ``Liquid``: make this more like VesselContents, just wit volume
* ``Liquid``: can ``id`` be made into a ``val`` now instead of a ``var``?
* ``Liquid``: can I get rid of ``def +`` in favor of ``VesselContent.+``?

Done:

* ``Liquid``: rename sName to id


Intermediate
------------

* ``ObjBase``: design a better separation of responsibilities between ``ObjBase``, ``Processor``, ``StateQuery``, and ``StateBuilder``.
* figure out a way to include other yaml files so that protocols can be made more self-contained for the purposes of testing; we may want to let the individual includes be overridden, however, so it's not so simple -- for example, we might want to test out a new class file on a whole set of protocols to see whether it works
that we can test out a new 
  For example, it might be a good idea to create a ``StateBase`` which holds the state information that's currently kept in ``ObjBase``.
* ``RobotQuery``: get rid of ``RobotState`` in favor of ``StateQuery``
* message handling:

  * ``CmdMessageWriter``: this was a bad idea, get rid of it
  * ``Result``: adapt monad to accommodate propogation of warnings too, or create a CmdResult
  * ``Result``: use it more in code in order to avoid such things as ``if (messages.hasErrors) ...``
  * create better, more structured error/warning messages, including handling nested property names and list indexes

* ``evoware`` module: add commands to script to export information and process it
* control program for evoware
* read in evoware export data and write results to database
* ``PipetteScheduler``: produce some form of navigatable log (SVG, HTML, CSS, JavaScript, or just RST) in order to make it possible to follow the choices made
* ``PipetteScheduler``: improve performance
* ``VesselContent``: track the cost of stock substances in the vessel
* Remove ``WellStateWriter`` and any other ``*StateWriter`` classes
* ``bsse`` module: move almost all the code to ``base`` or ``evoware``, and use the yaml classes file to load any ``bsse`` classes we need
* mixing:

  * add Vessel function to determine whether the vessel is currently mixed
  * ``PcrCmd``: smart mixing: decide whether final dispense caused sufficient mixing, or whether enough time has elapsed for mixing to have occured spontaneously, or whether to mix immediately after every final dispense, or mix after all dispenses have finished for all wells, or to seal and shake.
  * ``PcrCmd``: use AI somehow to make the mixing/sealing/shaking decisions.  That is, we know that the plate should be sealed and mixed prior to entering the thermocycler -- find the cheapest path to reach that goal.

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
* Consider using unicode: ℓ for list, rho for reverse list, σ for set, μ for map, º like prime in haskell, α and β for "numbering"
* YAML: write a converter between SnakeYAML and scala immutable objects
* See about integrating `Scalaz <http://code.google.com/p/scalaz/>`
* ``Liquid``: consider removing it and just using ``VolumeContent``

Unlikely to ever do
-------------------

* YAML: write or get a parser that matches better with scala?


Questions for Fabian
====================

* Any ideas about how to determine when we're allowed to waste an expensive substance by multipipetting?
* Let's review ``VesselContent`` class to see whether it makes sense
