(defproblem alan01 domain
 ; initial conditions
 (
  (is-agent r1)
  (is-agent user) ; user
  (is-labware offsite) ; offsite
  (is-labware pcrPlate1)
  (is-labware r1_bench_001x1) ; r1 bench Wash Station Clean site 1
  (is-labware r1_bench_001x2) ; r1 bench Wash Station Clean site 2
  (is-labware r1_bench_001x3) ; r1 bench Wash Station Clean site 3
  (is-labware r1_bench_002x1) ; r1 bench Wash Station Dirty site 1
  (is-labware r1_bench_002x2) ; r1 bench Wash Station Dirty site 2
  (is-labware r1_bench_002x3) ; r1 bench Wash Station Dirty site 3
  (is-labware r1_bench_003x1) ; r1 bench LI - Trough 3Pos 100ml site 1
  (is-labware r1_bench_003x2) ; r1 bench LI - Trough 3Pos 100ml site 2
  (is-labware r1_bench_003x3) ; r1 bench LI - Trough 3Pos 100ml site 3
  (is-labware r1_bench_004x1) ; r1 bench Cooled 8Pos*15ml 8Pos*50ml site 1
  (is-labware r1_bench_004x2) ; r1 bench Cooled 8Pos*15ml 8Pos*50ml site 2
  (is-labware r1_bench_007x1) ; r1 bench Trough 1000ml site 1
  (is-labware r1_bench_009x1) ; r1 bench Downholder site 1
  (is-labware r1_bench_009x2) ; r1 bench Downholder site 2
  (is-labware r1_bench_010x1) ; r1 bench MP 2Pos H+P Shake site 1
  (is-labware r1_bench_010x2) ; r1 bench MP 2Pos H+P Shake site 2
  (is-labware r1_bench_010x3) ; r1 bench MP 2Pos H+P Shake site 3
  (is-labware r1_bench_010x4) ; r1 bench MP 2Pos H+P Shake site 4
  (is-labware r1_bench_016x1) ; r1 bench Block 20Pos site 1
  (is-labware r1_bench_017x1) ; r1 bench MP 3Pos Cooled 1 PCR site 1
  (is-labware r1_bench_017x2) ; r1 bench MP 3Pos Cooled 1 PCR site 2
  (is-labware r1_bench_017x3) ; r1 bench MP 3Pos Cooled 1 PCR site 3
  (is-labware r1_bench_017x4) ; r1 bench MP 3Pos Cooled 1 PCR site 4
  (is-labware r1_bench_024x1) ; r1 bench MP 3Pos Cooled 2 PCR site 1
  (is-labware r1_bench_024x2) ; r1 bench MP 3Pos Cooled 2 PCR site 2
  (is-labware r1_bench_024x3) ; r1 bench MP 3Pos Cooled 2 PCR site 3
  (is-labware r1_bench_024x4) ; r1 bench MP 3Pos Cooled 2 PCR site 4
  (is-labware r1_bench_024x5) ; r1 bench MP 3Pos Cooled 2 PCR site 5
  (is-labware r1_bench_024x6) ; r1 bench MP 3Pos Cooled 2 PCR site 6
  (is-labware r1_bench_033x1) ; r1 bench Te-VacS site 1
  (is-labware r1_bench_033x2) ; r1 bench Te-VacS site 2
  (is-labware r1_bench_033x3) ; r1 bench Te-VacS site 3
  (is-labware r1_bench_033x4) ; r1 bench Te-VacS site 4
  (is-labware r1_bench_033x5) ; r1 bench Te-VacS site 5
  (is-labware r1_bench_033x6) ; r1 bench Te-VacS site 6
  (is-labware r1_bench_033x7) ; r1 bench Te-VacS site 7
  (is-labware r1_bench_033x8) ; r1 bench Te-VacS site 8
  (is-labware r1_bench_033x9) ; r1 bench Te-VacS site 9
  (is-labware r1_bench_061x1) ; r1 bench ReGrip Station site 1
  (is-labware r1_device_-1x1) ; r1 device System site 1
  (is-labware r1_device_126x1) ; r1 device Infinite M200 site 1
  (is-labware r1_device_214x1) ; r1 device Symbol954 site 1
  (is-labware r1_device_234x1) ; r1 device TRobot1 site 1
  (is-labware r1_device_235x1) ; r1 device TRobot2 site 1
  (is-labware r1_device_236x1) ; r1 device RoboSeal site 1
  (is-labware r1_device_237x1) ; r1 device RoboPeel site 1
  (is-labware r1_device_65x1) ; r1 device Centrifuge site 1
  (is-labware r1_hotel_245x1) ; r1 hotel Shelf 32Pos Microplate site 1
  (is-labware r1_hotel_245x10) ; r1 hotel Shelf 32Pos Microplate site 10
  (is-labware r1_hotel_245x11) ; r1 hotel Shelf 32Pos Microplate site 11
  (is-labware r1_hotel_245x12) ; r1 hotel Shelf 32Pos Microplate site 12
  (is-labware r1_hotel_245x13) ; r1 hotel Shelf 32Pos Microplate site 13
  (is-labware r1_hotel_245x14) ; r1 hotel Shelf 32Pos Microplate site 14
  (is-labware r1_hotel_245x15) ; r1 hotel Shelf 32Pos Microplate site 15
  (is-labware r1_hotel_245x16) ; r1 hotel Shelf 32Pos Microplate site 16
  (is-labware r1_hotel_245x17) ; r1 hotel Shelf 32Pos Microplate site 17
  (is-labware r1_hotel_245x18) ; r1 hotel Shelf 32Pos Microplate site 18
  (is-labware r1_hotel_245x19) ; r1 hotel Shelf 32Pos Microplate site 19
  (is-labware r1_hotel_245x2) ; r1 hotel Shelf 32Pos Microplate site 2
  (is-labware r1_hotel_245x20) ; r1 hotel Shelf 32Pos Microplate site 20
  (is-labware r1_hotel_245x21) ; r1 hotel Shelf 32Pos Microplate site 21
  (is-labware r1_hotel_245x22) ; r1 hotel Shelf 32Pos Microplate site 22
  (is-labware r1_hotel_245x23) ; r1 hotel Shelf 32Pos Microplate site 23
  (is-labware r1_hotel_245x24) ; r1 hotel Shelf 32Pos Microplate site 24
  (is-labware r1_hotel_245x25) ; r1 hotel Shelf 32Pos Microplate site 25
  (is-labware r1_hotel_245x26) ; r1 hotel Shelf 32Pos Microplate site 26
  (is-labware r1_hotel_245x27) ; r1 hotel Shelf 32Pos Microplate site 27
  (is-labware r1_hotel_245x28) ; r1 hotel Shelf 32Pos Microplate site 28
  (is-labware r1_hotel_245x29) ; r1 hotel Shelf 32Pos Microplate site 29
  (is-labware r1_hotel_245x3) ; r1 hotel Shelf 32Pos Microplate site 3
  (is-labware r1_hotel_245x30) ; r1 hotel Shelf 32Pos Microplate site 30
  (is-labware r1_hotel_245x31) ; r1 hotel Shelf 32Pos Microplate site 31
  (is-labware r1_hotel_245x32) ; r1 hotel Shelf 32Pos Microplate site 32
  (is-labware r1_hotel_245x4) ; r1 hotel Shelf 32Pos Microplate site 4
  (is-labware r1_hotel_245x5) ; r1 hotel Shelf 32Pos Microplate site 5
  (is-labware r1_hotel_245x6) ; r1 hotel Shelf 32Pos Microplate site 6
  (is-labware r1_hotel_245x7) ; r1 hotel Shelf 32Pos Microplate site 7
  (is-labware r1_hotel_245x8) ; r1 hotel Shelf 32Pos Microplate site 8
  (is-labware r1_hotel_245x9) ; r1 hotel Shelf 32Pos Microplate site 9
  (is-labware r1_hotel_85x1) ; r1 hotel Hotel 5Pos DeepWell site 1
  (is-labware r1_hotel_85x2) ; r1 hotel Hotel 5Pos DeepWell site 2
  (is-labware r1_hotel_85x3) ; r1 hotel Hotel 5Pos DeepWell site 3
  (is-labware r1_hotel_85x4) ; r1 hotel Hotel 5Pos DeepWell site 4
  (is-labware r1_hotel_85x5) ; r1 hotel Hotel 5Pos DeepWell site 5
  (is-labware sourcePlate1)
  (is-model m001) ; D-BSSE 96 Well DWP
  (is-model m002) ; D-BSSE 96 Well PCR Plate
  (is-model offsiteModel)
  (is-model sm1)
  (is-model sm2)
  (is-model sm3)
  (is-peeler r1_peeler) ; RoboPeel
  (is-peelerSpec peelerSpec1)
  (is-pipetter r1_pipetter1) ; r1 LiHa
  (is-plate pcrPlate1)
  (is-plate sourcePlate1)
  (is-plateModel m001) ; D-BSSE 96 Well DWP
  (is-plateModel m002) ; D-BSSE 96 Well PCR Plate
  (is-sealer r1_sealer) ; RoboSeal
  (is-sealerSpec sealerSpec1)
  (is-site offsite) ; offsite
  (is-site r1_bench_001x1) ; r1 bench Wash Station Clean site 1
  (is-site r1_bench_001x2) ; r1 bench Wash Station Clean site 2
  (is-site r1_bench_001x3) ; r1 bench Wash Station Clean site 3
  (is-site r1_bench_002x1) ; r1 bench Wash Station Dirty site 1
  (is-site r1_bench_002x2) ; r1 bench Wash Station Dirty site 2
  (is-site r1_bench_002x3) ; r1 bench Wash Station Dirty site 3
  (is-site r1_bench_003x1) ; r1 bench LI - Trough 3Pos 100ml site 1
  (is-site r1_bench_003x2) ; r1 bench LI - Trough 3Pos 100ml site 2
  (is-site r1_bench_003x3) ; r1 bench LI - Trough 3Pos 100ml site 3
  (is-site r1_bench_004x1) ; r1 bench Cooled 8Pos*15ml 8Pos*50ml site 1
  (is-site r1_bench_004x2) ; r1 bench Cooled 8Pos*15ml 8Pos*50ml site 2
  (is-site r1_bench_007x1) ; r1 bench Trough 1000ml site 1
  (is-site r1_bench_009x1) ; r1 bench Downholder site 1
  (is-site r1_bench_009x2) ; r1 bench Downholder site 2
  (is-site r1_bench_010x1) ; r1 bench MP 2Pos H+P Shake site 1
  (is-site r1_bench_010x2) ; r1 bench MP 2Pos H+P Shake site 2
  (is-site r1_bench_010x3) ; r1 bench MP 2Pos H+P Shake site 3
  (is-site r1_bench_010x4) ; r1 bench MP 2Pos H+P Shake site 4
  (is-site r1_bench_016x1) ; r1 bench Block 20Pos site 1
  (is-site r1_bench_017x1) ; r1 bench MP 3Pos Cooled 1 PCR site 1
  (is-site r1_bench_017x2) ; r1 bench MP 3Pos Cooled 1 PCR site 2
  (is-site r1_bench_017x3) ; r1 bench MP 3Pos Cooled 1 PCR site 3
  (is-site r1_bench_017x4) ; r1 bench MP 3Pos Cooled 1 PCR site 4
  (is-site r1_bench_024x1) ; r1 bench MP 3Pos Cooled 2 PCR site 1
  (is-site r1_bench_024x2) ; r1 bench MP 3Pos Cooled 2 PCR site 2
  (is-site r1_bench_024x3) ; r1 bench MP 3Pos Cooled 2 PCR site 3
  (is-site r1_bench_024x4) ; r1 bench MP 3Pos Cooled 2 PCR site 4
  (is-site r1_bench_024x5) ; r1 bench MP 3Pos Cooled 2 PCR site 5
  (is-site r1_bench_024x6) ; r1 bench MP 3Pos Cooled 2 PCR site 6
  (is-site r1_bench_033x1) ; r1 bench Te-VacS site 1
  (is-site r1_bench_033x2) ; r1 bench Te-VacS site 2
  (is-site r1_bench_033x3) ; r1 bench Te-VacS site 3
  (is-site r1_bench_033x4) ; r1 bench Te-VacS site 4
  (is-site r1_bench_033x5) ; r1 bench Te-VacS site 5
  (is-site r1_bench_033x6) ; r1 bench Te-VacS site 6
  (is-site r1_bench_033x7) ; r1 bench Te-VacS site 7
  (is-site r1_bench_033x8) ; r1 bench Te-VacS site 8
  (is-site r1_bench_033x9) ; r1 bench Te-VacS site 9
  (is-site r1_bench_061x1) ; r1 bench ReGrip Station site 1
  (is-site r1_device_-1x1) ; r1 device System site 1
  (is-site r1_device_126x1) ; r1 device Infinite M200 site 1
  (is-site r1_device_214x1) ; r1 device Symbol954 site 1
  (is-site r1_device_234x1) ; r1 device TRobot1 site 1
  (is-site r1_device_235x1) ; r1 device TRobot2 site 1
  (is-site r1_device_236x1) ; r1 device RoboSeal site 1
  (is-site r1_device_237x1) ; r1 device RoboPeel site 1
  (is-site r1_device_65x1) ; r1 device Centrifuge site 1
  (is-site r1_hotel_245x1) ; r1 hotel Shelf 32Pos Microplate site 1
  (is-site r1_hotel_245x10) ; r1 hotel Shelf 32Pos Microplate site 10
  (is-site r1_hotel_245x11) ; r1 hotel Shelf 32Pos Microplate site 11
  (is-site r1_hotel_245x12) ; r1 hotel Shelf 32Pos Microplate site 12
  (is-site r1_hotel_245x13) ; r1 hotel Shelf 32Pos Microplate site 13
  (is-site r1_hotel_245x14) ; r1 hotel Shelf 32Pos Microplate site 14
  (is-site r1_hotel_245x15) ; r1 hotel Shelf 32Pos Microplate site 15
  (is-site r1_hotel_245x16) ; r1 hotel Shelf 32Pos Microplate site 16
  (is-site r1_hotel_245x17) ; r1 hotel Shelf 32Pos Microplate site 17
  (is-site r1_hotel_245x18) ; r1 hotel Shelf 32Pos Microplate site 18
  (is-site r1_hotel_245x19) ; r1 hotel Shelf 32Pos Microplate site 19
  (is-site r1_hotel_245x2) ; r1 hotel Shelf 32Pos Microplate site 2
  (is-site r1_hotel_245x20) ; r1 hotel Shelf 32Pos Microplate site 20
  (is-site r1_hotel_245x21) ; r1 hotel Shelf 32Pos Microplate site 21
  (is-site r1_hotel_245x22) ; r1 hotel Shelf 32Pos Microplate site 22
  (is-site r1_hotel_245x23) ; r1 hotel Shelf 32Pos Microplate site 23
  (is-site r1_hotel_245x24) ; r1 hotel Shelf 32Pos Microplate site 24
  (is-site r1_hotel_245x25) ; r1 hotel Shelf 32Pos Microplate site 25
  (is-site r1_hotel_245x26) ; r1 hotel Shelf 32Pos Microplate site 26
  (is-site r1_hotel_245x27) ; r1 hotel Shelf 32Pos Microplate site 27
  (is-site r1_hotel_245x28) ; r1 hotel Shelf 32Pos Microplate site 28
  (is-site r1_hotel_245x29) ; r1 hotel Shelf 32Pos Microplate site 29
  (is-site r1_hotel_245x3) ; r1 hotel Shelf 32Pos Microplate site 3
  (is-site r1_hotel_245x30) ; r1 hotel Shelf 32Pos Microplate site 30
  (is-site r1_hotel_245x31) ; r1 hotel Shelf 32Pos Microplate site 31
  (is-site r1_hotel_245x32) ; r1 hotel Shelf 32Pos Microplate site 32
  (is-site r1_hotel_245x4) ; r1 hotel Shelf 32Pos Microplate site 4
  (is-site r1_hotel_245x5) ; r1 hotel Shelf 32Pos Microplate site 5
  (is-site r1_hotel_245x6) ; r1 hotel Shelf 32Pos Microplate site 6
  (is-site r1_hotel_245x7) ; r1 hotel Shelf 32Pos Microplate site 7
  (is-site r1_hotel_245x8) ; r1 hotel Shelf 32Pos Microplate site 8
  (is-site r1_hotel_245x9) ; r1 hotel Shelf 32Pos Microplate site 9
  (is-site r1_hotel_85x1) ; r1 hotel Hotel 5Pos DeepWell site 1
  (is-site r1_hotel_85x2) ; r1 hotel Hotel 5Pos DeepWell site 2
  (is-site r1_hotel_85x3) ; r1 hotel Hotel 5Pos DeepWell site 3
  (is-site r1_hotel_85x4) ; r1 hotel Hotel 5Pos DeepWell site 4
  (is-site r1_hotel_85x5) ; r1 hotel Hotel 5Pos DeepWell site 5
  (is-siteModel offsiteModel)
  (is-siteModel sm1)
  (is-siteModel sm2)
  (is-siteModel sm3)
  (is-thermocycler r1_thermocycler1) ; TRobot1
  (is-thermocyclerSpec thermocyclerSpec1)
  (is-transporter r1_transporter1)
  (is-transporter r1_transporter2)
  (is-transporter userArm)
  (is-transporterSpec r1_transporterSpec0) ; r1 Narrow
  (is-transporterSpec r1_transporterSpec2) ; r1 Wide
  (is-transporterSpec userArmSpec)
  (agent-has-device r1 r1_peeler)
  (agent-has-device r1 r1_pipetter1)
  (agent-has-device r1 r1_sealer)
  (agent-has-device r1 r1_thermocycler1)
  (agent-has-device r1 r1_transporter1)
  (agent-has-device r1 r1_transporter2)
  (agent-has-device user userArm)
  (device-can-model r1_peeler m002)
  (device-can-model r1_sealer m002)
  (device-can-model r1_thermocycler1 m002)
  (device-can-model userArm m001)
  (device-can-model userArm m002)
  (device-can-site r1_peeler r1_device_237x1)
  (device-can-site r1_sealer r1_device_236x1)
  (device-can-site r1_thermocycler1 r1_device_234x1)
  (device-can-spec r1_peeler peelerSpec1)
  (device-can-spec r1_sealer sealerSpec1)
  (device-can-spec r1_thermocycler1 thermocyclerSpec1)
  (device-can-spec r1_transporter1 r1_transporterSpec0)
  (device-can-spec r1_transporter1 r1_transporterSpec2)
  (device-can-spec r1_transporter2 r1_transporterSpec0)
  (device-can-spec r1_transporter2 r1_transporterSpec2)
  (device-can-spec userArm userArmSpec)
  (stackable offsiteModel m001)
  (stackable offsiteModel m002)
  (stackable sm1 m002)
  (stackable sm2 m001)
  (stackable sm2 m002)
  (stackable sm3 m001)
  (model pcrPlate1 m002)
  (model r1_bench_009x1 sm1)
  (model r1_bench_010x1 sm1)
  (model r1_bench_010x2 sm3)
  (model r1_bench_010x4 sm2)
  (model r1_bench_017x1 sm1)
  (model r1_bench_017x3 sm1)
  (model r1_bench_024x1 sm1)
  (model r1_bench_024x3 sm1)
  (model r1_bench_024x5 sm1)
  (model r1_bench_033x3 sm1)
  (model r1_bench_033x6 sm1)
  (model r1_bench_061x1 sm2)
  (model r1_device_214x1 sm2)
  (model r1_device_234x1 sm1)
  (model r1_device_236x1 sm1)
  (model r1_device_237x1 sm1)
  (model r1_device_65x1 sm2)
  (model r1_hotel_245x1 sm1)
  (model r1_hotel_245x10 sm1)
  (model r1_hotel_245x11 sm1)
  (model r1_hotel_245x12 sm1)
  (model r1_hotel_245x13 sm1)
  (model r1_hotel_245x14 sm1)
  (model r1_hotel_245x15 sm1)
  (model r1_hotel_245x16 sm1)
  (model r1_hotel_245x17 sm1)
  (model r1_hotel_245x18 sm1)
  (model r1_hotel_245x19 sm1)
  (model r1_hotel_245x2 sm1)
  (model r1_hotel_245x20 sm1)
  (model r1_hotel_245x21 sm1)
  (model r1_hotel_245x22 sm1)
  (model r1_hotel_245x23 sm1)
  (model r1_hotel_245x24 sm1)
  (model r1_hotel_245x25 sm1)
  (model r1_hotel_245x26 sm1)
  (model r1_hotel_245x27 sm1)
  (model r1_hotel_245x28 sm1)
  (model r1_hotel_245x29 sm1)
  (model r1_hotel_245x3 sm1)
  (model r1_hotel_245x30 sm1)
  (model r1_hotel_245x31 sm1)
  (model r1_hotel_245x32 sm1)
  (model r1_hotel_245x4 sm1)
  (model r1_hotel_245x5 sm1)
  (model r1_hotel_245x6 sm1)
  (model r1_hotel_245x7 sm1)
  (model r1_hotel_245x8 sm1)
  (model r1_hotel_245x9 sm1)
  (model r1_hotel_85x1 sm3)
  (model r1_hotel_85x2 sm3)
  (model r1_hotel_85x3 sm3)
  (model r1_hotel_85x4 sm3)
  (model r1_hotel_85x5 sm3)
  (model sourcePlate1 m001)
  (location pcrPlate1 offsite)
  (location sourcePlate1 offsite)
  (device-can-site r1_pipetter1 r1_bench_010x1)
  (device-can-site r1_pipetter1 r1_bench_010x2)
  (device-can-site r1_pipetter1 r1_bench_010x4)
  (device-can-site r1_pipetter1 r1_bench_017x1)
  (device-can-site r1_pipetter1 r1_bench_017x2)
  (device-can-site r1_pipetter1 r1_bench_017x3)
  (device-can-site r1_pipetter1 r1_bench_017x4)
  (device-spec-can-model r1_peeler peelerSpec1 m002)
  (device-spec-can-model r1_sealer sealerSpec1 m002)
  (transporter-can r1_transporter1 r1_bench_010x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_010x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_010x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_010x4 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_017x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_017x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_017x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_017x4 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_024x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_024x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_024x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_024x4 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_024x5 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_024x6 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x4 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x5 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x6 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x7 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x8 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_033x9 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_061x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_234x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_235x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_65x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_009x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_009x2 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_010x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_010x1 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_010x2 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_010x2 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_010x3 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_010x3 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_010x4 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_010x4 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_017x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_017x2 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_017x3 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_017x4 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_024x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_024x2 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_024x3 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_024x4 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_024x5 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_024x6 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x1 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x2 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x2 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x3 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x3 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x4 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x4 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x5 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x5 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x6 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x6 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x7 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x7 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x8 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x8 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_033x9 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_033x9 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_bench_061x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_bench_061x1 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_device_126x1 r1_transporterSpec2)
  (transporter-can r1_transporter2 r1_device_234x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_device_236x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_device_237x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_device_65x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x1 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x10 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x11 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x12 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x13 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x14 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x15 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x16 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x17 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x18 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x19 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x2 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x20 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x21 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x22 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x23 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x24 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x25 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x26 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x27 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x28 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x29 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x3 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x30 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x31 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x32 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x4 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x5 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x6 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x7 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x8 r1_transporterSpec0)
  (transporter-can r1_transporter2 r1_hotel_245x9 r1_transporterSpec0)
  (transporter-can userArm offsite userArmSpec)
  (transporter-can userArm r1_bench_010x2 userArmSpec)
  (transporter-can userArm r1_hotel_245x1 userArmSpec)
  (transporter-can userArm r1_hotel_245x2 userArmSpec)
 )
 ; tasks
 (
  (distribute2 ?a0001 ?d0002 spec0003 sourcePlate1 pcrPlate1)
  (sealer-run ?a0004 ?d0005 ?spec0006 pcrPlate1 ?s0007)
  (peeler-run ?a0008 ?d0009 ?spec0010 pcrPlate1 ?s0011)
  (distribute2 ?a0012 ?d0013 spec0014 sourcePlate1 pcrPlate1)
  (sealer-run ?a0015 ?d0016 ?spec0017 pcrPlate1 ?s0018)
  (peeler-run ?a0019 ?d0020 ?spec0021 pcrPlate1 ?s0022)
 )
)
