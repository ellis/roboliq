CADMAD
------

Liquid Handling Robot
~~~~~~~~~~~~~~~~~~~~~

:Author: Ellis Whitehead
:Institute: ETH Zurich, BSSE
:Date: 2012-04-15

Liquid Handling Robot Control
-----------------------------

What: a means for going from lab protocol to robot instructions

...Intro

Users
-----

* Other applications (via API or text)
* Programmers (via API or text)
* Biologists (via text)

Udi's Litmus Test
-----------------

Is it useful and usable for actual applications by biologists?

* Does the language offer the functions they want?
* Can they create their own scripts?
* Can they troubleshoot the protocol when the results are not what they expected?

Conservation Law
----------------

The Law of Conservation of Complexity

Within a closed user-software-robot system, the complexity of a given task can be shifted among the components but the overall complexity cannot be reduced.

We want to shift more complexity to the software

Complexity Shifting 1
---------------------

[Look at example RobeEase script (e.g. ``Cloning7_Script08.conf``)]

Complexity Shifting 2
---------------------

* Old RoboEase:

  * The complexity lies in the parameters and data supplied to the commands
  * All of that information is required to generate a concrete script

* New:

  * Read Evoware configuration data
  * Use database for concentration and location information

Complexity Shifting 3
---------------------

[Show examples of new scripts to illustrate how the complexity can conveniently be shifted away from the user]

Exchange Format for Other Software
----------------------------------

* YAML for structured and highly flexible specification data format

YAML Example: Data
------------------

.. code-block:: yaml

  substances:
    SEQUENCE_01: !dna
      sequence: TATAACGTTACTGGTTTCATGAATTCTTGTTAATTCAGTAAATTTTC

  plates:
    E2215:
      model: D-BSSE 96 Well Costar
      barcode: 059662E2215

YAML Example: Commands
----------------------

.. code-block:: yaml

  - !pipette
    src: P1(A01)
    dest: P4(C03)
    volume: 5 ul

Contrast with:

.. code-block:: csv

  pipette,P1(A01),P4(C03),5ul


YAML Example: AST
-----------------

.. code-block:: yaml

  output:
  - command: !pipette
      src: P1(A01)
      dest: P4(C03)
      volume: 5 ul
    doc: pipette 5ul of water from P1(A01) to P4(C03)
    events:
    - P1(A01): !rem {volume: 5e-6}
    - P4(C03): !add {src: P1(A01), volume: 5e-6}

YAML Example: AST (continued)
-----------------------------

.. code-block:: yaml

  ...
    children:
    - command: !aspirate
        items:
        - tip: TIP1
          well: P1(A01)
          volume: 5e-6
          policy: Roboliq_Water_Dry_1000
  ...

YAML Example: Settings
----------------------

.. code-block:: yaml

  plateModels:
    D-BSSE 96 Well PCR Plate: { rows: 8, cols: 12, volume: 200 ul }

  devices:
  - !!roboliq.labs.bsse.PipetteDevice

  commandHandlers:
  - !!roboliq.commands.pipette.AspirateCmdHandler
  - !!roboliq.commands.pipette.DispenseCmdHandler

API for Programmers
-------------------

* Programmers can use the data exchange format
* There is also the DSL for use as shown before

[Show example of library use]

Check-Items for a Scripting Language
------------------------------------

* Built-in functions
* Variables (constants)
* User-defined procedures
* Branching

Progress Since 6th Month
------------------------

* Conversion of many old RoboEase scripts
* Began more PCR-type labwork
* Database-supported protocols

Database-Supported Approach
---------------------------

* Use same representation for data, settings, commands, and intermediate output (AST)
* Facilitates protocol exchange
* Facilitates inspection and debugging

Database-Supported Approach: YAML
---------------------------------

* Represents data structures and programming object
* Semi-human-readable streaming format for storing and exchanging data
* Like XML but much easier to convert to internal program data
* Eases some aspects of potential GUI, while still allowing for use of text-editor

Import and Export for Portability
---------------------------------

* For a given set of commands, export all relevant object data along with the commands
* During import, we need to remove data which is specific to the other lab and doesn't fit ours
* Need to determine which data is lab specific
* Need to merge data from another lab (e.g. assigning substance IDs)

Next Steps
----------

* Run primer experiements
* Optimize PCR
* Construct "parts"
* Access to external databases
* Automated control of Evoware software
* Feedback loops

Input: YAML
-----------

As an exchange format?
How would you feel about writing this?

.. code-block:: yaml

  commands:
  - !pcr
    products:
    - { template: FRP128, forwardPrimer: FRO1259, backwardPrimer: FRO1262 }
    - { template: FRP572, forwardPrimer: FRO1261, backwardPrimer: FRO114 }
    mixSpec: Phusion Hot Start
    sampleVolume: 20 ul

Input: Feedback Loops 1
-----------------------

* Given an AST with conditional branching (but no ``goto``)
* Step through AST until feedback is required
* Compile that section of AST for Evoware
* Run that script and wait until execution is finished
* Then continue process depending on how we branch

Input: Feedback Loops 2
-----------------------

1. Loop1

  1.1. Command1

  1.2. Loop2

    1.2.1 Command2

    1.2.2 Condition2

  1.3 Command3

  1.4 Condidion1

.. footer::

    12 Month Review Meeting, April 17, 2012, Rehovot, Israel
