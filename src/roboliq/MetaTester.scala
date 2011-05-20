package roboliq

import meta._

class MetaTester {
	def run() {
		val carrier1, carrier2 = new MetaObject
		
		carrier1.index.put(0, Val(ValSource.Settings, 15))
		carrier1.setAttribute(AttributeKind.Cooled, 0, Val(ValSource.Settings, 1))
		
		carrier2.index.put(0, Val(ValSource.Settings, 15))
		carrier2.setAttribute(AttributeKind.Cooled, 0, Val(ValSource.Settings, 1))
		
		
	}
}