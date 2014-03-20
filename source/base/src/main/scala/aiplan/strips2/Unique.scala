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
sealed class Unique[A] private ( values : IndexedSeq[A], valueSet : Set[A] )
	extends Traversable[A]
	with TraversableLike[A, Unique[A]]
    with GenericTraversableTemplate[A,Unique]
{
	def apply( idx : Int ) : A = values( idx )

	override def seq = values.seq

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
*/

import collection.TraversableLike
import collection.generic.{CanBuildFrom, GenericCompanion, GenericTraversableTemplate, TraversableFactory}
import collection.mutable.{Builder, ListBuffer}
import scala.collection.mutable.HashSet

class Unique[A] private (list: List[A], set: Set[A]) extends Traversable[A]
	with TraversableLike[A, Unique[A]]
	with GenericTraversableTemplate[A, Unique]
{
	def apply(index: Int): A = list(index)
	override def companion: GenericCompanion[Unique] = Unique
	def foreach[U](f: A => U) { list foreach f }
	override def seq = list

	def + ( elem : A ) : Unique[A] = set.contains( elem ) match
		{
		case true => this
		case false => new Unique[A]( list ++ List(elem), set + elem )
		}

	def - ( elem : A ) : Unique[A] = set.contains( elem ) match
		{
		case true => new Unique[A]( list.filter( _ != elem ), set - elem )
		case false => this
		}
	
	def --(elems: Traversable[A]): Unique[A] = {
		val set = elems.toSet
		val values2 = list.filterNot(set.contains)
		new Unique[A](values2, values2.toSet)
	}
	
	def ++(elems: Traversable[A]): Unique[A] = {
		val list2 = elems.filterNot(set.contains)
		val values2 = list ++ list2
		new Unique[A](values2, values2.toSet)
	}
	
	def contains(elem: A): Boolean = set.contains(elem)

	def zipWithIndex: Traversable[(A, Int)] = list.zipWithIndex
}

object Unique extends TraversableFactory[Unique] {
	def newBuilder[A] = new UniqueBuilder[A]
	
	implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Unique[A]] = {
	    new CanBuildFrom[Coll, A, Unique[A]] {
	       def apply(): Builder[A, Unique[A]] = new UniqueBuilder()
	       def apply(from: Coll): Builder[A, Unique[A]] = apply()
	    }
	}

	class UniqueBuilder[A] extends Builder[A, Unique[A]] {
		private val list = new ListBuffer[A]()
		private val set = new HashSet[A]()
		
		def += (elem: A): this.type = {
			if (!set.contains(elem)) {
				set += elem
				list += elem
			}
			this
		}
		
		def clear() {
			set.clear()
			list.clear()
		}
		
		def result(): Unique[A] = new Unique(list.result(), set.toSet)
	}
}
