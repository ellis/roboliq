package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.FunSpec
import spray.json._
import roboliq.core._
import ConversionsDirect._


object A extends Enumeration {
	val B, C = Value
}

class ConversionsSpec extends FunSpec {
	private def getTypeTag[T: TypeTag](obj: T) = ru.typeTag[T]
	private def getType[T: TypeTag](obj: T) = ru.typeTag[T].tpe

	//def check[A: TypeTag](jsval: JsValue, exp: A) =
	//	assert(ConversionsDirect.conv(jsval, ru.typeTag[A].tpe) == RqSuccess(exp))

	describe("conv") {
		def check[A: TypeTag](succeed_l: List[(JsValue, A)], fail_l: List[JsValue]) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				for (pair <- succeed_l) {
					assert(conv(pair._1, typ) === RqSuccess(pair._2))
				}
				for (jsval <- fail_l) {
					assert(conv(jsval, typ).isError)
				}
			}
		}

		check[String](
			List(JsString("test") -> "test"),
			List(JsNumber(1), JsNull)
		)
		check[Int](
			List(JsNumber(42) -> 42),
			List(JsString("42"), JsNull)
		)
		check[Integer](
			List(JsNumber(42) -> 42),
			List(JsString("42"), JsNull)
		)
		check[BigDecimal](
			List(JsNumber(42) -> 42),
			List(JsString("42"), JsNull)
		)
		check[Boolean](
			List(JsBoolean(true) -> true, JsBoolean(false) -> false),
			List(JsString("42"), JsNumber(1), JsNumber(0), JsNull)
		)
		check[java.lang.Boolean](
			List(JsBoolean(true) -> true, JsBoolean(false) -> false),
			List(JsString("42"), JsNumber(1), JsNumber(0), JsNull)
		)
		check[A.Value](
			List(JsString("B") -> A.B, JsString("C") -> A.C),
			List(JsString(""), JsNumber(1), JsNull)
		)
		check[Option[String]](
			List(JsString("Hello") -> Some("Hello"), JsNull -> None),
			List(JsNumber(1))
		)
		check[List[Int]](
			List(
				JsArray(List(JsNumber(1), JsNumber(2), JsNumber(3))) -> List(1, 2, 3),
				JsArray(List()) -> Nil,
				JsNull -> Nil,
				JsNumber(1) -> List(1)),
			List(JsString("1"))
		)
		check[Set[Int]](
			List(
				JsArray(List(JsNumber(1), JsNumber(2), JsNumber(3))) -> Set(1, 2, 3),
				JsArray(List(JsNumber(1), JsNumber(2), JsNumber(2))) -> Set(1, 2),
				JsArray(List()) -> Set(),
				JsNull -> Set(),
				JsNumber(1) -> Set(1)),
			List(JsString("1"))
		)
		check[Map[String, Int]](
			List(
				JsonParser("""{"a": 1, "b": 2}""") -> Map("a" -> 1, "b" -> 2),
				JsNull -> Map()
			),
			List(JsString("1"))
		)
		check[PipettePolicy](
			List(
				JsonParser("""{"id": "myId", "pos": "WetContact"}""") -> PipettePolicy("myId", PipettePosition.WetContact)
			),
			List(JsNull)
		)
		check[Substance](
			List(
				JsonParser("""{"id": "water", "kind": "liquid", "physicalProperties": "Water", "cleanPolicy": {"enter": "Thorough", "within": "None", "exit": "Light"}}""") -> SubstanceLiquid("water", LiquidPhysicalProperties.Water, GroupCleanPolicy.TNL, None)
			),
			List(JsNull)
		)
		/*
		it("should parse String") {
			check(JsString("test"), "test")
			check(JsString("test"), "test")
		}
		it("should parse Int") {
			check(JsNumber(42), 42)
		}
			check(JsNumber(42), 42.asInstanceOf[Integer])
		it("should parse Enum") {
			assert(conv(JsString("B"), typeOf[A.Value]) === RqSuccess(A.B))
			assert(conv(JsString("C"), typeOf[A.Value]) === RqSuccess(A.C))
			assert(conv(JsString(""), typeOf[A.Value]).isError)
			assert(conv(JsNumber(1), typeOf[A.Value]).isError)
			assert(conv(JsNull, typeOf[A.Value]).isError)
		}*/
	}
	
	describe("convRequirements") {
		/*def checkList[A: TypeTag](jsval: JsValue, lookup_m: Map[String, Object]succeed_l: List[(JsValue, A)], fail_l: List[JsValue]) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				for (pair <- succeed_l) {
					assert(conv(pair._1, typ) === RqSuccess(pair._2))
				}
				for (jsval <- fail_l) {
					assert(conv(jsval, typ).isError)
				}
			}
		}*/
	/*val idVessel: String,
	val mapSolventToVolume: Map[SubstanceLiquid, LiquidVolume],
	val mapSoluteToMol: Map[SubstanceSolid, BigDecimal]
		check[VesselContent](
			List(
				JsonParser("""{"idVessel": "water", "kind": "liquid", "physicalProperties": "Water", "cleanPolicy": {"enter": "Thorough", "within": "None", "exit": "Light"}}""") -> SubstanceLiquid("water", LiquidPhysicalProperties.Water, GroupCleanPolicy.TNL, None)
			),
			List(JsNull)
		)*/
		it("should parse Map[Substance, Int]") {
			assert(
				convRequirements(JsonParser("""{"water": 1, "powder": 20}"""), typeOf[Map[Substance, Int]])
					=== RqSuccess(Left(Map(
						"water#" -> KeyClassOpt(KeyClass(TKP("substance", "water", Nil), typeOf[Substance]), false),
						"powder#" -> KeyClassOpt(KeyClass(TKP("substance", "powder", Nil), typeOf[Substance]), false)
					)))
			)
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, GroupCleanPolicy.TNL, None)
			val powder = SubstanceOther("powder", Some(3.5))
			assert(
				conv(
					JsonParser("""{"water": 1, "powder": 20}"""),
					typeOf[Map[Substance, Int]],
					Map(
						"water#" -> water,
						"powder#" -> powder
					))
					=== RqSuccess(Map(water -> 1, powder -> 20))
			)
		}
	}
}