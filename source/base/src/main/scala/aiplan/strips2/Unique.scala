package aiplan.strips2

import scala.language.implicitConversions
import scala.collection.immutable.SortedSet
import scala.collection.immutable.IndexedSeq
import scala.collection.SetLike
import scala.collection.SeqLike
import scala.collection.IterableLike
import scala.collection.IndexedSeqLike
import scala.collection.generic.GenericTraversableTemplate
import scala.collection.generic.GenericCompanion
import scala.collection.mutable.Builder
import scala.collection.mutable.ArrayBuffer
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.LazyBuilder
import scala.collection.generic.TraversableFactory
import scala.collection.TraversableLike

/*
class Unique[A](
	val list: Vector[A],
	val set: Set[A]
//) extends IndexedSeq[A] /*with SetLike[A, Unique[A]]*/ {
//) extends Set[A] with SetLike[A, Unique[A]] {
//) extends SetLike[A, Unique[A]] {
//) extends IterableLike[A, Unique[A]] {
) extends IndexedSeqLike[A, Unique[A]] {

	// IndexedSeq
	def apply(idx: Int): A = list(idx)
	def length: Int = list.length
	def map[B](fn: A => B): Unique[B] = list.map(fn)
	//override def map[B](f: A => B)(implicit bf: scala.collection.generic.CanBuildFrom[aiplan.strips2.Unique[A],B,Unique[B]]): Unique[B] = Unique(list.map(f) : _*)
	//override def map[B](f: A => B)(implicit bf: scala.collection.generic.CanBuildFrom[aiplan.strips2.Unique[A],B,Unique[B]]): Unique[B] = Unique(list.map(f) : _*)

	// Members declared in scala.collection.GenSetLike
	//def seq: scala.collection.Set[A] = set
	def seq: scala.collection.IndexedSeq[A] = list.seq
	override def iterator: Iterator[A] = list.iterator
	
	// Members declared in scala.collection.TraversableLike
	//protected[this] def newBuilder: scala.collection.mutable.Builder[A,aiplan.strips2.Unique[A]] = ???
	
	/*  
	The method that builds the new collection.  This is a simple implementation
	but it works.  There are other implementations to assist with implementation if
	needed
	*/
	protected[this] def newBuilder: scala.collection.mutable.Builder[A,aiplan.strips2.Unique[A]] = {
	//def newBuilder[A] = new scala.collection.mutable.LazyBuilder[A,Unique[A]] {
		new scala.collection.mutable.LazyBuilder[A,Unique[A]] {
			def result = {
				val data = parts.foldLeft(List[A]()){(l,n) => l ++ n}
				Unique(data : _*)
			}
		}
	}
	
	// Members declared in scala.collection.SetLike
	//override def empty: Unique[A] = new Unique(Vector(), Set())
	def -(elem: A): Unique[A] = {
		if (set.contains(elem)) new Unique(list.filterNot(_ == elem), set - elem)
		else this
	}
	def +(elem: A): Unique[A] = {
		if (set.contains(elem)) this
		else new Unique(list :+ elem, set + elem)
	}
	def contains(elem: A): Boolean = set.contains(elem)
	
	override def toString = list.toString
}
*/

sealed class Unique[A] private ( values : IndexedSeq[A], valueSet : Set[A] )
    //extends IndexedSeqLike[A,Unique[A]]
    //with IndexedSeq[A]
	extends Traversable[A]
	with TraversableLike[A, Unique[A]]
    with GenericTraversableTemplate[A,Unique]
{
	def apply( idx : Int ) : A = values( idx )

	//override def iterator = values.iterator

	//def length = values.length
	
	override def seq = values.seq

	/*override def companion: GenericCompanion[Unique] = new GenericCompanion[Unique]() 
	{
		def newBuilder[A]: Builder[A, Unique[A]] = new Builder[A, Unique[A]] 
		{
			var elems = Vector[A]()
			def +=(a:A) = { elems :+ a; this } 
			def clear() { Vector[A]() }
			def result(): Unique[A] = new Unique[A](elems, elems.toSet)
		}
	}*/

	/*def newBuilder[A]: Builder[A, Unique[A]] = new Builder[A, Unique[A]] 
	{
		var elems = Vector[A]()
		def +=(a:A) = { elems :+ a; this } 
		def clear() { Vector[A]() }
		def result(): Unique[A] = new Unique[A](elems, elems.toSet)
	}*/
	/*
	def newBuilder: Builder[A, Unique[A]] = new LazyBuilder[A, Unique[A]] 
	{
		def result: Unique[A] = {
			val data = parts.foldLeft(List[A]()){(l,n) => l ++ n}
			Unique(data:_*)
		}
	}*/

	def + ( elem : A ) : Unique[A] = valueSet.contains( elem ) match
		{
		case true => this
		case false => new Unique[A]( values :+ elem, valueSet + elem )
		}

	def - ( elem : A ) : Unique[A] = valueSet.contains( elem ) match
		{
		case true => new Unique[A]( values.filter( _ != elem ), valueSet - elem )
		case false => this
		}
	
	def --(elems: Traversable[A]): Unique[A] = {
		val set = elems.toSet
		val values2 = values.filterNot(set.contains)
		new Unique[A](values2, values2.toSet)
	}
	
	def ++(elems: Traversable[A]): Unique[A] = {
		val list = elems.filterNot(valueSet.contains)
		val values2 = values ++ list
		new Unique[A](values2, values2.toSet)
	}
	
	// Method in TraversableOnce
	//override def toSet[B >: A]: scala.collection.immutable.Set[B] = valueSet.toSet
	
	/*override def map[B](fn: A => B): Unique[B] = {
		val values2 = values.map(fn)
		new Unique[B](values2, values2.toSet)
	}*/
	/*override def map[B, That](f: A => B)(implicit bf: CanBuildFrom[Unique[A], B, That]): That = {
		def builder = { // extracted to keep method size under 35 bytes, so that it can be JIT-inlined
			val b = bf(this)
			b.sizeHint(this)
			b
		}
		val b = builder
		for (x <- this) b += f(x)
		b.result
	}*/

	def contains(elem: A): Boolean = valueSet.contains(elem)

	override def companion: GenericCompanion[Unique] = Unique
	def foreach[U](f: A => U) { values foreach f }
	
	def zipWithIndex: Traversable[(A, Int)] = values.zipWithIndex
}

object Unique extends TraversableFactory[Unique] {
	override def apply[A](elems: A*): Unique[A] = {
		new Unique(Vector(elems : _*), Set(elems : _*))
	}
	
	def newBuilder[A] = new UniqueBuilder[A]
	implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Unique[A]] = {
		new CanBuildFrom[Coll, A, Unique[A]] {
			def apply(): Builder[A, Unique[A]] = new UniqueBuilder()
			def apply(from: Coll): Builder[A, Unique[A]] = apply()
		}
	}
	
	class UniqueBuilder[A] extends Builder[A, Unique[A]] {
		private var elems = Vector[A]()
		def +=(a:A) = { elems :+ a; this } 
		def clear() { Vector[A]() }
		def result(): Unique[A] = new Unique[A](elems, elems.toSet)
	}
	
	implicit def seqToUnique[A](l: Iterable[A]): Unique[A] =
		apply(l.toList : _*)
}
