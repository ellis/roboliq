package roboliq.common


sealed abstract class Result[+T] {
	def flatMap[B](f: T => Result[B]): Result[B]
	def map[B](f: T => B): Result[B]
	def isError: Boolean
	def isSuccess: Boolean
	
	final def >>=[B](f: T => Result[B]): Result[B] = flatMap(f)
	def foreach[B](f: T => B): Unit = {  
		map(f)  
		()
	}
}
case class Error[T](lsError: Seq[String]) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = Error[B](lsError)
	def map[B](f: T => B): Result[B] = Error[B](lsError)
	def isError: Boolean = true
	def isSuccess: Boolean = false
}
object Error {
	def apply(sError: String) = new Error(Seq(sError))
}
case class Success[T](value: T) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = f(value)
	def map[B](f: T => B): Result[B] = Success[B](f(value))
	def isError: Boolean = false
	def isSuccess: Boolean = true
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
