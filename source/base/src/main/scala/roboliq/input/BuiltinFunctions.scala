package roboliq.input

import spray.json.JsValue
import roboliq.core.RsResult
import spray.json.JsObject
import roboliq.entities.EntityBase
import spray.json.JsNumber
import scala.collection.mutable.HashMap
import spray.json.JsString
import scala.collection.mutable.ArrayBuffer
import spray.json.JsArray

/*
case class BuiltinAdd2Params(
	n1: BigDecimal, 
	n2: BigDecimal 
)

class BuiltinAdd2 {
	def evaluate(scope: Map[String, JsValue], eb: EntityBase): ContextT[JsObject] = {
		ContextT.context("add2") {
			for {
				//params <- Converter.convAs[BuiltinAddParams](JsObject(scope), eb, None)
				n1 <- Converter2.toBigDecimal(scope, "n1")
				n2 <- Converter2.toBigDecimal(scope, "n2")
			} yield {
				Converter2.makeNumber(n1 + n2)
			}
		}
	}
}
*/

case class BuiltinAddParams(
	numbers: List[BigDecimal] 
)

class BuiltinAdd {
	def evaluate(): ContextE[JsObject] = {
		ContextE.context("add") {
			for {
				params <- ContextE.fromScope[BuiltinAddParams]()
			} yield {
				Converter2.makeNumber(params.numbers.sum)
			}
		}
	}
}

case class BuiltinBuildInput(
	item: List[Map[String, JsObject]],
	transform: List[String]
)

class BuiltinBuild {
	def evaluate(): ContextE[JsObject] = {
		ContextE.context("build") {
			for {
				input <- ContextE.fromScope[BuiltinBuildInput]()
				output_l <- makeItems(input.item)
			} yield Converter2.makeList(output_l)
		}
	}
	
	private def makeItems(item_l: List[Map[String, JsObject]]): ContextE[List[JsObject]] = {
		val var_m = new HashMap[String, JsObject]
		
		def handleVAR(m: Map[String, JsValue]): ContextE[Unit] = {
			m.get("NAME") match {
				case Some(JsString(name)) =>
					val jsobj = JsObject(m - "NAME")
					for {
						x <- ContextE.evaluate(jsobj)
					} yield {
						var_m(name) = x
					}
				case _ =>
					ContextE.error("variable `NAME` must be supplied")
			}
		}

		def handleADD(jsobj: JsObject): ContextE[Option[JsObject]] = {
			jsobj.fields.get("TYPE") match {
				case Some(JsString("map")) =>
					jsobj.fields.get("VALUE") match {
						case None =>
							ContextE.unit(Some(Converter2.makeMap(var_m.toMap)))
						case Some(JsObject(m3)) =>
							ContextE.unit(Some(Converter2.makeMap(var_m.toMap ++ m3)))
						case x =>
							ContextE.error("invalid `VALUE`: "+x)
					}
				case _ =>
					ContextE.evaluate(jsobj).map(x => Some(x))
			}
		}
		
		for {
			output0_l <- ContextE.mapAll(item_l.zipWithIndex) { case (m, i) =>
				ContextE.context("item["+(i+1)+"]") {
					m.toList match {
						case List(("VAR", JsObject(m2))) =>
							handleVAR(m2).map(_ => None)
						case List(("ADD", jsobj2@JsObject(m2))) =>
							handleADD(jsobj2)
					}
				}
			}
		} yield output0_l.flatten
	}
}