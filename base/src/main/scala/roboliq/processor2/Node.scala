package roboliq.processor2

import spray.json.JsValue
import spray.json.JsObject

/**
 * There are several basic types of nodes.
 * 
 * Inner nodes in the computation hierarchy:
 * 
 * Command node: this node represents a JsObject command.  The object's field "cmd" is
 * read, and we lookup a handler for the cmd.  If one is found, the ComputationResult
 * is associated with the command node.
 * 
 * Computation node: child of a command node or a computation node.  Inputs are a list of
 * IdClasses.  Outputs a ComputationResult.
 * 
 * Leaf nodes in the computation hierarchy:
 * 
 * Token node: represents a Token.  Is a leaf node in the computation hierarchy.
 * 
 * Event node: ...
 * 
 * Items in the conversion queue:
 * 
 * Conversion: Input is a list of IdClasses.  Outputs another Conversion or an Object.
 * 
 */

sealed trait Node

trait HasComputationHierarchy {
	val parent: Node with HasComputationHierarchy
	val index: Int
	val idCmd: List[Int]
	
	lazy val id_r = getId_r
	lazy val id = id_r.reverse
	
	private def getId: List[Int] = {
		getId_r.reverse
	}
	
	private def getId_r: List[Int] = {
		if (parent == null)
			Nil
		else
			index :: parent.getId
	}
}

trait Node_Computes extends Node {
	val input_l: List[IdClass]
}

case class Node_Command(
	parent: Node with HasComputationHierarchy,
	index: Int,
	cmd: JsObject
) extends Node_Computes with HasComputationHierarchy {
	val input_l: List[IdClass] = Nil
	val idCmd: List[Int] = id
}

case class Node_Computation(
	parent: Node with HasComputationHierarchy,
	index: Int,
	input_l: List[IdClass],
	fn: (List[Object]) => ComputationResult,
	idCmd: List[Int]
) extends Node_Computes with HasComputationHierarchy

case class Node_Token(
	parent: Node with HasComputationHierarchy,
	index: Int,
	token: Token
) extends Node with HasComputationHierarchy {
	val idCmd = Nil
}

/*
class Node_Result(
	parent: Node,
	index: Int,
	val result: ComputationItem
) extends Node(parent, index)
*/

case class Node_Conversion(
	//idclass: IdClass,
	input_l: List[IdClass],
	fn: (List[Object]) => ConversionResult
) extends Node_Computes

sealed trait ConversionItem
case class ConversionItem_Conversion(
	input_l: List[IdClass],
	fn: (List[Object]) => ConversionResult
) extends ConversionItem
case class ConversionItem_Object(idclass: IdClass, obj: Object) extends ConversionItem
