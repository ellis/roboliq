package roboliq.processor

import scalaz._
import Scalaz._

import spray.json._

import roboliq.core._
import roboliq.core.RqPimper._


sealed abstract class LookupVariable[A] {
	def lookup(env: Environment): RqResult[A]
	
	def apply(fn: (A) => RqResult[Step]): LookupList = {
		new LookupList1(this, fn)
	}
	
	def |@|[B](b: LookupVariable[B]): LookupListBuilder2[A, B] =
		new LookupListBuilder2(this, b)
	
	def flatMap[B](fn: A => LookupVariable[B]): LookupVariable[B] =
		new LookupVariableComposed(this, fn)
}

class LookupVariableComposed[A, B](
	a: LookupVariable[A],
	fn: A => LookupVariable[B]
) extends LookupVariable[B] {
	def lookup(env: Environment): RqResult[B] = {
		for {
			resA <- a.lookup(env)
			b = fn(resA)
			resB <- b.lookup(env)
		} yield resB
	}
}

class LookupCmdField[A](
	field: String, fn: JsValue => RqResult[A]
) extends LookupVariable[A] {
	def lookup(env: Environment): RqResult[A] = {
		for {
			jsval <- env.lookupField("cmd", "_", field)
			res <- fn(jsval)
		} yield res
	}
} 

class LookupObj[A](
	table: String, key: String, fn: JsObject => RqResult[A]
) extends LookupVariable[A] {
	def lookup(env: Environment): RqResult[A] = {
		for {
			jsval <- env.lookupObject(table, key)
			res <- fn(jsval)
		} yield res
	}
} 

class LookupObjField[A](
	table: String, key: String, field: String, fn: JsValue => RqResult[A]
) extends LookupVariable[A] {
	def lookup(env: Environment): RqResult[A] = {
		for {
			jsval <- env.lookupField(table, key, field)
			res <- fn(jsval)
		} yield res
	}
} 

trait LookupList {
	def run(env: Environment): RqResult[Step]
}

final class LookupList1[A](
	a: LookupVariable[A],
	fn: (A) => RqResult[Step]
) extends LookupList {
	def run(env: Environment): RqResult[Step] = {
		for {
			a1 <- a.lookup(env)
			res <- fn(a1)
		} yield res
	}
}

final class LookupList2[A, B](
	a: LookupVariable[A],
	b: LookupVariable[B],
	fn: (A, B) => RqResult[Step]
) extends LookupList {
	def run(env: Environment): RqResult[Step] = {
		for {
			res1 <- (a.lookup(env) |@| b.lookup(env)) { fn }
			res <- res1
		} yield res
	}
}

final class LookupList3[A, B, C](
	a: LookupVariable[A],
	b: LookupVariable[B],
	c: LookupVariable[C],
	fn: (A, B, C) => RqResult[Step]
) extends LookupList {
	def run(env: Environment): RqResult[Step] = {
		for {
			res1 <- (a.lookup(env) |@| b.lookup(env) |@| c.lookup(env)) { fn }
			res <- res1
		} yield res
	}
}

final class LookupListBuilder2[A, B](
	a: LookupVariable[A],
	b: LookupVariable[B]
) {
	def apply(fn: (A, B) => RqResult[Step]): LookupList = {
		new LookupList2(a, b, fn)
	}
	
	def |@|[C](c: LookupVariable[C]): LookupListBuilder3[C] = {
		new LookupListBuilder3(c)
	}
	
	final class LookupListBuilder3[C](c: LookupVariable[C]) {
		def apply(fn: (A, B, C) => RqResult[Step]): LookupList = {
			new LookupList3(a, b, c, fn)
		}
	}
}
