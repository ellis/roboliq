package roboliq.test

import roboliq.core._

class TestYamlRunner {
	def run(
		config_l: List[RoboliqYamlBean], cmd_l: List[CmdBean]
	): ProcessorResult = {
		val bb = new BeanBase
		config_l.foreach(bb.addBean)
		val ob = new ObjBase(bb)
	
		val builder = new StateBuilder(ob)
		val processor = Processor(bb, builder.toImmutable)
		val res = processor.process(cmd_l)
		res
	}
}