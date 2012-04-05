package roboliq.core

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedHashMap

class Processor private (bb: BeanBase, ob: ObjBase, lCmdHandler: List[CmdHandler], states0: RobotState) {
	def process(cmds: List[CmdBean]): List[CmdNodeBean] = {
		//val mapNodes = new HashMap[CmdBean, CmdNodeBean]
		val mapNodeToResources = new LinkedHashMap[CmdNodeBean, List[NeedResource]]
		val builder = new StateBuilder(states0)
		
		// Construct top-level command nodes
		def expand(indexParent: List[Int], cmds: List[CmdBean]): List[CmdNodeBean] = {
			cmds.zipWithIndex.map(pair => {
				val (cmd, iCmd) = pair
				val index = indexParent ++ List(iCmd + 1)
				val sIndex = index.mkString(".")
				val node = new CmdNodeBean
				lCmdHandler.find(_.canHandle(cmd)) match {
					case None =>
						val node = new CmdNodeBean
						node.command = cmd
						node.errors = List("no command handler found for command #"+sIndex+" "+cmd.getClass().getName())
					case Some(handler) =>
						node.handler = handler
						// EITHER: recursively expand children if command doesn't need objects
						// OR: gather resource IDs
						handler.expandWithoutObjBase(cmd, index) match {
							case Some(l) =>
								node.childCommands = l
								node.children = expand(index, l)
							case None =>
								//val ctx = new ProcessorContext(this, ob, Some(builder), builder.toImmutable)
								val resources = node.handler.getResources(node.command)
								mapNodeToResources(node) = resources
						}
				}
				node
			})
		}
		
		// Expand commands to get a node-tree
		val nodes = expand(Nil, cmds)
		
		// Load resource objects and their known states
		val seen = new HashSet[String]
		def needWells(node: CmdNodeBean, name: String) {
			// TODO: parse the name RoboEase-style into a list of well ids
			val lId = List(name)
			// Try to load any ids which we haven't looked for yet
			for (id <- lId) {
				if (!seen.contains(id)) {
					seen += id
					// Find the well in order to instantiate it as an object
					ob.findWell_?(id, node)
				}
			}
		}
		for ((node, resources) <- mapNodeToResources) {
			for (resource <- resources) {
				resource match {
					case NeedSrc(id) => needWells(node, id)
					case NeedDest(id) => needWells(node, id)
					case NeedPool(_, _, _) => // TODO: allocate new pool
				}
			}
		}
		
		// Choose bench locations for any resources which don't already have one
		
		// Expand all nodes in-order until we have only final tokens
		
		// Output node-tree
		
		// Send node-tree to robot compiler
		
		Nil
	}
}

object Processor {
	def apply(bb: BeanBase, states0: RobotState): Processor = {
		val ob = new ObjBase(bb)
		new Processor(bb, ob, bb.lCmdHandler, states0)
	}
}

class ProcessorContext(
	val processor: Processor,
	val ob: ObjBase,
	val builder_? : Option[StateBuilder],
	val states: RobotState
)
