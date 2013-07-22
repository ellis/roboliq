Started 2013-07-22

- [ ] domain: !evoware-transporter-run, !user-move-labware
- [ ] JshopTranslator: translate operations per agent (i.e., system, user, evoware)
- [ ] jshop: link in Jshop2
- [ ] jshop: compile .lisp file using Jshop2
- [ ] jshop: run java problem
- [ ] jshop: get solution
- [ ] send jshop solution to JshopTranslator

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
