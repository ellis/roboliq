package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.core._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._


// Return:
// - Error
// - Can't continue
// - Continue with new field

sealed trait GroupResult {
	def flatMap(f: GroupA => GroupResult): GroupResult
	def map(f: GroupA => GroupA): GroupResult
	def isError: Boolean = false
	def isSuccess: Boolean = false
	def isStop: Boolean = false
	
	final def >>=(f: GroupA => GroupResult): GroupResult = flatMap(f)
	def foreach(f: GroupA => GroupA): Unit = {  
		map(f)  
		()
	}
}
case class GroupError(groupA: GroupA, lsError: Seq[String]) extends GroupResult {
	def flatMap(f: GroupA => GroupResult): GroupResult = this
	def map(f: GroupA => GroupA): GroupResult = this
	override def isError: Boolean = true
}
case class GroupSuccess(groupA: GroupA) extends GroupResult {
	def flatMap(f: GroupA => GroupResult): GroupResult = f(groupA)
	def map(f: GroupA => GroupA): GroupResult = GroupSuccess(f(groupA))
	override def isSuccess: Boolean = true
}
case class GroupStop(groupA: GroupA, sReason: String) extends GroupResult {
	def flatMap(f: GroupA => GroupResult): GroupResult = this
	def map(f: GroupA => GroupA): GroupResult = this
	override def isStop: Boolean = true
}

abstract class GroupABuilderBase(
	val device: PipetteDevice,
	val ctx: ProcessorContext,
	val cmd: L3C_Pipette
) {
	protected val lTipAll: SortedSet[Tip] = device.getTips

	// Create the first group for this schedule
	def createGroupA(
		states0: RobotState,
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM]
	): GroupA = {
		val groupA0 = new GroupA(mItemToState, mLM, states0, Map(), Map(), Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, Nil, Nil, false, states0)
		groupA0
	}
	
	/**
	 * Create a group with @ref g0 as it's predecessor
	 * @param g0 predecessor to this group 
	 */
	def createGroupA(
		g0: GroupA
	): GroupA = {
		val g = new GroupA(
			g0.mItemToState, g0.mLM, g0.states1, g0.mTipToLM, g0.mTipToCleanSpecPending, Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, Nil, Nil, false, g0.states1
		)
		g
	}
	
	def addItemToGroup(
		g0: GroupA,
		item: Item
	): GroupResult
}
