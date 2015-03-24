package roboliq.entities

sealed trait PipetteAmount
case class PipetteAmount_Volume(volume: LiquidVolume) extends PipetteAmount
case class PipetteAmount_Dilution(num: BigDecimal, den: BigDecimal) extends PipetteAmount
//case class PipetteAmount_Concentration(conc: BigDecimal) extends PipetteAmount
