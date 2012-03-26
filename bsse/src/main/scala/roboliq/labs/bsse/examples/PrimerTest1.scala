package roboliq.labs.bsse.examples

import java.text.DecimalFormat

import roboliq.common
import roboliq.protocol._
import roboliq.protocol.commands._
//import LiquidAmountImplicits._


class PrimerTest1 {
	private val df = new DecimalFormat("00")
	
	private def combine(sTemplateKey: String, s1a: Int, s1b: Int, s2a: Int, s2b: Int): List[PcrProduct] = {
		val lProduct = (for (i1 <- s1a to s1b; i2 <- s2a to s2b) yield {
			val product = new PcrProduct
			product.template := new TempKeyA(sTemplateKey)
			product.forwardPrimer := new TempKeyA("SEQUENCE_"+df.format(i1+1))
			product.backwardPrimer := new TempKeyA("SEQUENCE_"+df.format(i2+1))
			product
		}).toList
		lProduct
	}
	
	def run() {
		val lProduct = 
			combine("pAct1", 0, 6, 7, 16) ++
			combine("lacI-NLS-STOP", 17, 28, 29, 36) ++
			combine("cyc1Term", 37, 44, 45, 48)
		lProduct.foreach(println)
	}
}