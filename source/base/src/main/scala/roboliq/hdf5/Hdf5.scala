package roboliq.hdf5

import ch.systemsx.cisd.hdf5._
import ch.systemsx.cisd.hdf5.HDF5Writer
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping
import ch.systemsx.cisd.hdf5.HDF5CompoundWriter
import roboliq.plan.AgentInstruction

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

case class DispenseEntry(
	
) {
	
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
}