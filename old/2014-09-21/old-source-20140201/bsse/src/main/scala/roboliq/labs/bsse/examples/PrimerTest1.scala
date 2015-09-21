/*package roboliq.labs.bsse.examples

import java.text.DecimalFormat

import roboliq.commands.pcr.PcrProductBean

class PrimerTest1 {
	println("A")
	private val df = new DecimalFormat("00")
	
	val idPlatePrimer = "E2215"
	
	println("B")
	private def combine(sPart: String, template: String, s1a: Int, s1b: Int, s2a: Int, s2b: Int): List[PcrProductBean] = {
		println("C")
		val lProduct = (for (i1 <- s1a to s1b; i2 <- s2a to s2b) yield {
			val product = new PcrProductBean
			product.template = template
			product.forwardPrimer = "SEQUENCE_"+df.format(i1+1)
			product.backwardPrimer = "SEQUENCE_"+df.format(i2+1)
			product
		}).toList
		lProduct
	}
	
	def run() {
		val lProduct: List[PcrProductBean] = 
			combine("pAct1",         "FRP572", 0, 6, 7, 16) ++
			combine("lacI-NLS-STOP", "FRP128", 17, 28, 29, 36) ++
			combine("cyc1Term",      "FRP572", 37, 44, 45, 48)
		lProduct.foreach(println)
		
		val lProduct96 = lProduct.take(96)
		val lSrc1 = lProduct96.map(_.template)
		val lSrc2 = lProduct96.map(_.forwardPrimer)
		val lSrc3 = lProduct96.map(_.backwardPrimer)
		println("  src: "+lSrc1.mkString(","))
		println("  src: "+lSrc2.mkString(","))
		println("  src: "+lSrc3.mkString(","))
	}
}
*/