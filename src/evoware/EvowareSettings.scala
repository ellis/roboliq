package evoware

sealed class EvowareSettings {
	sealed case class Loc(iGrid: Int, iSite: Int)
	
	val locations = Map[Int, Loc](
		15 -> Loc(17, 0),
		16 -> Loc(17, 1)
	)
}