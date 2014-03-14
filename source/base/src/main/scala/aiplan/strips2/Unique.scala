package aiplan.strips2

import scala.language.implicitConversions
import scala.collection.immutable.SortedSet
import scala.collection.immutable.IndexedSeq
import scala.collection.SetLike

class Unique[A](
	val list: Vector[A],
	val set: Set[A]
//) extends IndexedSeq[A] /*with SetLike[A, Unique[A]]*/ {
) extends Set[A] with SetLike[A, Unique[A]] {

	// IndexedSeq
	def apply(idx: Int): A = list(idx)
	def length: Int = list.length

	// Members declared in scala.collection.GenSetLike
	//def seq: scala.collection.Set[A] = ???
	override def iterator: Iterator[A] = list.iterator

	// Members declared in scala.collection.SetLike
	override def empty: Unique[A] = new Unique(Vector(), Set())
	def -(elem: A): Unique[A] = {
		if (set.contains(elem)) new Unique(list.filterNot(_ == elem), set - elem)
		else this
	}
	def +(elem: A): Unique[A] = {
		if (set.contains(elem)) this
		else new Unique(list :+ elem, set + elem)
	}
	def contains(elem: A): Boolean = set.contains(elem)
}

object Unique {
	def apply[A](elems: A*): Unique[A] = {
		new Unique(Vector(elems : _*), Set(elems : _*))
	}
	
	implicit def seqToUnique[A](l: Iterable[A]): Unique[A] =
		apply(l.toList : _*)
}