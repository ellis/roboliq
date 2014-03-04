# TODOs for roboliq

## For next CADMAD meeting

- [ ] '//' comments in protocol
- [ ] better 'shaker' command, better ShakerSpec
- [?] shaker: figure out how to add a user-defined program to eb.addDeviceSpec(shaker, ...) in Protocol.loadEvoware()
- [ ] shaker: make parser for duration (with 'min' and 's' units)
- [ ] shaker: make parser for frequency (with 'rpm' units)
- [ ] 'reader' command
- [ ] user-defined names for sites
- [ ] make sure alternative stflow protocol runs on our Tecan

- [ ] load additional file with user settings to complement the generic protocol
- [ ] 'dilute' command
- [ ] make sure stflow protocol runs on our Tecan

- [ ] integrate own planner
- [ ] redefine sites to fix problem I had with the Weizmann table definition
- [ ] make sure stflow protocol runs on Weizmann Tecan
- [ ] post code for CADMAD participants
- [ ] get it to run on windows

## AI Planning

- for single plate commands: agent,device,spec,model1,site1
- for moving plate, both site1 and site2 are accessible, assume that plate can be moved between them:
  - agent,device,spec,model,?site
- for user moving plate, assume user can move any model, so we only need to know which sites the user can access.
 
How to arrange plates initially?
- Fastest is when the user puts the plates onto the bench directly
- Alternatively, plates are put in hotels intially
  - This is a problem if the plates need to be cooled
  - Wouldn't work for tubes
- Might want to have a "home" site for each plate
  - Problem: this would limit flexibility in the plan
  - Perhaps only assign a "home" site to some plates
  - if a plate has a home, that should be its initial site if possible,
    and once the plate is no longer needed, it should be moved back to home.
  - the concept would need to be extended to allow for use on multiple robots, and
    what about hotel vs table homes?
- So instead of home sites, perhaps add a penalty for every plate placed on a site,
  in order to make it more likely that a plate will be placed back onto the same site.
- When user doesn't specify an initial site, some deterministic preference for
  sites should be used so that multiple translations of the same protocol lead to the
  same site assignments.

CMD1: pipette
    pre: plate1 -> site1
    pre: plate2 -> site2
    pre: plate1 unsealed
    pre: plate2 uncovered
    action: pipette with plate1 and plate2
    post: restore plate1 sealed
    post: restore plate2 covered
    post: plate1 -> prior site
    post: plate2 -> prior site
CMD2: thermocycle plate1
    pre: plate1 sealed
    effect: plate1 sealed (ensures that plate doesn't get automatically unsealed later)
    action: open thermocycler
    action: move plate1 to siteForThermocycler
    effect: ~(plate at previous site)
    effect: plate1 at siteForThermocycler
    action: close thermocycler
    action: run thermocycler
    action: open thermocycler
    action: move plate1 to ?siteNext
    effect: ~(plate at siteForThermocycler)
    effect: plate1 at ?siteNext
    action: close thermocycler

Step2:
    initial plate1.site = site1
    initial plate2.site = site3
    initial plate2.covered = true
    // CMD1: pipette
    -- pre: plate1 -> site1
    pre: plate2 -> site2
    action01a: move plate2 to site2
    effect: ~(plate2.site = site3)
    effect: plate2.site = site2
    -- pre: plate1 unsealed
    pre: plate2 uncovered
    action01b: move plate2.lid to site3
    effect: plate2.lid.site = site3
    effect: ~(plate2.covered = true)
    action01: pipette with plate1 and plate2
    -- post: restore plate1 sealed
    post: restore plate2 covered
    action01A: move plate2.lid to plate2
    effect: ~(plate2.lid.site = site3)
    effect: plate2.covered = true
    -- post: plate1 -> prior site
    post: plate2 -> prior site
    action01B: move plate2 to site3
    effect: ~(plate2.site = site2)
    effect: plate2.site = site3
    // CMD2: thermocycle plate1
    pre: plate1 sealed
    effect: plate1 sealed (ensures that plate doesn't get automatically unsealed later)
    action: open thermocycler
    action: move plate1 to siteForThermocycler
    effect: ~(plate at previous site)
    effect: plate1 at siteForThermocycler
    action: close thermocycler
    action: run thermocycler
    action: open thermocycler
    action: move plate1 to ?siteNext
    effect: ~(plate at siteForThermocycler)
    effect: plate1 at ?siteNext
    action: close thermocycler

In general, post-conditions get deleted if there are any later effects which overwrite the given value.
However, with plate sites, we need to handle this differently and keep a stack.


## For stflow to run water test

- [ ] manually choose initial locations for plates
- [ ] separate titration commands which should be separated and see if JSHOP can still handle it
- [ ] mix command
- [ ] add shaker
- [ ] add the user prompt to script
- [ ] comments
- [ ] Add evoware "Group"s to ESC files
- [ ] Dilute: create command
- [ ] reader commands

- [ ] Figure out site naming scheme for our table
- [ ] Allow for site naming

- [ ] BUG: AliquotFlat.apply needs to proportionally adjust amounts in mixtures
- [ ] TitrationSeries: let user specify destination well for individual steps rather than overall (but then check that every well has a destination)
- [ ] List of user variables should be read in from a separate file, to make lab customization possible without modifying the original script
- [ ] TitrationSeries: randomize dispenses, with a randomSeed property
- [ ] TitrationSeries: allow user to specify the sort order (e.g., the third component could be the primary grouping)
- [ ] EvowareClientScriptBuilder: add well events in spirate_checked

## Priorities for user friendliness

- [ ] When generating files from protocol, create a new directory that will hold all output files -- let each command create as multiple output files
- [ ] Print out liquid usage for source wells, so I know how much we need to put in
  - [x] print a table sorted by plate name, column, row
  - [ ] for reagents, print a table with these columns: plate, column, row, well index name, initial volume, final volume, volume change, dead volume, min volume recommended
  - [ ] Create csv with these columns for destination wells: well name, row, col, tip, pipette sequence number, multipipette sequence number, volume, liquid class, reagent, labware model, site, [plate name], [experiment name]
- [ ] Create nice HTML/SVG instructions for the user to prepare source plates
- [ ] Create nice HTML/SVG instructions for the user to setup the table
- [ ] Titrate: Web form for entering titration series
- [ ] Server program to control evoware

## Priorities for script language

- [ ] for evoware script, each protocol command should be an evoware Group
- [ ] TitrationSeries: Automatically figure out sterilization and liquid class again, so user doesn't need to specify it
- [ ] Allow for use of tubes

## Priorities for planning

- [ ] Better figuring out of labware positions
- [ ] Have user place plates at beginning of script rather than just when needed
- [ ] If a command changes the state of a plate (e.g. takes off lid or move to a different position), the state should be restored at the end of the command unless it gets modified again later by another command -- but at latest by the end of the script, restore the state
- [ ] Sometimes, we prefer to use different sites for different plates, but sometimes we want to reuse sites...

## Next

- [ ] Implement smart mixing planner that figures out how to mix, which plates to use, which wells to use, and considers dead volumes, plate costs, and reagent costs.
- [ ] let user specify initial volume for a source well
- [ ] add 'deadVolume' property to labwareModel, indicated the extra volume required in a well for aspiration
- [ ] keep track of minimum and maximum volumes in a well, and warn or produce errors when these are out of bounds
- [ ] TitrationSeries: allow use of more than one target plate
- [ ] TitrationSeries: create better data structure that supports recursive components and alternatives, and allows for different total volumes 
- [ ] pipetting: should sterilize tips before first aspirate (see tania01 without the sterilizeBefore spec)
- [ ] tania??: uria will require a thorough post-cleaning
- [ ] A '.' in a reagent name causes eb.lookupLiquidSource not to work for that reagent
- [ ] TitrationSeries: filter out 0-volume dispenses
- [ ] TitrationSeries/PipetteSpec: allow for a list of volumes rather than just a single volume
- [ ] TitrationSeries: handle min/max reagent, ranging the volume over the available wells
- [ ] TitrationSeries: handle concentrations in addition to volumes
- [ ] Consider making paths in config file relative to the config file itself
- [ ] Refactor the Converter code, and make more use of parsers
- [ ] Commands should be able to each produce their own reports, e.g. for TitrationSeries, showing a table with the contents of the wells

## Someday
- [ ] PipetteAmountParser: allow for '.25ul' (not just '0.25ul')
- I-X source code: http://i-x.info/release/current/
- from Austin Tate and Gerhard: try I-X, do a demo in it, then re-implement the algorithms you need; highly recommend the INCA ontology!  You might want to then get back to I-X for industrial application after PhD is done.

