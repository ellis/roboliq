# Writing a Roboliq protocol

Roboliq was developed to help automate protocols on liquid handling robots,
especially for lab experiments in molecular biology.
This tutorial will explain the four main parts of a protocol and how to write
write them.

First consider this short example -- it uses Roboliq version "v1"; it defines a `Plate` named `sourcePlate`; and it instructs us to shake the plate:

```yaml
roboliq: v1
description: Shake a plate, just because we can
objects:
  sourcePlate:
    type: Plate
steps:
  1:
    command: shaker.shakePlate
    object: sourcePlate
```

The protocol consists of *properties* and *values*.
A *property* is a name followed by a semicolon and then a *value*.
In the above example, we see the following properties and values:

1) The first property is `roboliq`, and it has a string value of `v1` which
  indicates we're using Roboliq version 1.

2) The second property is `description`, whose value is also a text string.

3) The third property is `objects`, which defines the materials we'll use in the
  protocol.  In contrast to the previous properties, the *value* starts on the
  next line and is indented by two spaces.  This means that the value of
  `objects` is another set of properties: `sourcePlate` is the name
  of a material whose `type` property is set to `Plate`.

4) The forth property is `steps`, which defines the steps to be performed.
  Its *value* is usually a numbered set of steps, and each numbered step
  is assigned properties as well: in this case, step `1` has the properties
  `command: shaker.shakePlate` and `object: sourcePlate` which tell Roboliq
  to shake the plate named `sourcePlate`.

Most of your protocols will have those four main properties (`roboliq`,
`description`, `objects`, and `steps`), and perhaps additional properties as well.

Next we will describe the `objects` and `steps` properties in more detail.

# Objects

In order to add more objects, just name them and assign them appropriate properties.
Here we add another plate for mixing named `mixPlate`:

```yaml
...
objects:
  sourcePlate:
    type: Plate
  mixPlate:
    type: Plate
...
```

Objects are the things that can be used in the protocol's steps, including
materials, equipment, and even user-defined variables.
Each object requires a `type` property (e.g. `Plate` in the
example above) -- the most common types used in protocols are:

* [`Plate`]{@link types.Plate}: for defining labware, including tubes and troughs.
* [`Liquid`]{@link types.Liquid}: for defining liquids.
* [`Variable`]{@link types.Variable}: for defining variables that the user can change.
  `Variable` object lets you name a value which can be referenced by name in
  the protocol steps -- this is a good idea if the steps reference the value
	multiple times and you expect to change the value in the future.
* [`Design`]{@link types.Design}: for defining an experimental design.
  The `Design` type facilitates complex experimental designs, and you can read
  more about it in the [Design Documentation]{@tutorial Writing-a-Design}.

A complete list of types can be found in the [type documentation]{@link types},
but most of them are only used in the
[robot configuration]{@tutorial write-a-robot-configuration} rather than in protocols.

In addition to the `type` property, object have other properties as well
(also available in the [type documentation]{@link types}), and
all objects have an optional `description` property that you can use to
add your own notes about the object.
Here are examples for each of the four main object types:

```yaml
objects:
  plate1:
    type: Plate
    description: Plate to be used for initial mixing
    model: ourlab.roboto.model.96microwell
    location: ourlab.roboto.P1
  specialMix:
    type: Liquid
    description: Our special mad-scientist mix
    wells: plate1(all)
  extractionVolume:
    type: Variable
    description: Amount to extract
    value: 100 ul
  design1:
    type: Design
    description: Assign various dilutions to each well on plate1
    conditions:
      ... {properties not included here}
```


# Steps

The `steps` property specifies the actions to be taken.  Normally, this is
a numbered list of commands, such as this example of sealing and shaking
a plate:

```yaml
steps:
  1:
    command: sealer.sealPlate
    object: plate1
  2:
    command: shaker.shakePlate
    object: plate1
```

A *command* is indicated by a `command` property, such as the `sealer.sealPlate`
and `shaker.shakePlate` commands above.
Besides the `command` property, you will need to specify additional properties
to tell the command exactly what to do.  In the above example, the line
`object: plate1` tells the command to act on `plate1`.

The 
Structure of command names
List of available commands

A step is either a command or a numbered list of sub-steps.



Numbered sub-steps

`description`, `data`