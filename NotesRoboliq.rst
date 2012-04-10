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
