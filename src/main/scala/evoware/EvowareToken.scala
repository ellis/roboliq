package evoware

sealed abstract class T0_Token

case class T0_Error(val sMessage: String) extends T0_Token

case class T0_Spirate(
	val sFunc: String,
	val mTips: Int,
	val sLiquidClass: String,
	val asVolumes: Seq[String],
	val iGrid: Int,
	val iSite: Int,
	val sPlateMask: String
) extends T0_Token {
	def toString = {
		(
			sFunc+"("+
			"%d,\"%s\","+
			"%s,"+
			"%d,%d,"+
			"1,"+
			"\"%s\","+
			"0,0);"
		).format(
			mTips, sLiquidClass,
			asVolumes.mkString(","),
			iGrid, iSite,
			sPlateMask
		)
	}
}
