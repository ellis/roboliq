# TODOs for roboliq

## Executables/Servers

Two kinds of servers to begin with:

* relay: interacts with an agent, e.g. the technician or Evoware on the computer with Evoware installed
* manager: controls the relays

To begin with, I'll create a technician relay and the manager (since these don't require me to be on the Evoware computer).
The relay and manager interact by sending each other messages via GETs.
Later there should also be the option for the relay to receive messages from the manager
via an open socket or something, so that the manager doesn't need to contact the relay server;
this way the relay doesn't need to be server and can be communicated with through a firewall.

The controller functions can be run either from the command line or via a web interface.

See http://jawinproject.sourceforge.net/jawin.html#callingScript for java interface to COM.

Protocol processing steps:
1) Step 1
  * load files: configuration, protocol, customization
  * find extra variables that user will need to specify (labware models, task methods, explicit protocol variables, additional action user variables)
  * display list of variables to user (preferably with possible values or ranges)
  * let the user assign values to the variables
  CONTINUE HERE

## AI planning flow

Test RsResult functions with:
import scala.language.higherKinds
import scala.collection.generic.CanBuildFrom
import ch.ethz.reactivesim._
val l1 = List[RsResult[Int]](RsSuccess(1), RsSuccess(2), RsSuccess(3))
val l2 = List[RsResult[Int]](RsSuccess(1), RsSuccess(2), RsError("nope"))

- [x] move ActionHandler_ShakePlate to it's own package, which will import both roboliq.plan and roboliq.evoware
- [x] extract planned variables from partial plan and use them to expand CallTree actions into operators
- [x] translate planned actions to tokens for Evoware translation
- [x] AutoActionHandler: create method for handling actions that were added by the planner
- [x] need to put `transportLabware` operator into domain
- [x] handle more than one command
- [x] TransportLabware: make sure that site is not occupied before moving plate to it
- [x] For now, no transport graph while planning.  But use the transport graph when generating the operators, and hope a path is found.
- [x] TransportLabware: handle move operators from move command
- [x] ActionHandler: should have `name: String` method
- [x] PartialPlan: write a function to make it concrete (completely ordered and with grounded variables)
- [x] Protocol: use scala-graph-core to build a graph of sites and possible transport links
- [x] Protocol: adapt to create a CallTree, see loadJsonProtocol_Protocol
- [x] Protocol: remove variable `tasks`
- [x] Main: let user specify output directory
- [x] Protocol: createProblem: get objects from eb
- [x] PartialPlan.toDot: optionally don't display initial state, because it may be too large
- [x] Main: figure out what to do with operator sequence to let it be translated to evoware
- [x] Shaker: EvowareScriptBuilder:430, properly format shaker command
- [x] RsResult: function to get value from an Option if there is one, or return a default value and a warning if there's None
- [x] RsResult: create a sequence function that works on List[RsResult[A]] or Seq[RsResult[A]] or other collections
- [x] RsResult.flatten: drop RsErrors
- [x] RsResult: sequenceDrop: drop RsErrors, accumulate warnings
- [ ] RsResult: sequenceFirst: return first Error if any errors, accumulate warnings
- [ ] RsResult: sequenceAll: return all Errors if any errors, accumulate warnings
- [ ] RsResult: removeWarnings
- [ ] RsResult: function for conveniently folding over a list, but abort if RsError; lookup `sequence`
- [ ] ActionHandler.getInstruction should be passed the world state
- [ ] Main: update world state while creating instructions from actions
- [ ] Protocol: load from config file which sites user can access (besides `offsite`)
- [ ] Protocol: createProblem: get relations from eb (in Protocol.createProblem, remove my hard-coded state atoms)
- [ ] Allow for comments in Strips.Atom?
- [ ] Protocol: createDomain
- [ ] Protocol: createPartialPlan
- [ ] Domain: don't accept multiple operators with the same name
- [ ] CommandSet: construct by taking a list of ActionHandlers, and using them to construct the CommandSet maps
- [ ] make sure that the order the operators is the same as in the partial plan
- [ ] ActionHandler: should have `methods: List[Call => RqResult[Call]]` method
- [ ] PddlParser: parse for type hierarchies
- [ ] Domain and Problem: handle type hierarchies
- [ ] Look into whether EntityBase and WorldState can be given a simpler PDDL-like representation
- [ ] aiplan.strip2: use RsResult instead of Either
- [ ] rename `toStripsText` to `toPddlString`
- [ ] TransportLabware: consider creating multiple transporation AutoActions to handle various conditions

Command types: task, procedure, function, action, instruction

A sequence of calls gets transformed into a command tree with a root node, and the call items are transformed into command nodes.
The command tree is iteratively expanded as follows:

* A task gets expanded into a list of possible commands for achieving the task.  A task is established by a map of task name to a list of command names.
  If the task has more than one possible method, the user will need to choose which one to use.
* A method is a way to convert a task call into another command call, and there may be multiple such conversions per task.
* A procedure gets expanded into a list of commands (recursion not allowed for now).  Procedures are generally user-defined, have parameters, and a list of commands.
* A function gets expanded into a list of commands; it differs from the other command types insofar as it is composed of code, and can therefore make decisions about how to expand based on its parameters; It can also request more information from the user before proceeding.
* A function may also need to use tasks, but in order to do so, it must request the methods for those tasks in advance in order to avoid additional user interaction.
* Once the leafs of the command tree are all actions or instructions, the expansion process is done and no more user interaction is required.
* The actions and instructions get passed to the planner.
* With a concrete plan, actions are turned into instructions, which are passed to the translator.

More:

* The user is given the opportunity to set predicates in the initial state (e.g. intial positions).
* During planning, the actions are added to our partial plan one at a time, and we see whether a solution can be found.
* If not, first retry planning with a new partial plan of all actions up to that point.
  If that also doesn't work, we inform the user.
  We can try to figure out where the problem occurred by planning for all subsets of the action's pre-requisites and finding the first one that fails.
* If a solution is found to the partial plan with all actions, the user is given the opportunity to add additional bindings and orderings.
* The user now selects which post-conditions to add to the plan.
* We try to find a solution to the partial plan with post-conditions in the goal state.
* If a solution is found, the user is given the opportunity to add additional bindings and orderings.
* We try to find a grounded, completely ordered plan.

A complication is labware placement and movement.
Moving labware from site A to B may require multiple agents (e.g. centrifuge a plate at the start of a protocol).
We can have a list of all possible pair-wise movements for a given labware model...

2) convert action calls to plan actions
3) plan
4) convert action call to operator calls using merger of original parameters and planned parameters
5) convert operator calls to operators
6) translate

## For next CADMAD meeting

- [ ] '//' comments in protocol
- [x] better 'shaker' command, better ShakerSpec
- [x] shaker: figure out how to add a user-defined program to eb.addDeviceSpec(shaker, ...) in Protocol.loadEvoware()
- [x] shaker: currently looking at Converter, why Map[String, ShakerSpec] isn't working
- [ ] shaker: make parser for duration (with 'min' and 's' units)
- [ ] shaker: make parser for frequency (with 'rpm' units)
- [x] shaker: figure out Evoware parameters for our shaker, and then generate them from ShakerSpec
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

## AI PoP planner

- [x] PddlParser: handle types in operators
- [x] PddlParser: handle `types` field in domain
- [x] PddlParser: handle `predicates` field in domain
- [x] PddlParser: handle `objects` field in problem
- [x] continue with domain: action tecan_pipette1
- [x] create PDDL for domain with `movePlate` and `distribute` commands
- [x] `3:moveLabware` should not be a provider for `location plateA siteA`
- [x] Why is `moveLabware(?labware:labware ?site1:site ?site2:site) using Map(3:?site1 -> siteA, 3:?labware -> plateA)` a provider?
- [x] Negative preconditions should always be satisfiable by the initial state if the initial state doesn't conflict
- [ ] PartialPlan: toDot: show variable options and inequalities
- [ ] PartialPlan: incrementally calculate threats
- [ ] PartialPlan: toDot: show threats
- [ ] PddlParser: handle comments
- [ ] PddlParser: handle domain constants

## Feedback loops

Loops:

* Script is created up until looping condition.
* Once the looping condition is reached, if the condition is true, a new script is created just for the loop segment,
  where the initial state contains the state at the condition check.

Alternatively, set a maximum number of loops allowed and unroll the loop that many times.

Conditional branches:

* For each branch of a conditional block, set the tested state to true and continue planning for the particular branch.
* To continue planning, create a dummy action whose effects are the intersection of all final effects for each branch.


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

Step 1 -- create a partially ordered plan from the protocol
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

Step2 -- create a partially ordered plan to satisfy the pre-conditions
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
    post: plate2.covered = true
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

Step3 -- create a partially ordered plan to satisfy the post-conditions
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
    post: plate2.covered = true
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

Step 4 -- calculate movement plans and pipetting plans

Post-conditions are optional.  The user should be able to remove them if they don't want them.

In general, post-conditions get deleted if there are any later effects which overwrite the given value.
However, with plate sites, we need to handle this differently.
Also consider that at some point, a plate may want to stay cool, meaning that it shouldn't be moved back to an uncooled position unless necessary.
If the user explicitly moves a plate with a move command, then the stack should be reset.
What we could do is propogate the desired post-condition value through commands, and commands can either pass that value on or overwrite it.


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

- [ ] Fabian says: clean, mix, ensureWellMixed
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

