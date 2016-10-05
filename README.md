# Roboliq

Roboliq aim to make it easier to use liquid handling robots for automation
in biological laboratories.

It provides a format for writing protocols in a portable manner (so that
they can be used in other labs too!) and for compiling such protocols
for execution by a liquid handling robot.

The only supported backend is for Tecan Evoware robots, but other backends
can also be added.

The important sub-directories are:

* `roboliq`: the source code for processing protocols and making sure they are complete
* `roboliq/src/evoware`: the source code for the Tecan Evoware backend
* `runtime-server`: the source code for a server which handles logging and measurements while experiments are running

# Getting started

Let's get started with Roboliq by starting with the most basic of protocols and
then building up from there.  We'll walk through these steps:

* a protocol that doesn't require any configuration or backend
* a protocol with a minimal configuration
* a protocol with a backend
* a protocol compiled for Evoware
* running the server and getting the data during execution of an Evoware script

## The simplest protocol

We'll test a simple protocol to make sure that roboliq 
To get you up and running with Roboliq

TODO STEPS:
* [ ] 


Let's assume that you have already configured Roboliq for your lab, and you
are ready to write and run your first experiment.  (But beware: configuring
a lab is a very complicated step, which is discussed in the next section).

For this example, we'll simply dispense a liquid into each well of a 384-well
plate, seal it, and shake it.  The script could look like this.

```yaml
roboliq: v1                                # version of Roboliq being used
description: |                             # description of this protocol;the bar symbol (|) allows for multi-line text
  Dispense a liquid into each well of a
  384-well plate, seal it, and shake it.
objects:                                   # the set of materials used in this protocol
  plate1:                                  # an object named "plate1"
    type: Plate                            # which is a type of plate
    model: ourlab.???                      # whose model is defined in the configuration as "ourlab.???"
    location: ourlab.robot1.site.P1        # which the user should place at the location "P1", as defined in the configuration
  liquidLabware1:                          # 
    type: Plate
    model: ourlab.???
    location: ourlab.robot1.site.P2
  liquid1:
    type: Liquid
    wells: liquidLabware1(A01 down D01)
steps:
  1:
    command: pipetter.pipette
    sources: liquid1
    destinations: plate1(all)
    volumes: 40ul
  2:
    command: sealer.sealPlate
    object: plate1
  3:
    command: shake.shakePlate
    object: plate1
```

Copy that content into a file 
