package roboliq.input

object YamlContent {
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
	
	val protocol2Text = """
TYPE: section
BODY:
- TYPE: import
  NAME: shakePlate
  VERSION: "1.0"
- TYPE: protocol
  labwares:
    plate1: { model: plateModel_384_square, location: P3 }
  commands:
  - TYPE: action
    NAME: shakePlate
    INPUT:
      agent: mario
      device: mario.shaker
      labware: plate1
      site: P3
      program:
        rpm: 200
        duration: 10
"""
	val protocol2DataText = """
planningDomainObjects:
  mario: agent
  mario.shaker: shaker
planningInitialState:
- agent-has-device mario mario.shaker
- device-can-site mario.shaker P3
"""

	val evowareAgentConfig1Text = """
TYPE: EvowareAgent
name: mario
evowareDir: ../testdata/bsse-mario
protocolData:
  objects:
    plateModel_96_pcr:
      type: PlateModel
      label: 96 well PCR plate
      evowareName: D-BSSE 96 Well PCR Plate
    plateModel_96_dwp:
      type: PlateModel
      label: 96 well deep-well plate
      evowareName: D-BSSE 96 Well DWP
    plateModel_384_round:
      type: PlateModel
      label: 384 round-well plate
      evowareName: D-BSSE 384 Well Plate White
    plateModel_384_square:
      type: PlateModel
      label: 384 square-well white plate
      evowareName: D-BSSE 384 Well Plate White
    plateModel_96_nunc_transparent:
      type: PlateModel
      label: 96 square-well transparent Nunc plate
      evowareName: Ellis Nunc F96 MicroWell
    troughModel_100ml:
      type: PlateModel
      label: Trough 100ml
      evowareName: Trough 100ml
    troughModel_100ml_lowvol_tips:
      type: PlateModel
      label: Trough 100ml LowVol Tips
      evowareName: Trough 100ml LowVol Tips
    tubeHolderModel_1500ul:
      type: PlateModel
      label: 20 tube block 1.5ml
      evowareName: Block 20Pos 1.5 ml Eppendorf
    tipModel_50: { type: TipModel, min: 0.1, max: 45 }
    tipModel_1000: { type: TipModel, min: 3, max: 950 }
    tip1: { type: Tip, row: 1, permanentModel: tipModel_1000 }
    tip2: { type: Tip, row: 2, permanentModel: tipModel_1000 }
    tip3: { type: Tip, row: 3, permanentModel: tipModel_1000 }
    tip4: { type: Tip, row: 4, permanentModel: tipModel_1000 }
    tip5: { type: Tip, row: 5, permanentModel: tipModel_50 }
    tip6: { type: Tip, row: 6, permanentModel: tipModel_50 }
    tip7: { type: Tip, row: 7, permanentModel: tipModel_50 }
    tip8: { type: Tip, row: 8, permanentModel: tipModel_50 }
    sealerProgram_96_pcr:
      type: SealerProgram
      model: plateModel_96_pcr
      filename: C:\HJBioanalytikGmbH\RoboSeal3\RoboSeal_PlateParameters\4titude_PCR_red.bcf
    sealerProgram_96_nunc:
      type: SealerProgram
      model: plateModel_96_nunc_transparent
      filename: C:\HJBioanalytikGmbH\RoboSeal3\RoboSeal_PlateParameters\PerkinElmer_weiss.bcf
    sealerProgram_384:
      type: SealerProgram
      model: plateModel_384_square
      filename: C:\HJBioanalytikGmbH\RoboSeal3\RoboSeal_PlateParameters\Greiner_384_schwarz.bcf
  planningInitialState:
  - site-closed CENTRIFUGE
  - site-closed CENTRIFUGE_1
  - site-closed CENTRIFUGE_2
  - site-closed CENTRIFUGE_3
  - site-closed CENTRIFUGE_4
  - site-closed TROBOT1
  - site-closed TROBOT2

evowareProtocolData:
  sites:
    CENTRIFUGE: { carrier: Centrifuge }
    CENTRIFUGE_1: { carrier: Centrifuge, site: 1, internal: true }
    CENTRIFUGE_2: { carrier: Centrifuge, site: 1, internal: true }
    CENTRIFUGE_3: { carrier: Centrifuge, site: 1, internal: true }
    CENTRIFUGE_4: { carrier: Centrifuge, site: 1, internal: true }
    #P1/DOWNHOLDER
    P2: { grid: 10, site: 2 }
    P3: { grid: 10, site: 4 }
    P4: { grid: 17, site: 2 }
    P4PCR: { grid: 17, site: 1 }
    P5: { grid: 17, site: 4 }
    P5PCR: { grid: 17, site: 3 }
    R1: { grid: 3, site: 1 }
    R2: { grid: 3, site: 2 }
    R3: { grid: 3, site: 3 }
    R4: { grid: 8, site: 1 }
    R5: { grid: 8, site: 2 }
    R6: { grid: 8, site: 3 }
    READER: { carrier: "Infinite M200" }
    REGRIP: { grid: 61, site: 1 }
    ROBOSEAL: { carrier: RoboSeal }
    SYSTEM: { carrier: System }
    T3: { grid: 16, site: 1 }
    TROBOT1: { carrier: TRobot1 }
    TROBOT2: { carrier: TRobot2 }
  devices:
    mario.centrifuge:
      type: Centrifuge
      evowareName: Centrifuge
      sitesOverride: [CENTRIFUGE, CENTRIFUGE_1, CENTRIFUGE_2, CENTRIFUGE_3, CENTRIFUGE_4]
    mario.shaker:
      type: Shaker
      evowareName: "MP 2Pos H+P Shake"
      sitesOverride: [P3]
    mario.sealer:
      type: Seeler
      evowareName: RoboSeal
    mario.peeler:
      type: Peeler
      evowareName: RoboPeel
    mario.thermocycler1:
      type: Thermocycler
      evowareName: TRobot1
    mario.thermocycler2:
      type: Thermocycler
      evowareName: TRobot2
    mario.reader:
      type: Reader
      evowareName: "Tecan part no. 30016056 or 30029757"
  transporterBlacklist:
  - { site: DOWNHOLDER }
  - { roma: 1, vector: Narrow, site: CENTRIFUGE_1 }
  - { roma: 1, vector: Narrow, site: CENTRIFUGE_3 }
  - { roma: 2, vector: Wide, site: P1 }
  - { roma: 2, vector: Wide, site: P2 }
  - { roma: 2, vector: Wide, site: P3 }
  - { roma: 2, vector: Wide, site: P4 }
  - { roma: 2, vector: Wide, site: P4PCR }
  - { roma: 2, vector: Wide, site: P5 }
  - { roma: 2, vector: Wide, site: P5PCR }
  - { roma: 2, vector: Narrow, site: CENTRIFUGE_2 }
  - { roma: 2, vector: Narrow, site: CENTRIFUGE_4 }

tableSetups:
  default:
    tableFile: ../testdata/bsse-mario/Template.ewt
    evowareProtocolData:
      sites:
        P1: { grid: 9, site: 3 }
      pipetterSites: [P1, P2, P3, P4PCR, P5PCR, R1, R2, R3, R4, R5, R6, SYSTEM, T3]
      userSites:     [P1, P2, P3, P4PCR, P5PCR, R1, R2, R3, R4, R5, R6, READER, REGRIP, T3]

  withDownholder:
    tableFile: ../testdata/bsse-mario/TemplateWithRealDownholder.ewt
    evowareConfigData:
      sites:
        DOWNHOLDER: { grid: 9, site: 1 }
      pipetterSites: [DOWNHOLDER, P2, P3, P4PCR, P5PCR, R1, R2, R3, R4, R5, R6, SYSTEM, T3]
      userSites:     [DOWNHOLDER, P2, P3, P4PCR, P5PCR, R1, R2, R3, R4, R5, R6, READER, REGRIP, T3]
"""
}