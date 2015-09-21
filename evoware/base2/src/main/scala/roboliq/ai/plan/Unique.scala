package roboliq.ai.plan

import collection.TraversableLike
import collection.generic.{CanBuildFrom, GenericCompanion, GenericTraversableTemplate, TraversableFactory}
import collection.mutable.{Builder, ListBuffer}
import scala.collection.mutable.HashSet
import scala.collection.immutable.VectorBuilder

class Unique[A] private (list: Vector[A], set: Set[A]) extends Traversable[A]
	with TraversableLike[A, Unique[A]]
	with GenericTraversableTemplate[A, Unique]
{
	def apply(index: Int): A = list(index)
	override def companion: GenericCompanion[Unique] = Unique
	def foreach[U](f: A => U) { list foreach f }
	override def seq = list

	def +(elem: A): Unique[A] = {
		if (set.contains(elem)) this
		else new Unique(list :+ elem, set + elem)
	}

	def -(elem: A): Unique[A] = {
		if (set.contains(elem)) new Unique(list.filter(_ != elem), set - elem)
		else this
	}
	
	def --(elems: Traversable[A]): Unique[A] = {
		val set = elems.toSet
		val values2 = list.filterNot(set.contains)
		new Unique(values2, values2.toSet)
	}
	
	def ++(elems: Traversable[A]): Unique[A] = {
		val list2 = elems.filterNot(set.contains)
		val values2 = list ++ list2
		new Unique(values2, values2.toSet)
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
		private val list = Vector.newBuilder[A]
		private val set = new HashSet[A]()
		
		def += (elem: A): this.type = {
			if (!set.contains(elem)) {
				list += elem
				set += elem
			}
			this
		}
		
		def clear() {
			list.clear()
			set.clear()
		}
		
		def result(): Unique[A] = new Unique(list.result, set.toSet)
	}
}
