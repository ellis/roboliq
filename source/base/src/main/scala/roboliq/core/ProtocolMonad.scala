package roboliq.core

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.language.higherKinds
import scala.language.implicitConversions
import roboliq.entities.WorldState
import roboliq.entities.EntityBase
import roboliq.entities.Aliquot
import roboliq.entities.Well


/**
 * ProtocolState should carry the eb, current world state, current command, current instruction, warnings and errors, and various HDF5 arrays
 */
case class ProtocolContext(
	eb: EntityBase,
	state: WorldState,
	command: List[Int],
	instruction: List[Int],
	warning_r: List[String],
	error_r: List[String],
	well_aliquot_r: List[(List[Int], Well, Aliquot)]
) {
	def setState(state: WorldState): ProtocolContext =
		copy(state = state)
	
	def setCommand(idx: List[Int]): ProtocolContext =
		copy(command = idx, instruction = Nil)

	def setInstruction(idx: List[Int]): ProtocolContext =
		copy(instruction = idx)
	
	def log[A](res: RsResult[A]): ProtocolContext = {
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r ++ this.warning_r, error_r = error_l.reverse ++ this.error_r)
		}
	}
}

/*
sealed trait ProtocolMonad[+A] {
	def run(ctx: ProtocolContext): (ProtocolContext, A)
	
	def map[B](f: A => B): ProtocolMonad[B] = {
		ProtocolMonad { ctx =>
			val (ctx1, a) = run(ctx)
			(ctx1, f(a))
		}
	}
	
	def flatMap[B](f: A => ProtocolMonad[B]): ProtocolMonad[B] = {
		ProtocolMonad { ctx =>
			val (ctx1, a) = run(ctx)
			f(a).run(ctx1)
		}
	}
}
*/
sealed trait ProtocolMonad[+A] {
	def run(ctx: ProtocolContext): (ProtocolContext, Option[A])
	
	def map[B](f: A => B): ProtocolMonad[B] = {
		ProtocolMonad { ctx =>
			if (ctx.error_r.isEmpty) {
				val (ctx1, optA) = run(ctx)
				val optB = optA.map(f)
				(ctx1, optB)
			}
			else {
				(ctx, None)
			}
		}
	}
	
	def flatMap[B](f: A => ProtocolMonad[B]): ProtocolMonad[B] = {
		ProtocolMonad { ctx =>
			if (ctx.error_r.isEmpty) {
				val (ctx1, optA) = run(ctx)
				optA match {
					case None => (ctx1, None)
					case Some(a) => f(a).run(ctx1)
				}
			}
			else {
				(ctx, None)
			}
		}
	}
	
	/*private def pair[A](ctx: ProtocolContext, res: RsResult[A]): (ProtocolContext, RsResult[A]) = {
		(ctx.log(res), res)
	}*/
}

object ProtocolMonad {
	def apply[A](f: ProtocolContext => (ProtocolContext, Option[A])): ProtocolMonad[A] = {
		new ProtocolMonad[A] {
			def run(ctx: ProtocolContext) = f(ctx)
		}
	}
	
	def from[A](res: RsResult[A]): ProtocolMonad[A] = {
		ProtocolMonad { ctx =>
			val ctx1 = ctx.log(res)
			(ctx1, res.toOption)
		}
	}
	
	def unit[A](a: A): ProtocolMonad[A] =
		ProtocolMonad { ctx => (ctx, Some(a)) }
	
	def get: ProtocolMonad[ProtocolContext] =
		ProtocolMonad { ctx => (ctx, Some(ctx)) }
	
	def gets[A](f: ProtocolContext => A): ProtocolMonad[A] =
		ProtocolMonad { ctx => (ctx, Some(f(ctx))) }
	
	def put(ctx: ProtocolContext): ProtocolMonad[Unit] =
		ProtocolMonad { _ => (ctx, Some(())) }
	
	def modify(f: ProtocolContext => ProtocolContext): ProtocolMonad[Unit] =
		ProtocolMonad { ctx => (f(ctx), Some(())) }
	
	//def getResult(a: A): ProtocolMonad[A] =
	//	RsError(ctx.error_r, ctx.warning_r)
}