package aiplan.strips2

import collection.TraversableLike
import collection.generic.{CanBuildFrom, GenericCompanion, GenericTraversableTemplate, TraversableFactory}
import collection.mutable.{Builder, ListBuffer}

object CustomCollection extends TraversableFactory[CustomCollection] {
  def newBuilder[A] = new CustomCollectionBuilder[A]
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, CustomCollection[A]] = 
    new CanBuildFrom[Coll, A, CustomCollection[A]] {
       def apply(): Builder[A, CustomCollection[A]] = new CustomCollectionBuilder()
       def apply(from: Coll): Builder[A, CustomCollection[A]] = apply()
    }
}
case class CustomCollection[A](list: List[A]) extends Traversable[A]
with TraversableLike[A, CustomCollection[A]]
with GenericTraversableTemplate[A, CustomCollection] {
  override def companion: GenericCompanion[CustomCollection] = CustomCollection
  def foreach[U](f: A => U) { list foreach f }
  override def seq = list
}

class CustomCollectionBuilder[A] extends Builder[A, CustomCollection[A]] {
  private val list = new ListBuffer[A]()
  def += (elem: A): this.type = {
    list += elem
    this
  }
  def clear() {list.clear()}
  def result(): CustomCollection[A] = CustomCollection(list.result())
}