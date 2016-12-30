
## <a name="absorbanceReader"></a>absorbanceReader

The `absorbanceReader` commands specify actions using equipment for absorbance readouts.


### `absorbanceReader.measurePlate`

{.command} Measure the absorbance of a plate.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] |  | *optional* | Program parameters
[programFile] | string | *optional* | Program filename
[programData] |  | *optional* | Program data
[outputFile] | string | *optional* | Filename for output (deprecated, use `output.writeTo`)
[outputDataset] | string | *optional* | Name of dataset to which the measurements values will be appended. (deprecated, use `output.appendTo`)
[output] |  | *optional* | Output definition for where and how to save the measurements
object | Plate |  | Plate identifier
[site] | Site | *optional* | Site identifier in reader
[destinationAfter] | SiteOrStay | *optional* | Site to move the plate to after measurement

</P>

## <a name="centrifuge"></a>centrifuge

The `centrifuge` commands specify actions using centrifuge equipment.


### `centrifuge.centrifuge2`

Centrifuge using two plate

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
program |  |  | Program for centrifuging
object1 | Plate |  | Plate identifier 1
object2 | Plate |  | Plate identifier 2
[site1] | Site | *optional* | Location identifier for the centrifugation site of object1
[site2] | Site | *optional* | Location identifier for the centrifugation site of object2
[destinationAfter1] | SiteOrStay | *optional* | Location identifier for where object1 should be placed after centrifugation
[destinationAfter2] | SiteOrStay | *optional* | Location identifier for where object2 should be placed after centrifugation

Example:

This will centrifuge 2 plates at 3000rpm for 2 minutes at 25°C:
```
{
  "command": "centrifuge.centrifuge2",
  "object1": "plate1",
  "object2": "plate2",
  "program": {
    "rpm": 3000,
    "duration": "2 minutes",
    "temperature": "25 degC"
  }
}
```



### `centrifuge.insertPlates2`

Insert up to two plates into the centrifuge.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[object1] | Plate | *optional* | Plate identifier 1
[object2] | Plate | *optional* | Plate identifier 2
[site1] | Site | *optional* | Location identifier for the centrifugation site of object1
[site2] | Site | *optional* | Location identifier for the centrifugation site of object2

Example:

This will insert two plates into the centrifuge:
```
{
  "command": "centrifuge.insertPlates2",
  "object1": "plate1",
  "object2": "plate2",
}
```




## <a name="equipment"></a>equipment

The `equipment` commands specify generic actions such as 'run' and 'open'
that may apply to various types of equipment.


### `equipment._run`

Run the given equipment.

This is a generic command, and any addition parameters may be passed that are required by the target equipment.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier

### `equipment.open`

Open the given equipment.

This is a generic command that expands to a sub-command named `equipment.open|${agent}|${equipment}`. That command should be defined in your configuration for your lab.

The handler should return effects indicating that the equipment is open.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier

### `equipment.openSite`

Open an equipment site.
This command assumes that only one equipment site can be open at a time.

This is a generic command that expands to a sub-command named
`equipment.openSite|${agent}|${equipment}`.
That command should be defined in your configuration for your lab.

The handler should return effects indicating that the equipment is open,
the given site is open, and all other equipment sites are closed.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
site | Site |  | Site identifier

### `equipment.close`

Close the given equipment.

This is a generic command that expands to a sub-command named
`equipment.close|${agent}|${equipment}`.
That command should be defined in your lab configuration.

The handler should return effects indicating the the equipment is closed
and all of its sites are closed.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier

### `equipment.start`

Start the given equipment.

This is a generic command that expands to a sub-command named
`equipment.start|${agent}|${equipment}`.
That command should be defined in your configuration for your lab.
Any addition parameters may be passed that are required by the target equipment.

The handler returns effects indicating that the equipment is running.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier

### `equipment.stop`

Stop the given equipment.

This is a generic command that expands to a sub-command named
`equipment.stop|${agent}|${equipment}`.
That command should be defined in your configuration for your lab.
Any addition parameters may be passed that are required by the target equipment.

The handler returns effects indicating that the equipment is not running.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier


## <a name="experiment"></a>experiment

The `experiment` module is for working with experimental designs.
Currently there is only one command, `experiment.run`.


### `experiment.forEachGroup`

Run an experiment based on a Design or the currently available `DATA` object.
This command will performs its sub-steps for every selected group of items in the experiment array."


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[design] | Design | *optional* | An experimental design to use
groupBy | array,string |  | The factor(s) to group by
[distinctBy] | array,string | *optional* | The factor(s) to distinguish row by
[orderBy] | array,string | *optional* | The factor(s) to order by
[durationTotal] | Duration | *optional* | The total duration of this step. The execution of all groups should complete within the allotted time. If execution completes earlier, the protocol will sleep for the remainder of the duration.
[durationGroup] | Duration | *optional* | The duration of each row. The execution of each group should complete within the allotted time. If execution completes earlier, the protocol will sleep for the remainder of the duration.
[interleave] | Duration | *optional* | The time offset for interleaving each group
[timers] | array | *optional* | Timers that should be used
[startTimerAfterStep] | integer | *optional* | The duration timer will be started after this step rather than from the beginning, if specified
[steps] | object | *optional* | The sequence of commands to perform for each set of factor values.

### `experiment.forEachRow`

Run an experiment based on a Design or the currently available `DATA` object.
This command will performs its sub-steps for every item in the experiment array, placing the item's values in SCOPE."


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[design] | Design | *optional* | An experimental design to use
[distinctBy] | array,string | *optional* | The factor(s) to distinguish row by
[orderBy] | array,string | *optional* | The factor(s) to order by
[durationTotal] | Duration | *optional* | The total duration of this step. The execution of all rows should complete within the allotted time. If execution completes earlier, the protocol will sleep for the remainder of the duration.
[durationRow] | Duration | *optional* | The duration of each row. The execution of each row should complete within the allotted time. If execution completes earlier, the protocol will sleep for the remainder of the duration.
[interleave] | Duration | *optional* | The time offset for interleaving each group
[timers] | array | *optional* | Timers that should be used
[steps] | object | *optional* | The sequence of commands to perform for each set of factor values.


## <a name="fluorescenceReader"></a>fluorescenceReader

The `fluorescenceReader` commands specify actions using equipment for fluorescence readouts.


### `fluorescenceReader.measurePlate`

Measure the fluorescence of a plate.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] |  | *optional* | Program parameters
[programFile] | string | *optional* | Program filename
[programData] |  | *optional* | Program data
[outputFile] | string | *optional* | Filename for output
object | Plate |  | Plate identifier
[site] | Site | *optional* | Site identifier in reader
[destinationAfter] | SiteOrStay | *optional* | Site to move the plate to after measurement


## <a name="incubator"></a>incubator

The `incubator` commands specify actions using incubator equipment.


### `incubator.incubatePlates`

Incubate the given plates

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
program |  |  | Program for incubation
plates | array |  | List of plates to incubate
[sites] | array | *optional* | Internal sites to put the plates on
[destinationAfters] | array | *optional* | Location identifier for where the plates should be placed after incubation

Example:

This will incubate 2 plates at 300rpm for 2 minutes at 25°C:
```
{
  "command": "incubator.incubatePlates",
  "plates": ["plate1", "plate2"],
  "program": {
    "rpm": 300,
    "duration": "2 minutes",
    "temperature": "25 degC"
  }
}
```



### `incubator.insertPlates`

Insert up to two plates into the incubator.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[plates] | array | *optional* | List of plates to incubate
[sites] | array | *optional* | Internal sites to put the plates on

Example:

This will insert two plates into the incubator:
```
{
  "command": "incubator.insertPlates",
  "plates": ["plate1", "plate2"]
}
```



### `incubator.run`

Run the incubator with the given program

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
program | object |  | Program for shaking and incubating
plates | array |  | List of plates to incubate
[sites] | array | *optional* | Internal sites to put the plates on
[destinationAfters] | array | *optional* | Location identifier for where the plates should be placed after incubation

Example:

This will incubate 2 plates at 300rpm for 2 minutes at 25°C:
```
{
  "command": "incubator.incubatePlates",
  "plates": ["plate1", "plate2"],
  "program": {
    "rpm": 300,
    "duration": "2 minutes",
    "temperature": "25 degC"
  }
}
```




## <a name="pipetter"></a>pipetter

The `pipetter` commands specify actions using pipetting equipment.


### `pipetter._MixSpec`

Parameters for mixing in higher-level commands like pipetter._PipetteItem.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
count | integer |  | Number of times to aspirate and re-dispense
volume | Volume |  | Amount to aspirate, either as a fraction or absolute volume

### `pipetter._AspirateItem`

Parameters for pipette items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
syringe | number,nameOf Syringe |  | Syringe identifier
source | Well |  | Source specifier
volume | Volume |  | Volume

### `pipetter._DispenseItem`

Parameters for pipette items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
syringe | number,nameOf Syringe |  | Syringe identifier
destination | Well |  | Destination specifier
volume | Volume |  | Volume

### `pipetter._MeasureVolumeItem`

Parameters for low-level pipetter._measureVolume items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
syringe | number,nameOf Syringe |  | Syringe identifier
well | Well |  | Well specifier

### `pipetter.MeasureItem`

Parameters for pipetter.measureVolume items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[syringe] | number,nameOf Syringe | *optional* | Syringe identifier
[well] | Well | *optional* | Well specifier
[cleanAfter] | pipetter.CleaningIntensity | *optional* | intensity of cleaning required after this item.

### `pipetter._MixItem`

Parameters for mixing with pipetter.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
syringe | number,nameOf Syringe |  | Syringe identifier
well | Well |  | Well to mix
[count] | integer | *optional* | Number of times to aspirate and re-dispense
[volume] | Volume | *optional* | Volume
[program] | string | *optional* | Program identifier

### `pipetter.CleaningIntensity`

Intensity of cleaning.

The enum lists the intensities in increase order.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------

### `pipetter._PipetteItem`

Parameters for low-level pipette items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
syringe | number,nameOf Syringe |  | Syringe identifier
[source] | Well | *optional* | Source specifier
[destination] | Well | *optional* | Destination specifier
volume | Volume |  | Volume
[sourceMixing] | pipetter._MixSpec | *optional* | Parameters for mixing the source prior to aspiration
[destinationMixing] | pipetter._MixSpec | *optional* | Parameters for mixing the destination after dispense
[tipModel] | string | *optional* | Tip model identifier, in order to use a specific tip model

### `pipetter.PipetteItem`

Parameters for pipette items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[syringe] | number,nameOf Syringe | *optional* | Syringe identifier
[source] | Source | *optional* | Source specifier
[destination] | Well | *optional* | Destination specifier
[volume] | Volume | *optional* | Volume
[volumeCalibrated] | object | *optional* | Calculate a calibrated volume
[volumeTotal] | Volume | *optional* | Volume that the well should be brought up to.
[layer] |  | *optional* | A layer identifier for hinting that to items in the same layer should be grouped together, even if they aren't in adjacent in the items list.
[cleanAfter] | pipetter.CleaningIntensity | *optional* | intensity of cleaning required after this item.
[sourceMixing] | boolean,pipetter.MixSpec | *optional* | Parameters for mixing the source prior to aspiration
[destinationMixing] | boolean,pipetter.MixSpec | *optional* | Parameters for mixing the destination after dispense

### `pipetter._PunctureItem`

Parameters for low-level puncture items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
syringe | number,nameOf Syringe |  | Syringe identifier
well | Well |  | Well specifier
distance | Length |  | Distance for puncturing

### `pipetter.PunctureItem`

Parameters for puncture items.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[syringe] | number,nameOf Syringe | *optional* | Syringe identifier
[well] | Well | *optional* | Well specifier
[distance] | Length | *optional* | Distance for puncturing
[cleanAfter] | pipetter.CleaningIntensity | *optional* | intensity of cleaning required after this item.

### `pipetter.DilutionItem`

Parameters for a dilution series.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[syringe] | number,Syringe | *optional* | Syringe identifier
[source] | Source | *optional* | Source specifier
destinations | Wells |  | Destination wells
[volume] | Volume | *optional* | Volume

### `pipetter.MixItem`

Parameters for mixing with pipetter.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[syringe] | number,nameOf Syringe | *optional* | Syringe identifier
[well] | Well | *optional* | Well to mix
[count] | integer | *optional* | Number of times to aspirate and re-dispense
[amount] | Volume,number | *optional* | Amount to aspirate, either as a fraction or absolute volume
[program] | string | *optional* | Program identifier

### `pipetter.MixSpec`

Parameters for mixing (used in higher-level commands like pipetter.pipette and pipetter.pipetteDilutionSeries).

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[count] | integer | *optional* | Number of times to aspirate and re-dispense
[amount] | Volume,number | *optional* | Amount to aspirate, either as a fraction or absolute volume
[program] | string | *optional* | Program identifier

### `pipetter._aspirate`

Aspirate liquids from sources into syringes.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
program | string |  | Program identifier
[items] | array | *optional* | Data about what should be aspirated from where

### `pipetter._dispense`

Dispense liquids from sryinges into destinations.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
program | string |  | Program identifier
[items] | array | *optional* | Data about what should be dispensed where

### `pipetter._measureVolume`

Measure well volume using pipetter tips.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
[program] | string | *optional* | Program identifier
items | array |  | List of parameters for individual wells
[output] |  | *optional* | Output definition for where and how to save the measurements

### `pipetter._mix`

Mix liquids by aspirating and re-dispensing.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
program | string |  | Program identifier
[itemDefaults] |  | *optional* | Default values for items
[items] | array | *optional* | Data about mixing

### `pipetter._pipette`

Pipette liquids from sources to destinations.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
program | string |  | Program identifier
[sourceProgram] | string | *optional* | Program identifier for aspirating from source, if it should differ from 'program'
[items] | array | *optional* | Data about what should be pipetted where
[sourceMixing] | pipetter._MixSpec | *optional* | Parameters for mixing the source prior to aspiration
[destinationMixing] | pipetter._MixSpec | *optional* | Parameters for mixing the destination after dispense

### `pipetter._punctureSeal`

Puncture the seal on a plate using pipetter tips.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
items | array |  | List of parameters for individual punctures

### `pipetter._washTips`

Clean the pipetter tips.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
program | name,object |  | Program identifier
syringes | array |  | List of syringe identifiers
intensity | pipetter.CleaningIntensity |  | Intensity of the cleaning

### `pipetter.cleanTips`

Clean the pipetter tips.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | string | *optional* | Program identifier
[items] | array | *optional* | List of which syringes to clean at which intensity
[syringes] | array | *optional* | Optional list of syringes to serve as default for missing syringes in items list
[intensity] | pipetter.CleaningIntensity | *optional* | Optional intensity to serve as default intensity for missing intensities in items list

### `pipetter.measureVolume`

Measure well volume using pipetter tips.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[items] | array | *optional* | List of parameters for individual wells to measure
[wellLabware] | Plate | *optional* | Labware for wells
[wells] | Wells | *optional* | Specifier for well(s) to measure, if missing from items
[syringes] | array | *optional* | Specifier for syringe(s) to use, if missing from items
[clean] | string | *optional* | Intensity of cleaning
[cleanBegin] | string | *optional* | intensity of cleaning before the first puncture.
[cleanBetween] | string | *optional* | Intensity of cleaning between wells.
[cleanEnd] | string | *optional* | Intensity of cleaning after the last puncture.
[tipModel] | string | *optional* | Tip model identifier, in order to use a specific tip model
[output] |  | *optional* | Output definition for where and how to save the measurements

### `pipetter.mix`

Mix well contents by aspirating and re-dispensing.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | string | *optional* | Program identifier
[items] | array | *optional* | Data about which wells to mix and how to mix them
[wellLabware] | Plate | *optional* | Labware for wells
[wells] | Wells | *optional* | Specifier for well(s) to mix, if missing from items
[counts] | integer | *optional* | Number of times to aspirate, if missing from items
[amounts] | Volume,number | *optional* | Amount to aspirate, either as a fraction or absolute volume, if missing from items
[syringes] | array | *optional* | Specifier for syringe(s) to use, if missing from items
[clean] | string | *optional* | Intensity of cleaning
[cleanBegin] | string | *optional* | intensity of first cleaning at beginning of pipetting, before first aspiration.
[cleanBetween] | string | *optional* | Intensity of cleaning between different liquids.
[cleanBetweenSameSource] | string | *optional* | Intensity of cleaning between transfers of the same liquid.
[cleanEnd] | string | *optional* | Intensity of cleaning after the last dispense.

### `pipetter.pipette`

Pipette liquids from sources to destinations.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | string | *optional* | Program identifier
[sourceProgram] | string | *optional* | Program identifier for aspirating from source, if it should differ from 'program'
[items] | array | *optional* | Data about what should be pipetted where
[sourceLabware] | Plate | *optional* | Labware for source wells
[destinationLabware] | Plate | *optional* | Labware for source wells
[sources] | Sources | *optional* | Specifier for source(s) to aspirate from, if missing from items
[destinations] | Wells | *optional* | Specifier for destination(s) to despense to, if missing from items
[volumes] | Volumes | *optional* | Volume(s) to pipette, if missing from items
[syringes] | array | *optional* | Specifier for syringe(s) to use, if missing from items
[clean] | string | *optional* | Intensity of cleaning
[cleanBegin] | string | *optional* | intensity of first cleaning at beginning of pipetting, before first aspiration.
[cleanBetween] | string | *optional* | Intensity of cleaning between different liquids.
[cleanBetweenSameSource] | string | *optional* | Intensity of cleaning between transfers of the same liquid.
[cleanEnd] | string | *optional* | Intensity of cleaning after the last dispense.
[sourceMixing] | boolean,pipetter.MixSpec | *optional* | Parameters for mixing the source wells with pipetter before aspirating
[destinationMixing] | boolean,pipetter.MixSpec | *optional* | Parameters for mixing the destination after dispense
[tipModel] | string | *optional* | Tip model identifier, in order to use a specific tip model

### `pipetter.punctureSeal`

Puncture the seal on a plate using pipetter tips.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[items] | array | *optional* | List of parameters for individual punctures
[wells] | Wells | *optional* | Specifier for well(s) to puncture, if missing from items
[syringes] | array | *optional* | Specifier for syringe(s) to use, if missing from items
[distances] | Length,array | *optional* | Distance for puncturing
[clean] | string | *optional* | Intensity of cleaning
[cleanBegin] | string | *optional* | intensity of cleaning before the first puncture.
[cleanBetween] | string | *optional* | Intensity of cleaning between wells.
[cleanEnd] | string | *optional* | Intensity of cleaning after the last puncture.
[tipModel] | string | *optional* | Tip model identifier, in order to use a specific tip model

### `pipetter.pipetteDilutionSeries`

Pipette a dilution series.

First the diluent is distributed to the destination wells (if diluent is specified).
Then the source is transfered to the first destination well of each item (if source is specified -- otherwise the first destination well is assumed to be already prepared).
Then aliquots are serially transfered from the first destination well to the next, and so on for each item.
In general, mixing should be performed after each dilution dispense, and perhaps also before the first aspriation -- this can be specified how ???


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | string | *optional* | Program identifier
[dilutionFactor] | number | *optional* | Dilution factor by which each subsequent well is diluted
[dilutionMethod] |  | *optional* | How to dilution -- `begin`: distribute diluent to destination wells first (default); `before`: add diluent right before transfering aliquot; `after`: add diluent right after transfering aliquot; `source`: dilute the well that the aliquot will be extracted from.
[lastWellHandling] | string | *optional* | How to handle the last well in a series
items | array |  | Array of dilution items
[diluent] | Source | *optional* | Diluent to be used in dilution
[sourceLabware] | Plate | *optional* | Labware for source wells
[destinationLabware] | Plate | *optional* | Labware for source wells
[volume] | Volume | *optional* | Final volume of dilution wells (with the possible exception of the last well, see parameter `lastWellHandling`)
[cleanBegin] | string | *optional* | Intensity of first cleaning at beginning of pipetting, before first aspiration.
[cleanEnd] | string | *optional* | Intensity of cleaning after the last dispense.
[sourceParams] |  | *optional* | Extra parameters for pipetting the source
[diluentParams] |  | *optional* | Extra parameters for pipetting the diluent
[dilutionParams] |  | *optional* | Extra parameters for pipetting the diluted aliquots

### `pipetter.pipetteMixtures`

Pipette the given mixtures into the given destinations.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | string | *optional* | Program identifier
mixtures | array |  | Array of arrays, where each sub-array is a list of components to be mixed into a destination well
[sourceLabware] | Plate | *optional* | Labware for source wells
[destinationLabware] | Plate | *optional* | Labware for source wells
[destinations] | Wells | *optional* | Destination specifier
[order] | array | *optional* | Order in which to pipette the mixtures.  Defaults to the order given in the mixtures array.


## <a name="scale"></a>scale

The `scale` commands specify actions using weight scale equipment.


### `scale.ScaleProgram`

Program for scale.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------

### `scale.weigh`

Weight an object

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | scale.ScaleProgram | *optional* | Program for shaking
object | Plate |  | Plate identifier
[site] | Site | *optional* | Site identifier on scale
[destinationAfter] | SiteOrStay | *optional* | Site to move the plate to after this command


## <a name="sealer"></a>sealer

The `sealer` commands specify actions using sealing equipment.


### `sealer.sealPlate`

Seal a plate.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | string | *optional* | Program identifier for sealing
object | Plate |  | Plate identifier
[site] | Site | *optional* | Site identifier of sealer
[count] | number | *optional* | Number of times to seal (defaults to 1)
[destinationAfter] | SiteOrStay | *optional* | Site to move the plate to after this command


## <a name="shaker"></a>shaker

The `shaker` commands specify actions using shaker equipment.


### `shaker.ShakerProgram`

Program for shaker.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[amplitude] |  | *optional* | Amplitude
duration | Duration |  | Duration of shaking
[rpm] | number | *optional* | Rotations per minute (RPM)

### `shaker.shakePlate`

Shake a plate.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
program | shaker.ShakerProgram |  | Program for shaking
object | Plate |  | Plate identifier
[site] | Site | *optional* | Site identifier on shaker
[destinationAfter] | SiteOrStay | *optional* | Site to move the plate to after this command

### `shaker.run`

Run the shaker.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
program | shaker.ShakerProgram |  | Program for shaking


## <a name="system"></a>system

The `system` commands specify several general, high-level actions that are
not specific to any particular type of equipment.


### `system._description`

Include the value as a description in the generated script

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[text] | string | *optional* | Description text

### `system._echo`

A trouble-shooting function to echo something

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[name] | string | *optional* | Name of echoed thing
[value] |  | *optional* | Thing to echo

### `system.call`

Call a template function.

The template function should be an object of type `Template` with a property `template` holding either a Mustache template string or an object whose properties may be Mustache template strings. The template will be expanded using the values passed in the `params` property.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
name | Object |  | Name of the template function.
[params] | object | *optional* | Parameters to pass to the template function.

### `system.description`

Output the value as a description to the generated script

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[value] |  | *optional* | Value to use as description

### `system.echo`

A trouble-shooting function to echo a variable or text

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[value] |  | *optional* | Value to echo

### `system.if`

Conditionally execute steps depending on a conditional test.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
test | boolean |  | A boolean value; if true, the `then` steps are executed.
[then] | object | *optional* | The sequence of steps to perform if the test is true.
[else] | object | *optional* | The sequence of steps to perform if the test is false.

### `system.repeat`

Repeat the given command a given number of times.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
count | integer |  | The number of times to repeat.
[steps] | object | *optional* | The sequence of commands to repeat.
[variableName] | string | *optional* | If provided, a variable will this name will be added to the scope containing the loop index (starting from 1).

### `system.runtimeExitLoop`

Perform a run-time check to test whether execution should exit the current loop

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
testType |  |  | The type of code to execute to determine whether to break; a JSON truthy value should be returned.
test | string |  | The code to execute at runtime that will test whether to exit.

### `system.runtimeLoadVariables`

Handle steps which require runtime variables

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
varset | string |  | Name of the variable set to load
variables | array |  | Array of variables to load from 'varset'

### `system.runtimeSteps`

Handle steps which require runtime variables

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
varset | string |  | Name of the variable set to load
variables | array |  | Array of variables to load from 'varset'
steps | array,object |  | Steps to compile at runtime


## <a name="timer"></a>timer

The `timer` commands specify actions using timer equipment.


### `timer._sleep`

Sleep for a given duration using a specific timer.

Handler should return `effects` that the timer is not running.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
duration | Duration |  | Duration to sleep
[stop] | boolean | *optional* | Whether to stop the timer after waiting, or let it continue

### `timer._start`

Start the given timer.

Handler should return `effects` that the timer is running.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier

### `timer._stop`

Stop the given timer.

Handler should return `effects` that the timer is not running.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier

### `timer._wait`

Wait until the given timer has reacher the given elapsed time.

Handler should:

- expect that the timer (identified by the `equipment` parameter) is running
- return `effects` that the timer is not running


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
till | Duration |  | Number of seconds to wait till from the time the timer was started
stop | boolean |  | Whether to stop the timer after waiting, or let it continue

### `timer.doAndWait`

A control construct to perform the given sub-steps and then wait
until a certain amount of time has elapsed since the beginning of this command.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
duration | Duration |  | Duration that this command should last
steps | object,array |  | Sub-steps to perform

### `timer.sleep`

Sleep for a given duration.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
duration | Duration |  | Duration to sleep

### `timer.start`

Start a timer.

If no parameters are supplied, a timer will be automatically chosen.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier

### `timer.stop`

Stop a running a timer.

If only one timer is running, this command can be called without any parameters.
Otherwise, the equipment identifier must be supplied.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier

### `timer.wait`

Wait until the given timer has reacher the given elapsed time.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
till | Duration |  | Time that the timer should reach before continuing with the next step
stop | boolean |  | Whether to stop the timer after waiting, or let it continue


## <a name="transporter"></a>transporter

The `transporter` commands specify actions using equipment to transport
labware from one location to another.


### `transporter._movePlate`

Transport a plate to a destination.

Handler should return `effects` with the plate's new location.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
agent | Agent |  | Agent identifier
equipment | Equipment |  | Equipment identifier
[program] | name | *optional* | Program identifier for transport
object | Plate |  | Plate identifier
destination | Site |  | Site to move the plate to

### `transporter.doThenRestoreLocation`

Perform steps, then make sure that the given labware is stored to the
locations before the steps.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
objects | array |  | Plate identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | name | *optional* | Program identifier for transport
[steps] | object,array | *optional* | Sub-steps to perform

### `transporter.movePlate`

Transport a plate to a destination.


Properties:

Name | Type | Argument | Description
-----|------|----------|------------
[agent] | Agent | *optional* | Agent identifier
[equipment] | Equipment | *optional* | Equipment identifier
[program] | name | *optional* | Program identifier for transport
object | Plate |  | Plate identifier
destination | Site |  | Site to move the plate to
