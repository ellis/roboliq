package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._


class KnowledgeBase {
	private val m_liqs = new HashSet[Liquid]
	private val m_objs = new HashSet[Obj]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[Plate]
	private val m_setups = new HashMap[Obj, ObjSetup]

	// Move this to Pipette device's knowledgebase
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	
	val setups: scala.collection.Map[Obj, ObjSetup] = m_setups
	
	def addLiquid(o: Liquid) {
		m_liqs += o
	}
	
	def addObject(o: Obj) {
		if (!m_objs.contains(o)) {
			m_objs += o
			m_setups(o) = o.createSetup()
		}
	}
	
	def addWell(o: Well) {
		addObject(o)
		m_wells += o
	}
	
	def addWell(o: Well, bSrc: Boolean) {
		val setup = getWellSetup(o)
		if (setup.bRequiresIntialLiq_?.isEmpty)
			setup.bRequiresIntialLiq_? = Some(bSrc)
	}
	
	def addPlate(o: Plate) {
		addObject(o)
		m_plates += o
	}
	
	def addPlate(o: Plate, bSrc: Boolean) {
		val setup = getPlateSetup(o)
		if (setup.dim_?.isDefined)
			setup.dim_?.get.wells.foreach(well => addWell(well, bSrc))
	}
	
	private def getObjData[T](o: Obj): T =
		m_setups(o).asInstanceOf[T]
	
	def getWellSetup(o: Well): WellSetup = {
		addWell(o)
		getObjData(o)
	}
	
	def getPlateSetup(o: Plate): PlateSetup = {
		addPlate(o)
		getObjData(o)
	}
	
	def getLiqWells(liq: Liquid): Set[Well] = {
		m_wells.filter(well => {
			val wellSetup = getWellSetup(well)
			wellSetup.liquid_? match {
				case None => false
				case Some(liq2) => (liq eq liq2)
			}
		}).toSet
	}
	
	def doesWellRequireInitialLiq(well: Well): Boolean = {
		getWellSetup(well).bRequiresIntialLiq_? match {
			case None => false
			case Some(b) => b
		}
	}
	
	def printKnown() {
		println("Liquids:")
		m_liqs.foreach(println)
		println()
		
		println("Plates:")
		m_plates.foreach(println)
		println()
		
		println("Wells:")
		m_wells.foreach(println)
		println()
	}
	
	type Errors = Seq[Tuple2[Obj, Seq[String]]]
	
	def concretize(): Either[Errors, ObjMapper] = {
		for (plate <- m_plates) {
			val pc = getPlateSetup(plate)
			if (pc.dim_?.isDefined && pc.sLabel_?.isDefined) {
				for ((well, i) <- pc.dim_?.get.wells.zipWithIndex) {
					val wc = getWellSetup(well)
					wc.sLabel_? = Some(pc.sLabel_?.get + ":" + (i+1))
					wc.holder_? = Some(plate)
					wc.index_? = Some(i)
				}
			}
		}
		
		val l = m_setups.map(pair => {
			val (obj, setup) = pair
			obj.createConfigAndState0(setup.asInstanceOf[obj.Setup]) match {
				case Left(ls) => Left(obj -> ls)
				case Right((conf, state)) => Right((obj, setup, conf, state))
			}
		})
		
		if (l.forall(_.isRight)) {
			val map = l.map(_.right.get match {
				case (obj, setup, conf, state) => obj -> new SetupConfigState(setup, conf, state)
			}).toMap
			val map31 = new ObjMapper(map)
			Right(map31)
		}
		else {
			val errors = l.filter(_.isLeft).map(_.left.get)
			Left(errors.toSeq)
		}
	}
}
