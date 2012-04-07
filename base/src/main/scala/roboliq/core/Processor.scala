package roboliq.core

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedHashMap

class Processor private (bb: BeanBase, ob: ObjBase, lCmdHandler: List[CmdHandler], states0: RobotState) {
	def process(cmds: List[CmdBean]): List[CmdNodeBean] = {
		//val mapNodes = new HashMap[CmdBean, CmdNodeBean]
		val mapNodeToResources = new LinkedHashMap[CmdNodeBean, List[NeedResource]]
		//val builder = new StateBuilder(states0)
		
		// Construct command nodes
		def expand1(indexParent: List[Int], cmds: List[CmdBean]): List[CmdNodeBean] = {
			cmds.zipWithIndex.map(pair => {
				val (cmd, iCmd) = pair
				val index = indexParent ++ List(iCmd + 1)
				val sIndex = index.mkString(".")
				val node = new CmdNodeBean
				node.command = cmd
				node.lIndex = index
				node.index = sIndex
				val messages = new CmdMessageWriter(node)
				lCmdHandler.find(_.canHandle(cmd)) match {
					case None =>
						node.errors = List("no command handler found for command #"+sIndex+" "+cmd.getClass().getName())
					case Some(handler) =>
						node.handler = handler
						// EITHER: recursively expand children if command doesn't need objects
						// OR: gather resource IDs
						handler.expand1(cmd, messages) match {
							case Expand1Errors() =>
							case Expand1Cmds(childCommands) =>
								node.childCommands = childCommands
								node.children = expand1(index, childCommands)
							case Expand1Resources(resources) =>
								mapNodeToResources(node) = resources
						}
				}
				node
			})
		}
		
		// Expand commands to get a node-tree
		val nodes = expand1(Nil, cmds)
		
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
					// This will also load its state
					ob.findWell_?(id, node)
				}
			}
		}
		for ((node, resources) <- mapNodeToResources) {
			val messages = new CmdMessageWriter(node)
			for (resource <- resources) {
				println(resource)
				resource match {
					case NeedTip(id) => ob.findTip_?(id, messages)
					case NeedSrc(name) => needWells(node, name)
					case NeedDest(name) => needWells(node, name)
					case NeedPool(_, _, _) => // TODO: allocate new pool
				}
			}
		}
		
		println("ob.builder.map: "+ob.builder.map)
		
		// TODO: Choose bench locations for any resources which don't already have one
		
		// Expand all nodes in-order until we have only final tokens
		def expand2(nodes: List[CmdNodeBean]) {
			for (node <- nodes if node.getErrorCount == 0 && node.childCommands == null) {
				val handler = node.handler
				val cmd = node.command
				val ctx = new ProcessorContext(this, node, ob, Some(ob.builder), ob.builder.toImmutable)
				val messages = new CmdMessageWriter(node)
				
				handler.expand2(cmd, ctx, messages) match {
					case Expand2Errors() =>
					case Expand2Cmds(childCommands, events) =>
						if (!childCommands.isEmpty) {
							node.childCommands = childCommands
							node.children = expand1(node.lIndex, node.childCommands.toList)
							if (node.getErrorCount == 0)
								expand2(node.children.toList)
						}
						if (!events.isEmpty) {
							node.events = events
							node.events.foreach(_.update(ob.builder)) 
						}
					case Expand2Tokens(tokens, events) =>
						if (!tokens.isEmpty)
							node.tokens = tokens
						if (!events.isEmpty)
							node.events = events
				}
			}
		}
		
		expand2(nodes)
		
		// Output node-tree
		nodes.foreach(println)
		
		// Send node-tree to robot compiler
		
		nodes
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
	val node: CmdNodeBean,
	val ob: ObjBase,
	val builder_? : Option[StateBuilder],
	val states: RobotState
)
