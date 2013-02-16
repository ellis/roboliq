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
		it("should parse Map[String, Substance]") {
			assert(
				convRequirements(JsonParser("""{"first": "water", "second": "powder"}"""), typeOf[Map[String, Substance]])
					=== RqSuccess(Left(Map(
						"first" -> KeyClassOpt(KeyClass(TKP("substance", "water", Nil), typeOf[Substance]), false),
						"second" -> KeyClassOpt(KeyClass(TKP("substance", "powder", Nil), typeOf[Substance]), false)
					)))
			)
			val water = SubstanceLiquid("water", LiquidPhysicalProperties.Water, GroupCleanPolicy.TNL, None)
			val powder = SubstanceOther("powder", Some(3.5))
			assert(
				conv(
					JsonParser("""{"first": "water", "second": "powder"}"""),
					typeOf[Map[String, Substance]],
					Map(
						"first" -> water,
						"second" -> powder
					))
					=== RqSuccess(Map("first" -> water, "second" -> powder))
			)
		}
	}
	
	describe("conv for database objects") {
		val config = JsonParser(
"""{
"tipModel": [
	{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" },
	{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }
],
"tip": [
	{ "id": "TIP1", "index": 0, "model": "Standard 1000ul" },
	{ "id": "TIP2", "index": 1, "model": "Standard 1000ul" },
	{ "id": "TIP3", "index": 2, "model": "Standard 1000ul" },
	{ "id": "TIP4", "index": 3, "model": "Standard 1000ul" },
	{ "id": "TIP5", "index": 4, "model": "Standard 50ul" },
	{ "id": "TIP6", "index": 5, "model": "Standard 50ul" },
	{ "id": "TIP7", "index": 6, "model": "Standard 50ul" },
	{ "id": "TIP8", "index": 7, "model": "Standard 50ul" }
],
"plateModel": [
	{ "id": "Reagent Cooled 8*50ml", "rows": 8, "cols": 1, "wellVolume": "50ml" },
	{ "id": "Reagent Cooled 8*15ml", "rows": 8, "cols": 1, "wellVolume": "50ml" },
	{ "id": "Block 20Pos 1.5 ml Eppendorf", "rows": 4, "cols": 5, "wellVolume": "1.5ml" },
	{ "id": "D-BSSE 96 Well PCR Plate", "rows": 8, "cols": 12, "wellVolume": "200ul" },
	{ "id": "D-BSSE 96 Well Costar Plate", "rows": 8, "cols": 12, "wellVolume": "350ul" },
	{ "id": "D-BSSE 96 Well DWP", "rows": 8, "cols": 12, "wellVolume": "1000ul" },
	{ "id": "Trough 100ml", "rows": 8, "cols": 1, "wellVolume": "100ul" },
	{ "id": "Ellis Nunc F96 MicroWell", "rows": 8, "cols": 12, "wellVolume": "400ul" }
],
"plateLocation": [
	{ "id": "trough1", "plateModels": ["Trough 100ml"] },
	{ "id": "trough2", "plateModels": ["Trough 100ml"] },
	{ "id": "trough3", "plateModels": ["Trough 100ml"] },
	{ "id": "uncooled2_low", "plateModels": ["D-BSSE 96 Well DWP", "Ellis Nunc F96 MicroWell"] },
	{ "id": "uncooled2_high", "plateModels": ["D-BSSE 96 Well Costar Plate"] },
	{ "id": "shaker", "plateModels": ["D-BSSE 96 Well Costar Plate", "D-BSSE 96 Well DWP"] },
	{ "id": "cooled1", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled2", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled3", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled4", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled5", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "regrip", "plateModels": ["D-BSSE 96 Well PCR Plate", "D-BSSE 96 Well Costar Plate"] },
	{ "id": "reader", "plateModels": ["D-BSSE 96 Well Costar Plate"] }
],
"tubeLocation": [
	{ "id": "reagents50", "tubeModels": ["Tube 50ml"], "rackModel": "Reagent Cooled 8*50ml" },
	{ "id": "reagents15", "tubeModels": ["Tube 15ml"], "rackModel": "Reagent Cooled 8*15ml" },
	{ "id": "reagents1.5", "tubeModels": ["Tube 1.5ml"], "rackModel": "Block 20Pos 1.5 ml Eppendorf" }
],
"plate": [
	{ "id": "reagents50", "model": "Reagent Cooled 8*50ml", "location": "reagents50" },
	{ "id": "reagents15", "model": "Reagent Cooled 8*15ml", "location": "reagents15" },
	{ "id": "reagents1.5", "model": "Block 20Pos 1.5 ml Eppendorf", "location": "reagents1.5" }
],
"plate": [
	{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" }
]
}""").asJsObject
	
		val db = new DataBase
		config.fields.foreach(pair => {
			val (table, JsArray(elements)) = pair
			elements.foreach(jsval => {
				val jsobj = jsval.asJsObject
				val key = jsobj.fields("id").asInstanceOf[JsString].value
				val tkp = TKP(table, key, Nil)
				db.set(tkp, Nil, jsval)
			})
		})
		
		//val tipModel = TipModel()
		val plateModel = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
		val plate_P1 = Plate("P1", plateModel, None)
		
		def read(jsval: JsValue, typ: ru.Type): RqResult[Any] = {
			for {
				either <- convRequirements(jsval, typ)
				ret <- either match {
					case Right(ret) => RqSuccess(ret)
					case Left(require_m) => 
						for {
							lookup_l <- RqResult.toResultOfList(require_m.toList.map(pair => {
								val (name, kco) = pair
								for {
									jsval2 <- db.get(kco.kc.key, Nil)
									ret <- read(jsval2, kco.kc.clazz)
								} yield name -> ret
							}))
							lookup_m = lookup_l.toMap
							ret <- conv(jsval, typ, lookup_m)
						} yield ret
				}
			} yield ret
		}
		
		def check[A <: Object : TypeTag](id: String, exp: A) = {
			val typ = ru.typeTag[A].tpe
			it(s"should parse $typ") {
				val ret = for {
					jsval <- db.get(TKP(ConversionsDirect.tableForType(typ), id, Nil))
					ret <- read(jsval, typ)
				} yield ret
				assert(ret === RqSuccess(exp))
			}
		}

		check[PlateModel]("D-BSSE 96 Well PCR Plate", plateModel)
		check[Plate]("P1", plate_P1)
	}
}