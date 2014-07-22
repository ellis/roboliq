package roboliq.hdf5

import ncsa.hdf.hdf5lib.H5
import ncsa.hdf.hdf5lib.HDF5Constants
import scala.collection.mutable.ArrayBuffer

sealed trait Hdf5Type
case class Hdf5Type_Ascii(length: Int) extends Hdf5Type
case class Hdf5Type_Utf8(length: Int) extends Hdf5Type
case class Hdf5Type_Int() extends Hdf5Type

case class Hdf5TypeBuilder(member_l: List[(String, Hdf5Type)]) {
	val length_l: List[Int] = member_l.map {
		case (_, Hdf5Type_Ascii(l)) => l + 1
		case (_, Hdf5Type_Utf8(l)) => l + 1
		case (_, Hdf5Type_Int()) => 4
	}
	val lengthTotal = length_l.sum
	// Created types as Right(id), standard types as Left(id)
	val memberType0_l: List[Either[Int, Int]] = member_l.map {
		case (_, Hdf5Type_Ascii(length)) =>
			val id = H5.H5Tcreate(HDF5Constants.H5T_STRING, length + 1);
			H5.H5Tset_strpad(id, HDF5Constants.H5T_STR_NULLTERM)
			H5.H5Tset_cset(id, HDF5Constants.H5T_CSET_ASCII)
			Right(id)
		case (_, Hdf5Type_Utf8(length)) =>
			val id = H5.H5Tcreate(HDF5Constants.H5T_STRING, length + 1);
			H5.H5Tset_strpad(id, HDF5Constants.H5T_STR_NULLTERM)
			H5.H5Tset_cset(id, HDF5Constants.H5T_CSET_UTF8)
			Right(id)
		case (_, Hdf5Type_Int()) =>
			Left(HDF5Constants.H5T_STD_I32LE)
	}
	val memberType_l: List[Int] = memberType0_l.map {
		case Left(id) => id
		case Right(id) => id
	}
	val compoundType = {
		val compoundId = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, lengthTotal);
		val l = (member_l, length_l, memberType_l).zipped.collect { case ((name, _), length, id) => (name, length, id) }
		l.foldLeft(0) { case (offset, (name, length, id)) =>
			H5.H5Tinsert(compoundId, name, offset, id)
			offset + length
		}
		compoundId
	}
	val createdType_l = compoundType :: memberType0_l.collect { case Right(id) => id }
}
