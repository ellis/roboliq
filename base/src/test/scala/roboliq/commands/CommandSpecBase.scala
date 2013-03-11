package roboliq.commands

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen
import spray.json._
import _root_.roboliq._
import _root_.roboliq.core._
import _root_.roboliq.processor._
import roboliq.test.Config01
//import ConversionsDirect._


abstract class CommandSpecBase extends FunSpec with GivenWhenThen {
	protected def makeProcessorBsse(configs: Object*): ProcessorData = {
		val p = new ProcessorData(List(
			new arm.MovePlateHandler,
			new commands.pipette.TipsHandler_Fixed,
			new commands.pipette.TransferHandler,
			new commands.pipette.low.AspirateHandler,
			new commands.pipette.low.DispenseHandler,
			new commands.pipette.low.MixHandler,
			new commands.pipette.low.WashTipsHandler
		))
		p.loadJsonData(Config01.benchJson)
		
		for (o <- configs) {
			if (o.isInstanceOf[JsObject])
				p.loadJsonData(o.asInstanceOf[JsObject])
		}

		When("commands are run")
		val g = p.run()
		//org.apache.commons.io.FileUtils.writeStringToFile(new java.io.File("temp.dot"), g.toDot)
		org.apache.commons.io.FileUtils.writeStringToFile(new java.io.File("temp.html"), g.toHtmlTable)
		p
	}
	
	protected def getObj[A <: Object : TypeTag](id: String)(implicit p: ProcessorData): A = {
		checkObj(p.getObjFromDbAt[A](id, Nil))
	}
	
	protected def getState[A <: Object : TypeTag](id: String, time: List[Int])(implicit p: ProcessorData): A = {
		checkObj(p.getObjFromDbBefore[A](id, time))
	}
	
	protected def checkObj[A <: Object : TypeTag](a_? : RqResult[A]): A = {
		a_? match {
			case RqSuccess(a, w) =>
				assert(w === Nil)
				a
			case RqError(e, w) =>
				info(w.toString)
				assert(e === Nil)
				null.asInstanceOf[A]
		}		
	}
}
