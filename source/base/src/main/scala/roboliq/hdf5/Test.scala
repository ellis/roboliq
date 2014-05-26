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
		reader.getType(classOf[CommandEntry], mapping("scriptId").length(50), mapping("cmdIndex"), mapping("description").length(255))
	}
}

case class SubstanceEntry(
	var scriptId: String,
	var substance: String
) {
	def this() = this("", "")
}

/*
Command table:
scriptId, cmd#, description

Substance table:
scriptId, substance

SourceMixture table:
scriptId, sourceName, substance, amount

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
	val hdf5Writer = HDF5Factory.open("test.h5")
	
	// Write a measurement
	//hdf5Writer.writeCompoundArray[CommandEntry]("commands", java.util.Arrays.asList(new CommandEntry("A", 1, "move"), new CommandEntry("A", 2, "groove")).toArray)
	// See what we've written out
	//System.out.println("Compound measurement record: " + hdf5Writer.readCompoundArray("measurement", classOf[CommandEntry]))
	
	val cmd1 = new CommandEntry("A", 1, "move")
	val cmd2 = new CommandEntry("A", 2, "groove")
	val cmd_l = Array[Object](cmd1, cmd2)
	val typ = CommandEntry.getHDF5Type(hdf5Writer.compound())
	val l = new JavaTest().make(cmd1, cmd2)
	hdf5Writer.compound().writeArray("commands", typ, l)

	// Close the HDF5 writer
	hdf5Writer.close()
}