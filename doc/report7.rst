Liquid Handling Robot
---------------------

Progress Report and Current Questions

:Author: Ellis Whitehead
:Department: D-BSSE
:Date: 2012-04-04

.. raw:: pdf

  PageBreak slidePage

Progress Since November
-----------------------

* visit to Weizmann
* PCR experiments

  - with and without master mix
  - sample volumes between 20ul and 50ul
  - TAQ and Phusion Hot Start
  - small and large tips for pipetting

* database-supported scripts

Database-Supported Approach: Motivation
---------------------------------------

* At Weizmann, their scripts usually only have 1 to 3 commands
* And yet they are difficult to program!
* The complexity lies in the parameters and data supplied to the commands

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
  - !pipette { src: P1(A01), dest: P4(C03), volume: 5 ul }
  - !pipette [ P1(A01), P4(C03), 5 ul ]


YAML Example: AST
-----------------

.. code-block:: yaml

  output:
  - command: !pipette
      src: P1(A01)
      dest: P5(C03)
      volume: 5 ul
    doc: pipette 5ul of water from P1(A01) to P4(C03)
    events:
    - P1(A01): !rem {volume: 5e-6}
    - P4(C03): !add {well: P1(A01), volume: 5e-6}
    - TIP1: !flag {dirty: true}

YAML Example: AST (continued)
-----------------------------

.. code-block:: yaml

  ...
    translations:
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

Next Steps
----------

* Run primer experiements (Fabian and Markus Uhr)
* 1-year meeting at Weizmann
* Complete database-supported approach
* openBIS database
* Automated control of Evoware software
* Feedback loops
* Portability via import and export

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

* Center heading
* More natural font?
* Black background?

.. footer::

    ###Page### of ###Total###
