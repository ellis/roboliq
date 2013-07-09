:Started: 2013-07-08

Data-flow from user-script to Tecan & control scripts

* input protocol is in yaml or json
* the json gets loaded into internal structure
* the json commands produce domain and problem entries for planning (as well as data about pseudo-low-level commands such as "pipette1")
* the domain and problem are parsed by the planner
* possibly re-plan using planner but with plates moved from offsite at the beginning
* planner output is parsed to lower level commands
* lower level commands are compiled into tecan scripts and control instructions

The json has sections for:

* entities
* scopes where variables, default values, and functions can be defined
* commands
* allow loading other json files?

:Started: 2013-06-21

Planning:

* Generate domain files from 1) tecan setup, 2) extra configuration files, 3) protocol
* Generate problem files from 1) tecan setup, 2) extra configuration files, 3) protocol
* Optimize such that the user places all plates onto robot at start of plan rather than waiting till they are needed
* We probably need to provide the problem with an extra set of plates which it can initialize if required
* Convert from SHOP2 to Tecan

Planning devices to get working:

* Two robots
* Centrifuge

:Started: 2013-05-30

Responsive framework for complex computations for Scala 2013 conference.

* Separate repository: put it on github and make a submodule link to it from roboliq

Planning descriptions for:

* Labware selection
* Sealer
* Thermocycler
* Shaker
* Centrifuge
* Filter
* Pipetting

  * Distribute
  * Mixture
  * Mixtures

* Optimize commands (drop unnecessary washes, have user put all plates on table before robot starts)
* Translate planner output to Evoware

:Started: 2013-03-16

For workshop.

* Roboease: show interface for other programs; show commands: several basic (move plates, transfer, post mix, prompt), PCR, custom-programmed
* Quality control: show scripts, graphs, application of data for improving performance; generating new liquid classes
* Protocols: show evoware scripts?
* Configuration: show example of reading in files and outputting yaml/json; automatic device loading

Improvements over roboease:

* work with well groups instead of table positions
* much more information tracked, so many things can be calculated
* data can be culled from multiple sources
* easier to use as a tool by other programs
* extensions using more powerful language (JVM vs perl)

:Started: 2013-03-06

OrangeG 0.08 and 0.8 g/L
* Try to modify CustomLCs.XML
* Find location of relevant files -- copy a bunch of evoware files

in ``database`` are files ``*LCs.XML, EVOWARE.{xml,inf,opt}, Carrier.cfg``

* DistributeCmd
* Well groups
* Converter from YAML to JSON for more easily writable script files
* Move some code from roboliq.processor and roboliq.events to roboliq.core so that command handlers only need to import roboliq.core._
* Maybe do something to put roboliq.events classes into roboliq.entity?
* Create package roboliq.device.pipette.planner and put planning/scheduling classes in there
* Load handlers from YAML (and add YAML to Config01)
* Make DbTable and DbField classes or typeclasses to handle database objects more gracefully
* Refactor TipState member names
* Move roboliq.commands to roboliq.device, arm => transport, devices => device

:Started: 2013-02-12

* ConversionHandler should use the same method for accessing parameters as ComputationHandler
* Be able to convert a list parameter to a list of referenced objects
* For conversions, automatically recognize lists and create an appropriate converter, rather than requiring a list converter to be registered.
* Consider adding min and max element count to KeyClass in order to treat all values as vectors
* Load in database json
* Use yaml only for configuration
* Put tecan configuration in yaml file
* Read more tecan configuration from tecan files

Earlier stuff:

* Why does Transfer_Rack use RoMA1 when moving from regrip to reader?
* Setup test framework for `base`
* Get rid of Map[LM, Item] and family, only using Map[Item, LM]
* Add AI search methods to roboliq
* Improve pipetting yaml
* Use TipModelSearcher1 in TipModelChooser.chooseTipModels_Min
* Use new, simpler pipetting methods
* Add automatic pipetting plan builder


http://netbeanside61.blogspot.com/2011/06/downloading-openjdk7-binary-for-mac-os.html

* Pipette into single well (check for bleed)
* Dilution in one row (check for bleed and linearity)

1:10000 is the highest concentration
That can be further diluted 1:10000

will use a buffer as solvent, diluted perhaps 1:20
Excite at 460, Read out at ~540, with the monochromator

Use upper reader
Reader:
Black plate: Nunclon 96 Flat White


======================
Statistics Experiments
======================

:Started: 2012-12-14

*Variables*:

:d: requested dispense volume
:v: actual volume
:a: absorbtion measurement
:z: z-level measurement

* $d$ to $a$
* $d$ to $z$
* absorption calibration by measuring shape of liquid surface at various volumes
* absorption calibration by dispensing 200ul with a calibrated pipette into each well
* low volume z-detection to see what the minimum volume for detection is
* multiple z-level testing to see whether the z-level keeps decreasing

From the above experiments we want to obtain the following.

* calibration curve for $z$ to $v$

-----
Tests
-----

* Fill all wells with 200ul in random order; check z level after each dispense; then randomly check each well with each tip, randomly
* Dispense 96 levels (perhaps 3-99ul) randomly to find a minimum volume for detection
* Dispense 50-200 ul randomly (8 levels), while checking z levels, then dispense 3-200ul (12 levels) in random order to each of the initial 8 levels.  This is factorial, 8*12.
* Dispense 50ul randomly to all wells; then randomly create wells of repeated 3, 5, 10, 20, 50, 100 dispenses until well volume would exceed 360ul.
* Dispense 50-200 ul 0.08 dye randomly (16 levels), then surface scan, then fill each well to 200ul with water, then surface scan

Repeat for air/wet, single/multi dispense, large/small tips, other plates (PCR, DWP), various washing parameters, various liquid classes?

50-200, 8 levels: [50, 61, 74, 91, 110, 135, 164, 200]
3-200, 12 levels: [3, 4, 6, 9, 14, 20, 30, 43, 64, 93, 137, 200]

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

Example
-------

diluents:
- liquid: FlorA
  diluent: BufferA(1/20)

preparations:
- liquid: FlorA(1/10000)
  volume: 50ul
  well: P2(F01)

We have FlorA stock and BufferA stock.
In P2(F01) we need 47.495ul water, 2.5ul BufferA, 0.005ul FlorA
The smallest volume we can use is 0.5ul, so we need to create an intermediate sample of diluted FlorA_a.
So FlorA_a must have concentration of at most 0.5*C_a/50 = 1/10000 => C_a = 1/100

Basic scenarios to test
-----------------------

===  ===  ===  ===  ===  ===  ===
D1   D2   D3   S1   S2   S3   S4
===  ===  ===  ===  ===  ===  ===
AB   AB        A    B
AB   ABC       A    B    C
AB   ABC       A    AB   AC
ABC  ABD       A    B    C    D
ABC  ABD       A    AB   AC   AD
ABC  ABD  ACD  A    B    C    D
===  ===  ===  ===  ===  ===  ===

Also test the dilution scenario described above, as well as other dilution requirements.

Approach by example
-------------------

First choose a set of sources which is independent in their contents substances.
This might get rid of some mixtures and some dilutions.

Sources
~~~~~~~

S1: A
S2: AB(20)
S3: AC(1M)
S4: AD(1M)

Dests
~~~~~

D1: 100ul AB(100)C(0.1M)
D2: 100ul AB(100)D(0.1M)

Dest Contents
~~~~~~~~~~~~~

Sort contents with smallest volumes first, because those are the ones which could benefit most from cominations (has a higher impact on homogeneity).

D1: 10ul S3, 20ul S2, 70ul S1
D2: 10ul S4, 20ul S2, 70ul S1

Combos 2
~~~~~~~~

D1:
  S23(2:1)
  S13(7:1)
  S12(7:2)
D2:
  S24(2:1)
  S14(7:1)
  S12(7:2)

Combo Counts 2
~~~~~~~~~~~~~~

S12(7:2) = 2

Conclusion
~~~~~~~~~~

Create temporary well S12(7:2) and use it for the appropriate destinations.


Another example
---------------

Sources
~~~~~~~

Need to start with a list of linearly independent sources, or at lease choose a set of linearly independent sources for each destination.

S1: A
S2: AB(20)
S3: AC(1M)
S4: AD(1M)
S5: AE(1M)

Dests
~~~~~

D1: 100ul AB(100)C(0.1M)D(0.1M)
D2: 100ul AB(100)C(0.1M)E(0.1M)

Dest Contents
~~~~~~~~~~~~~

Sort contents with smallest volumes first, because those are the ones which could benefit most from combinations (has a higher impact on homogeneity).

D1:
  S3 10ul
  S4 10ul
  S2 20ul
  S1 60ul
D2: 10ul S3, 10ul S5, 20ul S2, 60ul S1

Combos 2
~~~~~~~~

D1:
  S34(1:1)
  S32(1:2)
  S31(1:6)
  S42(1:2)
  S41(1:6)
  S21(2:6)
D2:
  S35(1:1)
  S32(1:2)
  S31(1:6)
  S52(1:2)
  S51(1:6)
  S21(2:6)

Combo Counts 2
~~~~~~~~~~~~~~

S32(1:2) = 2
S31(1:6) = 2
S21(2:6) = 2

Combos 3
~~~~~~~~

D1:
  S321(1:2:6)
D2:
  S321(1:2:6)

Combo Counts 3
~~~~~~~~~~~~~~

S321(1:2:6) = 2

Decide on pipetting strategy for dests
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Check the combos in this order for dispense into destinations:
  S321(1:2:6)
  S32(1:2)
  S31(1:6)
  S21(2:6)

D1: (sorted by volume descending)
  S321(1:2:6) 90ul
  S4 10ul

Then go through the combos and decide on order for dispense from original sources and smaller combos in the same manner as for the dests.

Minimal volume check
~~~~~~~~~~~~~~~~~~~~

Now check all dispensed volumes to see whether they are larger than the minimum allowed volume.
If any are too small, dilute the releveant source well (may be a source or a combo) somehow and rerun part or all of the calculation with the new dilution.


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


Terminology Choices
===================

* Vessel or plate location on the robot bench: Location, Platform, Dock, Station, Address, Bay, Site, Port, Position


Questions for Fabian
====================

* Any ideas about how to determine when we're allowed to waste an expensive substance by multipipetting?
* Let's review ``VesselContent`` class to see whether it makes sense
