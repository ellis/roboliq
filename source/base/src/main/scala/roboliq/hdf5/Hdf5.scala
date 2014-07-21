package roboliq.hdf5

import ch.systemsx.cisd.hdf5._
import ch.systemsx.cisd.hdf5.HDF5Writer
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping
import ch.systemsx.cisd.hdf5.HDF5CompoundWriter
import roboliq.input.AgentInstruction
import roboliq.entities.WorldState
import roboliq.entities.SubstanceUnits
import java.io.File
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures

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
		reader.getType("Instruction", classOf[InstructionEntry], mapping("scriptId").length(33), mapping("instructionIndex"), mapping("agent").length(25), mapping("description").length(256))
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
	
	def addFileText(scriptId: String, filename: String, content: String) {
		hdf5Writer.writeString(s"$scriptId/$filename", content)
	}
	
	def addFileBytes(scriptId: String, filename: String, content: Array[Byte]) {
		hdf5Writer.writeByteArray(s"$scriptId/$filename", content)
	}
	
	def copyFile(scriptId: String, filename: String, file: File) {
		val content = scala.io.Source.fromFile(file).mkString
		addFileText(scriptId, filename, content)
	}
	
	def addInstructions(scriptId: String, instruction_l: List[AgentInstruction]) {
		val entry_l = Array(instruction_l.zipWithIndex.map { case (x, i) =>
			InstructionEntry(scriptId, i + 1, x.agent.getName, x.instruction.toString.take(256))
		} : _*)
		val typ = InstructionEntry.getHDF5Type(hdf5Writer.compound())
		val features = HDF5GenericStorageFeatures.createDeflation(7)
		//hdf5Writer.compound().writeArray("instructions", typ, entry_l, features)
		hdf5Writer.compound().writeArray("instructions", typ, Array(InstructionEntry("a", 1, "mario", "hi")), features)
		//hdf5Writer.compound().writeArray("instructions", Array(InstructionEntry("a", 1, "mario", "hi")), features)
		//writeStringArray("test", Array("hi", "there", "world"), features)
		// FIXME: for debug only
		//val content = entry_l.toList.map(x => List(x.scriptId, x.instructionIndex, x.agent, "\""+x.description+"\"").mkString(",")).mkString("\n")
		//println("instructions:")
		//println(content)
		// ENDFIX
		import ncsa.hdf.hdf5lib.H5
		import ncsa.hdf.hdf5lib.HDF5Constants
		val file = H5.H5Fcreate(filename+"X", HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)
		val space1 = H5.H5Screate_simple(1, Array(2L), Array(2L))
		val linkCreationPropertyList = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE);
		H5.H5Pset_create_intermediate_group(linkCreationPropertyList, true)
		H5.H5Pset_char_encoding(linkCreationPropertyList, HDF5Constants.H5T_CSET_UTF8)
        val dataSetCreationPropertyListId = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
        H5.H5Pset_fill_time(dataSetCreationPropertyListId, HDF5Constants.H5D_FILL_TIME_ALLOC);
		val data1 = H5.H5Dcreate(file, "data1", HDF5Constants.H5T_STD_I32LE, space1, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		H5.H5Dwrite_int(data1, HDF5Constants.H5T_NATIVE_INT32, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, Array(1, 2, 3))
		val type2 = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);
		H5.H5Tinsert(type2, "a", 0, HDF5Constants.H5T_STD_I32LE)
		H5.H5Tinsert(type2, "b", 4, HDF5Constants.H5T_STD_I32LE)
		val space2 = H5.H5Screate_simple(1, Array(2L), Array(2L))
		val data2 = H5.H5Dcreate(file, "data2", type2, space2, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		H5.H5Dwrite(data2, type2, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, Array[Byte](1, 0, 0, 0,  0, 1, 0, 0,  2, 0, 0, 0,  0, 0, 1, 0))

		H5.H5Dclose(data1)
		H5.H5Dclose(data2)
		H5.H5Pclose(dataSetCreationPropertyListId)
		H5.H5Pclose(linkCreationPropertyList)
		H5.H5Tclose(type2)
		H5.H5Sclose(space1)
		H5.H5Sclose(space2)
		H5.H5Fclose(file)
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