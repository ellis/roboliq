# Roboliq

Roboliq aims to make it easier to use liquid handling robots for automation
in biological laboratories.

It lets you write protocols that are portable between labs,
and it compiles the protocols for execution by liquid handling robots.

The only supported backend is for Tecan Evoware robots, but other backends
can also be added.

The important sub-directories of this repository are:

* `roboliq-processor`: the source code for processing protocols and making sure they are complete
* `roboliq-processor/src/evoware`: the source code for the Tecan Evoware backend
* `roboliq-runtime-cli`: the source code for a command line utility that handles logging and measurements while experiments are running

# Installation

This guide assumes that you have some familiarity with using the command line terminal.

* Install [`nodejs`](https://nodejs.org/en/download/), which lets you execute
  Javascript programs.
* If you're using Microsoft Windows, install the [cygwin terminal](https://cygwin.com/install.html),
  which will be used for typing in commands.
* Copy the Roboliq repository to your computer.
* Open the terminal, navigate to the directory where you copied the Roboliq
  repository, and run `npm install` to download Roboliq's software requirements.

# Getting started

Let's get started with a hands-on walk through.
We'll begin with a very basic protocol and
build up from there as follows:

1. a protocol that doesn't require any configuration or backend
2. a protocol with a minimal configuration
3. a protocol with a backend
4. a protocol compiled for Evoware
5. running the server and getting the data during execution of an Evoware script

## 1. The simplest protocol

We'll test a simple protocol to show you how to run the software.

1) Navigate to the `protocols` subdirectory and copy this code into a file
  named `walkthrough1.yaml` (or copy the file `walkthrough1-sample.yaml`):

    ```yaml
    roboliq: v1
    description: test that doesn't require configuration
    steps:
      command: system.echo
      text: Hello, World!
    ```

2) Open the terminal, and navigate to the directory where you copied the Roboliq repository.

3) Run Roboliq from the terminal, passing `test1.yaml` as input:

    ```sh
    npm run processor -- protocols/walkthrough1.yaml
    ```

    If there were no errors, you can now find a file named `protocols/walkthrough1.out.json`.
    The output file contains a lot of information:
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

6) Run the command above again, but this time add the `-P compiled` argument
    to put the output in a separate `compiled` folder.  

    ```sh
    npm run processor -- protocols/walkthrough1.yaml -P compiled/
    ```

    This will automatically create a subfolder named `compiled/walkthrough1` and it will save
    the file `compiled/walkthrough1/walkthrough1.out.json`.
    It's important to have a separate folder for every experiment in order to
    properly organize measured data for each experimental run.

Now you know how to run the software and write a basic protocol.
Let's continue to the next step, where we'll see how to create a basic
robot configuration file.


## 2. A protocol with a minimal configuration

The robot configuration file lets you specify the capabilities of a robot.
Let's start building up such a file.

```javascript
module.exports = {
  "roboliq": "v1",
  "imports": ["//config/roboliq.js"],
  "objects": {
		"ourlab": {
			"type": "Namespace",
			"mario": {
				"type": "Namespace",
				"roma1": {
					"type": "Transporter",
				},
        "liha": {
					"type": "Pipetter",
					"syringe": {
						"1": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 1
						},
						"2": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 2
						},
						"3": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 3
						},
						"4": {
							"type": "Syringe",
							"tipModel": "ourlab.mario.liha.tipModel.tipModel1000",
							"tipModelPermanent": "ourlab.mario.liha.tipModel.tipModel1000",
							"row": 4
						}
					},
					"tipModel": {
						"tipModel1000": {"type": "TipModel", "programCode": "1000", "min": "3ul", "max": "950ul", "canHandleSeal": false, "canHandleCells": true}
					},
					"tipModelToSyringes": {
						"ourlab.mario.liha.tipModel.tipModel1000": ["ourlab.mario.liha.syringe.1", "ourlab.mario.liha.syringe.2", "ourlab.mario.liha.syringe.3", "ourlab.mario.liha.syringe.4"]
					}
				},
        "site": {
					"type": "Namespace",
          "P1": { "type": "Site" },
          "P2": { "type": "Site" },
          "P3": { "type": "Site" }
        }
};
```

PROBLEM:

* should we automatically load roboliq config?  even if another config is specified?
* how shall we specify which lab config to load?  Command line?  Config file?
* if the lab's config file is in a completely different directory, how shall it load the roboliq config?
* consider adapting ourlab.js to run with plain node, without babel
* how can we run `npm i roboliq` and have it make the binaries available?

Approach:
* create a new directory
* install roboliq by something like `npm i roboliq`
* create an ENV file that tells where to find the config file (or which ones to load)
* create a config file
* config file should load roboliq/evoware modules via a non-relative path
* somehow run roboliq from that directory (I'm just not sure whether the config's `require`s will work)

TODO:

* [x] figure out how to get npm to babel-compile the code when I run `npm install ~/src/roboliq/roboliq-processor`
* [x] figure out how to conveniently load `require(roboliq-processor)`
* [x] build the software using babel so that it runs faster
* [ ] create sample ENV file in root of project
* [ ] figure out tutorial for the user, whereby he creates `protocols`, `config`, and `compiled` sub-directories
    * [ ] put sample files in those directories?  add them to .gitignore, or copy the samples from another directory.
* [ ] make separate project roboliq-tecan-evoware?

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
