package roboliq.hdf5

import ch.systemsx.cisd.hdf5._
import ch.systemsx.cisd.hdf5.HDF5Writer
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping
import ch.systemsx.cisd.hdf5.HDF5CompoundWriter

case class MyData1(var a: Int, var b: Int) {
	def this() = this(0, 0)
}

case class CommandEntry(
	var scriptId: String,
	var cmdIndex: Int,
	var description: String
) {
	def this() = this("", 0, "")
}

object CommandEntry {
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[CommandEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("Command", classOf[CommandEntry], mapping("scriptId").length(50), mapping("cmdIndex"), mapping("description").length(255))
	}
}

case class SubstanceEntry(
	var scriptId: String,
	var substance: String,
	var description: String
) {
	def this() = this("", "", "")
}

object SubstanceEntry {
	def getHDF5Type(reader: IHDF5CompoundWriter): HDF5CompoundType[SubstanceEntry] = {
		import HDF5CompoundMemberMapping.mapping
		reader.getType("Substance", classOf[SubstanceEntry], mapping("scriptId").length(50), mapping("substance").length(50), mapping("description").length(255))
	}
}

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

/*
SourceWell table:
scriptId, well, sourceName, amount

SourceWellUsage table:
scriptId, well, amount

Source usage table: (do we want this?  It's duplicated information that could be queried from the SourceWell and SourceWellUsage tables)
scriptId, sourceName, amount

Aspirate/Dispense/Clean table:
scriptId, cmd#, tip, A(sp)/D(is)/C(lean), wells

Well mixture table:
scriptId, cmd#, well, substance, amount

Run -> Script (each run gets its own id and has a parent script id)
runId, scriptId

Aspirate/Dispense times table:
runId, cmd#, time

Measurement table:
runId, cmd#, object, variable, value
*/

object Test extends App {
	// Open "singleVoltageMeasurement.h5" for writing
	//val hdf5Writer = HDF5Factory.open("test.h5")
	val hdf5Writer = HDF5Factory.open("test2.h5")
	
	// Write a measurement
	//hdf5Writer.writeCompoundArray[CommandEntry]("commands", java.util.Arrays.asList(new CommandEntry("A", 1, "move"), new CommandEntry("A", 2, "groove")).toArray)
	// See what we've written out
	//System.out.println("Compound measurement record: " + hdf5Writer.readCompoundArray("measurement", classOf[CommandEntry]))
	
	val cmd1 = new CommandEntry("A", 1, "move")
	val cmd2 = new CommandEntry("A", 2, "groove")
	val cmd_l = Array(cmd1, cmd2)
	
	// WORKS:
	//val typ = CommandEntry.getHDF5Type(hdf5Writer.compound())
	//hdf5Writer.compound().writeArray("commands", typ, cmd_l)
	
	// DOESN'T WORK:
	hdf5Writer.compound().writeArray("commands", cmd_l)

	// Close the HDF5 writer
	hdf5Writer.close()
}
