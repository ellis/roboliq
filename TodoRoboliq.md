Started 2013-07-25

Current big steps:

- [ ] Get pipetting command to work
- [ ] Get pipeline working to compile a roboliq protocol on disk to an evoware script
- [ ] Create server/clients to monitor and control protocol execution
- [ ] Use simpler entities and a different method for querying state, one that is compatible with planning methods
- [ ] Start using sqlite database for storing relevant information
- [ ] Change domain to have a more generic concept of labware which subsumes sites, tubes, and plates.
- [ ] New command structure: preconditions leading to search through variable space, preconditions leading to search through actions to achieve that state, further task breakdown, add and delete lists for state

Details for while at Weizmann:
- [ ] Q: is there danger of running my scripts with regard to having enough tips?
- [ ] PipetteSpec: allow user to manually specify tip model
- [ ] read protocol from json file
- [ ] run jshop on our lisp output from within JshopMain
- [ ] read jshop operators from a .jshop file
- [ ] good cleaning command
- [?] add 'Decon' plate to bench.esc
- [?] domain: peeler
- [?] Protocol: peeler
- [?] JshopTranslator: peeler
- [?] EvowareClientScriptBuilder: peeler
- [ ] Protocol: pipetter mix
- [ ] JshopTranslator: pipetter mix
- [ ] EvowareClientScriptBuilder: pipetter mix
- [ ] mix after dispense of taq
- [ ] possibly take plate out of thermocycler
- [ ] tell user to centrifuge
- [ ] PipetteSpec: user manually specifies tip handling
- [ ] JshopTranslator: distribute: generate clean command
- [ ] JshopTranslator: need much better tip cleaning algorithm
- [ ] start testing PCR protocol with pipetting, sealing, thermocyling (without centrifugation or have user centrifuge)
- [ ] Protocol: 'wellContents.name' => 'wellContents.well'
- [ ] create PCR command
- [ ] run PCR protocol on our robot
- [ ] consider adding task for "bench-setup-labware" to place labware onto the robot bench before other tasks
- [ ] handle tubes
- [ ] Protocol: when a plate is given an initial position, enter this information into the WorldStateBuilder
- [ ] JshopTranslator: intelligently select liquid class (especially air, bot, wet contact)
- [ ] Protocol: section for lab-specific variables and their values (possibly use aliases)
- [x] figure out why prewashing is being performed more than once
- [x] drop tips after last dispense
- [x] merge PippeteTipsRefresh items (washes should not happen twice in a row)
- [x] wrong tip model name is given as "50ul" for GetDITI2
- [x] get name of new sealy computer: pcrobot
- [x] ?: Wash command in roboease scripts has "1, 1000" at end, whereas we have "1000, 0". We decide this can be safely ignored.
- [x] get sealer Carrier.cfg, a bench ESC, the liquid class files, and more ini files
- [x] PipettePlanner: make sure we are configured to be able to use both BSSE small and large tips
- [x] seal: use _red now instead of _blue
- [x] PipetteSpec: user manually specifies liquid class name
- [x] L0C_Spirate: decimal point in volume is displayed as a comma on my mac
- [x] pipetting: large tips are being chosen for small volumes instead of small tips
- [x] Distribute task test: distribute from multiple wells
- [x] Distribute task test: distribute to multiple wells
- [x] Distribute task test: use multiple plates
- [x] JshopTranslator: thermocycler-run
- [x] EvowareClientScriptBuilder: ThermocyclerRun
- [x] start testing more complete protocol to perform various PCR-related tasks: pipette, seal, thermocycle
- [x] Protocol: add configurable thermocycler specs
- [x] JshopTranslator: thermocycler-close
- [x] EvowareClientScriptBuilder: ThermocyclerClose
- [x] JshopTranslator: thermocycler-open
- [x] EvowareClientScriptBuilder: ThermocyclerOpen
- [x] EvowareClientScriptBuilder: SealerRun
- [x] JshopTranslator: sealer-run
- [x] Protocol: config for sealer specs
- [x] Protocol: config for (sealer device + plate model) -> spec
- [x] domain: thermocycler
- [x] Protocol evoware: setup thermocyclers
- [x] Protocol: add "thermocycle" command in loadJson()
- [x] EvowareClientScriptBuilder: PipetterDispense

First tasks for back at BSSE:
- [ ] Urs: should the centrifuge be at grid 54 or 55?  It is at 54 in the Empty template, but 55 in my scripts
- [ ] create a better bench file (correct centrifuge grid?, Peeler, Sealer, additional trough and hotels)
- [ ] Allow for a carrier model to be on bench multiple times, like for WIS external 5Pos hotels in carrier-orig.cfg

Restructure commands after Weizmann trip:
- ProtocolCommand: a case class for the command written in a json or yaml protocol
- ProtocolCommandHandler: translates a ProtocolCommand to a list of ProtocolCommands and ShopCommands, as well as internal map entries
- ShopCommand: a logical Rel representating a method or operator for processing by jshop
- Operator: the results of jshop processing
- OperatorTranslator: 

Details for later:
- [ ] EvowareClientScriptBuilder: PipetterTipsDrop
- [ ] EvowareClientScriptBuilder: PipetterTipsGet
- [ ] see what information we can get about tips and the LiHa from evoware configuration files
- [ ] RsResult: add a 'prefix' or 'context' which get prefixed to warning and error messages
- [ ] domain: shaker
- [ ] Protocol: shaker
- [ ] JshopTranslator: shaker
- [ ] EvowareClientScriptBuilder: shaker
- [ ] Protocol.loadEvoware: have it return an RsResult, and catch errors that may occur in the function
- [ ] instructions to turn on cooler and pump for PCR protocols
- [ ] ensure that PCR plates are on cooled carriers
- [ ] re-import TipEvents
- [ ] rename PipettePosition to PipetteContact, with values Air, Wet, Dry
- [ ] PipetteSpec: when user manually specifies liquid class name, verify whether that class exists, and automatically find its contact type (air, wet, dry).  Should start using EvowareLiquidClassParser.
- [ ] peeler
- [ ] pipetting and protocol: let liquid amounts be specified in concentrations
- [ ] protocol: possibly let the user specify which wells should be considered as sources when searching for substances for creating mixtures
- [ ] domain and Protocol: (agent, device, spec, site, model) needed to know whether a device can operate on a given labware.  Some of these may be irrelevant or orthogonal, however.  For each device type, have something in the config determine which relationships must be specified.
- [ ] domain: as an example of the above, there is no reason to have 'device-can-model' or 'device-can-spec' for sealer, since we have the sealer spec.  Use 'device-spec-can-model' instead.
- [ ] domain: use ?l for labware instead of ?p (which used to mean 'plate')
- [ ] Protocol: evoware objects are mapped to using idents as keys; change this to using entities where possible.  Then the Command classes can be easily changed to use entities instead of idents.
- [ ] PipettePlanner: more pipetting methods, allow multi-pipetting
- [ ] PipettePlanner: allow for dispensing large volumes by multiple aspirations/dispenses
- [ ] more sophisticated customization pipetting tasks, allowing user to specify pipette policy, multipipetting, tip size, tip handling, etc
- [ ] Protocol: handle substances
- [ ] Protocol: handle mixtures
- [ ] Protocol: handle wells with aliquot contents
- [ ] function from Aliquot + events => Aliquot
- [ ] Extend JshopMain, Protocol, EvowareClientScriptBuilder to handle a pipette command
- [ ] WashProgram: config for specifying wash programs; see also TipCleanEvent
- [x] JshopTranslator: distribute: generate dispense command
- [x] adapt VesselState, VesselContents for new system
- [x] TipWell.equidistant3: figure out where to calculate row/col from platemodel and well index
- [x] EvowareTranslator: let it translate one command at a time and produce multiple scripts

Substance, Liquids, WellContents, WellHistory:

We can take the Substance class from before.  This represents the basic substances which
may be used in protocols, either directly or in mixtures.

The are a number of possible unknowns in a mixture.  We might, for example, know the volume of the diliuter,
but not the amount of dilutee (e.g., someone puts some sugar in tee).  Or we may know the relative
concentrations of various substances, but not how much of the mixture we have.

In order to handle these cases, we probably need to trace the mixtures in various wells.
That is, a mixture may be composed of volumes of other mixtures taken from specific wells,
where the volume of each sub-mixture is subject to uncertainty.

We probably don't want to get too elaborate here, though, insofar as time may change a mixture,
and such changes shouldn't generally be propagated.

And of course, each pipetting action has a degree of uncertainty.

What about wells that get refilled, such as a trough for water, or the system water source.
For those, we can use liquid level detection to estimate volume during a given run,
but that information should not be preserved between non-sequential runs.

Started 2013-07-24

Trying to run pd.lisp on Evoware

- [x] check out carrier.cfg on robot
- [x] use RoMa2 instead of RoMa1
- [ ] the trough carrier to the left of the shaker (grid 9) should have 3 sites instead of 2
- [ ] EvowareCarrierParser: FIXME: currently, vectors are filtered out which have only two positions (starting and ending) -- but there could be valid vectors like that I think.  Might be able to improve the filter by seeing whether the lines are the same.  Problem is that "absolute" flat changes the X value by -250 on our machine.

Started 2013-07-22

- [x] protocol/domain: create TransporterSpec for Vector Class
- [x] Protocol: give labels to plate models
- [x] JshopTranslator: user move plate: insert Evoware labels instead of identifiers

- [ ] JshopTranslator: translate operations per agent (i.e., system, user, evoware)
- [ ] JshopTranslator: output system, evoware, and user instructions
- [ ] jshop: link in Jshop2
- [ ] jshop: compile .lisp file using Jshop2
- [ ] jshop: run java problem
- [ ] jshop: get solution
- [ ] send jshop solution to JshopTranslator
- [ ] distinct planning methods: overall method is like HTN planner, with pre-conditions and tasks.  Labware positioning requires some path-finding algorithm.  Pipetting requires its own complex search routines.
- [ ] evoware: log time of each command so that we get a database of how long commands require to execute.
- [ ] domain: !evoware-transporter-run, !user-move-labware (but can't do this until we implement our own path-finding algorithm)
- [ ] domain: get rid of site type, just using objects
- [ ] domain: what's a good way to indicate which sites the userArm can access?

## Translating ground operators

(agent, command)
(agent, command, [(client, client-command)])

Generally the first commands will need to be run by the user.  Those commands can go to a user client.
For now, we'll let
the user client be the main Evoware machine, but it should be made into a separate client as soon as possible.
When we want to switch clients, the client which is being paused should be informed to wait until it's activated
again.
For now, when we want to start a new script, this information should go to the user client.
Evoware should send information back to the server very frequently.
Evoware scripts should be able to start a next script, when we need to change the labware on a site.

Clients may need to switch whenever agents switch.

How should client switching be handled?

### Scenario: single client on Evoware machine

Everything is contained in a set of evoware scripts and perhaps a text file.

### Scenario: two clients, a primary Evoware machine and a secondary Evoware machine

Sets of scripts are made for each evoware client.  When a script is supposed to be run
on the secondary machine, the primary machine prompts the user to load that script and
to press OK when that script is finished.  The secondary scripts end as soon as another
agent is activated.

### Scenario: server, user client, evoware client

The lab worker starts a protocol from the user client.  The server
checks that all clients are working.  The server sends the user the
initial instructions.  Once the user's part is finished, the evoware
client receives instructions to start a script.  Whenever a command
is given to the user, the evoware script will run an external
command, which waits for the evoware client to receive a message that it
can continue (i.e., the user is done).

Started 2013-07-13

- [ ] Generate evoware output from tasks
- [x] Test pipetting command

- [ ] load all relations we can from Evoware
    - [x] add description field to Entity and use it to print comments in LISP planner file
    - [x] better name for device sites, such that they can't have spaces in them
    - [x] setup sealer device
        - [x] device site
        - [x] device models
        - [x] setup domain so that sealer uses spec
        - [x] user needs to provide sealer specs
    - [x] aliases (i.e. "Thermocycler Plate" => "D-BSSE 96 PCR")
    - [x] the plate specified in protocol should get the correct platemodel (m002, not m1)
    - [x] problem file: sealer-run plate1
- [ ] domain: transporter-run
    - [ ] check for closed device sites
    - [ ] handle sites which can accept multiple labware (e.g. offsite and centrifuge)
- [ ] load json data/scripts
- [ ] get planner state?
- [ ] handle pipetting
- [ ] handle centrifuge
- [ ] convert YAML to json
- [ ] special multi-aspirate pipetting
- [ ] purification with plate stacking
- [ ] Parse tokens, with agent always as first argument, into an Evoware script

- [ ] Control
    - [ ] Server that controls agent clients
    - [ ] Agent clients (one to interact with user, one to interact with Evoware)
    - [ ] Evoware scripts send feedback to server or client when measurements are obtained
    - [ ] Server can start, pause, resume, stop complex scripts with feedback

Big stuff

- [ ] Implement my own planner/optimizer using reactive-sim, in order to print errors and warnings, and allow for parsing execution up to a branching point
- [ ] 
