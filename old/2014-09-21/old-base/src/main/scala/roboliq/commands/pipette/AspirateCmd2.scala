/*package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._

class X {
	class Field(val data: Any) {
		def toOption[A]: Option[A] = Option(data.asInstanceOf[A])
		
		def toList[A]: List[A] = {
			if (data == null)
				Nil
			else if (data.isInstanceOf[java.util.List[_]]) {
				val l0 = data.asInstanceOf[java.util.List[A]]
				List(l0 : _*)
			}
			else {
				List(data.asInstanceOf[A])
			}
		}
		
		def toListOf[A](fn: Any => Option[A]): List[A] = {
			if (data == null)
				Nil
			else if (data.isInstanceOf[java.util.List[_]]) {
				val l0 = data.asInstanceOf[java.util.List[_]]
				List(l0.map(fn) : _*)
			}
			else {
				List(fn(data))
			}
		}
	}
	
	/*
	 * Variable
	 * Variable.id
	 * Variable.ver = 0
	 * Variable.calcFnData = (f, [a, b, c])
	 * print graph of variable dependencies
	 * 
	 * a command handler obtains variables from a Map[String, Any]
	 * a command handler returns a variable for a command or token
	 * 
	 * when a variable's value is calculated, its ver is incremented
	 * at that point it also notes the ver of all variables it's dependent on
	 * this allows us to check when a variable should be updated.
	 * Each yaml field should be represented as a Variable.
	 * Other variable properties, such as plate location are also Variables.
	 * In addition, some objects might be variables, especially if they
	 * need to be chosen entirely by the software.  Perhaps all objects
	 * should therefore be variables.
	 * 
	 * commands.1.src = "water"
	 * commands.1.dest = "P1(A01 x D12)"
	 * commands.1.volume = "190e-6"
	 * commands.2.plate = "P1"
	 * commands.2.plateDest = "regrip"
	 * 
	 * Variable(id = "commands.1.src", value = "water")
	 * plate = Variable(id = "P1")
	 * loc = Variable(parent = "", field = "location")
	 * 
	 * A VariableList is a Variable containing other Variables.
	 * 
	 * "Tip1" -> Tip1 -> 
	 * 
	 * Context, Generator
	 */
	
	class Context {
		def param(name: String): Variable
		def getValue[A](v: Variable[A]): Result[A]
	}
	
	def fetchTip(id: String): VariableDb = {
		
	}
	
	class VariableValue[A]
	
	class Variable[A] {
		def calcValue(ctx: Context): Result[A]
		
		def map[B](fn: A => B): Variable[B] =
			new CalculatedVariable1(this, fn)
	}
	
	class CalculatedVariable1[A, B](parent: Variable[A], fn: A => B) extends Variable[B] {
		def calcValue(ctx: Context): Result[B] = {
			ctx.getValue(parent).map(fn)
		}
	}
	
	class ParamVariable(name: String) {
		def getValue(ctx: Context): VariableValue = {
			ctx.param(name)
			null
		}
	}
	
	def fetchTip2(id_v: Variable[String]): Variable[Tip] = {
		val ob: ObjBase = null
		new Variable[Tip] {
			def calcValue(ctx: Context): Result[Tip] = {
				ctx.getValue(id_v).flatMap(ob.findTip)
			}
		}
	}
	
	def parseSpirateItem() {
		val tip_v = param("tip").map(fetchTip)
		val well_v = param("well").map(fetchWell)
		val policy_v = fetchPolicy(varField(cmd, "policy"))
		val mixSpec_v_? = varFieldOpt(cmd, "mixSpec").flatMap(parseMixSpec)
		
		(tip_v |+| well_v |+| policy_v |+| mixSpec_v_?) applyTo (new SpirateTokenItem)
	}
	
	def parseItem(o0: Any): Option[String] {
		val map = o0.asInstanceOf[java.util.Map[String, Any]]
		
	}
	
	def getField(cmd: java.util.Map[String, Any], id: String): Field = {
		cmd.get(id) match {
			case None => new Field(null)
			case Some(null) => new Field(null)
			case Some(o) => new Field(o)
		}
	}
	
	def parseList[A](cmd: Map[String, Any], id: String, fn: Any => A): Option[List[A]] = {
		cmd.get(id) match {
			case None =>
			case Some(null) => Some(Nil)
			case Some(o) =>
				if (o.isInstanceOf[java.util.List]) {
					val l1 = o.asInstanceOf[java.util.List]
					val l2 = l1.map(fn)
				}
				else {
					Some(List(fn(o)))
				}
		}
	}
}

/*
class AspirateCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class AspirateCmdHandler extends CmdHandlerA[AspirateCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("items")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			cmd.items.map(item => NeedSrc(item.well)).toList
		)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lItem = Result.sequence(cmd.items.toList.map(_.toTokenItem(ctx.ob, ctx.node))) match {
			case Error(ls) => ls.foreach(messages.addError); return Expand2Errors()
			case Success(l) => l
		}
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			val events = lItem.flatMap(item => {
				TipAspirateEventBean(item.tip, item.well, item.volume) ::
				WellRemoveEventBean(item.well, item.volume) :: Nil
			})
			val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(lItem, ctx.ob, ctx.states)
			Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
		}
	}
}

class SpirateCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	@BeanProperty var mixSpec: MixSpecBean = null
	
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Result[SpirateTokenItem] = {
		node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
		if (node.getErrorCount != 0)
			return Error(Nil)
		for {
			tipObj <- ob.findTip(tip)
			wellObj <- ob.findWell2(well)
		} yield {
			new SpirateTokenItem(tipObj, wellObj, LiquidVolume.l(volume), policy)
		}
	}
}

case class AspirateToken(
	val items: List[SpirateTokenItem]
) extends CmdToken

case class SpirateTokenItem(
	val tip: Tip,
	val well: Well,
	val volume: LiquidVolume,
	val policy: String
) extends HasTipWell

object SpirateTokenItem {
	
	def toAspriateDocString(item_l: Seq[SpirateTokenItem], ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All wells
		val well_l = item_l.map(_.well).toList
		
		val well_? : Option[Well] = well_l.distinct match {
			case well :: Nil => Some(well)
			case _ => None
		}
		
		val well_s = well_? match {
			case Some(well) => well.id
			case None => getWellsString(well_l)
		}
		
		val lLiquid = well_l.flatMap(_.wellState(states).map(_.liquid).toOption)
		val liquid_? : Option[Liquid] = lLiquid.distinct match {
			case liquid :: Nil => Some(liquid)
			case _ => None
		}
		
		val sLiquids_? = liquid_? match {
			case Some(liquid) => Some(liquid.id)
			case _ =>
				val lsLiquid = lLiquid.map(_.id)
				val lsLiquid2 = lsLiquid.distinct
				if (lsLiquid2.isEmpty)
					None
				else {
					val sLiquids = {
						if (lsLiquid2.size <= 3)
							lsLiquid2.mkString(", ")
						else
							List(lsLiquid2.head, "...", lsLiquid2.last).mkString(", ")
					}
					Some(sLiquids)
				}
		}
		
		val volume_l = item_l.map(_.volume)
		val volume_? : Option[LiquidVolume] = volume_l.distinct match {
			case volume :: Nil => Some(volume)
			case _ => None
		}
		val sVolumes_? = volume_? match {
			case Some(volume) => Some(volume.toString())
			case _ => None
		}

		val sVolumesAndLiquids = List(sVolumes_?, sLiquids_?).flatten.mkString(" of ")
		
		//Printer.getWellsDebugString(args.items.map(_.dest))
		//val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
		
		val doc = List("Aspriate", sVolumesAndLiquids, "from", well_s).filterNot(_.isEmpty).mkString(" ") 
		(doc, null)
	}
	
	def toDispenseDocString(item_l: Seq[SpirateTokenItem], ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All wells
		val well_l = item_l.map(_.well).toList
		val tip_l = item_l.map(_.tip).toList
		
		val well_? : Option[Well] = well_l.distinct match {
			case well :: Nil => Some(well)
			case _ => None
		}
		
		val well_s = well_? match {
			case Some(well) => well.id
			case None => getWellsString(well_l)
		}
		
		val lLiquid = tip_l.flatMap(tip => states.findTipState(tip.id).map(_.liquid).toOption)
		val liquid_? : Option[Liquid] = lLiquid.distinct match {
			case liquid :: Nil => Some(liquid)
			case _ => None
		}
		
		val sLiquids_? = liquid_? match {
			case Some(liquid) => Some(liquid.id)
			case _ =>
				val lsLiquid = lLiquid.map(_.id)
				val lsLiquid2 = lsLiquid.distinct
				if (lsLiquid2.isEmpty)
					None
				else {
					val sLiquids = {
						if (lsLiquid2.size <= 3)
							lsLiquid2.mkString(", ")
						else
							List(lsLiquid2.head, "...", lsLiquid2.last).mkString(", ")
					}
					Some(sLiquids)
				}
		}
		
		val volume_l = item_l.map(_.volume)
		val volume_? : Option[LiquidVolume] = volume_l.distinct match {
			case volume :: Nil => Some(volume)
			case _ => None
		}
		val sVolumes_? = volume_? match {
			case Some(volume) => Some(volume.toString())
			case _ => None
		}

		val sVolumesAndLiquids = List(sVolumes_?, sLiquids_?).flatten.mkString(" of ")
		
		//Printer.getWellsDebugString(args.items.map(_.dest))
		//val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
		
		val doc = List("Dispense", sVolumesAndLiquids, "to", well_s).filterNot(_.isEmpty).mkString(" ") 
		(doc, null)
	}
}
*/*/