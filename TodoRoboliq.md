Started 2013-07-22

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
- [ ] protocol/domain: create TransporterSpec for Vector Class

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
    - [ ] add description field to Entity and use it to print comments in LISP planner file
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
