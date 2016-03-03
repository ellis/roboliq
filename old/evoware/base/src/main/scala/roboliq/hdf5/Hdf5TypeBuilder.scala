package roboliq.hdf5

import ncsa.hdf.hdf5lib.H5
import ncsa.hdf.hdf5lib.HDF5Constants
import scala.collection.mutable.ArrayBuffer
import java.io.ByteArrayOutputStream

sealed trait Hdf5Type[A]
case class Hdf5Type_Ascii[A](get: A => String, length: Int) extends Hdf5Type[A]
case class Hdf5Type_Utf8[A](get: A => String, length: Int) extends Hdf5Type[A]
case class Hdf5Type_Int[A](get: A => Int) extends Hdf5Type[A]

case class Hdf5TypeBuilder[A](member_l: List[(String, Hdf5Type[A])]) {
	val length_l: List[Int] = member_l.map {
		case (_, Hdf5Type_Ascii(_, l)) => l + 1
		case (_, Hdf5Type_Utf8(_, l)) => l + 1
		case (_, Hdf5Type_Int(_)) => 4
	}
	val lengthTotal = length_l.sum
	// Created types as Right(id), standard types as Left(id)
	val memberType0_l: List[Either[Int, Int]] = member_l.map {
		case (_, Hdf5Type_Ascii(_, length)) =>
			val id = H5.H5Tcreate(HDF5Constants.H5T_STRING, length + 1);
			H5.H5Tset_strpad(id, HDF5Constants.H5T_STR_NULLTERM)
			H5.H5Tset_cset(id, HDF5Constants.H5T_CSET_ASCII)
			Right(id)
		case (_, Hdf5Type_Utf8(_, length)) =>
			val id = H5.H5Tcreate(HDF5Constants.H5T_STRING, length + 1);
			H5.H5Tset_strpad(id, HDF5Constants.H5T_STR_NULLTERM)
			H5.H5Tset_cset(id, HDF5Constants.H5T_CSET_UTF8)
			Right(id)
		case (_, Hdf5Type_Int(_)) =>
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
	
	def createByteArray(entry_l: Iterable[A]): Array[Byte] = {
		val bs = new ByteArrayOutputStream(lengthTotal * entry_l.size)
		val hs = new Hdf5Stream(bs)
		entry_l.foreach(entry => saveToStream(entry, hs))
		bs.toByteArray()
	}
	
	def saveToStream(entry: A, hs: Hdf5Stream) {
		member_l.zip(length_l).foreach {
			case ((_, Hdf5Type_Ascii(get, _)), length) => hs.putAscii(get(entry), length)
			case ((_, Hdf5Type_Utf8(get, _)), length) => hs.putUtf8(get(entry), length)
			case ((_, Hdf5Type_Int(get)), _) => hs.putInt32(get(entry))
		}
	}
}
