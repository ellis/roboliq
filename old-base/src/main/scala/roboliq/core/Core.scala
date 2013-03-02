package roboliq.core


object Core {
	import scalaz.{Success => _, _}
	import Scalaz._

	/*
	implicit def resultToValidation[A](res: roboliq.core.Result[A]): ResultV[A] = {
		res match {
			case roboliq.core.Error(ls) => Failure(ls)
			case roboliq.core.Success(a) => Success(a)
		}
	}

	
	def toV[A](res: roboliq.core.Result[A]): ResultV[A] = {
		res match {
			case roboliq.core.Error(ls) => Failure(ls)
			case roboliq.core.Success(a) => Success(a)
		}
	}
	*/
}
