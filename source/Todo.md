# TODOs for roboliq

- [x] Create titationSeries command
- [x] TitrationSeries: handle multiple volumes for a given step, for grouping
- [x] TitrationSeries: handle reagent without volume, to use as filler
- [x] TitrationSeries: handle multiple reagents for a given step, for grouping
- [x] TitrationSeries: BUG: for the multi-source step, all aspirations are happening from a single well
- [x] Speed up pipetting algorithm -- need to get it fast enough to handle 384 well plate
- [x] TitrationSeries: fix hack for stepToList_l in TitrationSeriesSub()
- [x] TitrationSeries: allow for alternative fillers
- [x] TitrationSeries: allow for combinations of liquids (e.g. salt_ph55+buffer_ph55 and salt_ph70+buffer_ph70, but not salt_ph55+buffer_ph70)
- [x] TitrationSeries: let user explicitly specify number of replicates
- [x] BUG: TitrationSeries: tania03_urea: why is no Assam reagent used, according to table printed at end of execution?
- [x] Transfer: create command
- [x] EvowareClientScriptBuilder: update tip state in spirate_checked
- [x] EvowareClientScriptBuilder: update well state in spirate_checked
- [x] in stflow1.prot, second step of titration command needs to be expanded to TitrationItem_Or (see toPipetteSources in Converter)
- [x] TitrationSeries: put code in separate file
- [x] TitrationSeries: rename to titrate
- [x] TitrationSeries: use PipetteAmount instead of LiquidVolume for sources
- [x] TitrationSeries: intelligently handle PipetteAmount (as volume, dilution, or concentration), considering totalVolume_?
- [ ] Create 'setReagents' command
- [ ] for stflow1.prot, see whether titration command can be made a subcommand of pipette, so that JSHOP2 doesn't crash
- [ ] TitrationSeries: let user specify destination well for individual steps rather than overall (but then check that every well has a destination)
- [ ] TitrationSeries: randomize dispenses, with a randomSeed property
- [ ] TitrationSeries: allow user to specify the sort order (e.g., the third component could be the primary grouping)
- [ ] Dilute: create command
- [ ] List of user variables should be read in from a separate file, to make lab customization possible without modifying the original script
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

