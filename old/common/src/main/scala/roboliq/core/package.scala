package robolic

/**
 * ==Levels of command token abstraction==
 * 
 * L4 is for sequential execution without feedback.
 * 
 * L4: not all parameters need to be specified if defaults can be chosen
 * 
 * L3: tokens have all L4 parameters specified
 * 
 * Translation L3 to L2: here is where the bulk of decision making can be performed.
 * All well locations need to be made explicit rather than referring to liquids or
 * all wells of a plate.
 * 
 * L2: tokens have access to *ConfigL2 and *StateL2 objects.
 * 
 * L1: tokens do not have access to RobotState information.
 * The idea is to have the command fully specified by its parameters.
 * They should be a simple as possible in order to make the cross-platform translators
 * for various robot platforms as simple as possible.
 * (NOTE: As of 2012-03-28, the L1 commands receive *ConfigL2 objects for tips and wells.
 * It would be even better if they received *Id objects instead of the *ConfigL2 objects.)
 * 
 * Translation L1 to L0: performed by a translator which was designed for a specific robot platform.
 * 
 * L0: concrete tokens for the target robot
 * 
 */
package object core {}