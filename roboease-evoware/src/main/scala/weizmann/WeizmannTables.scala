package weizmann

import scala.collection.mutable.ArrayBuffer

import roboliq.roboease._


class WeizmannTables {
	private class X {
		val racks = new ArrayBuffer[Rack]
		
		def DefineRack(name: String, grid: Int, site: Int, xsize: Int, ysize: Int, nVolumeMax: Double, carrierType: String = "") {
			racks += Rack(name, xsize, ysize, grid, site, nVolumeMax, carrierType)
		}
	}
	
	val map = Map[String, Table]("TABLE_DNE", t1)
	
	val t1 = new Table(
"""302F0FE9
20110530_155010 No log in       
                                                                                                                                
No user logged in                                                                                                               
--{ RES }--
V;200
--{ CFG }--
999;219;32;
14;-1;239;240;130;241;-1;-1;52;-1;242;249;-1;-1;-1;-1;-1;250;243;-1;-1;-1;-1;-1;-1;244;-1;-1;-1;-1;-1;-1;-1;-1;35;-1;-1;-1;-1;-1;-1;234;-1;-1;-1;-1;-1;-1;235;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;246;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;
998;0;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;3;Trough 100ml;Trough 100ml;Trough 100ml;
998;Labware7;Labware8;Decon;
998;2;Reagent Cooled 8*15ml;Reagent Cooled 8*50ml;
998;Labware5;Labware6;
998;0;
998;0;
998;1;Trough 1000ml;
998;Labware10;
998;0;
998;1;;
998;;
998;2;;;
998;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;;
998;;
998;2;D-BSSE 96 Well PCR Plate;D-BSSE 96 Well PCR Plate;
998;Labware14;Labware15;
998;0;
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
998;0;
998;0;
998;0;
998;9;;;;;;;MTP Waste;;;
998;;;;;;;Waste;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;D-BSSE 96 Well PCR Plate;
998;Labware12;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;D-BSSE 96 Well PCR Plate;
998;Labware13;
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
998;1;;
998;;
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
998;5;
998;245;11;
998;93;55;
998;252;54;
998;251;59;
998;253;64;
998;6;
998;4;0;System;
998;0;0;Shelf 32Pos Microplate;
998;0;4;Hotel 5Pos SPE;
998;0;1;Hotel 3Pos DWP;
998;0;2;Hotel 4Pos DWP 1;
998;0;3;Hotel 4Pos DWP 2;
998;0;
998;1;
998;11;
998;55;
998;54;
998;59;
998;64;
996;0;0;
--{ RPG }--""",
new X {
		DefineRack("WASTE",1,6,8,12,0) ;
		DefineRack("CSL",2,0,1,8, 5000000,"Carousel MTP") ;
		DefineRack("TS4",15,0,1,16, 5000000) ;
		DefineRack("TS5",16,0,1,16, 5000000) ;
		//DefineRack("TR1",14,0,1,8, 5000000) ;
		DefineRack("TR2",14,1,1,8, 5000000) ;
		DefineRack("TR3",14,2,1,8, 5000000) ;
		DefineRack("TR4",17,0,1,8, 5000000) ;
		DefineRack("TR5",17,1,1,8, 5000000) ;
		DefineRack("TR6",17,2,1,8, 5000000) ;
		DefineRack("TR7",18,0,1,8, 5000000) ;
		DefineRack("TR8",18,1,1,8, 5000000) ;
		DefineRack("TR9",18,2,1,8, 5000000) ;
		//DefineRack("T1",21,0,1,16, 2100) ;
		//DefineRack("T2",20,0,1,16, 2100) ;
		//DefineRack("T3",19,0,1,16, 2100) ;
		DefineRack("E3",22,0,1,16, 2100) ;
		//DefineRack("P1",23,0,12,8, 200,"MP 3Pos Fixed") ;
		DefineRack("P2",23,1,12,8, 1200,"MP 3Pos Fixed") ;
		DefineRack("T2",23,1,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF2",23,1,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M2",23,1,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P3",23,2,12,8, 1200,"MP 3Pos Fixed") ;
		DefineRack("T3",23,2,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF3",23,2,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M3",23,2,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P4",29,0,12,8, 1200,"MP 3Pos Fixed") ;
		DefineRack("T4",29,0,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF4",29,0,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M4",29,0,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P5",29,1,12,8, 200,"MP 3Pos Fixed") ;
		DefineRack("T5",29,1,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF5",29,1,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M5",29,1,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P6",29,2,12,8, 200,"MP 3Pos Fixed") ;
		DefineRack("T6",29,2,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF6",29,2,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M6",29,2,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P7",35,0,12,8, 200,"MP 3Pos Fixed PCR") ;
		DefineRack("P8",35,1,12,8, 200,"MP 3Pos Fixed PCR") ;
		DefineRack("P9",35,2,12,8, 200,"MP 3Pos Fixed PCR") ;
		DefineRack("P10",41,0,12,8, 200,"MP 3Pos Fixed 2+clips") ;
		DefineRack("T10",41,0,6,4, 2100,"MP 3Pos Fixed 2+clips") ;
		DefineRack("BUF10",41,0,6,8, 2100,"MP 3Pos Fixed 2+clips") ;
		DefineRack("M10",41,0,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P11",41,1,12,8, 1200,"MP 3Pos Fixed 2+clips") ;
		DefineRack("T11",41,1,6,4, 2100,"MP 3Pos Fixed 2+clips") ;
		DefineRack("BUF11",41,1,6,8, 2100,"MP 3Pos Fixed 2+clips") ;
		DefineRack("M11",41,1,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P12",41,2,12,8, 200,"MP 3Pos Fixed 2+clips") ;
		DefineRack("T12",41,2,6,4, 2100,"MP 3Pos Fixed 2+clips") ;
		DefineRack("BUF12",41,2,6,8, 2100,"MP 3Pos Fixed 2+clips") ;
		DefineRack("M12",41,2,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P13",47,0,12,8, 200,"MP 3Pos Fixed") ;
		DefineRack("T13",47,0,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF13",47,0,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M13",47,0,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P14",47,1,12,8, 1200,"MP 3Pos Fixed") ;
		DefineRack("T14",47,1,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF14",47,1,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M14",47,1,24,16,1200,"MP 3Pos Fixed") ;
		DefineRack("P15",47,2,12,8, 200,"MP 3Pos Fixed") ;
		DefineRack("T15",47,2,6,4, 2100,"MP 3Pos Fixed") ;
		DefineRack("BUF15",47,2,6,8, 2100,"MP 3Pos Fixed") ;
		DefineRack("M15",47,2,24,16,1200,"MP 3Pos Fixed") ;
		//DefineRack("E10",41,0,6,4, 2100) ;
		//DefineRack("E2",47,1,6,4, 2100) ;
		//DefineRack("TR1",47,2,6,8, 2100) ;
		DefineRack("TR1",41,2,6,8, 2100) ;
		DefineRack("LNK",65,0,12,8, 200,"Te-Link") ;
		DefineRack("S1",53,0,12,8, 200,"Te-Shake 2Pos") ;
		DefineRack("S2",53,1,12,8, 1200,"Te-Shake 2Pos") ;
		DefineRack("MS1",53,0,24,16, 200,"Te-Shake 2Pos") ;
		DefineRack("MS2",53,1,24,16, 1200,"Te-Shake 2Pos") ;
		DefineRack("TP1",59,0,12,8, 200,"Torrey pines") ;
		DefineRack("TP2",59,1,12,8, 1200,"Torrey pines") ;
		//DefineRack("T1",41,0,6,4, 2100) ;
		//DefineRack("T2",59,1,6,4, 2100) ;
		DefineRack("HA1",4,0,12,8, 200,"HOTEL5A") ;
		DefineRack("HA2",4,1,12,8, 200,"HOTEL5A") ;
		DefineRack("HA3",4,2,12,8, 200,"HOTEL5A") ;
		DefineRack("HA4",4,3,12,8, 200,"HOTEL5A") ;
		DefineRack("HA5",4,4,12,8, 200,"HOTEL5A") ;
		DefineRack("HB1",10,0,12,8, 200,"HOTEL5A") ;
		DefineRack("HB2",10,1,12,8, 200,"HOTEL5A") ;
		DefineRack("HB3",10,2,12,8, 200,"HOTEL5A") ;
		DefineRack("HB4",10,3,12,8, 200,"HOTEL5A") ;
		DefineRack("HB5",10,4,12,8, 200,"HOTEL5A") ;
		DefineRack("HC1",66,0,12,8, 200,"HOTEL5B") ;
		DefineRack("HC2",66,1,12,8, 200,"HOTEL5B") ;
		DefineRack("HC3",66,2,12,8, 200,"HOTEL5B") ;
		DefineRack("HC4",66,3,12,8, 200,"HOTEL5B") ;
		DefineRack("HC5",66,4,12,8, 200,"HOTEL5B") ;
		DefineRack("RCH",9,0,12,8, 200,"ROCHE") ;
		DefineRack("READER",48,0,12,8, 200,"PLATE_READER") ;
}.racks.toSeq)

}