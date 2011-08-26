package roboliq.commands.pipette

import roboliq.common._


case class L4C_Pipette(args: L4A_PipetteArgs) extends CommandL4 {
	type L3Type = L3C_Pipette

	def addKnowledge(kb: KnowledgeBase) {
		for (item <- args.items) {
			item.src match {
				case WPL_Well(o) => kb.addWell(o, true)
				case WPL_Plate(o) => kb.addPlate(o, true)
				case WPL_Liquid(o) => kb.addLiquid(o)
			}
			item.dest match {
				case WP_Well(o) => kb.addWell(o, false)
				case WP_Plate(o) => kb.addPlate(o, false)
			}
			(item.src, item.dest) match {
				case (WPL_Plate(plate1), WP_Plate(plate2)) =>
					val setup1 = kb.getPlateSetup(plate1)
					val setup2 = kb.getPlateSetup(plate2)
					(setup1.dim_?, setup2.dim_?) match {
						case (Some(dim1), None) =>
							new PlateProxy(kb, plate2).setDimension(dim1.nRows, dim1.nCols)
						case (None, Some(dim2)) =>
							new PlateProxy(kb, plate1).setDimension(dim2.nRows, dim2.nCols)
						case _ =>
					}
				case _ =>
			}
		}
	}
	
	def toL3(states: RobotState): Either[Seq[String], L3Type] = {
		args.toL3(states) match {
			case Left(lsErrors) => Left(lsErrors)
			case Right(args3) => Right(new L3C_Pipette(args3))
		}
	}
}

case class L3C_Pipette(args: L3A_PipetteArgs) extends CommandL3

case class L4A_PipetteArgs(
	val items: Seq[L4A_PipetteItem],
	val mixSpec_? : Option[MixSpec] = None,
	val sAspirateClass_? : Option[String] = None,
	val sDispenseClass_? : Option[String] = None,
	val sMixClass_? : Option[String] = None,
	val sTipKind_? : Option[String] = None,
	val fnClean_? : Option[Unit => Unit] = None
) {
	def toL3(states: RobotState): Either[Seq[String], L3A_PipetteArgs] = {
		val items3_? = items.map(_.toL3(states))
		if (items3_?.exists(_.isLeft)) {
			val lsErrors = items3_?.filter(_.isLeft).flatMap(_.left.get)
			return Left(lsErrors)
		}
		
		val items3 = items3_?.flatMap(_.right.get)
		Right(new L3A_PipetteArgs(
			items3,
			mixSpec_? = mixSpec_?,
			sAspirateClass_? = sAspirateClass_?,
			sDispenseClass_? = sDispenseClass_?,
			sMixClass_? = sMixClass_?,
			sTipKind_? = sTipKind_?,
			fnClean_? = fnClean_?
		))
	}
}

class L3A_PipetteArgs(
	val items: Seq[L3A_PipetteItem],
	val mixSpec_? : Option[MixSpec] = None,
	val sAspirateClass_? : Option[String] = None,
	val sDispenseClass_? : Option[String] = None,
	val sMixClass_? : Option[String] = None,
	val sTipKind_? : Option[String] = None,
	val fnClean_? : Option[Unit => Unit] = None
)

case class L4A_PipetteItem(
	val src: WellOrPlateOrLiquid,
	val dest: WellOrPlate,
	val nVolume: Double
) {
	def toL3(states: RobotState): Either[Seq[String], Seq[L3A_PipetteItem]] = {
		val srcs3 = PipetteHelperL4.getWells1(states, src)
		if (srcs3.isEmpty) {
			return Left(Seq("INTERNAL: no config found for pipette source "+src))
		}
		val dests3 = PipetteHelperL4.getWells1(states, dest)
		//println("dests3: "+dests3)
		def createItemsL3() = Right(dests3.map(dest3 => new L3A_PipetteItem(srcs3, dest3, nVolume)).toSeq)
						
		src match {
			case WPL_Well(_) =>
				createItemsL3()
			case WPL_Plate(plate1) =>
				dest match {
					case WP_Well(_) =>
						Left(Seq("when source is a plate, destination must also be a plate"))
					case WP_Plate(plate2) =>
						if (plate1.state(states).conf.nWells != plate1.state(states).conf.nWells)
							Left(Seq("source and destination plates must have the same number of wells"))
						else {
							val items = (srcs3.toSeq zip dests3.toSeq).map(pair => new L3A_PipetteItem(Set(pair._1), pair._2, nVolume))
							Right(items)
						}
				}
			case WPL_Liquid(liquid1) =>
				createItemsL3()
		}
	}
}

case class L3A_PipetteItem(
		val srcs: Set[WellConfigL2],
		val dest: WellConfigL2,
		val nVolume: Double
		)
