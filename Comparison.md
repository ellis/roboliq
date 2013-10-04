# Comparison to RoboEase

## Advantages

* Uses standard file formats (JSON and YAML), which allows other programs to easily read and write protocols
* Multi-agent coordination
* Do not need to specify plate movement commands
* Takes advantage of existing Evoware configuration files
* Intelligent selection of liquid classes and tip handling
* Adaptation to different labs is more straight-forward (command handling, capabilities)
* Multi-script protocols for allowing multiple labwares to be placed on a single site (at different times)
* FUTURE: Stackable labware
* FUTURE: Partial scripts and script continuation to allow for dynamic feedback
* FUTURE: Probabilistic state

## Disadvantages

* Plate movement instructions to user come one at a time, rather than all at once
* Does not yet support biologist-written subroutines (though programmers can add them)
* Does not yet have a number of the extra functions for helping the user to track labware and reagent usage
* Not tested well like RoboEase

## Unknown

* Initial setup?  Which is easier?
