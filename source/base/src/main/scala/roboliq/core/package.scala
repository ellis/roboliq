package roboliq

import scalaz._
import Scalaz._
import ch.ethz.reactivesim.RsPimper


/**
 * =Roboliq Core=
 * 
 * The `roboliq.core` package contains the classes representing objects,
 * events, databases, commands, and protocols.
 * 
 * 
 * 
 * 
 * ==Objects==
 * 
 * The main objects which roboliq utilizes and manipulates are
 * substances, liquids, vessel contents, vessels, plates, and labware models.
 * 
 * ===Substances===
 * 
 * We have two primary categories of substances: liquids and powders.
 * A liquid has a volume and can be pipetted (see [[roboliq.core.SubstanceLiquid]]).
 * Powders are specified in mol units.
 * Currently there are two powder subclasses:
 * [[roboliq.core.SubstanceDna]] and [[roboliq.core.SubstanceOther]].
 * 
 * ===Liquids===
 * 
 * The [[roboliq.core.Liquid]] represents one or more solvents containing zero or more solutes.
 * A `Liquid` represents the ratios its contents and is therefore independent of volume.
 * Any mixture with the same rations is considered to be the same liquid,
 * so a particular liquid can be present in multiple vessels.
 * 
 * However, the class does not interface well with the other classes;
 * it should be reprogrammed to properly represent ratios in a manner similar to VesselContent. 
 * 
 * ===Vessel contents===
 * 
 * The vessel's contents are represented by [[roboliq.core.VesselContent]].
 * This is similar to the description of liquids above, 
 * except that absolute amounts are used instead of ratios,
 * and the contents are specific to a particular vessel.
 *  
 * ===Vessels===
 * 
 * A vessel is an object which can contain substances, or more precisely has [[roboliq.core.VesselContent]].
 * There are two kinds of vessels:
 * [[roboliq.core.PlateWell]] and [[roboliq.core.Tube]].
 * 
 * Also of interest is the poorly-named [[roboliq.core.Well]],
 * which represents a vessel on a vessel holder.
 * A `PlateWell` is automatically also a `Well`, 
 * whereas a `Tube` doesn't have `Well` information until it has been placed on a rack.
 * 
 * ===Vessel holders===
 * 
 * Conceptually, there are two kinds of holders:
 * 1) plates, which have wells built into them, and
 * 2) racks, which can accommodate removable tubes.
 * Currently, we assume that the tubes on a rack will not change during the execution
 * of a protocol, so we use [[roboliq.core.Plate]] for both cases.
 *  
 * ===Labware models===
 * 
 * Every piece of labware is considered to be an instance of a labware model, as follows:
 * 
 * [[roboliq.core.Tip]]: [[roboliq.core.TipModel]]
 * 
 * [[roboliq.core.Plate]]: [[roboliq.core.PlateModel]]
 * 
 * [[roboliq.core.Tube]]: [[[[roboliq.core.TubeModel]]
 * 
 * 
 * 
 *  
 * ==State and events==
 *
 * *State* refers to the properties of an object which can change over time,
 * and an object's state information represent the cumulative effect of events.
 * [[roboliq.core.StateQuery]] and [[roboliq.core.StateMap]] 
 * are interfaces to the state of all objects in the system.
 * There are two concrete implementations,
 * an immutable [[roboliq.core.RobotState]] and a mutable [[roboliq.core.StateBuilder]].
 * 
 * Events inherit from [[roboliq.core.EventBean]].
 * They have an `update` method to update the object's state.
 *
 * 
 * 
 * 
 * ==Databases==
 *
 * The term "database" is used in a broad sense here to mean a
 * large set of data which can be queried by ID.
 * There are two "databases" in roboliq.
 * 
 * [[roboliq.core.BeanBase]]: holds the YAML JavaBeans which get read in from files.
 * 
 * [[roboliq.core.ObjBase]]: holds instantiations of the objects required for executing
 * a protocol, and also holds a map of the initial state of those objects.
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
 * 
 * 
 * 
 * 
 * ==Levels of command token abstraction==
 * 
 * Command trees go through several levels of processing,
 * starting at the most abstract level `L4` and progressing down to
 * concrete low-level robot instructions at `L0`. 
 * 
 * `L4` is for sequential execution without feedback.
 * Once feedback is added in, we will need an additional level `L5`.
 * 
 * `L4`: not all parameters need to be specified if defaults can be chosen.
 * 
 * `L3`: tokens have all L4 parameters specified.
 * 
 * Translation L3 to L2: here is where the bulk of decision making can be performed.
 * All well locations need to be made explicit rather than referring to liquids or
 * all wells of a plate.
 * 
 * `L2`: tokens have access to `*ConfigL2` and `*StateL2` objects.
 * 
 * `L1`: tokens do not have access to RobotState information.
 * The idea is to have the command fully specified by its parameters.
 * They should be a simple as possible in order to make the cross-platform translators
 * for various robot platforms as simple as possible.
 * 
 * Translation `L1` to `L0`: performed by a translator which was designed for a specific robot platform.
 * 
 * `L0`: concrete tokens for the target robot.
 * 
 * 
 * 
 * 
 */
package object core extends ch.ethz.reactivesim.RsPimper {
	type RsResult[A] = ch.ethz.reactivesim.RsResult[A]
	type RsSuccess[A] = ch.ethz.reactivesim.RsSuccess[A]
	type RsError[A] = ch.ethz.reactivesim.RsError[A]
	val RsResult = ch.ethz.reactivesim.RsResult
	val RsSuccess = ch.ethz.reactivesim.RsSuccess
	val RsError = ch.ethz.reactivesim.RsError

	type RqResult[A] = ch.ethz.reactivesim.RsResult[A]
	type RqSuccess[A] = ch.ethz.reactivesim.RsSuccess[A]
	type RqError[A] = ch.ethz.reactivesim.RsError[A]
	val RqResult = ch.ethz.reactivesim.RsResult
	val RqSuccess = ch.ethz.reactivesim.RsSuccess
	val RqError = ch.ethz.reactivesim.RsError
}