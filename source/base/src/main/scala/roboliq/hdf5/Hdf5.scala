package roboliq.hdf5

import ch.systemsx.cisd.hdf5._
import ch.systemsx.cisd.hdf5.HDF5Writer
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping
import ch.systemsx.cisd.hdf5.HDF5CompoundWriter
import roboliq.input.AgentInstruction
import roboliq.entities.WorldState
import roboliq.entities.SubstanceUnits

case class InstructionEntry(
	var scriptId: String,
	var instructionIndex: Int,
	var agent: String,
	var description: String
) {
	def this() = this("", 0, "", "")
}

object InstructionEntry {
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[InstructionEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("Instruction", classOf[InstructionEntry], mapping("scriptId").length(50), mapping("instructionIndex"), mapping("agent").length(25), mapping("description").length(255))
	}
}

case class WellDispenseEntry(
	var scriptId: String,
	var instructionIndex: Int,
	var well: String,
	var substance: String,
	var amount: String,
	var unit: String,
	var agent: String,
	var tip: Int
)

object WellDispenseEntry {
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[WellDispenseEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("WellDispense", classOf[WellDispenseEntry],
			mapping("scriptId").length(50),
			mapping("instructionIndex"),
			mapping("well").length(25),
			mapping("substance").length(25),
			mapping("amount").length(25),
			mapping("unit").length(5),
			mapping("agent").length(25),
			mapping("tip")
		)
	}
}

case class WellMixtureEntry(
	var scriptId: String,
	var instructionIndex: Int,
	var well: String,
	var mixture: String,
	var amount: String
) {
	
}

object WellMixtureEntry {
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[WellMixtureEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("WellMixture", classOf[WellMixtureEntry], mapping("scriptId").length(50), mapping("instructionIndex"), mapping("well").length(25), mapping("mixture").length(255), mapping("amount").length(20))
	}
}

class Hdf5(
	filename: String
) {
	private val hdf5Writer = HDF5Factory.open(filename)
	
	def addInstructions(scriptId: String, instruction_l: List[AgentInstruction]) {
		val entry_l = Array(instruction_l.zipWithIndex.map { case (x, i) =>
			InstructionEntry(scriptId, i + 1, x.agent.getName, x.instruction.toString.take(255))
		} : _*)
		val typ = InstructionEntry.getHDF5Type(hdf5Writer.compound())
		hdf5Writer.compound().writeArray("instructions", typ, entry_l)
		
	}
	
	/*def addWellMixtures(scriptId: String, instruction_l: List[AgentInstruction]) {
		val entry_l = Array(instruction_l.zipWithIndex.map { case (x, i) =>
			InstructionEntry(scriptId, i + 1, x.agent.getName, x.instruction.toString.take(255))
		} : _*)
		val typ = InstructionEntry.getHDF5Type(hdf5Writer.compound())
		hdf5Writer.compound().writeArray("instructions", typ, entry_l)
	}*/
	
	def addInstructionData(scriptId: String, agentInstruction_l: List[AgentInstruction]) {
		val l: List[(Object, Int)] = agentInstruction_l.zipWithIndex.flatMap(pair => pair._1.instruction.data.map(_ -> pair._2))
		
		l.collect({ case (x: roboliq.input.WellDispenseEntry, i) => (x, i) }) match {
			case l0 =>
				val l1 = l0.map { case (x, i) =>
					WellDispenseEntry(
						scriptId, i + 1, x.well, x.substance, x.amount.amount.toString, SubstanceUnits.toShortString(x.amount.units), x.agent, x.tip.getOrElse(0)
					)
				}
				val typ = WellDispenseEntry.getHDF5Type(hdf5Writer.compound())
				hdf5Writer.compound().writeArray("wellDispenses", typ, l1.toArray)
		}
	}
	
}