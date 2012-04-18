Users
-----

* Other applications
* Programmers
* Biologists

Udi's Litmus Test
-----------------

Is it useful and usable for actual applications by biologists?

* Does the language offer the functions they want?
* Can they create their own scripts?
* Can they troubleshoot the protocol when the results are not what they expected?

Conservation Law of Complexity
------------------------------

Within a closed user-software-robot system, the complexity of a given task can be shifted among the components but the overall complexity cannot be reduced.

We want to shift more of the complexity to the software

Complexity Shifting 1
---------------------

.. code-block:: text

  ...
  
  DIST_REAGENT2 LB P4:G6+30 BAC_DDW_LIST PIE_TROUGH_AUTAIR
    TIPTYPE:1000,TIPMODE:KEEPTIP
  TRANSFER_LOCATIONS P4:A1+30 P4:G6+30 BAC_SAMP_LIST PIE_BOTAIR
    TIPTYPE:200,MIXBEFORE:PIE_MIX:3x100,MIX:PIE_MIX:3x100

Complexity Shifting 2
---------------------

* Prior RoboEase:

  * Most complexity lies in the parameters and data supplied to the commands
  * All of that information is required to generate a concrete script

* New:

  * read evoware configuration data for bench, plate, tip data (want liquid classes)
  * use database for substance, concentration and location information

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

Complexity Shifting 3
---------------------

.. code-block:: yaml

  commands:
  - !transferAndDilute
    src: P4(A1+30)
    dest: P4(G6+30)
    destConc: 0.05 mmol
    destVolume: 200 ul
    premix: { count: 3 }
    postmix: { count: 3 }

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

Check-Items for a Scripting Language
------------------------------------

* Built-in functions
* Variables (constants)
* User-defined procedures
* Branching

Variables
---------

.. code-block:: yaml

  commands:
  - !context
    vars:
      MYVOL: 20 ul
      MYPLATE: PCR0139
      MYLIQUID: wine
    commands:
    ...

Import and Export for Portability
---------------------------------

* For a given set of commands, export all relevant object data along with the commands
* During import, we need to remove data which is specific to the other lab and doesn't fit ours
* Need to determine which data is lab specific
* Need to merge data from another lab (e.g. assigning substance IDs)

Input: Feedback Loops 1
-----------------------

* Given an AST with conditional branching (but no ``goto``)
* Step through AST until feedback is required
* Compile that section of AST for Evoware
* Run that script and wait until execution is finished
* Then continue process depending on how we branch
* Execution can be suspended and continued later

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
