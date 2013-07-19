(defproblem pd domain
 ; initial conditions
 (
  (is-agent r1) ; e521b091-b055-478e-a756-c9a0059a017f
  (is-agent user) ; 39c8f6cd-30fc-4430-b0cd-7e854ed845f6
  (is-labware bench_001x1) ; (239,0)
  (is-labware bench_001x2) ; (239,1)
  (is-labware bench_001x3) ; (239,2)
  (is-labware bench_002x1) ; (240,0)
  (is-labware bench_002x2) ; (240,1)
  (is-labware bench_002x3) ; (240,2)
  (is-labware bench_003x1) ; (130,0)
  (is-labware bench_003x2) ; (130,1)
  (is-labware bench_003x3) ; (130,2)
  (is-labware bench_004x1) ; (241,0)
  (is-labware bench_004x2) ; (241,1)
  (is-labware bench_007x1) ; (52,0)
  (is-labware bench_009x1) ; (242,0)
  (is-labware bench_009x2) ; (242,1)
  (is-labware bench_010x1) ; (249,0)
  (is-labware bench_010x2) ; (249,1)
  (is-labware bench_010x3) ; (249,2)
  (is-labware bench_010x4) ; (249,3)
  (is-labware bench_016x1) ; (250,0)
  (is-labware bench_017x1) ; (243,0)
  (is-labware bench_017x2) ; (243,1)
  (is-labware bench_017x3) ; (243,2)
  (is-labware bench_017x4) ; (243,3)
  (is-labware bench_024x1) ; (244,0)
  (is-labware bench_024x2) ; (244,1)
  (is-labware bench_024x3) ; (244,2)
  (is-labware bench_024x4) ; (244,3)
  (is-labware bench_024x5) ; (244,4)
  (is-labware bench_024x6) ; (244,5)
  (is-labware bench_033x1) ; (35,0)
  (is-labware bench_033x2) ; (35,1)
  (is-labware bench_033x3) ; (35,2)
  (is-labware bench_033x4) ; (35,3)
  (is-labware bench_033x5) ; (35,4)
  (is-labware bench_033x6) ; (35,5)
  (is-labware bench_033x7) ; (35,6)
  (is-labware bench_033x8) ; (35,7)
  (is-labware bench_033x9) ; (35,8)
  (is-labware bench_061x1) ; (246,0)
  (is-labware device_-1x1) ; System
  (is-labware device_126x1) ; Infinite M200
  (is-labware device_214x1) ; Symbol954
  (is-labware device_234x1) ; TRobot1
  (is-labware device_235x1) ; TRobot2
  (is-labware device_236x1) ; RoboSeal
  (is-labware device_237x1) ; RoboPeel
  (is-labware device_65x1) ; Centrifuge
  (is-labware hotel_245x1) ; hotel_245x1
  (is-labware hotel_245x10) ; hotel_245x10
  (is-labware hotel_245x11) ; hotel_245x11
  (is-labware hotel_245x12) ; hotel_245x12
  (is-labware hotel_245x13) ; hotel_245x13
  (is-labware hotel_245x14) ; hotel_245x14
  (is-labware hotel_245x15) ; hotel_245x15
  (is-labware hotel_245x16) ; hotel_245x16
  (is-labware hotel_245x17) ; hotel_245x17
  (is-labware hotel_245x18) ; hotel_245x18
  (is-labware hotel_245x19) ; hotel_245x19
  (is-labware hotel_245x2) ; hotel_245x2
  (is-labware hotel_245x20) ; hotel_245x20
  (is-labware hotel_245x21) ; hotel_245x21
  (is-labware hotel_245x22) ; hotel_245x22
  (is-labware hotel_245x23) ; hotel_245x23
  (is-labware hotel_245x24) ; hotel_245x24
  (is-labware hotel_245x25) ; hotel_245x25
  (is-labware hotel_245x26) ; hotel_245x26
  (is-labware hotel_245x27) ; hotel_245x27
  (is-labware hotel_245x28) ; hotel_245x28
  (is-labware hotel_245x29) ; hotel_245x29
  (is-labware hotel_245x3) ; hotel_245x3
  (is-labware hotel_245x30) ; hotel_245x30
  (is-labware hotel_245x31) ; hotel_245x31
  (is-labware hotel_245x32) ; hotel_245x32
  (is-labware hotel_245x4) ; hotel_245x4
  (is-labware hotel_245x5) ; hotel_245x5
  (is-labware hotel_245x6) ; hotel_245x6
  (is-labware hotel_245x7) ; hotel_245x7
  (is-labware hotel_245x8) ; hotel_245x8
  (is-labware hotel_245x9) ; hotel_245x9
  (is-labware hotel_85x1) ; hotel_85x1
  (is-labware hotel_85x2) ; hotel_85x2
  (is-labware hotel_85x3) ; hotel_85x3
  (is-labware hotel_85x4) ; hotel_85x4
  (is-labware hotel_85x5) ; hotel_85x5
  (is-labware offsite) ; c7c9f05a-7ba3-47d6-8f90-bdeea13220ef
  (is-labware plate1) ; 561e3f8a-a066-4f29-bf99-179dff906829
  (is-model m001) ; D-BSSE 96 Well DWP
  (is-model m002) ; D-BSSE 96 Well PCR Plate
  (is-model offsiteModel) ; 31a5579a-bb3a-45b7-9216-b2452b966293
  (is-model sm1) ; Set(PlateModel(D-BSSE 96 Well PCR Plate,8,12,287.642ul))
  (is-model sm2) ; Set(PlateModel(D-BSSE 96 Well PCR Plate,8,12,287.642ul), PlateModel(D-BSSE 96 Well DWP,8,12,2.515122ml))
  (is-model sm3) ; Set(PlateModel(D-BSSE 96 Well DWP,8,12,2.515122ml))
  (is-peeler peeler) ; RoboPeel
  (is-plate plate1) ; 561e3f8a-a066-4f29-bf99-179dff906829
  (is-plateModel m001) ; D-BSSE 96 Well DWP
  (is-plateModel m002) ; D-BSSE 96 Well PCR Plate
  (is-sealer sealer) ; RoboSeal
  (is-site bench_001x1) ; (239,0)
  (is-site bench_001x2) ; (239,1)
  (is-site bench_001x3) ; (239,2)
  (is-site bench_002x1) ; (240,0)
  (is-site bench_002x2) ; (240,1)
  (is-site bench_002x3) ; (240,2)
  (is-site bench_003x1) ; (130,0)
  (is-site bench_003x2) ; (130,1)
  (is-site bench_003x3) ; (130,2)
  (is-site bench_004x1) ; (241,0)
  (is-site bench_004x2) ; (241,1)
  (is-site bench_007x1) ; (52,0)
  (is-site bench_009x1) ; (242,0)
  (is-site bench_009x2) ; (242,1)
  (is-site bench_010x1) ; (249,0)
  (is-site bench_010x2) ; (249,1)
  (is-site bench_010x3) ; (249,2)
  (is-site bench_010x4) ; (249,3)
  (is-site bench_016x1) ; (250,0)
  (is-site bench_017x1) ; (243,0)
  (is-site bench_017x2) ; (243,1)
  (is-site bench_017x3) ; (243,2)
  (is-site bench_017x4) ; (243,3)
  (is-site bench_024x1) ; (244,0)
  (is-site bench_024x2) ; (244,1)
  (is-site bench_024x3) ; (244,2)
  (is-site bench_024x4) ; (244,3)
  (is-site bench_024x5) ; (244,4)
  (is-site bench_024x6) ; (244,5)
  (is-site bench_033x1) ; (35,0)
  (is-site bench_033x2) ; (35,1)
  (is-site bench_033x3) ; (35,2)
  (is-site bench_033x4) ; (35,3)
  (is-site bench_033x5) ; (35,4)
  (is-site bench_033x6) ; (35,5)
  (is-site bench_033x7) ; (35,6)
  (is-site bench_033x8) ; (35,7)
  (is-site bench_033x9) ; (35,8)
  (is-site bench_061x1) ; (246,0)
  (is-site device_-1x1) ; System
  (is-site device_126x1) ; Infinite M200
  (is-site device_214x1) ; Symbol954
  (is-site device_234x1) ; TRobot1
  (is-site device_235x1) ; TRobot2
  (is-site device_236x1) ; RoboSeal
  (is-site device_237x1) ; RoboPeel
  (is-site device_65x1) ; Centrifuge
  (is-site hotel_245x1) ; hotel_245x1
  (is-site hotel_245x10) ; hotel_245x10
  (is-site hotel_245x11) ; hotel_245x11
  (is-site hotel_245x12) ; hotel_245x12
  (is-site hotel_245x13) ; hotel_245x13
  (is-site hotel_245x14) ; hotel_245x14
  (is-site hotel_245x15) ; hotel_245x15
  (is-site hotel_245x16) ; hotel_245x16
  (is-site hotel_245x17) ; hotel_245x17
  (is-site hotel_245x18) ; hotel_245x18
  (is-site hotel_245x19) ; hotel_245x19
  (is-site hotel_245x2) ; hotel_245x2
  (is-site hotel_245x20) ; hotel_245x20
  (is-site hotel_245x21) ; hotel_245x21
  (is-site hotel_245x22) ; hotel_245x22
  (is-site hotel_245x23) ; hotel_245x23
  (is-site hotel_245x24) ; hotel_245x24
  (is-site hotel_245x25) ; hotel_245x25
  (is-site hotel_245x26) ; hotel_245x26
  (is-site hotel_245x27) ; hotel_245x27
  (is-site hotel_245x28) ; hotel_245x28
  (is-site hotel_245x29) ; hotel_245x29
  (is-site hotel_245x3) ; hotel_245x3
  (is-site hotel_245x30) ; hotel_245x30
  (is-site hotel_245x31) ; hotel_245x31
  (is-site hotel_245x32) ; hotel_245x32
  (is-site hotel_245x4) ; hotel_245x4
  (is-site hotel_245x5) ; hotel_245x5
  (is-site hotel_245x6) ; hotel_245x6
  (is-site hotel_245x7) ; hotel_245x7
  (is-site hotel_245x8) ; hotel_245x8
  (is-site hotel_245x9) ; hotel_245x9
  (is-site hotel_85x1) ; hotel_85x1
  (is-site hotel_85x2) ; hotel_85x2
  (is-site hotel_85x3) ; hotel_85x3
  (is-site hotel_85x4) ; hotel_85x4
  (is-site hotel_85x5) ; hotel_85x5
  (is-site offsite) ; c7c9f05a-7ba3-47d6-8f90-bdeea13220ef
  (is-siteModel offsiteModel) ; 31a5579a-bb3a-45b7-9216-b2452b966293
  (is-siteModel sm1) ; Set(PlateModel(D-BSSE 96 Well PCR Plate,8,12,287.642ul))
  (is-siteModel sm2) ; Set(PlateModel(D-BSSE 96 Well PCR Plate,8,12,287.642ul), PlateModel(D-BSSE 96 Well DWP,8,12,2.515122ml))
  (is-siteModel sm3) ; Set(PlateModel(D-BSSE 96 Well DWP,8,12,2.515122ml))
  (is-transporter r1_transporter1) ; RoMa1
  (is-transporter r1_transporter2) ; RoMa2
  (is-transporter userArm) ; fa61bf48-d5dd-47cb-abbb-b77f28527297
  (agent-has-device r1 peeler)
  (agent-has-device r1 r1_transporter1)
  (agent-has-device r1 r1_transporter2)
  (agent-has-device r1 sealer)
  (agent-has-device user userArm)
  (device-can-model peeler m002)
  (device-can-model sealer m002)
  (device-can-model userArm m001)
  (device-can-model userArm m002)
  (device-can-site peeler device_237x1)
  (device-can-site sealer device_236x1)
  (stackable offsiteModel m001)
  (stackable offsiteModel m002)
  (stackable sm1 m002)
  (stackable sm2 m001)
  (stackable sm2 m002)
  (stackable sm3 m001)
  (model bench_009x1 sm1)
  (model bench_010x1 sm1)
  (model bench_010x2 sm3)
  (model bench_010x4 sm2)
  (model bench_017x1 sm1)
  (model bench_017x3 sm1)
  (model bench_024x1 sm1)
  (model bench_024x3 sm1)
  (model bench_024x5 sm1)
  (model bench_033x3 sm1)
  (model bench_033x6 sm1)
  (model bench_061x1 sm2)
  (model device_214x1 sm2)
  (model device_234x1 sm1)
  (model device_236x1 sm1)
  (model device_237x1 sm1)
  (model device_65x1 sm2)
  (model hotel_245x1 sm1)
  (model hotel_245x10 sm1)
  (model hotel_245x11 sm1)
  (model hotel_245x12 sm1)
  (model hotel_245x13 sm1)
  (model hotel_245x14 sm1)
  (model hotel_245x15 sm1)
  (model hotel_245x16 sm1)
  (model hotel_245x17 sm1)
  (model hotel_245x18 sm1)
  (model hotel_245x19 sm1)
  (model hotel_245x2 sm1)
  (model hotel_245x20 sm1)
  (model hotel_245x21 sm1)
  (model hotel_245x22 sm1)
  (model hotel_245x23 sm1)
  (model hotel_245x24 sm1)
  (model hotel_245x25 sm1)
  (model hotel_245x26 sm1)
  (model hotel_245x27 sm1)
  (model hotel_245x28 sm1)
  (model hotel_245x29 sm1)
  (model hotel_245x3 sm1)
  (model hotel_245x30 sm1)
  (model hotel_245x31 sm1)
  (model hotel_245x32 sm1)
  (model hotel_245x4 sm1)
  (model hotel_245x5 sm1)
  (model hotel_245x6 sm1)
  (model hotel_245x7 sm1)
  (model hotel_245x8 sm1)
  (model hotel_245x9 sm1)
  (model hotel_85x1 sm3)
  (model hotel_85x2 sm3)
  (model hotel_85x3 sm3)
  (model hotel_85x4 sm3)
  (model hotel_85x5 sm3)
  (model plate1 m002)
  (location plate1 offsite)
  (transporter-can r1_transporter1 bench_009x1 Narrow)
  (transporter-can r1_transporter1 bench_009x2 Narrow)
  (transporter-can r1_transporter1 bench_010x1 Narrow)
  (transporter-can r1_transporter1 bench_010x2 Narrow)
  (transporter-can r1_transporter1 bench_010x3 Narrow)
  (transporter-can r1_transporter1 bench_010x4 Narrow)
  (transporter-can r1_transporter1 bench_017x1 Narrow)
  (transporter-can r1_transporter1 bench_017x2 Narrow)
  (transporter-can r1_transporter1 bench_017x3 Narrow)
  (transporter-can r1_transporter1 bench_017x4 Narrow)
  (transporter-can r1_transporter1 bench_024x1 Narrow)
  (transporter-can r1_transporter1 bench_024x2 Narrow)
  (transporter-can r1_transporter1 bench_024x3 Narrow)
  (transporter-can r1_transporter1 bench_024x4 Narrow)
  (transporter-can r1_transporter1 bench_024x5 Narrow)
  (transporter-can r1_transporter1 bench_024x6 Narrow)
  (transporter-can r1_transporter1 bench_033x1 Narrow)
  (transporter-can r1_transporter1 bench_033x2 Narrow)
  (transporter-can r1_transporter1 bench_033x3 Narrow)
  (transporter-can r1_transporter1 bench_033x4 Narrow)
  (transporter-can r1_transporter1 bench_033x5 Narrow)
  (transporter-can r1_transporter1 bench_033x6 Narrow)
  (transporter-can r1_transporter1 bench_033x7 Narrow)
  (transporter-can r1_transporter1 bench_033x8 Narrow)
  (transporter-can r1_transporter1 bench_033x9 Narrow)
  (transporter-can r1_transporter1 bench_061x1 Narrow)
  (transporter-can r1_transporter1 device_126x1 Narrow)
  (transporter-can r1_transporter1 device_214x1 Narrow)
  (transporter-can r1_transporter1 device_234x1 Narrow)
  (transporter-can r1_transporter1 device_235x1 Narrow)
  (transporter-can r1_transporter1 device_65x1 Narrow)
  (transporter-can r1_transporter1 hotel_245x1 Narrow)
  (transporter-can r1_transporter1 hotel_245x10 Narrow)
  (transporter-can r1_transporter1 hotel_245x11 Narrow)
  (transporter-can r1_transporter1 hotel_245x12 Narrow)
  (transporter-can r1_transporter1 hotel_245x13 Narrow)
  (transporter-can r1_transporter1 hotel_245x14 Narrow)
  (transporter-can r1_transporter1 hotel_245x15 Narrow)
  (transporter-can r1_transporter1 hotel_245x16 Narrow)
  (transporter-can r1_transporter1 hotel_245x17 Narrow)
  (transporter-can r1_transporter1 hotel_245x18 Narrow)
  (transporter-can r1_transporter1 hotel_245x19 Narrow)
  (transporter-can r1_transporter1 hotel_245x2 Narrow)
  (transporter-can r1_transporter1 hotel_245x20 Narrow)
  (transporter-can r1_transporter1 hotel_245x21 Narrow)
  (transporter-can r1_transporter1 hotel_245x22 Narrow)
  (transporter-can r1_transporter1 hotel_245x23 Narrow)
  (transporter-can r1_transporter1 hotel_245x24 Narrow)
  (transporter-can r1_transporter1 hotel_245x25 Narrow)
  (transporter-can r1_transporter1 hotel_245x26 Narrow)
  (transporter-can r1_transporter1 hotel_245x27 Narrow)
  (transporter-can r1_transporter1 hotel_245x28 Narrow)
  (transporter-can r1_transporter1 hotel_245x29 Narrow)
  (transporter-can r1_transporter1 hotel_245x3 Narrow)
  (transporter-can r1_transporter1 hotel_245x30 Narrow)
  (transporter-can r1_transporter1 hotel_245x31 Narrow)
  (transporter-can r1_transporter1 hotel_245x32 Narrow)
  (transporter-can r1_transporter1 hotel_245x4 Narrow)
  (transporter-can r1_transporter1 hotel_245x5 Narrow)
  (transporter-can r1_transporter1 hotel_245x6 Narrow)
  (transporter-can r1_transporter1 hotel_245x7 Narrow)
  (transporter-can r1_transporter1 hotel_245x8 Narrow)
  (transporter-can r1_transporter1 hotel_245x9 Narrow)
  (transporter-can r1_transporter1 hotel_85x1 Narrow)
  (transporter-can r1_transporter1 hotel_85x2 Narrow)
  (transporter-can r1_transporter1 hotel_85x3 Narrow)
  (transporter-can r1_transporter1 hotel_85x4 Narrow)
  (transporter-can r1_transporter1 hotel_85x5 Narrow)
  (transporter-can r1_transporter2 bench_009x1 Narrow)
  (transporter-can r1_transporter2 bench_009x2 Narrow)
  (transporter-can r1_transporter2 bench_010x1 Narrow)
  (transporter-can r1_transporter2 bench_010x1 Wide)
  (transporter-can r1_transporter2 bench_010x2 Narrow)
  (transporter-can r1_transporter2 bench_010x2 Wide)
  (transporter-can r1_transporter2 bench_010x3 Narrow)
  (transporter-can r1_transporter2 bench_010x3 Wide)
  (transporter-can r1_transporter2 bench_010x4 Narrow)
  (transporter-can r1_transporter2 bench_010x4 Wide)
  (transporter-can r1_transporter2 bench_017x1 Narrow)
  (transporter-can r1_transporter2 bench_017x2 Narrow)
  (transporter-can r1_transporter2 bench_017x3 Narrow)
  (transporter-can r1_transporter2 bench_017x4 Narrow)
  (transporter-can r1_transporter2 bench_024x1 Narrow)
  (transporter-can r1_transporter2 bench_024x2 Narrow)
  (transporter-can r1_transporter2 bench_024x3 Narrow)
  (transporter-can r1_transporter2 bench_024x4 Narrow)
  (transporter-can r1_transporter2 bench_024x5 Narrow)
  (transporter-can r1_transporter2 bench_024x6 Narrow)
  (transporter-can r1_transporter2 bench_033x1 Narrow)
  (transporter-can r1_transporter2 bench_033x1 Wide)
  (transporter-can r1_transporter2 bench_033x2 Narrow)
  (transporter-can r1_transporter2 bench_033x2 Wide)
  (transporter-can r1_transporter2 bench_033x3 Narrow)
  (transporter-can r1_transporter2 bench_033x3 Wide)
  (transporter-can r1_transporter2 bench_033x4 Narrow)
  (transporter-can r1_transporter2 bench_033x4 Wide)
  (transporter-can r1_transporter2 bench_033x5 Narrow)
  (transporter-can r1_transporter2 bench_033x5 Wide)
  (transporter-can r1_transporter2 bench_033x6 Narrow)
  (transporter-can r1_transporter2 bench_033x6 Wide)
  (transporter-can r1_transporter2 bench_033x7 Narrow)
  (transporter-can r1_transporter2 bench_033x7 Wide)
  (transporter-can r1_transporter2 bench_033x8 Narrow)
  (transporter-can r1_transporter2 bench_033x8 Wide)
  (transporter-can r1_transporter2 bench_033x9 Narrow)
  (transporter-can r1_transporter2 bench_033x9 Wide)
  (transporter-can r1_transporter2 bench_061x1 Narrow)
  (transporter-can r1_transporter2 bench_061x1 Wide)
  (transporter-can r1_transporter2 device_126x1 Wide)
  (transporter-can r1_transporter2 device_214x1 Narrow)
  (transporter-can r1_transporter2 device_234x1 Narrow)
  (transporter-can r1_transporter2 device_236x1 Narrow)
  (transporter-can r1_transporter2 device_237x1 Narrow)
  (transporter-can r1_transporter2 device_65x1 Narrow)
  (transporter-can r1_transporter2 hotel_245x1 Narrow)
  (transporter-can r1_transporter2 hotel_245x10 Narrow)
  (transporter-can r1_transporter2 hotel_245x11 Narrow)
  (transporter-can r1_transporter2 hotel_245x12 Narrow)
  (transporter-can r1_transporter2 hotel_245x13 Narrow)
  (transporter-can r1_transporter2 hotel_245x14 Narrow)
  (transporter-can r1_transporter2 hotel_245x15 Narrow)
  (transporter-can r1_transporter2 hotel_245x16 Narrow)
  (transporter-can r1_transporter2 hotel_245x17 Narrow)
  (transporter-can r1_transporter2 hotel_245x18 Narrow)
  (transporter-can r1_transporter2 hotel_245x19 Narrow)
  (transporter-can r1_transporter2 hotel_245x2 Narrow)
  (transporter-can r1_transporter2 hotel_245x20 Narrow)
  (transporter-can r1_transporter2 hotel_245x21 Narrow)
  (transporter-can r1_transporter2 hotel_245x22 Narrow)
  (transporter-can r1_transporter2 hotel_245x23 Narrow)
  (transporter-can r1_transporter2 hotel_245x24 Narrow)
  (transporter-can r1_transporter2 hotel_245x25 Narrow)
  (transporter-can r1_transporter2 hotel_245x26 Narrow)
  (transporter-can r1_transporter2 hotel_245x27 Narrow)
  (transporter-can r1_transporter2 hotel_245x28 Narrow)
  (transporter-can r1_transporter2 hotel_245x29 Narrow)
  (transporter-can r1_transporter2 hotel_245x3 Narrow)
  (transporter-can r1_transporter2 hotel_245x30 Narrow)
  (transporter-can r1_transporter2 hotel_245x31 Narrow)
  (transporter-can r1_transporter2 hotel_245x32 Narrow)
  (transporter-can r1_transporter2 hotel_245x4 Narrow)
  (transporter-can r1_transporter2 hotel_245x5 Narrow)
  (transporter-can r1_transporter2 hotel_245x6 Narrow)
  (transporter-can r1_transporter2 hotel_245x7 Narrow)
  (transporter-can r1_transporter2 hotel_245x8 Narrow)
  (transporter-can r1_transporter2 hotel_245x9 Narrow)
  (transporter-can userArm hotel_245x1 nil)
  (transporter-can userArm offsite nil)
 )
 ; tasks
 (
  (!log ?a0001 text0002)
 )
)
