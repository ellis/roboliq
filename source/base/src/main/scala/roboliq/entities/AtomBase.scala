package roboliq.entities

import roboliq.ai.plan.Strips.Atom

case class AtomBase(
	val atom_l: Set[Atom]
) {
	def add(atom: Atom): AtomBase =
		copy(atom_l = atom_l + atom)
	
	def add(ss: String*): AtomBase =
		copy(atom_l = atom_l + Atom(ss : _*))
		
	def query(name: Option[String], params: List[Option[String]]): Set[Atom] = {
		atom_l.filter { atom =>
			val nameOk = name.map(_ == atom.name).getOrElse(true)
			val paramsOk = (atom.params zip params).flatMap({
				case (x1, Some(x2)) => Some(x1 == x2)
				case _ => None
			}).forall(_ == true)
			nameOk && paramsOk
		}
	}
}