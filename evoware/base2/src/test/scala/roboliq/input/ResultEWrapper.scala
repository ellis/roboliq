package roboliq.input

import scala.language.implicitConversions
import org.scalatest.exceptions.TestFailedException

class ResultEWrapper[A](res: (ResultEData, Option[A])) {
	def value: A = {
		val data = res._1
		if (!data.error_r.isEmpty) {
			throw new TestFailedException(sde => Some(data.error_r.reverse.mkString("\n")), None, sde => 1)
		}
		else if (!data.warning_r.isEmpty)
			throw new TestFailedException(sde => None, None, sde => 1)
		else {
			res._2 match {
				case None =>
					throw new TestFailedException(sde => None, None, sde => 1)
				case Some(a) =>
					a
			}
		}
	}

	def errors: List[String] = {
		res._1.error_r.reverse
	}

	def warnings: List[String] = {
		res._1.warning_r.reverse
	}
}

object ResultEWrapper {
	implicit def toResultEWrapper[A](
		res: (ResultEData, Option[A])
	): ResultEWrapper[A] = {
		new ResultEWrapper(res)
	}
}
