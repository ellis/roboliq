# Writing a Roboliq protocol

Roboliq was developed to handle protocols in molecular biology, with the particular goal of supporting liquid handling robots for lab automation.
The protocols are written with a particular structure, and this tutorial will lead you through the process.

First consider this short example -- it uses Roboliq version "v1"; it defines a `Plate` named `sourcePlate`; and it instructs us to shake the plate:

```yaml
roboliq: v1
objects:
  sourcePlate:
    type: Plate
steps:
  1:
    command: shaker.shakePlate
    object: sourcePlate
```

Most protocols have those three main components: `roboliq` specifies the version of Roboliq used, `objects` are the required materials, and `steps` are the commands to be performed.
The protocol will mostly be written in the style above, where a property name (e.g. `type`) is followed by a semicolon and either
1) a value (e.g. `Plate`), or
2) a new-line, two blank spaces, and then sub-properties.

<aside>
The format used here is YAML, which is a well-established format for encoding program data that is also reasonably legible for casual users.
It is also possible to use the JSON format instead of YAML.
</aside>

To add more objects, just name them and assign them appropriate properties.
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

# Objects

Objects can have certain properties.
For example, all objects require a `type` property that indicates what type of
object it is, and all objects can have an optional `description` with helpful
descriptive test.
The most common object types are:

* `Plate`: plate labware
* `Tube`: tube labware
* `Liquid`: liquid substance
* `Variable`: a user-defined variable



# Advanced remarks

* It is possible to use the JSON format instead of YAML.

# TODOS:

- [ ] variables
- [ ] include information about the basic object types and their properties
- [ ] complete list of object types and their properties
