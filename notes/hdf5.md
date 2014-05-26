# HDF5

# JHDF5

2014-05-26: Using JHDF5 <https://wiki-bsse.ethz.ch/display/JHDF5/JHDF5+%28HDF5+for+Java%29>

## Saddle

2014-05-26: won't work for me, can't read/write dataframes like I require.

https://github.com/saddle/saddle/blob/master/saddle-hdf5/src/main/scala/org/saddle/io/H5Store.scala
import org.saddle.io._
java.lang.Runtime.getRuntime.load("/home/ellisw/src/roboliq/source/base/lib/libjhdf5.so")
val h = H5Store.openFile("/home/ellisw/tmp/temp.h5")
H5Store.readFrame[Int, String, Any](h, "people")

DEBUG!:

import org.saddle._
val df6 = Panel(Vec("string", "anotherString", "unrelated"), vec.randi(3), vec.rand(3))
H5Store.writeFrame(h, "df6", df6)


## Java HDF5 JDI

Low-level interface.  I could probably get it to work, but it's rather complicated because
it wants to read/write compound objects from contiguous RAM, which doesn't work well in Java.

    http://www.hdfgroup.org/products/java/hdf-java-html/javadocs/ncsa/hdf/hdf5lib/H5.html
    import ncsa.hdf.hdf5lib.H5
    import ncsa.hdf.hdf5lib.HDF5Constants
    val fname = "/home/ellisw/tmp/temp.h5"
    val file_id = H5.H5Fopen(fname, HDF5Constants.H5F_ACC_RDWR, HDF5Constants.H5P_DEFAULT)
    val dataset_id = H5.H5Dopen(file_id, "people", HDF5Constants.H5P_DEFAULT)
    val group1_id = H5.H5Gcreate(file_id, "/group1", HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)

The HDF Object Java code looks a lot nicer than the JNI.  Examples here:

    /home/public/src/roboliq/source/hdf-java-2.10/HDF-JAVA-2.10.0-Linux/HDFJavaExamples-0.1.1-Source/jnative/h5

Based on this tutorial: http://beige.ucs.indiana.edu/I590/node121.html

    import ncsa.hdf.hdf5lib.H5
    import ncsa.hdf.hdf5lib.HDF5Constants
    val fname = "temp.h5"
    val file_id = H5.H5Fcreate(fname, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)
    val dataspace_id = H5.H5Screate_simple(2, Array[Long](4, 6), null)
    val dataset_id = H5.H5Dcreate(file_id, "/dset", HDF5Constants.H5T_STD_I32BE, dataspace_id, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULTT, HDF5Constants.H5P_DEFAULT)
    val dataset_id = H5.H5Dopen(file_id, "people", HDF5Constants.H5P_DEFAULT)
    val group1_id = H5.H5Gcreate(file_id, "/group1", HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT)

For compound data types, see:
    ``/home/public/src/roboliq/sourceH5Ex_T_Compound.java``

    case class MyData1(int a, int b)
    val memtype_id = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8)
    H5.H5Tinsert(memtype_id, "a", 0, HDF5Constants.H5T_NATIVE_INT)
    H5.H5Tinsert(memtype_id, "b", 4, HDF5Constants.H5T_NATIVE_INT)
    val filetype_id = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8)
    H5.H5Tinsert(filetype_id, "a", 0, HDF5Constants.H5T_STD_I32BE)
    H5.H5Tinsert(filetype_id, "b", 4, HDF5Constants.H5T_STD_I32BE) 
    val dataspace_id = H5.H5Screate_simple(1, Array[Long](2), null)
    val dataset_id = H5.H5Dcreate(file_id, "/data1", filetype_id, dataspace_id, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);



