package roboliq.hdf5

import ch.systemsx.cisd.hdf5._
import ch.systemsx.cisd.hdf5.HDF5Writer
import ch.systemsx.cisd.hdf5.HDF5CompoundMemberMapping
import ch.systemsx.cisd.hdf5.HDF5CompoundWriter

class Hdf5(
	filename: String
) {
	private val hdf5Writer = HDF5Factory.open(filename)
	
}