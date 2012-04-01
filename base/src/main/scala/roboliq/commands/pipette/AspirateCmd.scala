package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class AspirateCmdBean extends CmdBean(isFinal = true) {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[AspirateCmdItemBean] = null
	
	def process(ob: ObjBase): Result[Unit] = {
		if (items == null)
			Error("`items` must be set")
		else if (items.isEmpty())
			Error("`items` must not be empty")
		else {
			for {
				items <- Result.mapOver(items.toList) {item => item.toTokenItem(ob)}
			} yield {
				tokens = List(new AspirateToken(items))
			}
		}
	}
}

class AspirateCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	@BeanProperty var location: String = null
	
	
	def toTokenItem(ob: ObjBase): Result[AspirateTokenItem] = {
		for {
			idTip <- Result.mustBeSet(tip, "tip")
			idWell <- Result.mustBeSet(well, "well")
			idPolicy <- Result.mustBeSet(policy, "policy")
			idLocation <- Result.mustBeSet(location, "location")
			well <- ob.findWell(idWell)
		} yield {
			val iTip = idTip.drop(3).toInt
			new AspirateTokenItem(iTip, well, idPolicy, idLocation)
		}
	}
}

case class AspirateToken(
	val items: List[AspirateTokenItem]
) extends CmdToken

case class AspirateTokenItem(
	val tip: java.lang.Integer,
	val well: Well,
	val policy: String,
	val location: String
)

class YamlTest {
	import org.yaml.snakeyaml._
	
	roboliq.yaml.RoboliqYaml.constructor.addTypeDescription(new TypeDescription(classOf[AspirateCmdBean], "!_Aspirate"))
	val bean = roboliq.yaml.RoboliqYaml.loadFile("example1b.yaml")
	val text = roboliq.yaml.RoboliqYaml.toString(bean)
	
	def run {
		println(text)
		
		val bb = new BeanBase
		bb.addBean(bean)
		val ob = new ObjBase(bb)
		
		val cmd = bean.commands.get(0).asInstanceOf[AspirateCmdBean]
		val res = cmd.process(ob)
		println(res)
	}
}

object Main extends App {
	new YamlTest().run
}
