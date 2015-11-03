## centrifuge.centrifuge2

Centrifuge using two plate

Properties:

* `[agent]: Agent` -- Agent identifier
* `[equipment]: Equipment` -- Equipment identifier
* `object1: Plate` -- Plate identifier 1
* `object2: Plate` -- Plate identifier 2
* `[site1]: Site` -- Location identifier for the centrifugation site of object1
* `[site2]: Site` -- Location identifier for the centrifugation site of object2
* `[destinationAfter1]: Site` -- Location identifier for where object1 should be placed after centrifugation
* `[destinationAfter2]: Site` -- Location identifier for where object2 should be placed after centrifugation

## centrifuge.insertPlates2

Insert up to two plates into the centrifuge.

Properties:

* `[agent]: Agent` -- Agent identifier
* `[equipment]: Equipment` -- Equipment identifier
* `[object1]: Plate` -- Plate identifier 1
* `[object2]: Plate` -- Plate identifier 2
* `[site1]: Site` -- Location identifier for the centrifugation site of object1
* `[site2]: Site` -- Location identifier for the centrifugation site of object2

## equipment._run

Run the given equipment.

This is a generic command, and any addition parameters may be passed that are required by the target equipment.


Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier

## equipment.open

Open the given equipment.

This is a generic command that expands to a sub-command named `equipment.open|${agent}|${equipment}`. That command should be defined in your configuration for your lab.

The handler should return effects indicating that the equipment is open.


Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier

## equipment.openSite

Open an equipment site.
This command assumes that only one equipment site can be open at a time.

This is a generic command that expands to a sub-command named
`equipment.openSite|${agent}|${equipment}`.
That command should be defined in your configuration for your lab.

The handler should return effects indicating that the equipment is open,
the given site is open, and all other equipment sites are closed.


Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier
* `site: Site` -- Site identifier

## equipment.close

Close the given equipment.

This is a generic command that expands to a sub-command named
`equipment.close|${agent}|${equipment}`.
That command should be defined in your lab configuration.

The handler should return effects indicating the the equipment is closed
and all of its sites are closed.


Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier

## fluorescenceReader.measurePlate

Measure the fluorescence of a plate.

Properties:

* `[agent]: Agent` -- Agent identifier
* `[equipment]: Equipment` -- Equipment identifier
* `program: object` -- Program specification for measurement
* `outputFile: string` -- Filename for output
* `object: Plate` -- Plate identifier
* `[site]: Site` -- Site identifier in reader
* `[destinationAfter]: Site` -- Site to move the plate to after measurement

## pipetter.PipetteItem

Parameters for pipette items.

Properties:

* `[syringe]: string,integer` -- Syring identifier
* `[source]: Source` -- Source specifier
* `[destination]: Site` -- Destination specifier
* `[volume]: Volume` -- Volume

## pipetter._PipetteItem

Parameters for low-level pipette items.

Properties:

* `syringe: string,integer` -- Syring identifier
* `source: Source` -- Source specifier
* `destination: Well` -- Destination specifier
* `volume: Volume` -- Volume

## pipetter._aspirate

Aspirate liquids from sources into syringes.

Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier
* `program: string` -- Program identifier
* `[items]: array` -- Data about what should be aspirated from where

## pipetter._cleanTips

Clean the pipetter tips.

Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier
* `program: string` -- Program identifier
* `[items]: array` -- Data about which tips to clean and with what intensity

## pipetter._dispense

Dispense liquids from sryinges into destinations.

Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier
* `program: string` -- Program identifier
* `[items]: array` -- Data about what should be dispensed where

## pipetter._pipette

Pipette liquids from sources to destinations.

Properties:

* `agent: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier
* `program: string` -- Program identifier
* `[items]: array` -- Data about what should be pipetted where

## pipetter.cleanTips

Clean the pipetter tips.

Properties:

* `[agent]: Agent` -- Agent identifier
* `equipment: Equipment` -- Equipment identifier
* `[program]: string` -- Program identifier
* `[syringes]` -- 
* `intensity` -- 

## pipetter.pipette

Pipette liquids from sources to destinations.

Properties:

* `[agent]: Agent` -- Agent identifier
* `[equipment]: Equipment` -- Equipment identifier
* `[program]: string` -- Program identifier
* `[items]: array` -- Data about what should be pipetted where
* `[sources]: Sources` -- Specifier for source(s) to aspirate from, if missing from items
* `[destinations]: Wells` -- Specifier for destination(s) to despense to, if missing from items
* `[volumes]: Volumes` -- Volume(s) to pipette, if missing from items

## pipetter.pipetteMixtures

Pipette the given mixtures into the given destinations.

Properties:

* `[agent]: Agent` -- Agent identifier
* `[equipment]: Equipment` -- Equipment identifier
* `[program]: string` -- Program identifier
* `mixtures: array` -- Array of arrays, where each sub-array is a list of components to be mixed into a destination well
* `destinations: Wells` -- Destination specifier
* `[order]: string` -- Order in which to pipette the mixtures.  Defaults to the order given in the mixtures array.

## sealer.sealPlate

Seal a plate.

Properties:

* `[agent]: string` -- Agent identifier
* `[equipment]: string` -- Equipment identifier
* `[program]: string` -- Program identifier for sealing
* `object: string` -- Plate identifier
* `[site]: string` -- Site identifier in reader
* `[destinationAfter]: string` -- Site to move the plate to after measurement

## system.call

Call a template function.

The template function should be an object of type `Template` with a property `template` holding either a Mustache template string or an object whose properties may be Mustache template strings. The template will be expanded using the values passed in the `params` property.

Properties:

* `name: string` -- Name of the template function.
* `[params]: object` -- Parameters to pass to the template function.

## system.repeat

Repeat the given command a given number of times.

Properties:

* `count: integer` -- The number of times to repeat.
* `[steps]: object` -- The sequence of commands to repeat.

## timer._sleep

Sleep for a given duration using a specific timer.

Handler should return `effects` that the timer is not running.


Properties:

* `agent: string` -- Agent identifier
* `equipment: string` -- Equipment identifier
* `duration: number` -- Number of seconds to sleep

## timer._start

Start the given timer.

Handler should return `effects` that the timer is running.


Properties:

* `agent: string` -- Agent identifier
* `equipment: string` -- Equipment identifier

## timer._stop

Stop the given timer.

Handler should return `effects` that the timer is not running.


Properties:

* `agent: string` -- Agent identifier
* `equipment: string` -- Equipment identifier

## timer._wait

Wait until the given timer has reacher the given elapsed time.

Handler should:

- expect that the timer (identified by the `equipment` parameter) is running
- return `effects` that the timer is not running


Properties:

* `agent: string` -- Agent identifier
* `equipment: string` -- Equipment identifier
* `till: number` -- Number of seconds to wait till from the time the timer was started
* `stop: boolean` -- Whether to stop the timer after waiting, or let it continue

## timer.doAndWait

A control construct to perform the given sub-steps and then wait
until a certain amount of time has elapsed since the beginning of this command.


Properties:

* `[agent]: string` -- Agent identifier
* `[equipment]: string` -- Equipment identifier
* `duration: number` -- Number of seconds this command should last
* `steps: object,array` -- Sub-steps to perform

## timer.sleep

Sleep for a given duration.

Properties:

* `[agent]: string` -- Agent identifier
* `[equipment]: string` -- Equipment identifier
* `duration: string,number` -- Duration to sleep (default units is in seconds)

## timer.start

Start a timer.

If no parameters are supplied, a timer will be automatically chosen.


Properties:

* `[agent]: string` -- Agent identifier
* `[equipment]: string` -- Equipment identifier

## timer.stop

Stop a running a timer.

If only one timer is running, this command can be called without any parameters.
Otherwise, the equipment identifier must be supplied.


Properties:

* `[agent]: string` -- Agent identifier
* `[equipment]: string` -- Equipment identifier

## transporter._movePlate

Transport a plate to a destination.

Handler should return `effects` with the plate's new location.


Properties:

* `agent: string` -- Agent identifier
* `equipment: string` -- Equipment identifier
* `object: string` -- Plate identifier
* `destination: string` -- Site to move the plate to

## transporter.movePlate

Transport a plate to a destination.


Properties:

* `[agent]: string` -- Agent identifier
* `object: string` -- Plate identifier
* `destination: string` -- Site to move the plate to
