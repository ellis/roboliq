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
import roboliq.entities.Substance
import roboliq.entities.Mixture
import roboliq.entities.Amount

case class InstructionEntry(
	scriptId: String,
	instructionIndex: Int,
	agent: String,
	description: String
)

/*
case class SourceMixtureEntry(
	var scriptId: String,
	var sourceName: String,
	var substance: String,
	var amount: String
) {
	def this() = this("", "", "", "")
}

object SourceMixtureEntry {
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[SourceMixtureEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("Substance", classOf[SourceMixtureEntry], mapping("scriptId").length(50), mapping("sourceName").length(50), mapping("substance").length(50), mapping("amount").length(30))
	}
}
*/
case class WellDispenseEntry(
	scriptId: String,
	instructionIndex: Int,
	well: String,
	substance: String,
	amount: String,
	unit: String,
	agent: String,
	tip: Int
)

object EntryTypes {
	def createInstructionTypeBuilder(): Hdf5TypeBuilder[InstructionEntry] = {
		Hdf5TypeBuilder(List(
			"scriptId" -> Hdf5Type_Ascii((x: InstructionEntry) => x.scriptId, 33),
			"instructionIndex" -> Hdf5Type_Int((x: InstructionEntry) => x.instructionIndex),
			"agent" -> Hdf5Type_Utf8((x: InstructionEntry) => x.agent, 25),
			"description" -> Hdf5Type_Utf8((x: InstructionEntry) => x.description, 255)
		))
	}

	def createSubstanceTypeBuilder(): Hdf5TypeBuilder[Substance] = {
		Hdf5TypeBuilder(List(
			"substance" -> Hdf5Type_Ascii((x: Substance) => x.label.getOrElse(x.key), 25)
		))
	}
	
	def createSourceMixtureTypeBuilder(): Hdf5TypeBuilder[(String, Substance, Amount)] = {
		Hdf5TypeBuilder(List(
			"source" -> Hdf5Type_Utf8((x: (String, Substance, Amount)) => x._1, 25),
			//"mixture" -> Hdf5Type_Utf8((x: (String, Mixture)) => x._2.toString, 255)
			"substance" -> Hdf5Type_Utf8((x: (String, Substance, Amount)) => x._2.getName, 25),
			"amount" -> Hdf5Type_Utf8((x: (String, Substance, Amount)) => x._3.amount.bigDecimal.toEngineeringString(), 25),
			"unit" -> Hdf5Type_Utf8((x: (String, Substance, Amount)) => SubstanceUnits.toShortString(x._3.units), 5)
		))
	}
	
	def createWellDispenseTypeBuilder(): Hdf5TypeBuilder[WellDispenseEntry] = {
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

	/*def getWellMixtureTypeBuilder(reader: IHDF5CompoundWriter): HDF5CompoundType[WellMixtureEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("WellMixture", classOf[WellMixtureEntry], mapping("scriptId").length(50), mapping("instructionIndex"), mapping("well").length(25), mapping("mixture").length(255), mapping("amount").length(20))
	}*/
}

/*case class WellMixtureEntry(
	var scriptId: String,
	var instructionIndex: Int,
	var well: String,
	var mixture: String,
	var amount: String
) {
	
}*/

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
	
	def saveInstructions(scriptId: String, instruction_l: List[AgentInstruction]) {
		val entry_l = Array(instruction_l.zipWithIndex.map { case (x, i) =>
			InstructionEntry(scriptId, i + 1, x.agent.getName, x.instruction.toString.take(256))
		} : _*)
		val builder = EntryTypes.createInstructionTypeBuilder()
		writeArray(scriptId, "instructions", builder, entry_l)
	}
	
	def saveSubstances(scriptId: String, substance_l: List[Substance]) {
		writeArray(scriptId, "substances", EntryTypes.createSubstanceTypeBuilder(), substance_l)
	}
	
	def saveSourceMixtures(scriptId: String, l: List[(String, Mixture)]) {
		val l2: List[(String, Substance, Amount)] = l.flatMap { case (name, mixture) => mixture.toSubstanceAmountMap.toList.sortBy(_._1.getName).map(x => (name, x._1, x._2)) }
		writeArray(scriptId, "sourceMixtures", EntryTypes.createSourceMixtureTypeBuilder(), l2)
	}
	
	def saveInstructionData(scriptId: String, agentInstruction_l: List[AgentInstruction]) {
		val l: List[(Object, Int)] = agentInstruction_l.zipWithIndex.flatMap(pair => pair._1.instruction.data.map(_ -> pair._2))
		
		l.collect({ case (x: roboliq.input.WellDispenseEntry, i) => (x, i) }) match {
			case l0 =>
				val entry_l = l0.map { case (x, i) =>
					WellDispenseEntry(
						scriptId, i + 1, x.well, x.substance, x.amount.amount.toString, SubstanceUnits.toShortString(x.amount.units), x.agent, x.tip.getOrElse(0)
					)
				}
				val builder = EntryTypes.createWellDispenseTypeBuilder()
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