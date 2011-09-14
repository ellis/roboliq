package roboliq.labs.weizmann.station1

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.roboease._
import roboliq.robots.evoware
import roboliq.robots.evoware.roboeaseext._

private case class EvolabRack(id: String, iGrid: Int, iSite: Int, nCols: Int, nRows: Int, nVolume: Double, sCarrierModel: String)

object Table_DNE extends EvowareRoboeaseTable {
	private def DefineRack(id: String, iGrid: Int, iSite: Int, nCols: Int, nRows: Int, nVolume: Double, sCarrierModel: String = null) =
		EvolabRack(id, iGrid, iSite, nRows, nCols, nVolume, sCarrierModel)
		
	private val evolabRacks: Seq[EvolabRack] = Seq(
		DefineRack("WASTE",1,6,8,12,0, null),
		DefineRack("CSL",2,0,1,8, 5000000,"Carousel MTP"),
		DefineRack("TS4",15,0,1,16, 5000000, null),
		DefineRack("TS5",16,0,1,16, 5000000, null),
		//DefineRack("TR1",14,0,1,8, 5000000),
		DefineRack("TR2",14,1,1,8, 5000000, null),
		DefineRack("TR3",14,2,1,8, 5000000, null),
		DefineRack("TR4",17,0,1,8, 5000000, null),
		DefineRack("TR5",17,1,1,8, 5000000, null),
		DefineRack("TR6",17,2,1,8, 5000000, null),
		DefineRack("TR7",18,0,1,8, 5000000, null),
		DefineRack("TR8",18,1,1,8, 5000000, null),
		DefineRack("TR9",18,2,1,8, 5000000, null),
		//DefineRack("T1",21,0,1,16, 2100),
		//DefineRack("T2",20,0,1,16, 2100),
		//DefineRack("T3",19,0,1,16, 2100),
		DefineRack("E3",22,0,1,16, 2100, null),
		//DefineRack("P1",23,0,12,8, 200,"MP 3Pos Fixed"),
		DefineRack("P2",23,1,12,8, 1200,"MP 3Pos Fixed"),
		DefineRack("T2",23,1,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF2",23,1,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M2",23,1,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P3",23,2,12,8, 1200,"MP 3Pos Fixed"),
		DefineRack("T3",23,2,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF3",23,2,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M3",23,2,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P4",29,0,12,8, 1200,"MP 3Pos Fixed"),
		DefineRack("T4",29,0,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF4",29,0,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M4",29,0,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P5",29,1,12,8, 200,"MP 3Pos Fixed"),
		DefineRack("T5",29,1,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF5",29,1,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M5",29,1,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P6",29,2,12,8, 200,"MP 3Pos Fixed"),
		DefineRack("T6",29,2,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF6",29,2,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M6",29,2,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P7",35,0,12,8, 200,"MP 3Pos Fixed PCR"),
		DefineRack("P8",35,1,12,8, 200,"MP 3Pos Fixed PCR"),
		DefineRack("P9",35,2,12,8, 200,"MP 3Pos Fixed PCR"),
		DefineRack("P10",41,0,12,8, 200,"MP 3Pos Fixed 2+clips"),
		DefineRack("T10",41,0,6,4, 2100,"MP 3Pos Fixed 2+clips"),
		DefineRack("BUF10",41,0,6,8, 2100,"MP 3Pos Fixed 2+clips"),
		DefineRack("M10",41,0,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P11",41,1,12,8, 1200,"MP 3Pos Fixed 2+clips"),
		DefineRack("T11",41,1,6,4, 2100,"MP 3Pos Fixed 2+clips"),
		DefineRack("BUF11",41,1,6,8, 2100,"MP 3Pos Fixed 2+clips"),
		DefineRack("M11",41,1,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P12",41,2,12,8, 200,"MP 3Pos Fixed 2+clips"),
		DefineRack("T12",41,2,6,4, 2100,"MP 3Pos Fixed 2+clips"),
		DefineRack("BUF12",41,2,6,8, 2100,"MP 3Pos Fixed 2+clips"),
		DefineRack("M12",41,2,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P13",47,0,12,8, 200,"MP 3Pos Fixed"),
		DefineRack("T13",47,0,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF13",47,0,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M13",47,0,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P14",47,1,12,8, 1200,"MP 3Pos Fixed"),
		DefineRack("T14",47,1,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF14",47,1,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M14",47,1,24,16,1200,"MP 3Pos Fixed"),
		DefineRack("P15",47,2,12,8, 200,"MP 3Pos Fixed"),
		DefineRack("T15",47,2,6,4, 2100,"MP 3Pos Fixed"),
		DefineRack("BUF15",47,2,6,8, 2100,"MP 3Pos Fixed"),
		DefineRack("M15",47,2,24,16,1200,"MP 3Pos Fixed"),
		//DefineRack("E10",41,0,6,4, 2100),
		//DefineRack("E2",47,1,6,4, 2100),
		//DefineRack("TR1",47,2,6,8, 2100),
		DefineRack("TR1",41,2,6,8, 2100, null),
		DefineRack("LNK",65,0,12,8, 200,"Te-Link"),
		DefineRack("S1",53,0,12,8, 200,"Te-Shake 2Pos"),
		DefineRack("S2",53,1,12,8, 1200,"Te-Shake 2Pos"),
		DefineRack("MS1",53,0,24,16, 200,"Te-Shake 2Pos"),
		DefineRack("MS2",53,1,24,16, 1200,"Te-Shake 2Pos"),
		DefineRack("TP1",59,0,12,8, 200,"Torrey pines"),
		DefineRack("TP2",59,1,12,8, 1200,"Torrey pines"),
		//DefineRack("T1",41,0,6,4, 2100),
		//DefineRack("T2",59,1,6,4, 2100),
		DefineRack("HA1",4,0,12,8, 200,"HOTEL5A"),
		DefineRack("HA2",4,1,12,8, 200,"HOTEL5A"),
		DefineRack("HA3",4,2,12,8, 200,"HOTEL5A"),
		DefineRack("HA4",4,3,12,8, 200,"HOTEL5A"),
		DefineRack("HA5",4,4,12,8, 200,"HOTEL5A"),
		DefineRack("HB1",10,0,12,8, 200,"HOTEL5A"),
		DefineRack("HB2",10,1,12,8, 200,"HOTEL5A"),
		DefineRack("HB3",10,2,12,8, 200,"HOTEL5A"),
		DefineRack("HB4",10,3,12,8, 200,"HOTEL5A"),
		DefineRack("HB5",10,4,12,8, 200,"HOTEL5A"),
		DefineRack("HC1",66,0,12,8, 200,"HOTEL5B"),
		DefineRack("HC2",66,1,12,8, 200,"HOTEL5B"),
		DefineRack("HC3",66,2,12,8, 200,"HOTEL5B"),
		DefineRack("HC4",66,3,12,8, 200,"HOTEL5B"),
		DefineRack("HC5",66,4,12,8, 200,"HOTEL5B"),
		DefineRack("RCH",9,0,12,8, 200,"ROCHE"),
		DefineRack("READER",48,0,12,8, 200,"PLATE_READER")
	)
	
	val sEvowareHeader =
"""3A748C70
20100328_144238 apiuser         
                                                                                                                                
                                                                                                                                
--{ RES }--
V;200
--{ CFG }--
999;218;32;
14;-1;104;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;221;-1;-1;-1;-1;-1;222;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;44;-1;-1;-1;-1;-1;216;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;
998;0;
998;8;Washstation 2Grid Cleaner short;Washstation 2Grid Waste;;;;;Washstation 2Grid DiTi Waste;;
998;;;;;;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;DiTi 1000ul;DiTi 200ul;DiTi 50ul;
998;1000;200;50;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;DiTi 20ul;;;
998;20;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;;;;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;;;;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;Block Eppendorf 24 Pos;;6 pos DeepWell trough;
998;T10;;BUF12;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;;;;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;2;;;
998;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;2;;;
998;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;4;
998;93;4;
998;93;50;
998;85;66;
998;225;48;
998;11;
998;4;0;System;
998;0;7;Te-Shake 2Pos;
998;0;3;Carousel MTP;
998;0;4;Carousel DiTi 200;
998;0;5;Carousel DiTi 1000;
998;0;8;Carousel DiTi 10;
998;0;6;Te-Link;
998;0;0;Hotel 5Pos SPE;
998;0;1;Hotel 5Pos SPE;
998;0;2;Hotel 5Pos DeepWell;
998;4;1;Reader;
998;1;
998;215;96_Well_Microplate;
998;1;
998;53;
998;2;
998;4;
998;5;
998;3;
998;65;
998;4;
998;50;
998;66;
998;48;
996;0;0;
--{ RPG }--
"""
	val racks = evolabRacks.map(r => Rack(r.id, r.nCols, r.nRows, r.iGrid, r.iSite, r.nVolume, r.sCarrierModel))

	import roboliq.robots.evoware._
	
	object EvowareTableMaps {
		type PlateModelKey = Tuple3[Int, Int, Double]
		val carrierModels = new HashMap[String, CarrierModel]
		
		val mapKeyToPlateModel: Map[PlateModelKey, PlateModel] = {
			def toPlateModel(key: PlateModelKey): PlateModel = {
				val (nRows, nCols, nVolume) = key
				val id = nRows.toString+"x"+nCols+" "+nVolume+"ul"
				new PlateModel(id, nRows, nCols, nVolume)
			}
			val keys = evolabRacks.groupBy(r => ((r.nRows, r.nCols, r.nVolume))).keys
			keys.map(key => key -> toPlateModel(key)).toMap
		}
		
		private val mapCarrierToRacks: Map[String, Seq[EvolabRack]] =
			evolabRacks.filter(_.sCarrierModel != null).groupBy(_.sCarrierModel)
			
		val mapCarrierToSiteCount: Map[String, Int] =
			mapCarrierToRacks.mapValues(_.foldLeft(0) { (acc, r) => math.max(acc, r.iSite) })
			
		val mapCarrierModels: Map[String, CarrierModel] =
			mapCarrierToSiteCount.map(pair => pair._1 -> new CarrierModel(pair._1, pair._2, false))
		
		val mapGridToCarrierModel: Map[Int, CarrierModel] =
			evolabRacks.groupBy(_.iGrid).map(pair => {
				val (iGrid, racks) = pair
				val ls: Iterable[String] = racks.groupBy(_.sCarrierModel).keys
				val model = {
					if (ls.size > 1 || ls.head == null)
						new CarrierModel("Carrier for grid"+iGrid.toString, 1, false)
					else
						mapCarrierModels(ls.head)
				}
				iGrid -> model
			})
		
		val mapCarrierModelToGrids: Map[CarrierModel, Seq[Int]] =
			mapGridToCarrierModel.toSeq.groupBy(_._2).mapValues(_.map(_._1))
			
		val mapGridToCarrier: Map[Int, CarrierObj] =
			mapCarrierModelToGrids.flatMap(pair => {
				val (model, liGrid) = pair
				liGrid.sortBy(identity).zipWithIndex.map(pair => {
					val (iGrid, iObj) = pair
					val id = model.sName + (if (liGrid.size > 1) " "+(iObj+1) else "")
					iGrid -> new CarrierObj(id, model, iGrid)
				})
			})
		
		val sites: Seq[SiteObj] = {
			def toSiteObj(pair: Tuple2[Int, Int]): SiteObj = {
				val (iGrid, iSite) = pair
				val id = "["+iGrid+","+iSite+"]"
				val carrier = mapGridToCarrier(iGrid)
				new SiteObj(id, carrier, iSite)
			}
			evolabRacks.map(r => r.iGrid -> r.iSite).toSet.toSeq.map(toSiteObj)
		}

		/*
		val mapGridToCarrier: Map[Int, CarrierObj] = {
			// Count how many times each carrier model is used
			val mapModelToInstanceCount = new HashMap[CarrierModel, Int]().withDefaultValue(0)
			def countInstances(id: String, iGrid: Int, iSite: Int, nCols: Int, nRows: Int, nVolume: Double, sCarrierModel: String) {
				if (sCarrierModel == null)
					return
				val model = mapCarrierModels(sCarrierModel)
				mapModelToInstanceCount(model) = mapModelToInstanceCount(model) + 1
			}
			stuff(countInstances)
			
			val mapModelToInstanceCount2 = new HashMap[CarrierModel, Int]().withDefaultValue(0)
			val carriers = new HashMap[Int, CarrierObj]
			def createCarriers(id: String, iGrid: Int, iSite: Int, nCols: Int, nRows: Int, nVolume: Double, sCarrierModel: String) {
				if (!carriers.contains(iGrid)) {
					val (model, id) = {
						if (sCarrierModel == null) {
							new CarrierModel(iGrid.toString, 1, false) -> ("Carrier at grid "+iGrid)
						}
						else {
							val model = mapCarrierModels(sCarrierModel)
							mapModelToInstanceCount(model) match {
								case 1 => sCarrierModel
								case n =>
									mapModelToInstanceCount2(model) = mapModelToInstanceCount2(model) + 1
							}
						}
					}
					val carrier = new CarrierObj()
				}
				if (sCarrierModel == null)
					return
				sites.get(sCarrierModel) match {
					case None => sites(sCarrierModel) = iSite + 1
					case Some(nSites) =>
						if (iSite + 1 > nSites)
							sites(sCarrierModel) = iSite +1
				}
			}
			stuff(handleSiteCounter)
			
			def toCarrierModel(pair: Tuple2[String, Int]): CarrierModel = {
				val (id, nSites) = pair
				new CarrierModel(id, nSites, false)
			}
			sites.map(pair => pair._1 -> toCarrierModel(pair)).toMap
		}

		val sites: Iterable[SiteObj] = {
			val keys = new HashSet[Tuple2[Int, Int]]
			def handleSites(id: String, iGrid: Int, iSite: Int, nCols: Int, nRows: Int, nVolume: Double, sCarrierModel: String) {
				keys += ((iGrid, iSite))
			}
			stuff(handleSites)

			def toSiteObj(pair: Tuple2[Int, Int]): SiteObj = {
				new SiteObj()
			}
			val sites = new HashMap[Tuple2[Int, Int], SiteObj]
			def handleSites(id: String, iGrid: Int, iSite: Int, nCols: Int, nRows: Int, nVolume: Double, sCarrierModel: String) {
				val key = (iGrid, iSite)
				if (!sites.contains(key)) {
					val carrier = new CarrierObj()
				}
				if (sCarrierModel == null)
					return
				sites.get(sCarrierModel) match {
					case None => sites(sCarrierModel) = iSite + 1
					case Some(nSites) =>
						if (iSite + 1 > nSites)
							sites(sCarrierModel) = iSite +1
				}
			}
			stuff(handleSiteCounter)
			
			def toCarrierModel(pair: Tuple2[String, Int]): CarrierModel = {
				val (id, nSites) = pair
				new CarrierModel(id, nSites, false)
			}
			sites.map(pair => pair._1 -> toCarrierModel(pair)).toMap
		}
	*/
	}
	
	val evowareSites = EvowareTableMaps.sites
	val roboeaseTable = new Table(sEvowareHeader, racks)
}
