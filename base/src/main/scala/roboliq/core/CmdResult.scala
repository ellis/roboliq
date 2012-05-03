package roboliq.core

class CmdResultLog(
	val lsError: List[String],
	val lsWarning: List[String],
	val lsInfo: List[String]
) {
	def ++(that: CmdResultLog): CmdResultLog =
		new CmdResultLog(lsError ++ that.lsError, lsWarning ++ that.lsWarning, lsInfo ++ that.lsInfo)
}

sealed abstract class CmdResult[+A] {
	val log: CmdResultLog
	
	def getOptR: CmdContinue[Option[A]]
	def getOrNull: A
	def continue: CmdContinue[Unit]

	def prependLog[B](that: CmdResult[B]): CmdResult[A]
	
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
	def flatMap[B](f: A => CmdResult[B]): CmdResult[B]
	
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
	def map[B](f: A => B): CmdResult[B]
}

case class CmdContinue[+A](value: A, log: CmdResultLog) extends CmdResult[A] {
	def getOptR: CmdContinue[Option[A]] = CmdContinue(Some(value), log)
	def getOrNull = value
	def continue: CmdContinue[Unit] = CmdContinue[Unit]((), log)
	def prependLog[B](that: CmdResult[B]): CmdResult[A] = CmdContinue[A](value, that.log ++ log)
	def flatMap[B](f: A => CmdResult[B]): CmdResult[B] = f(value).prependLog(this)
	def map[B](f: A => B): CmdResult[B] = CmdContinue(f(value), log)
}

object CmdContinue {
	def apply[A](value: A): CmdContinue[A] = CmdContinue(value, new CmdResultLog(Nil, Nil, Nil))
}

case class CmdStop[A](log: CmdResultLog) extends CmdResult[A] {
	def getOptR: CmdContinue[Option[A]] = CmdContinue(None, log)
	def getOrNull: A = null.asInstanceOf[A]
	def continue: CmdContinue[Unit] = CmdContinue[Unit]((), log)
	def prependLog[B](that: CmdResult[B]): CmdResult[A] = CmdStop[A](that.log ++ log)
	def flatMap[B](f: A => CmdResult[B]): CmdResult[B] = CmdStop[B](log)
	def map[B](f: A => B): CmdResult[B] = CmdStop(log)
}