(defproblem pa domain
 ; initial conditions
 (
  (is-agent r1)
  (is-agent user) ; user
  (is-labware offsite) ; offsite
  (is-labware pcrPlate1)
  (is-labware r1_bench_001x1) ; r1 bench Washstation 2Grid Trough DiTi site 1
  (is-labware r1_bench_001x2) ; r1 bench Washstation 2Grid Trough DiTi site 2
  (is-labware r1_bench_001x3) ; r1 bench Washstation 2Grid Trough DiTi site 3
  (is-labware r1_bench_001x4) ; r1 bench Washstation 2Grid Trough DiTi site 4
  (is-labware r1_bench_001x5) ; r1 bench Washstation 2Grid Trough DiTi site 5
  (is-labware r1_bench_001x6) ; r1 bench Washstation 2Grid Trough DiTi site 6
  (is-labware r1_bench_001x7) ; r1 bench Washstation 2Grid Trough DiTi site 7
  (is-labware r1_bench_001x8) ; r1 bench Washstation 2Grid Trough DiTi site 8
  (is-labware r1_bench_035x1) ; r1 bench MP 3Pos Fixed PCR site 1
  (is-labware r1_bench_035x2) ; r1 bench MP 3Pos Fixed PCR site 2
  (is-labware r1_bench_035x3) ; r1 bench MP 3Pos Fixed PCR site 3
  (is-labware r1_bench_041x1) ; r1 bench MP 3Pos Fixed 2+clips site 1
  (is-labware r1_bench_041x2) ; r1 bench MP 3Pos Fixed 2+clips site 2
  (is-labware r1_bench_041x3) ; r1 bench MP 3Pos Fixed 2+clips site 3
  (is-labware r1_bench_047x1) ; r1 bench MP 3Pos Fixed site 1
  (is-labware r1_bench_047x2) ; r1 bench MP 3Pos Fixed site 2
  (is-labware r1_bench_047x3) ; r1 bench MP 3Pos Fixed site 3
  (is-labware r1_bench_059x1) ; r1 bench MCA384 DiTi Carrier site 1
  (is-labware r1_bench_059x2) ; r1 bench MCA384 DiTi Carrier site 2
  (is-labware r1_device_-1x1) ; r1 device System site 1
  (is-labware r1_device_213x1) ; r1 device Te-Link site 1
  (is-labware r1_device_219x1) ; r1 device Carousel DiTi 1000 site 1
  (is-labware r1_device_299x1) ; r1 device Carousel MTP site 1
  (is-labware r1_device_300x1) ; r1 device Carousel DiTi 200 site 1
  (is-labware r1_device_301x1) ; r1 device Carousel DiTi 10 site 1
  (is-labware r1_device_44x1) ; r1 device Te-Shake 2Pos site 1
  (is-labware r1_device_44x2) ; r1 device Te-Shake 2Pos site 2
  (is-labware r1_device_87x1) ; r1 device BarcodeScanner site 1
  (is-labware r1_hotel_225x1) ; r1 hotel Reader site 1
  (is-labware r1_hotel_85x1) ; r1 hotel Hotel 5Pos DeepWell site 1
  (is-labware r1_hotel_85x2) ; r1 hotel Hotel 5Pos DeepWell site 2
  (is-labware r1_hotel_85x3) ; r1 hotel Hotel 5Pos DeepWell site 3
  (is-labware r1_hotel_85x4) ; r1 hotel Hotel 5Pos DeepWell site 4
  (is-labware r1_hotel_85x5) ; r1 hotel Hotel 5Pos DeepWell site 5
  (is-labware r1_hotel_93x1) ; r1 hotel Hotel 5Pos SPE site 1
  (is-labware r1_hotel_93x2) ; r1 hotel Hotel 5Pos SPE site 2
  (is-labware r1_hotel_93x3) ; r1 hotel Hotel 5Pos SPE site 3
  (is-labware r1_hotel_93x4) ; r1 hotel Hotel 5Pos SPE site 4
  (is-labware r1_hotel_93x5) ; r1 hotel Hotel 5Pos SPE site 5
  (is-labware reagentPlate1)
  (is-model m001) ; 96 Well PCR Plate
  (is-model m002) ; 96 Well DeepWell
  (is-model offsiteModel)
  (is-model sm1)
  (is-model sm2)
  (is-pipetter r1_pipetter1) ; r1 LiHa
  (is-plate pcrPlate1)
  (is-plate reagentPlate1)
  (is-plateModel m001) ; 96 Well PCR Plate
  (is-plateModel m002) ; 96 Well DeepWell
  (is-site offsite) ; offsite
  (is-site r1_bench_001x1) ; r1 bench Washstation 2Grid Trough DiTi site 1
  (is-site r1_bench_001x2) ; r1 bench Washstation 2Grid Trough DiTi site 2
  (is-site r1_bench_001x3) ; r1 bench Washstation 2Grid Trough DiTi site 3
  (is-site r1_bench_001x4) ; r1 bench Washstation 2Grid Trough DiTi site 4
  (is-site r1_bench_001x5) ; r1 bench Washstation 2Grid Trough DiTi site 5
  (is-site r1_bench_001x6) ; r1 bench Washstation 2Grid Trough DiTi site 6
  (is-site r1_bench_001x7) ; r1 bench Washstation 2Grid Trough DiTi site 7
  (is-site r1_bench_001x8) ; r1 bench Washstation 2Grid Trough DiTi site 8
  (is-site r1_bench_035x1) ; r1 bench MP 3Pos Fixed PCR site 1
  (is-site r1_bench_035x2) ; r1 bench MP 3Pos Fixed PCR site 2
  (is-site r1_bench_035x3) ; r1 bench MP 3Pos Fixed PCR site 3
  (is-site r1_bench_041x1) ; r1 bench MP 3Pos Fixed 2+clips site 1
  (is-site r1_bench_041x2) ; r1 bench MP 3Pos Fixed 2+clips site 2
  (is-site r1_bench_041x3) ; r1 bench MP 3Pos Fixed 2+clips site 3
  (is-site r1_bench_047x1) ; r1 bench MP 3Pos Fixed site 1
  (is-site r1_bench_047x2) ; r1 bench MP 3Pos Fixed site 2
  (is-site r1_bench_047x3) ; r1 bench MP 3Pos Fixed site 3
  (is-site r1_bench_059x1) ; r1 bench MCA384 DiTi Carrier site 1
  (is-site r1_bench_059x2) ; r1 bench MCA384 DiTi Carrier site 2
  (is-site r1_device_-1x1) ; r1 device System site 1
  (is-site r1_device_213x1) ; r1 device Te-Link site 1
  (is-site r1_device_219x1) ; r1 device Carousel DiTi 1000 site 1
  (is-site r1_device_299x1) ; r1 device Carousel MTP site 1
  (is-site r1_device_300x1) ; r1 device Carousel DiTi 200 site 1
  (is-site r1_device_301x1) ; r1 device Carousel DiTi 10 site 1
  (is-site r1_device_44x1) ; r1 device Te-Shake 2Pos site 1
  (is-site r1_device_44x2) ; r1 device Te-Shake 2Pos site 2
  (is-site r1_device_87x1) ; r1 device BarcodeScanner site 1
  (is-site r1_hotel_225x1) ; r1 hotel Reader site 1
  (is-site r1_hotel_85x1) ; r1 hotel Hotel 5Pos DeepWell site 1
  (is-site r1_hotel_85x2) ; r1 hotel Hotel 5Pos DeepWell site 2
  (is-site r1_hotel_85x3) ; r1 hotel Hotel 5Pos DeepWell site 3
  (is-site r1_hotel_85x4) ; r1 hotel Hotel 5Pos DeepWell site 4
  (is-site r1_hotel_85x5) ; r1 hotel Hotel 5Pos DeepWell site 5
  (is-site r1_hotel_93x1) ; r1 hotel Hotel 5Pos SPE site 1
  (is-site r1_hotel_93x2) ; r1 hotel Hotel 5Pos SPE site 2
  (is-site r1_hotel_93x3) ; r1 hotel Hotel 5Pos SPE site 3
  (is-site r1_hotel_93x4) ; r1 hotel Hotel 5Pos SPE site 4
  (is-site r1_hotel_93x5) ; r1 hotel Hotel 5Pos SPE site 5
  (is-siteModel offsiteModel)
  (is-siteModel sm1)
  (is-siteModel sm2)
  (is-transporter r1_transporter1)
  (is-transporter r1_transporter2)
  (is-transporter userArm)
  (is-transporterSpec r1_transporterSpec0) ; r1 Narrow
  (is-transporterSpec userArmSpec)
  (agent-has-device r1 r1_pipetter1)
  (agent-has-device r1 r1_transporter1)
  (agent-has-device r1 r1_transporter2)
  (agent-has-device user userArm)
  (device-can-model userArm m001)
  (device-can-model userArm m002)
  (device-can-spec r1_transporter1 r1_transporterSpec0)
  (device-can-spec r1_transporter2 r1_transporterSpec0)
  (device-can-spec userArm userArmSpec)
  (stackable offsiteModel m001)
  (stackable offsiteModel m002)
  (stackable sm1 m001)
  (stackable sm1 m002)
  (stackable sm2 m002)
  (model pcrPlate1 m001)
  (model r1_bench_035x1 sm1)
  (model r1_bench_035x2 sm1)
  (model r1_bench_035x3 sm1)
  (model r1_bench_041x1 sm1)
  (model r1_bench_041x2 sm1)
  (model r1_bench_041x3 sm1)
  (model r1_bench_047x1 sm1)
  (model r1_bench_047x2 sm1)
  (model r1_bench_047x3 sm1)
  (model r1_device_213x1 sm2)
  (model r1_device_300x1 sm1)
  (model r1_device_301x1 sm2)
  (model r1_device_44x1 sm2)
  (model r1_device_44x2 sm2)
  (model r1_device_87x1 sm1)
  (model r1_hotel_85x2 sm2)
  (model r1_hotel_85x3 sm2)
  (model r1_hotel_85x4 sm2)
  (model r1_hotel_85x5 sm2)
  (model reagentPlate1 m001)
  (location pcrPlate1 offsite)
  (location reagentPlate1 offsite)
  (transporter-can r1_transporter1 r1_bench_035x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_035x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_035x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_041x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_041x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_bench_041x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_213x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_219x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_299x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_300x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_301x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_44x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_device_44x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_225x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_85x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_85x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_85x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_85x4 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_85x5 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_93x1 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_93x2 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_93x3 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_93x4 r1_transporterSpec0)
  (transporter-can r1_transporter1 r1_hotel_93x5 r1_transporterSpec0)
  (transporter-can userArm offsite userArmSpec)
  (transporter-can userArm r1_bench_035x1 userArmSpec)
  (transporter-can userArm r1_bench_035x2 userArmSpec)
  (transporter-can userArm r1_bench_035x3 userArmSpec)
 ; user initial conditions
  (device-can-site r1_pipetter1 r1_bench_035x1)
  (device-can-site r1_pipetter1 r1_bench_035x2)
  (device-can-site r1_pipetter1 r1_bench_035x3)

 )
 ; tasks
 (
  (distribute2 ?a0001 ?d0002 spec0003 reagentPlate1 pcrPlate1)
 )
)
