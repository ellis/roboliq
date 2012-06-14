package roboliq.planner

import scala.collection.mutable.LinkedHashSet
import org.ejml.simple.SimpleMatrix
import roboliq.core._
import scala.collection.immutable.BitSet


/**
 * @param dst_i index of destination well
 * @param src_li list of indexes of source wells which may be used
 * @param vol_m map of source index to volume used of that source
 */
case class DestX(
	dst_i: Int,
	src_li: List[Int],
	vol_m: Map[Int, Double]
)

case class Step(
	src_n: Int,
	dst_n: Int,
	temp_m: Map[Int, Map[Int, Double]],
	dst_l: List[DestX]
)


case class Combo(ℓχsrc: List[Int], ℓp: List[Double])
case class ComboInfo(combo: Combo, ℓχdst: List[Int]) {
	val n = ℓχdst.length
	override def toString = {
		List(
				"srcs: "+combo.ℓχsrc.mkString("[", ",", "]"),
				"p: "+combo.ℓp.mkString("[", ",", "]"),
				"dsts: "+ℓχdst.mkString("[", ",", "]")
		).mkString("ComboInfo: {", ", ", "}")
	}
}
object Combo {
	def add(ci: ComboInfo, χsrc: Int, ℓmixture: List[List[Double]]): List[ComboInfo] = {
		val ℓχsrc = χsrc :: ci.combo.ℓχsrc
		//println("ℓχsrc: "+ℓχsrc)
		
		// Get the proportion of src to prior combo for each destination
		val ℓp1 = ci.ℓχdst map { χdst =>
			val ℓvol = ℓmixture(χdst)
			// Volume of initial combo
			val vol0 = (ci.combo.ℓχsrc map ℓvol).foldLeft(0.0)(_ + _)
			// New src volume
			val vol1 = ℓvol(χsrc)
			// Proportion of new src vol compared to prior combo 
			val p1 =
				if (vol1 <= 0) 0
				else if (vol0 > 0) vol1 / vol0
				else 1
			(χdst, p1)
		} filter (_._2 > 0) sortBy (_._2)
		//println("ℓp1: "+ℓp1)
		
		// Group approximately equal proportions together
		def group(ℓp1: List[Tuple2[Int, Double]], accR: List[List[Tuple2[Int, Double]]]): List[List[Tuple2[Int, Double]]] = {
			ℓp1 match {
				case Nil => accR.reverse
				case p1 :: rest =>
					accR match {
						case Nil =>
							group(rest, List(List(p1)))
						case ℓ0 :: accRest =>
							val d: Double = math.abs((p1._2 - ℓ0.head._2) / ℓ0.head._2)
							if (d < 0.05)
								group(rest, (p1 :: ℓ0) :: accRest)
							else
								group(rest, List(p1) :: ℓ0 :: accRest)
					}
			}
		}
		// Only keep the groups with size >= 2
		val ℓℓp1 = group(ℓp1, Nil) filter (!_.tail.isEmpty)
		//println("ℓℓp1: "+ℓℓp1)
		
		ℓℓp1 map { ℓp1 =>
			val ℓχdst = ℓp1 map (_._1)
			val p = (ℓp1 map (_._2) reduce (_ + _)) / ℓp1.size
			val ℓp = p :: ci.combo.ℓp
			ComboInfo(Combo(ℓχsrc, ℓp), ℓχdst)
		}
	}

	def add(ci: ComboInfo, χsrc: Int, step: Step): List[ComboInfo] = {
		val ℓχsrc = χsrc :: ci.combo.ℓχsrc
		//println("ℓχsrc: "+ℓχsrc)
		
		// Get the proportion of src to prior combo for each destination
		val ℓp1 = ci.ℓχdst map { χdst =>
			val dst = step.dst_l(χdst)
			val vol_m = dst.vol_m
			// Volume of initial combo
			val vol0 = (ci.combo.ℓχsrc map vol_m).foldLeft(0.0)(_ + _)
			// New src volume
			val vol1 = vol_m.getOrElse(χsrc, 0.0)
			// Proportion of new src vol compared to prior combo 
			val p1 =
				if (vol1 <= 0) 0
				else if (vol0 > 0) vol1 / vol0
				else 1
			(χdst, p1)
		} filter (_._2 > 0) sortBy (_._2)
		//println("ℓp1: "+ℓp1)
		
		// Group approximately equal proportions together
		def group(ℓp1: List[Tuple2[Int, Double]], accR: List[List[Tuple2[Int, Double]]]): List[List[Tuple2[Int, Double]]] = {
			ℓp1 match {
				case Nil => accR.reverse
				case p1 :: rest =>
					accR match {
						case Nil =>
							group(rest, List(List(p1)))
						case ℓ0 :: accRest =>
							val d: Double = math.abs((p1._2 - ℓ0.head._2) / ℓ0.head._2)
							if (d < 0.05)
								group(rest, (p1 :: ℓ0) :: accRest)
							else
								group(rest, List(p1) :: ℓ0 :: accRest)
					}
			}
		}
		// Only keep the groups with size >= 2
		val ℓℓp1 = group(ℓp1, Nil) filter (!_.tail.isEmpty)
		//println("ℓℓp1: "+ℓℓp1)
		
		ℓℓp1 map { ℓp1 =>
			val ℓχdst = ℓp1 map (_._1)
			val p = (ℓp1 map (_._2) reduce (_ + _)) / ℓp1.size
			val ℓp = p :: ci.combo.ℓp
			ComboInfo(Combo(ℓχsrc, ℓp), ℓχdst)
		}
	}
}

class LiquidPlanner {
	/**
	 * For each destination, calculate the volume needed from each source
	 */
	def calcMixture(src_l: List[VesselContent], dst_l: List[VesselContent]): List[List[Double]] = {
		// Get list of solvents and solutes
		val ℓsolvent = Set(dst_l.flatMap(_.mapSolventToVolume.keys) : _*).toList
		val ℓsolute = Set(dst_l.flatMap(_.mapSoluteToMol.keys) : _*).toList
		
		// Get mixtures of sources used to prepare the destination wells
		dst_l.map(dst => dstToSrcVolumes(ℓsolvent, ℓsolute, dst, src_l).map(_.ul.toDouble))
	}
	
	/**
	 * Turn the mixture into a list of bitsets, where any positive entry in the mixture
	 * corresponds to a 1 in the bitset.
	 */
	def calcBitset(mixture_l: List[List[Double]]): List[BitSet] = {
		mixture_l map { vol_l => 
			val i_l = vol_l.zipWithIndex filter (_._1 > 0) map (_._2)
			BitSet(i_l : _*)
		}
	}

	def createStep0(mixture_l: List[List[Double]]): Step = {
		val src_n = mixture_l.head.size
		val dst_n = mixture_l.size
		val src_li = (0 until src_n).toList
		val x_l = mixture_l.zipWithIndex map { pair =>
			val volToSrc = pair._1.zipWithIndex filter (_._1 > 0)
			DestX(pair._2, volToSrc map (_._2), volToSrc map (_.swap) toMap)
		}
		Step(src_n, dst_n, Map(), x_l)
	}
	
	def createStep0(src_l: List[VesselContent], dst_l: List[VesselContent]): Step = {
		val mixture_l = calcMixture(src_l, dst_l)
		createStep0(mixture_l)
	}

	def advance(step: Step): Option[Step] = {
		val dst_n = step.dst_l.size
		val ci0 = ComboInfo(Combo(Nil, Nil), (0 until dst_n).toList)
		val src_li = (0 until step.src_n).toList
		
		def add(ci: ComboInfo, step: Step)(χsrc: Int): List[ComboInfo] = {
			Combo.add(ci, χsrc, step)
		}
		
		val combo1_lº = src_li flatMap add(ci0, step)
		val combo1_l = combo1_lº sortBy (_.n)
		val combo2_lº = combo1_l flatMap { combo =>
			val src0_i = combo.combo.ℓχsrc.head + 1
			(src0_i until step.src_n) flatMap add(combo, step)
		}
		val combo2_l = combo2_lº sortBy (_.n)
		combo1_l foreach println
		combo2_l foreach println
		combo2_l match {
			case Nil => None
			case combo :: _ =>
				val dst_l = combo.ℓχdst
				Some(step.copy(
					src_n = step.src_n + 1,
					temp_m = step.temp_m.updated(step.src_n, combo.combo.ℓχsrc.map(_ -> 1.0).toMap)
				))
		}
	}
	
	/*def countSourceFrequency(dst_l: List[VesselContent], mixture_l: List[List[Double]]): List[List[Int]] = {
		mixture_l.to
	}*/
	
	def run(src_l: List[VesselContent], dst_l: List[VesselContent]) {
		// Get list of solvents and solutes
		val ℓsolvent = Set(dst_l.flatMap(_.mapSolventToVolume.keys) : _*).toList
		val ℓsolute = Set(dst_l.flatMap(_.mapSoluteToMol.keys) : _*).toList
		
		// Get mixtures of sources used to prepare the destination wells
		val ℓmixture = dst_l.map(dst => dstToSrcVolumes(ℓsolvent, ℓsolute, dst, src_l).map(_.ul.toDouble))
		val X = new SimpleMatrix(src_l.size, dst_l.size, false, ℓmixture.flatten : _*)
		println(X)
		
		// Find number of wells that each source is in
		def volsToOnes(ℓvol: List[Double]): List[Int] = ℓvol.map(vol => if (vol > 0) 1 else 0)
		val ℓcount = ℓmixture.tail.foldLeft(volsToOnes(ℓmixture.head)) { (ℓn, mixture) =>
			(ℓn, volsToOnes(mixture)).zipped.map(_ + _)
		}
		println("ℓcount: "+ℓcount)
		
		val ci0 = ComboInfo(Combo(Nil, Nil), (0 until dst_l.size).toList)
		val x10 = Combo.add(ci0, 0, ℓmixture)
		println("x10: "+x10)
		val x20 = Combo.add(x10.head, 1, ℓmixture)
		println("x20: "+x20)
		val x30 = Combo.add(x20.head, 2, ℓmixture)
		println("x30: "+x30)
		val x40 = Combo.add(x30.head, 3, ℓmixture)
		println("x40: "+x40)
		
		x(src_l, dst_l.size, ℓmixture)
		/*
		val nSrc = src_l.size
		for (i <- 0 until (nSrc - 1); j <- i + 1 until nSrc) {
			
		}
		*/
		
	}
	
	private def x(src_l: List[VesselContent], dst_n: Int, ℓmixture: List[List[Double]]): List[ComboInfo] = {
		val ci0 = ComboInfo(Combo(Nil, Nil), (0 until dst_n).toList)
		val src_li = (0 until src_l.size).toList
		
		def add(ci: ComboInfo, src_l: List[VesselContent], ℓmixture: List[List[Double]])(χsrc: Int): List[ComboInfo] = {
			Combo.add(ci, χsrc, ℓmixture)
		}
		
		val combo1_l = src_li flatMap add(ci0, src_l, ℓmixture)
		combo1_l foreach println
		Nil
	}
	
	private def dstToSrcVolumes(
		ℓsolvent: List[SubstanceLiquid],
		ℓsolute: List[Substance],
		dst: VesselContent,
		src_l: List[VesselContent]
	): List[LiquidVolume] = {
		val b = dstToVector(ℓsolvent, ℓsolute, dst)
		val A = srcsToMatrix(ℓsolvent, ℓsolute, dst, src_l)
		//println("b:")
		//println(b)
		//println("A:")
		//println(A)
		val x = A.solve(b)
		println(x)
		x.getMatrix().getData().toList.map(n => LiquidVolume.ul(BigDecimal(n)))
	}
	
	private def dstToVector(
		ℓsolvent: List[SubstanceLiquid],
		ℓsolute: List[Substance],
		dst: VesselContent
	): SimpleMatrix = {
		val bº = 
			(ℓsolvent map { sub => (dst.mapSolventToVolume.getOrElse(sub, LiquidVolume.empty).ul).toDouble }) ++
			(ℓsolute map { sub => dst.mapSoluteToMol.getOrElse(sub, BigDecimal(0.0)).toDouble })
		new SimpleMatrix(bº.size, 1, false, bº : _*)
	}
	
	/**
	 * Divide solvent volumes by total src volume
	 * Divide solute concentrations by dst volume in ul
	 * In this way, we get the correct volumes and concentrations per ul relative to the destination well 
	 */
	private def srcsToMatrix(
		ℓsolvent: List[SubstanceLiquid],
		ℓsolute: List[Substance],
		dst: VesselContent,
		src_l: List[VesselContent]
	): SimpleMatrix = {
		val aº = src_l flatMap { src =>
			(ℓsolvent map { sub => (src.mapSolventToVolume.getOrElse(sub, LiquidVolume.empty).ul / src.volume.ul).toDouble }) ++
			(ℓsolute map { sub => src.mapSoluteToMol.getOrElse(sub, BigDecimal(0.0)).toDouble / dst.volume.ul.toDouble })
		}
		new SimpleMatrix(ℓsolvent.size + ℓsolute.size, src_l.size, false, aº : _*)
	}
	
	private def getVolume(dst: VesselContent, src: VesselContent): LiquidVolume = {
		
		val ℓvolSolute = src.mapSoluteToMol map { pair => 
			val (solute, srcMol) = pair
			dst.mapSoluteToMol.get(solute) match {
				case None => return LiquidVolume.empty
				case Some(dstMol) => dst.volume * dstMol / srcMol
			}
		} toList
		
		val ℓvolSolvent = src.mapSolventToVolume map { pair => 
			val (solvent, srcVol) = pair
			dst.mapSolventToVolume.get(solvent) match {
				case None => return LiquidVolume.empty
				case Some(dstVol) =>
					if (dstVol <= srcVol)
						dstVol
					else
						return LiquidVolume.empty
			}
		} toList
		
		val vol = (ℓvolSolute ++ ℓvolSolvent).map(_.nl).min
		LiquidVolume.nl(vol)
	}
}