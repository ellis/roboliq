Started 2013-07-13

- [ ] Generate evoware output from tasks
- [ ] Test pipetting command

- [ ] load all relations we can from Evoware
    - [ ] add description field to Entity and use it to print comments in LISP planner file
    - [x] better name for device sites, such that they can't have spaces in them
    - [x] setup sealer device
        - [x] device site
        - [x] device models
        - [x] setup domain so that sealer uses spec
        - [x] user needs to provide sealer specs
    - [ ] aliases (i.e. "Thermocycler Plate" => "D-BSSE 96 PCR")
    - [ ] the plate specified in protocol should get the correct platemodel (m002, not m1)
    - [ ] problem file: sealer-run plate1
- [ ] domain: transporter-run
    - [ ] check for closed device sites
    - [ ] handle sites which can accept multiple labware (e.g. offsite and centrifuge)
- [ ] load json data/scripts
- [ ] aliases
- [ ] convert planner output to evoware
- [ ] get planner state?
- [ ] handle pipetting
- [ ] handle centrifuge
- [ ] convert YAML to json
- [ ] special multi-aspirate pipetting
- [ ] purification with plate stacking
