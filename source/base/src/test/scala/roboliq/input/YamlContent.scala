package roboliq.input

private object YamlContent {
val protocol1Text = """
TYPE: protocol
labwares:
  plate1:
    model: plateModel_384_square
    location: P3

substances:
  water: {}
  dye: {}

sources:
  dyeLight:
    well: trough1(A01|H01)
    substances:
    - name: dye
      amount: 1/10
    - name: water

commands:
- TYPE: action
  NAME: distribute
  INPUT:
    source: dyeLight
    destination: plate1(B01)
    amount: 20ul
"""
	val protocol1 = RjsProtocol(
		labwares = Map("plate1" -> RjsProtocolLabware(model_? = Some("plateModel_384_square"), location_? = Some("P3"))),
		substances = Map("water" -> RjsProtocolSubstance(), "dye" -> RjsProtocolSubstance()),
		sources = Map("dyeLight" -> RjsProtocolSource("trough1(A01|H01)", List(RjsProtocolSourceSubstance("dye", amount_? = Some("1/10")), RjsProtocolSourceSubstance("water", None)))),
		commands = List(RjsBasicMap("action", Map(
			"NAME" -> RjsString("distribute"),
			"INPUT" -> RjsBasicMap(Map(
				"source" -> RjsString("dyeLight"),
				"destination" -> RjsString("plate1(B01)"),
				"amount" -> RjsString("20ul")
			))
		)))
	)
}