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
	val offsite = Site(gid)
	val s1 = Site(gid)
	val s2 = Site(gid)
	val sealerSite = Site(gid)
	val thermocyclerSite = Site(gid)
	val m1 = LabwareModel(gid)
	val m2 = LabwareModel(gid)
	val shakerSpec1 = ShakerSpec(gid)
	val thermocyclerSpec1 = ThermocyclerSpec(gid)
	
	val entities = List[(String, Entity)](
		"user" -> user,
		"r1" -> r1,
		"userArm" -> userArm,
		"r1Arm" -> r1arm,
		"pipetter" -> pipetter,
		"sealer" -> sealer,
		"shaker" -> shaker,
		"thermocycler" -> thermocycler,
		"offsite" -> offsite
	)
}
