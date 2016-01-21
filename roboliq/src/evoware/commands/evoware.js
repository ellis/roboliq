private def handleFacts(
	objects: JsObject,
	step: JsObject
): ResultC[List[Token]] = {
	for {
		inst <- JsConverter.fromJs[EvowareFacts](step)
	} yield {
		val line = createFactsLine(inst.factsEquipment, inst.factsVariable, inst.factsValue_?.getOrElse(""))
		List(Token(line))
	}
}
