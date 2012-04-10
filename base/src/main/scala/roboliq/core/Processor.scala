package roboliq.core

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedHashMap

case class ProcessorResult(
	val ob: ObjBase,
	val lNode: List[CmdNodeBean],
	val locationTracker: LocationTracker
)

class Processor private (bb: BeanBase, ob: ObjBase, lCmdHandler: List[CmdHandler], states0: RobotState) {
	def process(cmds: List[CmdBean]): ProcessorResult = {
		bb.lDevice.foreach(_.setObjBase(ob))
		
		val mapNodeToResources = new LinkedHashMap[CmdNodeBean, List[NeedResource]]
		val builder = new StateBuilder(ob, HashMap[String, Object](states0.map.toSeq : _*))
		
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
		
		// Helper function to load resource objects and their known states
		val seen = new HashSet[String]
		val mPlate = new LinkedHashMap[Plate, CmdNodeBean]
		var mTube = new LinkedHashMap[Tube, CmdNodeBean]
		val mSubstance = new LinkedHashMap[Substance, CmdNodeBean]
		def needPlate(id: String, node: CmdNodeBean): Result[Unit] = {
			for {plate <- ob.findPlate(id)} yield {
				mPlate(plate) = node
			}
		}
		def needWell(id: String, node: CmdNodeBean): Result[Unit] = {
			for {
				well <- ob.findWell(id)
				plate <- needPlate(well.idPlate, node)
			} yield {}
		}
		def needTube(id: String, node: CmdNodeBean): Result[Unit] = {
			for {tube <- ob.findTube(id)} yield {
				mTube(tube) = node
			}
		}
		def needId(node: CmdNodeBean, id: String): Result[Unit] = {
			// Try to load any ids which we haven't looked for yet
			if (!seen.contains(id)) {
				seen += id
				// If it's a plate
				needPlate(id, node) orElse
				// If it's a substance
				(for {substance <- ob.findSubstance(id)} yield {
					mSubstance(substance) = node
				}) orElse
				// Else if it's a well
				needTube(id, node) orElse
				// Else if it's a tube
				needWell(id, node) match {
					case Error(ls) => ls.foreach(node.addError); Error(ls)
					case ret => ret
				}
			}
			else {
				Success(())
			}
		}
		def needIds(node: CmdNodeBean, ids: String) {
			WellSpecParser.parseToIds(ids, ob) match {
				case Error(ls) => ls.foreach(node.addError)
				case Success(lId) =>
					for (id <- lId) {
						needId(node, id) match {
							case Error(ls) => ls.foreach(node.addError)
							case _ =>
						}
					}
			}
		}
		// Load resource objects and their known states saved in the database
		for ((node, resources) <- mapNodeToResources) {
			val messages = new CmdMessageWriter(node)
			for (resource <- resources) {
				println(resource)
				resource match {
					case NeedTip(id) => ob.findTip_?(id, messages)
					case NeedSrc(ids) => needIds(node, ids)
					case NeedDest(ids) => needIds(node, ids)
					case NeedPool(_, _, _) => // TODO: allocate new pool
				}
			}
		}
		
		// Load history in order to reconstruct object states
		ob.reconstructHistory()
		
		// Find substance wells
		val mapSubstanceToWells = new HashMap[String, List[String]]
		println("mSubstance: "+mSubstance)
		for ((substance, node) <- mSubstance) {
			ob.findAllIdsContainingSubstance(substance) match {
				case Error(ls) => ls.foreach(node.addError)
				case Success(lId) =>
					println("substance: "+substance.id+", lId: "+lId)
					// TODO: intelligently choose wells rather than taking them all
					lId.foreach(id => needId(node, id))
					mapSubstanceToWells(substance.id) = lId
			}
		}
		println("mapSubstanceToWells: "+mapSubstanceToWells)

		println("mTube: "+mTube)

		val locationBuilder = new LocationBuilder
		
		// If tube locations are defined in database
		ob.findAllTubeLocations().foreach(lLocation => {
			// Construct mutable list of all free locations
			val lLocFree = ArrayBuffer[TubeLocation](lLocation : _*)
			println("lLocFree: "+lLocFree)
			// Assign location to each plate
			for ((tube, node) <- mTube) {
				lLocFree.find(loc => loc.tubeModels.contains(tube.model)) match {
					case None => node.addError("couldn't find location for `"+tube.id+"`")
					case Some(location) =>
						lLocFree -= location
						// Add a plate and location with the same name for tube racks
						locationBuilder.addLocation(location.id, node.index, location.id)
						//locationBuilder.addLocation(tube.id, node.index, location.id)
						//println("added to location builder: ", tube.id, node.index, location)
						// Set TubeState properties
						ob.setInitialTubeLocation(tube, location.id, 0, 0)
				}
			}
		})

		// Object to assign location to each plate
		println("ob.findAllLocations(): "+ob.findAllPlateLocations())
		println("mPlate: "+mPlate)
		// If locations are defined in database
		ob.findAllPlateLocations().foreach(lLocation => {
			// Construct mutable list of all free locations
			val lLocFree = ArrayBuffer[PlateLocation](lLocation : _*)
			println("lLocFree: "+lLocFree)
			// Assign location to each plate
			for ((plate, node) <- mPlate) {
				plate.locationPermanent_? match {
					case Some(location) =>
						locationBuilder.addLocation(plate.id, node.index, location)
						println("added to location builder: ", plate.id, node.index, location)
					case None =>
						lLocFree.find(loc => loc.plateModels.contains(plate.model)) match {
							case None => node.addError("couldn't find location for `"+plate.id+"`")
							case Some(location) =>
								lLocFree -= location
								locationBuilder.addLocation(plate.id, node.index, location.id)
								println("added to location builder: ", plate.id, node.index, location)
						}
				}
			}
		})
		println("locationBuilder.map: "+locationBuilder.map)
		//println("test: "+ob.builder.findWellPosition("T50water1"))
		
		//println("ob.builder.map: "+ob.builder.map)
		
		// TODO: Choose bench locations for any resources which don't already have one
		
		// Expand all nodes in-order until we have only final tokens
		def expand2(nodes: List[CmdNodeBean]) {
			for (node <- nodes if node.getErrorCount == 0 && node.childCommands == null) {
				node.states0 = builder.toImmutable

				val handler = node.handler
				val cmd = node.command
				val ctx = new ProcessorContext(this, node, ob, Some(builder), node.states0)
				val messages = new CmdMessageWriter(node)
				
				//println("expand2: command "+node.index)
				//println("expand2: TIP1 state: "+builder.findTipState("TIP1").get)
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
						}
					case Expand2Tokens(tokens, events) =>
						if (!tokens.isEmpty)
							node.tokens = tokens
						if (!events.isEmpty) {
							node.events = events
						}
				}
				// Update states based on events
				if (node.events != null) {
					node.events.foreach(_.update(builder)) 
				}
				node.states1 = builder.toImmutable
			}
		}
		
		expand2(nodes)
		
		// Output node-tree
		nodes.foreach(println)
		
		// Send node-tree to robot compiler
		
		ProcessorResult(ob, nodes, new LocationTracker(locationBuilder.map.toMap))
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
