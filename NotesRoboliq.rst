=============
Roboliq Notes
=============
:Author: Ellis Whitehead <ellis.whitehead@bsse.ethz.ch>

``CmdResult``
=============

:Date: 2012-04-08

These are thoughts about creating a monad for handling well-structured messages propogation (errors and warnings) when processing commands.

``CmdSuccess[A](x: A)``
  for a successful calculation

.. code-block:: scala

  for {
    _ <- check1 // should accumulate messages and continue to next check
    _ <- context param "blah" check2 // give messages context of parameter "blah"
    var1 <- getOrNull // try to get value, return null on error, but continue
    _ <- fail if error
  } yield {
    ...
  }

  for {
    lItem <- CmdResult.sequence(cmd.items.toList.map(_.toTokenItem(ctx.ob)))
    lEvent <- CmdResult.sequence(lItem.map(expandItem)).map(_.flatten)
  } yield {
    Expand2Tokens(...)
  }

Command Processing
==================

:Date: 2012-04-07

This idea is not currently implemented.

Commands can have one of several different data requirements before they can be processed:

* no requirements beyond their own parameters
* knowledge of the *invariant properties* of objects referred to by the parameters
* knowledge of the *state* of objects referred to by the parameters

A list of commands should be processed in the order deteremined by a priority stack.
We start with a list of commands given to use by the user.
Each of the commands is given an index according to its position in the list.
The commands are pushed onto the priority stack.
The priority stack orders commands by lowest data requirements first, and then lexigraphical order of index.
Note that this ensures that no command which requires state information will be processed before the state information has become fixed for that command.
When a command gets processed, it may produce new child commands.
These commands are given the index of their parent plus a subindex according to their position in the child list.
The children are then pushed onto the priority stack, and the next command is selected from the priority stack.

:Date: 2012-04-10

The command ``Processor`` will need to choose specific wells when a liquid resource is requested.
In order to do this properly, it should know how much of that liquid is required, so that it can choose an appropriate number of source wells if they are available.

The Pipette command needs to know the following information before it can constuct it's pipetting plan:

* the ``PipetteDevice`` to use (this is a class which makes many choices about how pipetting is done, and is particular to each lab)
* the wells chosen for source liquids
* the locations of tubes (so that it can treat them like wells on a plate and potentially aspirate or dispense to adjacent tubes simulaneously)


Format For Wells
================

Many commands have source and destination parameters which refer to a well or set of wells.  For the following, assume that P1 is a 96-well plate with 8 rows and 12 columns.

``P1``
  All wells on plate P1 or a tube if P1 is a tube
``P1(A01)``
  The well in row A and column 01 on plate P1
``P1(A01,B04)``
  2 wells A01 and B04 on plate P1
``P1(A01 d B02)``
  10 wells starting at A01 and extending vertically downward till well B02 is reached, wrapping back around to the top row whenever necessary
``P1(A01 r B02)``
  14 wells starting at A01 and extending horizontally rightward till well B02 is reached, wrapping back round to the left colume whenever necessary
``P1(A01 r 04)``
  4 wells in row A columns 1 thru 4 on plate P1
``P1(A01 d B)`` or ``P1(A01dB)``
  2 wells in column 1 rows A thru B on plate P1
``P1(A01 x C12)``
  24 wells in rows A thru C and columns 1 thru 12
``P1(A01 * 4)``
  Well A01 selected 4 times
``P1(A01),P2(D04)``
  2 wells, A01 on plate P1 and D04 on plate P2

Limitations
===========

* Tube locations must be determined prior to execution of the script, and we assume that they will not be moved later on [2012-04-09]

Liquid Preparation
==================

* insufficient volumes
* could not generate the requested concentrations

Pipetting Algorithms
====================

:Date: 2012-04-12

These are thoughts on a new algorithm for pipetting.
The current algorithm is too fragile, because it tries to make a lot of decisions to cut down on the number of possibilities it needs to search through.
I'd like to create several other algorithms as references:

* a very simple one which makes as few decisions as possible and yet produces reasonable results for a certain set of pipetting scenarios.
* a combinatorial algorithm which looks through a lot of possible pipetting approaches, but doesn't try to do so very intelligently.  It's performance may be slow, as long as it's output is not incorrect.
* specialized algorithms for particular scenarios, such as distributing a single liquid to a set of cells
* finally, I might try to create a "smart" algorithm

Simple Algorithm
----------------

Characteristics of this algorithm are:

* uses a single tip model, and raises an error if that's not possible
* by default, performs one dispense per tip

Basic Pipetting Methods
-----------------------

Ultimately, the robot must pipette specific volumes of liquid.
These volumes can either be given explicitly or calculated from concentration specifications.
You can use the following means of specifying volume or concentration:

* by source volume: distribute explicit volumes
* to source conc: distribute enough to achieve a target concentration of the source liquid in the target well
* to dest volume: distribute enough to to reach a target volume in the target wells
* to dest conc: distribute enough to achieve a target concentration of the substance in the target well

Distribute:
  Transfer a liquid from a set of source wells (the source wells must all contain the same liquid) to a set of destination wells (the order in which the destination wells are added may or may not matter).
  May or may not need to premix/postmix.

Transfer:
  Transfer from list of source wells to a list of destination wells (order is preserved).

Mixture:
  Create a mixture of various source liquids in an empty target well.
  A mixture is a multilayered sequence of distibutes and transfers in which the volumes that are calculated from concentration specifications at each step take the final volume into consideration after all layers have been transfered.
  At least one volume must be specified, either for a source liquid or the target volume.
  A solvent (water, by default) will be used to fill any additional volume required to achieve the specified concentrations.
  Each source may require premix.
  The final mixture may require postmix.

Mixin:
  Mix sources into an existing well.
  Either the total target volume *or* the desired concentration the target's original substance can be specified, but not both.
  A solvent (water, by default) will be used to fill any additional volume required to achieve the specified concentrations.
  
Specialized Pipetting Methods
-----------------------------

Dilute (In-Place):
  Distribute a solvent to achieve a certain a target concentration or target volume of the substance in the target wells.

Distribute+Transfer:
  Distribute a solvent at a given volume and then perform transfers at given volume

Dilution:
  In target wells, create mixtures with uniform volumes and concentrations.
  So distribute a solvent at appropriate volumes and then perform transfer of source wells at appropriate volumes.

Copy:
  A form of transfer in which the destination well is empty

Copy with Dilution:
  Distribute a solvent to empty wells and then perform transfer
  Possible optimization: we often need to postmix -- this can sometimes be achieved by dispensing the solvent last.

Serial Dilution:

Gradient:

Mixture:

Combinatorial Mixture:

Parsing steps for pipetting command
-----------------------------------

#) Filter out items with 0 volumes.
#) Determine tip model for each item.
#) Divide items with excessive volumes into multiple items.
#) Group items into cycles (the method for grouping should be exchangable, as the various methods described above)
#) Optimize when tip cleaning is performed

Grouping into cycles consists of multiple components.

* A function to determine whether the next item can be added to a cycle (i.e. whether the robot can handle it).
* A function to determine whether we want to add that item to the current cycle.
* A cost function? I'm not sure how this would work, since cleaning should be optimized before calculating cost.
* A search algorithm to find a path to the solution (e.g. greedy, A*).

Tip Handling
============

:Date: 2012-04-18

We have three different tip usage scenarios:

* tips are permanent and must be washed instead of disposed
* tips are disposed of after use
* used tips may be temporarily set aside and later used again

Optimizing the first scenario involves minimizing the number of washes, because washing takes a long time.

Optimizing the second scenario is more of a challenge, because there are sometimes two competiting costs: for some procedures, the more tips you use, the lower your time cost, but the higher the tip cost.
I don't have a solution for this at this time, and use the same approach as for permanent tips.

At this time, I am not considering the third scenario.


Multipipetting
==============

:Date: 2012-04-18

Multipipetting requires our robot to aspirate additional liquid beyond what actually gets dispensed.  This waste is sometimes not permissible.


Doc generation
==============

:Date: 2012-05-21

When generating documentation, we need to accommodate two distinct requirements:
1. a single line of plain text documentation
2. short markdown documentation that is probably only one line long
3. more detailed markdown documentation that may extend over multiple lines

For that purpose, various components of roboliq will need to supply or generate the following:
1. Names in plain text
2. Names in markdown
3. Single lines of plain text
4. Multiple lines in markdown

``Liquid`` names
----------------

The ``Liquid`` class has the following properties that are related to documentation.

:``id``:
  A string ID which uniquely identifies the liquid's type.
  Two liquid objects with the same ID can be treated as equivalent.
:``nameShort_?``:
  An optional human-friendly name for the liquid.
:``doc``:
  A Markdown 


Glossary
========

Substances
----------

:Date: 2012-04-10

``Substance``
  Represents a material which can be placed in a ``Well``.
``Liquid``
  A ``Substance`` in liquid form and can be pipetted.
``Powder``
  A ``Substance`` in dry form, cannot be pipetted.
``Cell``
  A ``Substance`` composed of cells
Solvent
  A ``Liquid`` which is added to a ``Vessel`` in order to suspend a ``Powder`` or ``Cell``

Substance containers
--------------------

:Date: 2012-04-10

``Well`` (rename to ``Vessel``?)
  Container for a substance.
``PlateWell`` (rename to ``Well``?)
  A ``Well`` on a ``Plate``.
``Tube``
  A ``Well`` which can be placed in a ``Rack``.
``Holder`` (not currently used)
  An object that can hold wells in a row/column format.
``Plate``
  A ``Holder`` with permanent ``PlateWell`` wells.
``Rack``
  A ``Holder`` with removable ``Tube`` wells [not actually used in the code at this time].

