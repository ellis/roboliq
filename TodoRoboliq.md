Started 2013-07-25

Current big steps:

- [ ] Get pipetting command to work
- [ ] Get pipeline working to compile a roboliq protocol on disk to an evoware script
- [ ] Create server/clients to monitor and control protocol execution
- [ ] Use simpler entities and a different method for querying state, one that's compatible with planning methods
- [ ] Start using sqlite database for storing relevant information
- [ ] Change domain to have a more generic concept of labware which subsumes sites, tubes, and plates.
- [ ] New command structure: preconditions leading to search through variable space, preconditions leading to search through actions to achieve that state, further task breakdown, add and delete lists for state

Details:
- [ ] adapt VesselState, VesselContents for new system
- [ ] function from MixtureDistribution + events => MixtureDistribution, or from MixtureEstimate

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
(agent, command, client)

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
