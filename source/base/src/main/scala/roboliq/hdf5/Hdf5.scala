package roboliq.hdf5

import java.io.ByteArrayOutputStream
import java.io.File

import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping
import ch.systemsx.cisd.hdf5.HDF5CompoundType
import ch.systemsx.cisd.hdf5.HDF5Factory
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures
import ch.systemsx.cisd.hdf5.IHDF5CompoundWriter
import ncsa.hdf.hdf5lib.H5
import ncsa.hdf.hdf5lib.HDF5Constants
import roboliq.entities.SubstanceUnits
import roboliq.input.AgentInstruction

case class InstructionEntry(
	var scriptId: String,
	var instructionIndex: Int,
	var agent: String,
	var description: String
)

object InstructionEntry {
	def createTypeBuilder(): Hdf5TypeBuilder[InstructionEntry] = {
		Hdf5TypeBuilder(List(
			"scriptId" -> Hdf5Type_Ascii((x: InstructionEntry) => x.scriptId, 33),
			"instructionIndex" -> Hdf5Type_Int((x: InstructionEntry) => x.instructionIndex),
			"agent" -> Hdf5Type_Utf8((x: InstructionEntry) => x.agent, 25),
			"description" -> Hdf5Type_Utf8((x: InstructionEntry) => x.description, 255)
		))
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
) {
}

object WellDispenseEntry {
	def createTypeBuilder(): Hdf5TypeBuilder[WellDispenseEntry] = {
		Hdf5TypeBuilder(List(
			"scriptId" -> Hdf5Type_Ascii((x: WellDispenseEntry) => x.scriptId, 33),
			"instructionIndex" -> Hdf5Type_Int((x: WellDispenseEntry) => x.instructionIndex),
			"well" -> Hdf5Type_Utf8((x: WellDispenseEntry) => x.well, 25),
			"substance" -> Hdf5Type_Utf8((x: WellDispenseEntry) => x.substance, 25),
			"amount" -> Hdf5Type_Utf8((x: WellDispenseEntry) => x.amount, 25),
			"unit" -> Hdf5Type_Utf8((x: WellDispenseEntry) => x.unit, 5),
			"agent" -> Hdf5Type_Utf8((x: WellDispenseEntry) => x.agent, 25),
			"tip" -> Hdf5Type_Int((x: WellDispenseEntry) => x.tip)
		))
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
	import ncsa.hdf.hdf5lib.H5
	import ncsa.hdf.hdf5lib.HDF5Constants

	private val fileId = H5.H5Fcreate(filename, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)
	private val linkCreationPropertyList = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE)
	H5.H5Pset_create_intermediate_group(linkCreationPropertyList, true)
	H5.H5Pset_char_encoding(linkCreationPropertyList, HDF5Constants.H5T_CSET_UTF8)

	private val dataSetCreationPropertyListId = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE)
	H5.H5Pset_fill_time(dataSetCreationPropertyListId, HDF5Constants.H5D_FILL_TIME_ALLOC);
	
	def addFileText(scriptId: String, filename: String, content: String) {
		writeUtf8(scriptId, filename, content)
	}
	
	def addFileBytes(scriptId: String, filename: String, content: Array[Byte]) {
		writeAscii(scriptId, filename, content)
	}
	
	def copyFile(scriptId: String, filename: String, file: File) {
		val content = scala.io.Source.fromFile(file).mkString
		addFileText(scriptId, filename, content)
	}
	
	def addInstructions(scriptId: String, instruction_l: List[AgentInstruction]) {
		val entry_l = Array(instruction_l.zipWithIndex.map { case (x, i) =>
			InstructionEntry(scriptId, i + 1, x.agent.getName, x.instruction.toString.take(256))
		} : _*)
		val builder = InstructionEntry.createTypeBuilder()
		writeArray(scriptId, "instructions", builder, entry_l)
	}
	
	def addInstructionData(scriptId: String, agentInstruction_l: List[AgentInstruction]) {
		val l: List[(Object, Int)] = agentInstruction_l.zipWithIndex.flatMap(pair => pair._1.instruction.data.map(_ -> pair._2))
		
		l.collect({ case (x: roboliq.input.WellDispenseEntry, i) => (x, i) }) match {
			case l0 =>
				val entry_l = l0.map { case (x, i) =>
					WellDispenseEntry(
						scriptId, i + 1, x.well, x.substance, x.amount.amount.toString, SubstanceUnits.toShortString(x.amount.units), x.agent, x.tip.getOrElse(0)
					)
				}
				val builder = WellDispenseEntry.createTypeBuilder()
				writeArray(scriptId, "wellDispenses", builder, entry_l)
		}
	}
	
	def close() {
		H5.H5Pclose(dataSetCreationPropertyListId)
		H5.H5Pclose(linkCreationPropertyList)
		H5.H5Fclose(fileId)
	}
	
	private def writeUtf8(scriptId: String, path: String, content: String) {
		val bs = new ByteArrayOutputStream()
		bs.write(content.getBytes("UTF-8"))
		bs.write(0)
		val byte_l = bs.toByteArray()

		val typeId = H5.H5Tcreate(HDF5Constants.H5T_STRING, byte_l.size)
		H5.H5Tset_strpad(typeId, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeId, HDF5Constants.H5T_CSET_UTF8)
		
		val spaceId = H5.H5Screate_simple(1, Array[Long](1), Array[Long](1))
		val dataId = H5.H5Dcreate(fileId, scriptId+"/"+path, typeId, spaceId, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)

		H5.H5Dwrite(dataId, typeId, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, byte_l)

		H5.H5Tclose(typeId)
		H5.H5Dclose(dataId)
		H5.H5Sclose(spaceId)
	}

	private def writeAscii(scriptId: String, path: String, content: Array[Byte]) {
		val bs = new ByteArrayOutputStream()
		bs.write(content)
		bs.write(0)
		val byte_l = bs.toByteArray()

		val typeId = H5.H5Tcreate(HDF5Constants.H5T_STRING, byte_l.size)
		H5.H5Tset_strpad(typeId, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeId, HDF5Constants.H5T_CSET_ASCII)
		
		val spaceId = H5.H5Screate_simple(1, Array[Long](1), Array[Long](1))
		val dataId = H5.H5Dcreate(fileId, scriptId+"/"+path, typeId, spaceId, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)

		H5.H5Dwrite(dataId, typeId, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, byte_l)

		H5.H5Tclose(typeId)
		H5.H5Dclose(dataId)
		H5.H5Sclose(spaceId)
	}

	private def writeArray[A](scriptId: String, path: String, builder: Hdf5TypeBuilder[A], entry_l: Iterable[A]) {
		val length = entry_l.size
		val spaceId = H5.H5Screate_simple(1, Array[Long](length), Array[Long](length))
		val dataId = H5.H5Dcreate(fileId, scriptId+"/"+path, builder.compoundType, spaceId, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		val byte_l = builder.createByteArray(entry_l)
		H5.H5Dwrite(dataId, builder.compoundType, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, byte_l)

		builder.createdType_l.foreach(H5.H5Tclose)
		H5.H5Dclose(dataId)
		H5.H5Sclose(spaceId)
	}
}