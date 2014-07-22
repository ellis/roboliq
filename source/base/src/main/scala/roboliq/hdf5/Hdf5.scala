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
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteBufferAsIntBufferL
import java.io.DataOutputStream

class Hdf5Stream(os: ByteArrayOutputStream) {
	def putAscii(s: String, length: Int) {
		putBytesPlusNull(s.getBytes("ASCII"), length)
	}

	def putUtf8(s: String, length: Int) {
		// FIXME: this could lead to an error in which a multi-byte UTF-8 code gets truncated
		putBytesPlusNull(s.getBytes("UTF-8"), length)
	}
	
	private def putBytesPlusNull(l: Array[Byte], length: Int) {
		putBytes(l, length - 1)
		os.write(0)
	}
	
	def putBytes(l: Array[Byte], length: Int) {
		val n = math.min(length, l.length)
		os.write(l, 0, n)
		for (i <- n until length)
			os.write(0)
	}
	
	def putInt32(x: Int) {
		val buf = ByteBuffer.allocate(4)
		buf.order(java.nio.ByteOrder.LITTLE_ENDIAN)
		buf.putInt(x)
		putBytes(buf.array(), 4)
	}
}

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
	}

	def createByteArray(entry_l: Iterable[InstructionEntry]): Array[Byte] = {
		val bs = new ByteArrayOutputStream((34+4+26+256) * entry_l.size)
		val hs = new Hdf5Stream(bs)
		for (entry <- entry_l) {
			hs.putAscii(entry.scriptId, 34)
			hs.putInt32(entry.instructionIndex)
			hs.putUtf8(entry.agent, 26)
			hs.putUtf8(entry.description, 256)
			/*hs.putAscii("123456789012345678901234567890123", 34)
			hs.putInt32(0x01020304)
			hs.putUtf8("abcdefghijklmnopqrstuvwxyz", 26)
			hs.putUtf8("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 256)*/
		}
		val x = bs.toByteArray()
		//x.toList.take(34+4+26+256).zipWithIndex.foreach(println)
		x
	}
	
	/*
	def saveToStream(entry: InstructionEntry, os: ByteArrayOutputStream) {
		val os2 = new Hdf5Stream(os)
		os2.putAscii(entry.scriptId, 34)
		os2.putInt32(entry.instructionIndex)
		os2.putUtf8(entry.agent, 26)
		os2.putUtf8(entry.description, 256)
	}*/
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

		val type3 = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);
		H5.H5Tinsert(type3, "a", 0, HDF5Constants.H5T_STD_I32LE)
		val type3b = H5.H5Tcreate(HDF5Constants.H5T_STRING, 4);
		H5.H5Tset_strpad(type3b, HDF5Constants.H5T_STR_NULLTERM)
		H5.H5Tset_cset(type3b, HDF5Constants.H5T_CSET_UTF8)
		H5.H5Tinsert(type3, "b", 4, type3b)
		val space3 = H5.H5Screate_simple(1, Array(2L), Array(2L))
		val data3 = H5.H5Dcreate(file, "data3", type3, space3, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		H5.H5Dwrite(data3, type3, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, Array[Byte](1, 0, 0, 0,  'H', 'i', 0, 0,  2, 0, 0, 0,  'B', 'a', 'b', 0))
		
		val (type4, more4) = InstructionEntry.createHdf5Type()
		val space4 = H5.H5Screate_simple(1, Array[Long](entry_l.length), Array[Long](entry_l.length))
		val data4 = H5.H5Dcreate(file, "instructions", type4, space4, linkCreationPropertyList, dataSetCreationPropertyListId, HDF5Constants.H5P_DEFAULT)
		val byte4_l = InstructionEntry.createByteArray(entry_l)
		H5.H5Dwrite(data4, type4, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, byte4_l)
		

		H5.H5Dclose(data1)
		H5.H5Dclose(data2)
		H5.H5Dclose(data3)
		H5.H5Dclose(data4)
		H5.H5Pclose(dataSetCreationPropertyListId)
		H5.H5Pclose(linkCreationPropertyList)
		H5.H5Tclose(type2)
		H5.H5Tclose(type3)
		H5.H5Tclose(type3b)
		H5.H5Tclose(type4)
		more4.foreach(H5.H5Tclose)
		H5.H5Sclose(space1)
		H5.H5Sclose(space2)
		H5.H5Sclose(space3)
		H5.H5Sclose(space4)
		H5.H5Fclose(file)
		// TODO: Also need to close the string types
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