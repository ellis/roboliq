### `Reader`

Reader equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Centrifuge`

Centrifuge equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
sitesInternal | array |  | undefined

Example:

```
"centrifuge": {
	"type": "Centrifuge",
	"sitesInternal": ["ourlab.mario.site.CENTRIFUGE_1", "ourlab.mario.site.CENTRIFUGE_2", "ourlab.mario.site.CENTRIFUGE_3", "ourlab.mario.site.CENTRIFUGE_4"]
}
```



### `Incubator`

Incubator equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
sitesInternal | array |  | undefined

Example:

```
"incubator": {
  "type": "Incubator",
  "sitesInternal": ["ourlab.luigi.site.BOX_1", "ourlab.luigi.site.BOX_2", "ourlab.luigi.site.BOX_3", "ourlab.luigi.site.BOX_4", "ourlab.luigi.site.BOX_5", "ourlab.luigi.site.BOX_6", "ourlab.luigi.site.BOX_7", "ourlab.luigi.site.BOX_8"],
}
```



### `Pipetter`

Pipetting equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Syringe`

Pipetting syringe.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
[tipModel] | string | *optional* | Tip model identifier
[tipModelPermanent] | string | *optional* | undefined

### `Agent`

An agent that can execute commands.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Design`

Specification of an experimental design.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[conditions] | object | *optional* | undefined
[actions] | array | *optional* | undefined

### `Liquid`

Liquid substance.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
[wells] | array | *optional* | undefined

### `Plate`

Plate labware.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
[model] | PlateModel | *optional* | undefined
[location] | Site | *optional* | undefined
[contents] | object,array | *optional* | undefined

### `PlateModel`

Model for plate labware.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
rows | integer |  | undefined
columns | integer |  | undefined

### `Site`

Represents a bench site where labware can placed.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Template`

A template object, used by the `system.call` command.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
[template] |  | *optional* | undefined

### `Variable`

User-defined variable.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined
[value] |  | *optional* | undefined

### `Scale`

Scale equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Sealer`

Sealing equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Shaker`

Shaker equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Timer`

Timer equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined

### `Transporter`

Labware transporter equipment.

Properties:

Name | Type | Argument | Description
-----|------|----------|------------
type |  |  | undefined
[description] | string | *optional* | undefined
[label] | string | *optional* | undefined