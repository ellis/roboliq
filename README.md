# Roboliq

Roboliq aim to make it easier to use liquid handling robots for automation
in biological laboratories.

It provides a format for writing protocols in a portable manner (so that
they can be used in other labs too!) and for compiling such protocols
for execution by a liquid handling robot.

The only supported backend is for Tecan Evoware robots, but other backends
can also be added.

The important sub-directories are:

* `roboliq-processor`: the source code for processing protocols and making sure they are complete
* `roboliq-processor/src/evoware`: the source code for the Tecan Evoware backend
* `roboliq-runtime-cli`: the source code for a command line utility that handles logging and measurements while experiments are running

# Getting started

Let's get started with Roboliq with a hands-on walk through.
We'll begin with the most basic of protocols and
build up from there as follows:

* a protocol that doesn't require any configuration or backend
* a protocol with a minimal configuration
* a protocol with a backend
* a protocol compiled for Evoware
* running the server and getting the data during execution of an Evoware script

## The simplest protocol

We'll test a simple protocol to show you how to run the software.

1) Create a new folder that will contains your protocols.

2) Create a new folder that will contain the compiled versions of your protocols.

3) Copy this code into a file named `test1.yaml`:

    ```yaml
    roboliq: v1
    description: test that doesn't require configuration
    steps:
      command: system.echo
      text: Hello, World!
    ```

4) To run Roboliq, you'll first need to open a terminal window and navigate to
the `roboliq-processor` subdirectory.

5) Run Roboliq from the terminal, passing `test1.yaml` as input:

    ```sh
    npm start $PROTOCOLS/test1.yaml
    ```

    whereby you'll need to substitute `$PROTOCOLS` with the path to your
    protocols folder created in step 1.
    If there were no errors, you can now find a file named `test1.out.json` in
    your protocols folder.  The output file contains a lot of information:
    in addition to your script, it also contains the core configuration data
    that comes with Roboliq by default.  If you open the file, you can find
    a `steps` property which looks like this:

    ```json
    "steps": {
    	"1": {
    		"command": "system._echo",
    		"value": "Hello, World!"
    	},
    	"command": "system.echo",
    	"value": "Hello, World!"
    }
    ```

    You can see a substep with the command `system._echo`.  Commands that
    begin with and underscore are "final" instructions that
    will be passed the the backend without further processing.
    It may be confusing that the original command (`system.echo`) and its
    properties appear *after* the subcommand, but this is just an artifact of
    how JavaScript always prints numeric properties first in JSON data.

    An important difference between `system.echo` and `system._echo` is that
    `system.echo` is a higher level command that can handles variables,
    whereas `system._echo` just takes the value verbatim.

6) Let's run the same command again, but this time we'll put the output in its
    proper folder, which you created in step 2.  Run this from the terminal:

    ```sh
    npm start -- $PROTOCOLS/test1.yaml -P $COMPILED
    ```

    This will automatically create a subfolder named `test1` and it will save
    the file `test1.out.json` in that folder.
    It's important to have a separate folder for each experiment in order to
    properly organize measured data for each experimental run.

CONTINUE


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
