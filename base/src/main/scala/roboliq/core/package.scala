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
 * 
 * 
 * 
 * ==Objects==
 * 
 * ===Models===
 * 
 * TipModel: [[roboliq.core.TipModel]]
 * 
 * PlateModel: [[roboliq.core.PlateModel]]
 * 
 * 
 * 
 *  
 * ==Events and State==
 * 
 * A State object has properties which represent the accumulated effect of its events.
 * Events inherit from [[roboliq.core.EventBean]].
 * They have an `update` method to update the object's state.
 * 
 * 
 * 
 * 
 * ==Commands==
 * 
 * Command data is contained in a [[roboliq.core.CmdBean]].
 * The code which actually handles the command processing is in a class which
 * inherits from [[roboliq.core.CmdHandler]].
 * 
 * There are multiple phases to the evaluation of a command.
 * 
 * - The ''check'' phase gathers all variables which be needed for its execution
 *   from the [[roboliq.core.ObjBase]].  A command may need to obtain several kinds of information:
 *   objects, object states, object settings.
 *   
 * - The ''handle'' phase translates the command into a list of subcommands or tokens.
 *   Tokens are used by the robot-specific translator to generate its scripts.
 * 
 * When a command is checked, it may find that 1) not all information is available which it needs
 * or 2) some preprocessing needs to be performed.
 * 
 * Missing information includes things like the location where a plate should be placed on the bench,
 * which new plates to use when new new plates are required, or which of several thermocyclers to 
 * use if more than one is available.
 * After getting a list of missing information, the processors ([[roboliq.core.Processor]]) can
 * try to find sensible defaults.  The remaining values must be chosen by the user.  Once that is
 * done, the commands should be processed again, now with the complete information set.
 * 
 * When preprocessing needs to be performed, the processor should decide whether this should
 * be done by the user, performed automatically, or whether scripts should be executed prior to this script. 
 * 
 * When scripts should be executed first, those should be performed under the user's oversight,
 * and then the user can return to the original script.
 * 
 * For preprocessing which can be performed automatically,
 * the appropriate commands should be prepended to the command list,
 * and the whole list should be processed again.
 * 
 * For proprocessing which must be done by the user, instructions should be provided.
 * Internally, processor assumes that the user performed the instructions successfully,
 * and the information is set accordingly, and the command list is processed again.
 * 
 */
package object core {}