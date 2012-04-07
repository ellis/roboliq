package roboliq.core


/**
 * Adapted from Option.scala by Martin Odersky
 */
sealed abstract class Result[+A] extends Product {
	self =>
	
	/** Returns the result of applying $f to this $option's value if
	  * this $option is nonempty.
	  * Returns $none if this $option is empty.
	  * Slightly different from `map` in that $f is expected to
	  * return an $option (which could be $none).
	  *
	  * @param  f   the function to apply
	  * @see map
	  * @see foreach
	  */
	def flatMap[B](f: A => Result[B]): Result[B]
	
	/** Returns a $some containing the result of applying $f to this $option's
	  * value if this $option is nonempty.
	  * Otherwise return $none.
	  *
	  * @note This is similar to `flatMap` except here,
	  * $f does not need to wrap its result in an $option.
	  *
	  * @param  f   the function to apply
	  * @see flatMap
	  * @see foreach
	  */
	def map[B](f: A => B): Result[B]
	
	def isError: Boolean
	final def isSuccess: Boolean = !isError
	
	private def isEmpty = isError
	
	final def >>=[B](f: A => Result[B]): Result[B] = flatMap(f)

	/** Returns the option's value.
	  * @note The option must be nonEmpty.
	  * @throws Predef.NoSuchElementException if the option is empty.
	  */
	def get: A

	/** Returns the option's value if the option is nonempty, otherwise
	  * return the result of evaluating `default`.
	  *
	  * @param default  the default expression.
	  */
	@inline final def getOrElse[B >: A](default: => B): B =
		if (isEmpty) default else this.get

	/** Returns the option's value if it is nonempty,
	  * or `null` if it is empty.
	  * Although the use of null is discouraged, code written to use
	  * $option must often interface with code that expects and returns nulls.
	  * @example {{{
	  * val initalText: Result[String] = getInitialText
	  * val textField = new JComponent(initalText.orNull,20)
	  * }}}
	  */
	@inline final def orNull[A1 >: A](implicit ev: Null <:< A1): A1 = this getOrElse null

	/** Returns true if this option is nonempty '''and''' the predicate
	  * $p returns true when applied to this $option's value.
	  * Otherwise, returns false.
	  *
	  * @param  p   the predicate to test
	  */
	@inline final def exists(p: A => Boolean): Boolean =
		!isEmpty && p(this.get)

	/** Apply the given procedure $f to the option's value,
	  * if it is nonempty. Otherwise, do nothing.
	  *
	  * @param  f   the procedure to apply.
	  * @see map
	  * @see flatMap
	  */
	@inline final def foreach[U](f: A => U) {
		if (!isEmpty) f(this.get)
	}

	/** Returns this $option if it is nonempty,
	  * otherwise return the result of evaluating `alternative`.
	  * @param alternative the alternative expression.
	  */
	@inline final def orElse[B >: A](alternative: => Result[B]): Result[B] =
		if (isEmpty) alternative else this

	/** Returns a singleton iterator returning the $option's value
	  * if it is nonempty, or an empty iterator if the option is empty.
	  */
	def iterator: Iterator[A] =
		if (isEmpty) collection.Iterator.empty else collection.Iterator.single(this.get)

	/** Returns a singleton list containing the $option's value
	  * if it is nonempty, or the empty list if the $option is empty.
	  */
	def toList: List[A] =
		if (isEmpty) List() else List(this.get)

	/** Returns a [[scala.Left]] containing the given
	  * argument `left` if this $option is empty, or
	  * a [[scala.Right]] containing this $option's value if
	  * this is nonempty.
	  *
	  * @param left the expression to evaluate and return if this is empty
	  * @see toLeft
	  */
	@inline final def toRight[X](left: => X) =
		if (isEmpty) Left(left) else Right(this.get)

	/** Returns a [[scala.Right]] containing the given
	  * argument `right` if this is empty, or
	  * a [[scala.Left]] containing this $option's value
	  * if this $option is nonempty.
	  *
	  * @param right the expression to evaluate and return if this is empty
	  * @see toRight
	  */
	@inline final def toLeft[X](right: => X) =
		if (isEmpty) Right(right) else Left(this.get)
		
	/** Returns a [[scala.None]] containing the given
	  * argument `left` if this $option is empty, or
	  * a [[scala.Some]] containing this $option's value if
	  * this is nonempty.
	  *
	  * @param left the expression to evaluate and return if this is empty
	  * @see toLeft
	  */
	@inline final def toOption: Option[A] =
		if (isEmpty) None else Some(this.get)
}

case class Error[T](lsError: Seq[String]) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = Error[B](lsError)
	def map[B](f: T => B): Result[B] = Error[B](lsError)
	def isError: Boolean = true
	def get: T = throw new NoSuchElementException("Error.get")
}

object Error {
	def apply(sError: String) = new Error(Seq(sError))
}

case class Success[T](value: T) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = f(value)
	def map[B](f: T => B): Result[B] = Success[B](f(value))
	def isError: Boolean = false
	def get = value
}

object Result {
	def assert(b: Boolean, sError: String): Result[Unit] =
		if (b) Success(()) else Error(sError)
	
	def get[T](opt: Option[T], sError: String): Result[T] = opt match {
		case None => Error(sError)
		case Some(v) => Success(v)
	}
	
	def get[T](opt: Either[Seq[String], T], sError: String): Result[T] = opt match {
		case Left(lsError) => Error(sError)
		case Right(v) => Success(v)
	}
	
	def mustBeSet[T](o: T, name: String): Result[T] = {
		if (o == null) Error("`"+name+"` must be set")
		else Success(o)
	}
	
	def getNonNull[T](o: T, sError: String): Result[T] = {
		if (o == null) Error(sError)
		else Success(o)
	}
	
	def sequence[A](l: Seq[Result[A]]): Result[Seq[A]] = {
		val llsError = l.collect({ case Error(lsError) => lsError })
		if (!llsError.isEmpty)
			Error(llsError.flatten)
		else
			Success(l.map(_.asInstanceOf[Success[A]].value))
	}
	
	def map[A, B](l: Iterable[A], f: (A) => Result[B]): Result[Iterable[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def map[A, B](l: Seq[A], f: (A) => Result[B]): Result[Seq[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def mapOver[A, B](l: Seq[A])(f: A => Result[B]): Result[Seq[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def mapOver[A, B](l: List[A])(f: A => Result[B]): Result[List[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def flatMap[A, B](l: Iterable[A], f: A => Result[Iterable[B]]): Result[Iterable[B]] = {
		Success(l.flatMap(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def flatMap[A, B](l: Seq[A])(f: A => Result[Seq[B]]): Result[Seq[B]] = {
		Success(l.flatMap(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def flatMap[A, B](l: List[A])(f: A => Result[List[B]]): Result[List[B]] = {
		Success(l.flatMap(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def forall[A](l: Seq[A], f: A => Result[_ <: Any]): Result[Unit] = {
		l.foreach(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case _ =>
		})
		Success(())
	}
}
