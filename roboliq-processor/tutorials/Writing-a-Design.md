# Writing a Design Specification

In Roboliq terminology, a *Design Table* is a table that specifies all relevant
factors for later analyzing the experimental results.

Experiments on microwell plates can easily involve hundreds of unique
liquid combinations, so specifying them manually can be tedious and error-prone.
Here we present a short-hand for creating design tables.

WARNING: This is very abstract.  Don't bother with it if you're looking for something
easy.  That said, it makes complex experiments much, much easier to design,
trouble-shoot, and analyze.

NOTE: this document uses the following terms interchangeably: column, factor, variable.

# First examples

Let's create a design table with a single row like this:

plate | source | destination | volume
----- | ------ | ----------- | -----:
plate1 | water | A01 | 25 ul

This table could be used to specify a single pipetting operation that dispenses 25 ul of water into well `A01` of `plate1`.

Here is how to specify this in Roboliq:

```yaml
roboliq: v1
objects:
  design1:
    type: Design
    description: First example
    conditions:
      plate: plate1
      source: water
      destination: A01
      volume: 25 ul
---
```

Within a Roboliq protocol, the design would be place in the `objects` property.  Its object name is `design1`, its type is `Design`, and it has a description.
The `conditions` property is where factors are specified.




Let's expand the design table a bit to dispenses 25 ul of water into several destinations.

plate | source | destination | volume
----- | ------ | ----------- | -----:
plate1 | water | A01 | 25 ul
plate1 | water | B01 | 25 ul
plate1 | water | C01 | 25 ul

To specify this in Roboliq, we change the `destination` property to a *branching* factor:

```yaml
roboliq: v1
objects:
  design1:
    type: Design
    description: First example
    conditions:
      plate: plate1
      source: water
      destination*: [A01, B01, C01]
      volume: 25 ul
---
```

Notice the asterisk in `destination*`.  An asterisk at the end of a factor name indicates *branching*.  Branching factors require an array of values, and for each value in the array, the existing rows are first replicated and then the value is assigned to the factor in that row.



Now let's assign a different volume to each row:

plate | source | destination | volume
----- | ------ | ----------- | -----:
plate1 | water | A01 | 25 ul
plate1 | water | B01 | 50 ul
plate1 | water | C01 | 75 ul

To specify this in Roboliq, we change the `volume` property to an array:

```yaml
roboliq: v1
objects:
  design1:
    type: Design
    description: First example
    conditions:
      plate: plate1
      source: water
      destination*: [A01, B01, C01]
      volume: [25 ul, 50 ul, 75 ul]
---
```

When a factor value is an array, a new column is added with those values.

**Try it**. Copy the above code to a new file in Roboliq's root directory named `design1Test.yaml`.  Open a terminal and change directory to the Roboliq root, and run the following command:

```sh
npm run design -- --path objects.design1 design1Test.yaml
```

It should produce this output:

```
plate   source  destination  volume
======  ======  ===========  ======
plate1  water   A01          25 ul
plate1  water   B01          50 ul
plate1  water   C01          75 ul
======  ======  ===========  ======
```




# Nested branching

Nested branching provides a lot of power to the design specification, but it
is also where the complexity starts.

plate | destination | source | volume | liquidClass
----- | ------ | ----------- | -----: | -------
plate1 | A01 | water | 50 ul | Roboliq_Water_Air_1000
plate1 | A01 | dye | 25 ul | Roboliq_Water_Air_1000
plate1 | B01 | water | 50 ul | Roboliq_Water_Air_1000
plate1 | B01 | dye | 25 ul | Roboliq_Water_Air_1000
plate1 | C01 | water | 50 ul | Roboliq_Water_Air_1000
plate1 | C01 | dye | 25 ul | Roboliq_Water_Air_1000

```yaml
roboliq: v1
objects:
  design2:
    type: Design
    description: Nested example
    conditions:
      plate: plate1
      destination*: [A01, B01, C01]
      source*:
        water:
          volume: 50 ul
        dye:
          volume: 25 ul
      liquidClass: Roboliq_Water_Air_1000
---
```
