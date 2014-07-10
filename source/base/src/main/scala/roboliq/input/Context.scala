package roboliq.input

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsSuccess
import roboliq.entities.Aliquot
import roboliq.entities.Entity
import roboliq.entities.EntityBase
import roboliq.entities.Labware
import roboliq.entities.LabwareModel
import roboliq.entities.Well
import roboliq.entities.WorldState
import roboliq.entities.WorldStateBuilder
import spray.json.JsValue
import scala.reflect.runtime.universe.TypeTag
import roboliq.entities.WellInfo
import roboliq.entities.Agent
import roboliq.entities.WorldStateEvent

case class WellDispenseEntry(
	well: String,
	substance: String,
	amount: String,
	agent: String,
	tip: Option[Int]
)

/**
 * ProtocolState should carry the eb, current world state, current command, current instruction, warnings and errors, and various HDF5 arrays
 */
case class ProtocolData(
	protocol: Protocol,
	eb: EntityBase,
	state: WorldState,
	command: List[Int] = Nil,
	instruction: Option[Int] = None,
	warning_r: List[String] = Nil,
	error_r: List[String] = Nil,
	instruction_l: Seq[(AgentInstruction, WorldState, WorldState)] = Nil,
	well_aliquot_r: List[(List[Int], Well, Aliquot)] = Nil,
	wellDispenseEntry_r: List[WellDispenseEntry] = Nil
) {
	def setState(state: WorldState): ProtocolData =
		copy(state = state)
	
	def setCommand(idx: List[Int]): ProtocolData =
		copy(command = idx, instruction = None)

	def setInstruction(idx: Option[Int]): ProtocolData =
		copy(instruction = idx)
	
	def log[A](res: RsResult[A]): ProtocolData = {
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r, error_r = error_l.reverse.map(prefixMessage) ++ this.error_r)
		}
	}
	
	def logWarning(s: String): ProtocolData = {
		copy(warning_r = prefixMessage(s) :: warning_r)
	}
	
	def logError(s: String): ProtocolData = {
		copy(error_r = prefixMessage(s) :: error_r)
	}
	
	def prefixMessage(s: String): String = {
		(command, instruction) match {
			case (Nil, None) => s
			case (c, None) => s"Command ${c.mkString(".")}: $s" 
			case (Nil, Some(i)) => s"Instruction ${i}: $s" 
			case (c, Some(i)) => s"Command ${c.mkString(".")}: Instruction ${i}: $s" 
		}
	}
}

/*
sealed trait Context[+A] {
	def run(data: ProtocolData): (ProtocolData, A)
	
	def map[B](f: A => B): Context[B] = {
		Context { data =>
			val (data1, a) = run(data)
			(data1, f(a))
		}
	}
	
	def flatMap[B](f: A => Context[B]): Context[B] = {
		Context { data =>
			val (data1, a) = run(data)
			f(a).run(data1)
		}
	}
}
*/
sealed trait Context[+A] {
	def run(data: ProtocolData): (ProtocolData, Option[A])
	
	def map[B](f: A => B): Context[B] = {
		Context { data =>
			if (data.error_r.isEmpty) {
				val (data1, optA) = run(data)
				val optB = optA.map(f)
				(data1, optB)
			}
			else {
				(data, None)
			}
		}
	}
	
	def flatMap[B](f: A => Context[B]): Context[B] = {
		Context { data =>
			if (data.error_r.isEmpty) {
				val (data1, optA) = run(data)
				optA match {
					case None => (data1, None)
					case Some(a) => f(a).run(data1)
				}
			}
			else {
				(data, None)
			}
		}
	}
	
	/*private def pair[A](data: ProtocolData, res: RsResult[A]): (ProtocolData, RsResult[A]) = {
		(data.log(res), res)
	}*/
}

object Context {
	def apply[A](f: ProtocolData => (ProtocolData, Option[A])): Context[A] = {
		new Context[A] {
			def run(data: ProtocolData) = f(data)
		}
	}
	
	def from[A](res: RsResult[A]): Context[A] = {
		Context { data =>
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def from[A](opt: Option[A], error: => String): Context[A] = {
		Context { data =>
			val data1 = opt match {
				case None => data.logError(error)
				case _ => data
			}
			(data1, opt)
		}
	}
	
	def unit[A](a: A): Context[A] =
		Context { data => (data, Some(a)) }
	
	def get: Context[ProtocolData] =
		Context { data => (data, Some(data)) }
	
	def gets[A](f: ProtocolData => A): Context[A] =
		Context { data => (data, Some(f(data))) }
	
	def getsResult[A](f: ProtocolData => RsResult[A]): Context[A] = {
		Context { data =>
			val res = f(data)
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def getsOption[A](f: ProtocolData => Option[A], error: => String): Context[A] = {
		Context { data =>
			f(data) match {
				case None => (data.logError(error), None)
				case Some(a) => (data, Some(a))
			}
		}
	}
	
	def put(data: ProtocolData): Context[Unit] =
		Context { _ => (data, Some(())) }
	
	def modify(f: ProtocolData => ProtocolData): Context[Unit] =
		Context { data => (f(data), Some(())) }
	
	def modifyStateBuilder(f: WorldStateBuilder => Unit): Context[Unit] = {
		Context { data => 
			// Update state
			var state1 = data.state.toMutable
			f(state1)
			(data.setState(state1.toImmutable), Some(()))
		}
	}
	
	def modifyState(f: WorldState => WorldState): Context[Unit] = {
		Context { data =>
			(data.setState(f(data.state)), Some(()))
		}
	}
	
	def modifyStateResult(f: WorldState => RsResult[WorldState]): Context[Unit] = {
		for {
			state0 <- Context.gets(_.state)
			state1 <- Context.from(f(state0))
			_ <- Context.modify(_.setState(state1))
		} yield ()
	}
	
	def assert(condition: Boolean, msg: => String): Context[Unit] = {
		if (condition) unit(())
		else error(msg)
	}
	
	def error[A](s: String): Context[A] = {
		Context { data => (data.logError(s), None) }
	}
	
	//def getResult(a: A): Context[A] =
	//	RsError(data.error_r, data.warning_r)
	
	def getEntityAs[A <: Entity : Manifest](ident: String): Context[A] =
		getsResult[A](_.eb.getEntityAs[A](ident))

	def getEntityAs[A: TypeTag](json: JsValue): Context[A] = {
		for {
			data <- Context.get
			a <- Context.from(Converter.convAs[A](json, data.eb, Some(data.state)))
		} yield a
	}
	
	def getEntityIdent(e: Entity): Context[String] =
		getsResult[String](_.eb.getIdent(e))
		
	def getLabwareModel(labware: Labware): Context[LabwareModel] = {
		getsResult[LabwareModel]{ data =>
			for {
				labwareIdent <- data.eb.getIdent(labware)
				model <- RsResult.from(data.eb.labwareToModel_m.get(labware), s"missing model for labware `$labwareIdent`")
			} yield model
		}
	}
	
	//def getWellInfo(well: Well): Context[WellInfo] =
	//	getsResult[WellInfo](data => data.eb.wellToWellInfo(data.state, well))
	
	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	def mapFirst[A, B, C[_]](
		l: C[A]
	)(
		fn: A => Context[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): Context[C[B]] = {
		Context { data0 =>
			var data = data0
			val builder = cbf()
			for (x <- c2i(l)) {
				if (data.error_r.isEmpty) {
					val ctx1 = fn(x)
					val (data1, opt) = ctx1.run(data)
					if (data1.error_r.isEmpty && opt.isDefined) {
						builder += opt.get
					}
					data = data1
				}
			}
			if (data.error_r.isEmpty) (data, Some(builder.result()))
			else (data, None)
		}
	}

	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	def foreachFirst[A, C[_]](
		l: C[A]
	)(
		fn: A => Context[Any]
	)(implicit
		c2i: C[A] => Iterable[A]
	): Context[Unit] = {
		Context { data0 =>
			var data = data0
			for (x <- c2i(l)) {
				if (data.error_r.isEmpty) {
					val ctx1 = fn(x)
					val (data1, opt) = ctx1.run(data)
					data = data1
				}
			}
			if (data.error_r.isEmpty) (data, Some(()))
			else (data, None)
		}
	}
	
	def addInstruction(agentInstruction: AgentInstruction): Context[Unit] = {
		for {
			state0 <- Context.gets(_.state)
			state2 <- Context.getsResult(data => WorldStateEvent.update(agentInstruction.instruction.effects, data.state))
			_ <- Context.modify { data => 
				val instruction2_l = data.instruction_l :+ (agentInstruction, state0, state2)
				data.copy(state = state2, instruction_l = instruction2_l, instruction = Some(instruction2_l.size))
			}
			_ <- translate(agentInstruction)
		} yield ()
	}
	
	def addInstruction(agent: Agent, instruction: Instruction): Context[Unit] =
		addInstruction(AgentInstruction(agent, instruction))
	
	def addInstructions(agent: Agent, instruction_l: List[Instruction]): Context[Unit] = {
		Context.foreachFirst(instruction_l)(instruction => Context.addInstruction(AgentInstruction(agent, instruction)))
	}
	
	private def translate(agentInstruction: AgentInstruction): Context[Unit] = {
		for {
			agentToBuilder_m <- Context.gets(_.protocol.agentToBuilder_m.toMap)
			agentIdent <- Context.getEntityIdent(agentInstruction.agent)
			builder = agentToBuilder_m(agentIdent)
			_ <- builder.addCommand(agentIdent, agentInstruction.instruction)
		} yield ()
	}
}
