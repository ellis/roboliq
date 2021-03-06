package roboliq.processor

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.GivenWhenThen
import spray.json._
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import ConversionsDirect._
import _root_.roboliq.commands._
import _root_.roboliq._
import roboliq.commands.pipette.TipWellVolumePolicy


class ProcessorSpec extends FunSpec with GivenWhenThen {
	describe("A Processor") {
		val tipModel1 = TipModel("TIPMODEL1", LiquidVolume.ul(950), LiquidVolume.ul(4))
		val tip1 = Tip("TIP1", "LiHa", 0, 0, 0, Some(tipModel1))
		val tip2 = Tip("TIP2", "LiHa", 1, 1, 0, Some(tipModel1))
		val plateModel1 = PlateModel("PLATEMODEL1", 8, 12, LiquidVolume.ul(200))
		val plateLocation1 = PlateLocation("PLATELOCATION1", List(plateModel1), true)
		val plateLocation2 = PlateLocation("PLATELOCATION2", List(plateModel1), true)
		val tubeModel1 = TubeModel("TUBEMODEL1", LiquidVolume.ml(15))
		val plate1 = Plate("PLATE1", plateModel1, None)
		val plate2 = Plate("PLATE2", plateModel1, None)
		val vessel_PLATE1_A01 = Vessel("PLATE1(A01)", None)
		val vessel_TUBE1 = Vessel("TUBE1", Some(tubeModel1))
		val tipState1 = TipState.createEmpty(tip1)
		val plateState_P1 = PlateState(plate1, Some(plateLocation1))
		val plateState_P2 = PlateState(plate2, Some(plateLocation2))
		val vesselState_PLATE1_A01 = VesselState(vessel_PLATE1_A01, VesselContent.Empty)
		val vesselState_T1 = VesselState(vessel_TUBE1, VesselContent.Empty)
		val vesselSituatedState_PLATE1_A01 = VesselSituatedState(vesselState_PLATE1_A01, VesselPosition(plateState_P1, 0))
		val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_P2, 0))
		
		def makeTable[A <: Object : TypeTag](a_l: A*): RqResult[(String, JsArray)] = {
			val l: List[A] = a_l.toList
			for {
				table <- ConversionsDirect.findTableForType(ru.typeTag[A].tpe)
				jsval_l <- RqResult.toResultOfList(l.map(ConversionsDirect.toJson[A]))
			} yield table -> JsArray(jsval_l)
		}
		val l1 : List[RqResult[(String, JsArray)]] = List(
			makeTable[TipModel](tipModel1),
			makeTable[Tip](tip1, tip2),
			makeTable[PlateModel](plateModel1),
			makeTable[PlateLocation](plateLocation1, plateLocation2),
			makeTable[TubeModel](tubeModel1),
			makeTable[Plate](plate1, plate2),
			makeTable[Vessel](vessel_PLATE1_A01, vessel_TUBE1),
			makeTable(tipState1),
			makeTable(plateState_P1, plateState_P2),
			makeTable(vesselState_PLATE1_A01, vesselState_T1),
			makeTable(vesselSituatedState_PLATE1_A01, vesselSituatedState_T1)
		)
		//info("l1: "+l1)
		val l_? : RqResult[List[(String, JsArray)]] = RqResult.toResultOfList(l1)
		val jsobj_? = l_?.map(l => JsObject(l.toMap))
		
		val p = new ProcessorData(List(
			new roboliq.device.transport.MovePlateHandler,
			new roboliq.commands.pipette.low.AspirateHandler,
			new roboliq.commands.pipette.low.DispenseHandler,
			new roboliq.commands.pipette.low.MixHandler
		))
		
		//info("jsobj_?: "+jsobj_?)
		assert(jsobj_?.isSuccess)
		jsobj_?.foreach(jsobj => {
			//println("jsobj: "+jsobj)
			Given("a specific configuration")
			p.loadJsonData(jsobj)
			println("p.db:")
			println(p.db)

			it("should hold a copy of each of those configuration objects") {
				assert(p.getObjFromDbAt[TipModel]("TIPMODEL1", Nil) === RqSuccess(tipModel1))
				assert(p.getObjFromDbAt[Tip]("TIP1", Nil) === RqSuccess(tip1))
				assert(p.getObjFromDbAt[PlateModel]("PLATEMODEL1", Nil) === RqSuccess(plateModel1))
				assert(p.getObjFromDbAt[PlateLocation]("PLATELOCATION1", Nil) === RqSuccess(plateLocation1))
				assert(p.getObjFromDbAt[PlateLocation]("PLATELOCATION2", Nil) === RqSuccess(plateLocation2))
				assert(p.getObjFromDbAt[TubeModel]("TUBEMODEL1", Nil) === RqSuccess(tubeModel1))
				assert(p.getObjFromDbAt[Plate]("PLATE1", Nil) === RqSuccess(plate1))
				assert(p.getObjFromDbAt[Plate]("PLATE2", Nil) === RqSuccess(plate2))
				assert(p.getObjFromDbAt[Vessel]("PLATE1(A01)", Nil) === RqSuccess(vessel_PLATE1_A01))
				assert(p.getObjFromDbAt[Vessel]("TUBE1", Nil) === RqSuccess(vessel_TUBE1))
				assert(p.getObjFromDbAt[TipState]("TIP1", List(0)) === RqSuccess(tipState1))
				assert(p.getObjFromDbAt[PlateState]("PLATE1", List(0)) === RqSuccess(plateState_P1))
				assert(p.getObjFromDbAt[PlateState]("PLATE2", List(0)) === RqSuccess(plateState_P2))
				assert(p.getObjFromDbAt[VesselState]("PLATE1(A01)", List(0)) === RqSuccess(vesselState_PLATE1_A01))
				assert(p.getObjFromDbAt[VesselState]("TUBE1", List(0)) === RqSuccess(vesselState_T1))
				assert(p.getObjFromDbAt[VesselSituatedState]("PLATE1(A01)", List(0)) === RqSuccess(vesselSituatedState_PLATE1_A01))
				assert(p.getObjFromDbAt[VesselSituatedState]("TUBE1", List(0)) === RqSuccess(vesselSituatedState_T1))
			}
		})
	}
}
