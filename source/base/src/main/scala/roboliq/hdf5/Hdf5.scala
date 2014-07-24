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
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[InstructionEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getAnonType(classOf[InstructionEntry],
			mapping("scriptId").length(33),
			mapping("instructionIndex"),
			mapping("agent").length(25),
			mapping("description").length(256)
		)
	}
	
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
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[WellDispenseEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getAnonType(classOf[WellDispenseEntry],
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
	
	/*def createHdf5Type(): (Int, List[Int]) = {
		val builder = Hdf5TypeBuilder(List(
			"scriptId" -> Hdf5Type_Ascii(33),
			"instructionIndex" -> Hdf5Type_Int(),
			"well" -> Hdf5Type_Utf8(25),
			"substance" -> Hdf5Type_Utf8(25),
			"amount" -> Hdf5Type_Utf8(25),
			"unit" -> Hdf5Type_Utf8(5),
			"agent" -> Hdf5Type_Utf8(25),
			"tip" -> Hdf5Type_Int()
		))
		(builder.compoundType, builder.createdType_l)
	}

	def createByteArray(entry_l: Iterable[InstructionEntry]): Array[Byte] = {
		val bs = new ByteArrayOutputStream((34+4+26+256) * entry_l.size)
		val hs = new Hdf5Stream(bs)
		entry_l.foreach(entry => saveToStream(entry, hs))
		bs.toByteArray()
	}
	
	def saveToStream(entry: InstructionEntry, hs: Hdf5Stream) {
		hs.putAscii(entry.scriptId, 34)
		hs.putInt32(entry.instructionIndex)
		hs.putUtf8(entry.agent, 26)
		hs.putUtf8(entry.description, 256)
	}*/
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

		val builder = InstructionEntry.createTypeBuilder()
		writeArray(scriptId, builder, entry_l)
	}

	private def writeArray[A](scriptId: String, builder: Hdf5TypeBuilder[A], entry_l: Iterable[A]) {
		import ncsa.hdf.hdf5lib.H5
		import ncsa.hdf.hdf5lib.HDF5Constants
		
		val file = H5.H5Fcreate(filename+"X", HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)
		
		val linkCreationPropertyList = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE);
		H5.H5Pset_create_intermediate_group(linkCreationPropertyList, true)
		H5.H5Pset_char_encoding(linkCreationPropertyList, HDF5Constants.H5T_CSET_UTF8)

		val dataSetCreationPropertyListId = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
		H5.H5Pset_fill_time(dataSetCreationPropertyListId, HDF5Constants.H5D_FILL_TIME_ALLOC);
		
		val length = entry_l.size
		val spaceId = H5.H5Screate_simple(1, Array[Long](length), Array[Long](length))
		val dataId = H5.H5Dcreate(file, scriptId+"/instructions", builder.compoundType, spaceId, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		val byte_l = builder.createByteArray(entry_l)
		H5.H5Dwrite(dataId, builder.compoundType, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, byte_l)

		builder.createdType_l.foreach(H5.H5Tclose)
		H5.H5Dclose(dataId)
		H5.H5Sclose(spaceId)
		H5.H5Pclose(dataSetCreationPropertyListId)
		H5.H5Pclose(linkCreationPropertyList)
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