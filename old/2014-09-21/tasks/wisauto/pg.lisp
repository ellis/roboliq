(defproblem pg domain
 ; initial conditions
 (
  (is-agent r1)
  (is-agent user) ; user
  (is-labware offsite) ; offsite
  (is-labware plate1)
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
  (is-model m001) ; 96 Well PlateReader
  (is-model m002) ; 96 Well PCR Plate- Roche
  (is-model m003) ; 96 Well PCR Plate
  (is-model m004) ; 96 Well DeepWell
  (is-model offsiteModel)
  (is-model sm1)
  (is-model sm2)
  (is-model sm3)
  (is-model sm4)
  (is-model sm5)
  (is-model sm6)
  (is-pipetter r1_pipetter1) ; r1 LiHa
  (is-plate plate1)
  (is-plateModel m001) ; 96 Well PlateReader
  (is-plateModel m002) ; 96 Well PCR Plate- Roche
  (is-plateModel m003) ; 96 Well PCR Plate
  (is-plateModel m004) ; 96 Well DeepWell
  (is-shaker r1_shaker) ; Te-Shake 2Pos
  (is-shakerSpec shakerSpec1)
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
  (is-siteModel sm3)
  (is-siteModel sm4)
  (is-siteModel sm5)
  (is-siteModel sm6)
  (is-transporter r1_transporter1)
  (is-transporter r1_transporter2)
  (is-transporter userArm)
  (is-transporterSpec r1_transporterSpec0) ; r1 Narrow
  (is-transporterSpec userArmSpec)
  (agent-has-device r1 r1_pipetter1)
  (agent-has-device r1 r1_shaker)
  (agent-has-device r1 r1_transporter1)
  (agent-has-device r1 r1_transporter2)
  (agent-has-device user userArm)
  (device-can-model r1_shaker m001)
  (device-can-model r1_shaker m004)
  (device-can-model userArm m001)
  (device-can-model userArm m002)
  (device-can-model userArm m003)
  (device-can-model userArm m004)
  (device-can-site r1_shaker r1_device_44x1)
  (device-can-site r1_shaker r1_device_44x2)
  (device-can-spec r1_shaker shakerSpec1)
  (device-can-spec r1_transporter1 r1_transporterSpec0)
  (device-can-spec r1_transporter2 r1_transporterSpec0)
  (device-can-spec userArm userArmSpec)
  (stackable offsiteModel m001)
  (stackable offsiteModel m002)
  (stackable offsiteModel m003)
  (stackable offsiteModel m004)
  (stackable sm1 m004)
  (stackable sm2 m001)
  (stackable sm2 m002)
  (stackable sm2 m003)
  (stackable sm2 m004)
  (stackable sm3 m002)
  (stackable sm3 m003)
  (stackable sm3 m004)
  (stackable sm4 m001)
  (stackable sm4 m004)
  (stackable sm5 m002)
  (stackable sm6 m001)
  (stackable sm6 m003)
  (stackable sm6 m004)
  (model plate1 m001)
  (model r1_bench_035x1 sm3)
  (model r1_bench_035x2 sm3)
  (model r1_bench_035x3 sm3)
  (model r1_bench_041x1 sm3)
  (model r1_bench_041x2 sm3)
  (model r1_bench_041x3 sm3)
  (model r1_bench_047x1 sm6)
  (model r1_bench_047x2 sm6)
  (model r1_bench_047x3 sm6)
  (model r1_device_213x1 sm1)
  (model r1_device_300x1 sm2)
  (model r1_device_301x1 sm1)
  (model r1_device_44x1 sm4)
  (model r1_device_44x2 sm4)
  (model r1_device_87x1 sm3)
  (model r1_hotel_225x1 sm5)
  (model r1_hotel_85x2 sm1)
  (model r1_hotel_85x3 sm1)
  (model r1_hotel_85x4 sm1)
  (model r1_hotel_85x5 sm1)
  (location plate1 offsite)
  (agent-has-device user thermocycler1)
  (device-can-site r1_pipetter1 r1_bench_035x1)
  (device-can-site r1_pipetter1 r1_bench_035x2)
  (device-can-site r1_pipetter1 r1_bench_035x3)
  (is-thermocycler thermocycler1)
  (is-thermocyclerSpec thermocyclerSpec1)
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
  (transporter-can userArm r1_device_44x1 userArmSpec)
  (transporter-can userArm r1_device_44x2 userArmSpec)
 )
 ; tasks
 (
  (shaker-run ?a0001 ?d0002 shakerSpec1 plate1 ?s0003)
 )
)
