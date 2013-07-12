package roboliq.entities

object TestMain extends App {
	def gid: String = java.util.UUID.randomUUID().toString()
	val user = Agent(gid)
	val r1 = Agent(gid)
	val userArm = Transporter(gid)
	val r1arm = Transporter(gid)
	val pipetter = Pipetter(gid)
	val sealer = Sealer(gid)
	val shaker = Shaker(gid)
	val thermocycler = Thermocycler(gid)
	val siteModelAll = SiteModel(gid)
	val siteModel1 = SiteModel(gid)
	val siteModel12 = SiteModel(gid)
	val offsite = Site(gid)
	val s1 = Site(gid)
	val s2 = Site(gid)
	val sealerSite = Site(gid)
	val shakerSite = Site(gid)
	val thermocyclerSite = Site(gid)
	val m1 = PlateModel(gid)
	val m2 = PlateModel(gid)
	val shakerSpec1 = ShakerSpec(gid)
	val thermocyclerSpec1 = ThermocyclerSpec(gid)
	
	val p1 = Plate(gid)
	val p2 = Plate(gid)
	
	val entities = List[(String, Entity)](
		"user" -> user,
		"r1" -> r1,
		"userArm" -> userArm,
		"r1Arm" -> r1arm,
		"pipetter" -> pipetter,
		"sealer" -> sealer,
		"shaker" -> shaker,
		"thermocycler" -> thermocycler,
		"siteModelAll" -> siteModelAll,
		"siteModel1" -> siteModel1,
		"siteModel12" -> siteModel12,
		"offsite" -> offsite,
		"s1" -> s1,
		"s2" -> s2,
		"sealerSite" -> sealerSite,
		"shakerSite" -> shakerSite,
		"thermocyclerSite" -> thermocyclerSite,
		"m1" -> m1,
		"m2" -> m2,
		"shakerSpec1" -> shakerSpec1,
		"thermocyclerSpec1" -> thermocyclerSpec1,
		
		"p1" -> p1,
		"p2" -> p2
	)
	val names: Map[Entity, String] = entities.map(pair => pair._2 -> pair._1).toMap
	val agentToDevices_m = Map[Agent, List[Device]](
		user -> List(userArm),
		r1 -> List(r1arm, pipetter, sealer, shaker, thermocycler)
	)
	// LabwareModels that devices can use
	val deviceToModels_m = Map[Device, List[LabwareModel]](
		userArm -> List(m1, m2),
		r1arm -> List(m1, m2),
		pipetter -> List(m1, m2),
		sealer -> List(m1),
		shaker -> List(m1, m2),
		thermocycler -> List(m1)
	)
	// Sites that devices can access
	val deviceToSites_m = Map[Device, List[Site]](
		userArm -> List(offsite, s1),
		r1arm -> List(s1, s2, sealerSite, thermocyclerSite),
		pipetter -> List(s1, s2),
		sealer -> List(sealerSite),
		shaker -> List(shakerSite),
		thermocycler -> List(thermocyclerSite)
	)
	// Specs that devices accept
	val deviceToSpecs_m = Map[Device, List[Entity]](
		shaker -> List(shakerSpec1),
		thermocycler -> List(thermocyclerSpec1)
	)
	// Models which another model can have stacked on top of it
	val stackables_m = Map[LabwareModel, List[LabwareModel]](
		siteModelAll -> List(m1, m2),
		siteModel1 -> List(m1),
		siteModel12 -> List(m1, m2)
	)
	// Each labware's model
	val labwareToModel_m = Map[Labware, LabwareModel](
		offsite -> siteModelAll,
		s1 -> siteModel12,
		s2 -> siteModel12,
		sealerSite -> siteModel1,
		shakerSite -> siteModel12,
		thermocyclerSite -> siteModel1,
		p1 -> m1,
		p2 -> m2
	)
	val labwareToLocation_m = Map[Labware, Entity](
		p1 -> offsite,
		p2 -> offsite
	)
	
	def makeInitialConditionsList(): List[Rel] = {
		entities.map(pair => Rel(s"is-${pair._2.typeName}", List(pair._1))) ++
		agentToDevices_m.flatMap(pair => pair._2.map(device => {
			Rel(s"agent-has-device", List(names(pair._1), names(device)))
		})) ++
		deviceToModels_m.flatMap(pair => pair._2.map(model => {
			Rel(s"device-can-model", List(names(pair._1), names(model)))
		})) ++
		deviceToSites_m.flatMap(pair => pair._2.map(site => {
			Rel(s"device-can-site", List(names(pair._1), names(site)))
		})) ++
		deviceToSpecs_m.flatMap(pair => pair._2.map(spec => {
			Rel(s"device-can-spec", List(names(pair._1), names(spec)))
		})) ++
		stackables_m.flatMap(pair => pair._2.map(model => {
			Rel(s"stackable", List(names(pair._1), names(model)))
		})) ++
		labwareToModel_m.map(pair => Rel(s"stackable", List(names(pair._1), names(pair._2)))) ++
		labwareToLocation_m.map(pair => Rel(s"location", List(names(pair._1), names(pair._2))))
	}
	
	def makeInitialConditions(): String = {
		val l: List[Rel] = makeInitialConditionsList
		val l2: List[String] =
			" ; initial conditions" ::
			" (" ::
			l.map(r => "  " + r) ++
			List(" )")
		l2.mkString("\n")
	}
	
	println(makeInitialConditions)
}
