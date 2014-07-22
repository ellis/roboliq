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
	
	def createHdf5Type(): (Int, List[Int]) = {
		val builder = Hdf5TypeBuilder(List(
			"scriptId" -> Hdf5Type_Ascii(33),
			"instructionIndex" -> Hdf5Type_Int(),
			"agent" -> Hdf5Type_Utf8(25),
			"description" -> Hdf5Type_Utf8(255)
		))
		/*
		import ncsa.hdf.hdf5lib.H5
		import ncsa.hdf.hdf5lib.HDF5Constants
		
		val typeCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 34+4+26+256);
		val typeString1 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 34);
		val typeString2 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 26);
		val typeString3 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 256);
		
		H5.H5Tset_strpad(typeString1, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeString1, HDF5Constants.H5T_CSET_ASCII)

		H5.H5Tset_strpad(typeString2, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeString2, HDF5Constants.H5T_CSET_UTF8)

		H5.H5Tset_strpad(typeString3, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeString3, HDF5Constants.H5T_CSET_UTF8)

		H5.H5Tinsert(typeCompound, "scriptId", 0, typeString1)
		H5.H5Tinsert(typeCompound, "instructionIndex", 0+34, HDF5Constants.H5T_STD_I32LE)
		H5.H5Tinsert(typeCompound, "agent", 0+34+4, typeString2)
		H5.H5Tinsert(typeCompound, "description", 0+34+4+26, typeString3)
		
		(typeCompound, List(typeString1, typeString2, typeString3))
		*/
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
	
	def createHdf5Type(): (Int, List[Int]) = {
		import ncsa.hdf.hdf5lib.H5
		import ncsa.hdf.hdf5lib.HDF5Constants
		
		val typeCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 34+4+26+26+26+6+26+4);
		val typeString1 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 34);
		val typeString2 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 26);
		val typeString3 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 26);
		val typeString4 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 26);
		val typeString5 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 6);
		val typeString6 = H5.H5Tcreate(HDF5Constants.H5T_STRING, 26);
		
		H5.H5Tset_strpad(typeString1, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeString1, HDF5Constants.H5T_CSET_ASCII)

		H5.H5Tset_strpad(typeString2, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeString2, HDF5Constants.H5T_CSET_UTF8)

		H5.H5Tset_strpad(typeString3, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(typeString3, HDF5Constants.H5T_CSET_UTF8)

		H5.H5Tinsert(typeCompound, "scriptId", 0, typeString1)
		H5.H5Tinsert(typeCompound, "instructionIndex", 0+34, HDF5Constants.H5T_STD_I32LE)
		H5.H5Tinsert(typeCompound, "agent", 0+34+4, typeString2)
		H5.H5Tinsert(typeCompound, "description", 0+34+4+26, typeString3)
		
		(typeCompound, List(typeString1, typeString2, typeString3))
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
		
		val linkCreationPropertyList = H5.H5Pcreate(HDF5Constants.H5P_LINK_CREATE);
		H5.H5Pset_create_intermediate_group(linkCreationPropertyList, true)
		H5.H5Pset_char_encoding(linkCreationPropertyList, HDF5Constants.H5T_CSET_UTF8)
        
		val dataSetCreationPropertyListId = H5.H5Pcreate(HDF5Constants.H5P_DATASET_CREATE);
        H5.H5Pset_fill_time(dataSetCreationPropertyListId, HDF5Constants.H5D_FILL_TIME_ALLOC);
		
		val (type4, created4) = InstructionEntry.createHdf5Type()
		val space4 = H5.H5Screate_simple(1, Array[Long](entry_l.length), Array[Long](entry_l.length))
		val data4 = H5.H5Dcreate(file, "instructions", type4, space4, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		val byte4_l = InstructionEntry.createByteArray(entry_l)
		H5.H5Dwrite(data4, type4, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, byte4_l)

		created4.foreach(H5.H5Tclose)
		H5.H5Sclose(space4)
		H5.H5Dclose(data4)
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