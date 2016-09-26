/**
 * Namespace for the ``absorbanceReader`` commands.
 * @namespace types
 * @version v1 
 */

/**
 * Reader equipment.
 * 
 * @typedef Reader
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Centrifuge equipment.
 * 
 * @typedef Centrifuge
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property {array} sitesInternal 
 * @example
 * ```
 * "centrifuge": {
 * 	"type": "Centrifuge",
 * 	"sitesInternal": ["ourlab.mario.site.CENTRIFUGE_1", "ourlab.mario.site.CENTRIFUGE_2", "ourlab.mario.site.CENTRIFUGE_3", "ourlab.mario.site.CENTRIFUGE_4"]
 * }
 * ```
 * 
 */



/**
 * Incubator equipment.
 * 
 * @typedef Incubator
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property {array} sitesInternal 
 * @example
 * ```
 * "incubator": {
 *   "type": "Incubator",
 *   "sitesInternal": ["ourlab.luigi.site.BOX_1", "ourlab.luigi.site.BOX_2", "ourlab.luigi.site.BOX_3", "ourlab.luigi.site.BOX_4", "ourlab.luigi.site.BOX_5", "ourlab.luigi.site.BOX_6", "ourlab.luigi.site.BOX_7", "ourlab.luigi.site.BOX_8"],
 * }
 * ```
 * 
 */



/**
 * Pipetting equipment.
 * 
 * @typedef Pipetter
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Pipetting syringe.
 * 
 * @typedef Syringe
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property {string} [tipModel] - Tip model identifier
 * @property {string} [tipModelPermanent] 
 */



/**
 * An agent that can execute commands.
 * 
 * @typedef Agent
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Specification of an experimental design.
 * 
 * @typedef Design
 * @memberof types
 * @property  type 
 * @property {object} [conditions] 
 * @property {array} [actions] 
 */



/**
 * Liquid substance.
 * 
 * @typedef Liquid
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property {array} [wells] 
 */



/**
 * Plate labware.
 * 
 * @typedef Plate
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property {PlateModel} [model] 
 * @property {Site} [location] 
 * @property {object|array} [contents] 
 */



/**
 * Model for plate labware.
 * 
 * @typedef PlateModel
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property {integer} rows 
 * @property {integer} columns 
 */



/**
 * Represents a bench site where labware can placed.
 * 
 * @typedef Site
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * A template object, used by the `system.call` command.
 * 
 * @typedef Template
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property  [template] 
 */



/**
 * User-defined variable.
 * 
 * @typedef Variable
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 * @property  [value] 
 */



/**
 * Scale equipment.
 * 
 * @typedef Scale
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Sealing equipment.
 * 
 * @typedef Sealer
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Shaker equipment.
 * 
 * @typedef Shaker
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Timer equipment.
 * 
 * @typedef Timer
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */



/**
 * Labware transporter equipment.
 * 
 * @typedef Transporter
 * @memberof types
 * @property  type 
 * @property {string} [description] 
 * @property {string} [label] 
 */

