(define (problem X-problem)
  (:domain X)
  (:objects
    CENTRIFUGE_1 - site
    CENTRIFUGE_2 - site
    CENTRIFUGE_3 - site
    CENTRIFUGE_4 - site
    P1 - site
    P2 - site
    P3 - site
    P4 - site
    P4PCR - site
    P5 - site
    P5PCR - site
    R1 - site
    R2 - site
    R3 - site
    R4 - site
    R5 - site
    R6 - site
    READER - site
    REGRIP - site
    ROBOSEAL - site
    T3 - site
    mario - agent
    mario__Centrifuge - centrifuge
    mario__Infinite_M200 - reader
    mario__RoboPeel - peeler
    mario__RoboSeal - peeler
    mario__TRobot1 - thermocycler
    mario__pipetter1 - pipetter
    mario__transporter1 - transporter
    mario__transporter2 - transporter
    mario__transporterSpec0 - transporterSpec
    mario__transporterSpec2 - transporterSpec
    offsite - site
    offsiteModel - siteModel
    plateModel_384_square - model
    plateModel_96_dwp - model
    plateModel_96_nunc_transparent - model
    plateModel_96_pcr - model
    sm1 - siteModel
    sm10 - siteModel
    sm11 - siteModel
    sm12 - siteModel
    sm13 - siteModel
    sm2 - siteModel
    sm3 - siteModel
    sm4 - siteModel
    sm5 - siteModel
    sm6 - siteModel
    sm7 - siteModel
    sm8 - siteModel
    sm9 - siteModel
    troughModel_100ml - model
    tubeHolderModel_1500ul - model
    user - agent
    userArm - transporter
    userArmSpec - transporterSpec
  )
  (:init
    (agent-has-device mario mario__Centrifuge)
    (agent-has-device mario mario__Infinite_M200)
    (agent-has-device mario mario__RoboPeel)
    (agent-has-device mario mario__RoboSeal)
    (agent-has-device mario mario__TRobot1)
    (agent-has-device mario mario__pipetter1)
    (agent-has-device mario mario__transporter1)
    (agent-has-device mario mario__transporter2)
    (agent-has-device user userArm)
    (device-can-model mario__Centrifuge plateModel_96_dwp)
    (device-can-model mario__Centrifuge plateModel_96_nunc_transparent)
    (device-can-model mario__Centrifuge plateModel_96_pcr)
    (device-can-model mario__Infinite_M200 plateModel_384_square)
    (device-can-model mario__Infinite_M200 plateModel_96_nunc_transparent)
    (device-can-model mario__RoboSeal plateModel_384_square)
    (device-can-model mario__RoboSeal plateModel_96_nunc_transparent)
    (device-can-model mario__RoboSeal plateModel_96_pcr)
    (device-can-model userArm plateModel_384_square)
    (device-can-model userArm plateModel_96_dwp)
    (device-can-model userArm plateModel_96_nunc_transparent)
    (device-can-model userArm plateModel_96_pcr)
    (device-can-model userArm troughModel_100ml)
    (device-can-model userArm tubeHolderModel_1500ul)
    (device-can-open-site mario__Centrifuge CENTRIFUGE_1)
    (device-can-open-site mario__Centrifuge CENTRIFUGE_2)
    (device-can-open-site mario__Centrifuge CENTRIFUGE_3)
    (device-can-open-site mario__Centrifuge CENTRIFUGE_4)
    (device-can-open-site mario__Infinite_M200 READER)
    (device-can-site mario__Centrifuge CENTRIFUGE_1)
    (device-can-site mario__Centrifuge CENTRIFUGE_2)
    (device-can-site mario__Centrifuge CENTRIFUGE_3)
    (device-can-site mario__Centrifuge CENTRIFUGE_4)
    (device-can-site mario__Infinite_M200 READER)
    (device-can-site mario__RoboSeal ROBOSEAL)
    (device-can-site mario__pipetter1 P1)
    (device-can-site mario__pipetter1 P2)
    (device-can-site mario__pipetter1 P3)
    (device-can-site mario__pipetter1 P4PCR)
    (device-can-site mario__pipetter1 P5PCR)
    (device-can-site mario__pipetter1 R1)
    (device-can-site mario__pipetter1 R2)
    (device-can-site mario__pipetter1 R3)
    (device-can-site mario__pipetter1 R4)
    (device-can-site mario__pipetter1 R5)
    (device-can-site mario__pipetter1 R6)
    (device-can-site mario__pipetter1 T3)
    (device-can-site r1_pipetter1 r1_bench_010x2)
    (device-can-site r1_pipetter1 r1_bench_010x4)
    (device-can-site r1_pipetter1 r1_bench_017x1)
    (device-can-site r1_pipetter1 r1_bench_017x2)
    (device-can-site r1_pipetter1 r1_bench_017x3)
    (device-can-site r1_pipetter1 r1_bench_017x4)
    (device-can-spec mario__transporter1 mario__transporterSpec0)
    (device-can-spec mario__transporter1 mario__transporterSpec2)
    (device-can-spec mario__transporter2 mario__transporterSpec0)
    (device-can-spec mario__transporter2 mario__transporterSpec2)
    (device-can-spec userArm userArmSpec)
    (model CENTRIFUGE_1 sm6)
    (model CENTRIFUGE_2 sm6)
    (model CENTRIFUGE_3 sm6)
    (model CENTRIFUGE_4 sm6)
    (model P1 sm6)
    (model P2 sm3)
    (model P3 sm8)
    (model P4 sm13)
    (model P4PCR sm7)
    (model P5 sm13)
    (model P5PCR sm7)
    (model R1 sm11)
    (model R2 sm11)
    (model R3 sm11)
    (model R4 sm11)
    (model R5 sm11)
    (model R6 sm11)
    (model READER sm13)
    (model REGRIP sm8)
    (model ROBOSEAL sm2)
    (model T3 sm5)
    (site-closed CENTRIFUGE_1)
    (site-closed CENTRIFUGE_2)
    (site-closed CENTRIFUGE_3)
    (site-closed CENTRIFUGE_4)
    (stackable offsiteModel plateModel_384_square)
    (stackable offsiteModel plateModel_96_dwp)
    (stackable offsiteModel plateModel_96_nunc_transparent)
    (stackable offsiteModel plateModel_96_pcr)
    (stackable offsiteModel troughModel_100ml)
    (stackable offsiteModel tubeHolderModel_1500ul)
    (stackable sm1 plateModel_96_dwp)
    (stackable sm1 plateModel_96_nunc_transparent)
    (stackable sm10 plateModel_96_nunc_transparent)
    (stackable sm10 plateModel_96_pcr)
    (stackable sm11 troughModel_100ml)
    (stackable sm12 plateModel_96_dwp)
    (stackable sm13 plateModel_384_square)
    (stackable sm13 plateModel_96_nunc_transparent)
    (stackable sm2 plateModel_384_square)
    (stackable sm2 plateModel_96_nunc_transparent)
    (stackable sm2 plateModel_96_pcr)
    (stackable sm3 plateModel_384_square)
    (stackable sm3 plateModel_96_dwp)
    (stackable sm3 plateModel_96_nunc_transparent)
    (stackable sm4 plateModel_384_square)
    (stackable sm4 plateModel_96_dwp)
    (stackable sm5 tubeHolderModel_1500ul)
    (stackable sm6 plateModel_96_dwp)
    (stackable sm6 plateModel_96_nunc_transparent)
    (stackable sm6 plateModel_96_pcr)
    (stackable sm7 plateModel_96_pcr)
    (stackable sm8 plateModel_384_square)
    (stackable sm8 plateModel_96_dwp)
    (stackable sm8 plateModel_96_nunc_transparent)
    (stackable sm8 plateModel_96_pcr)
    (stackable sm9 plateModel_384_square)
    (transporter-can mario__transporter1 CENTRIFUGE_1 mario__transporterSpec0)
    (transporter-can mario__transporter1 CENTRIFUGE_2 mario__transporterSpec0)
    (transporter-can mario__transporter1 CENTRIFUGE_3 mario__transporterSpec0)
    (transporter-can mario__transporter1 CENTRIFUGE_4 mario__transporterSpec0)
    (transporter-can mario__transporter1 P1 mario__transporterSpec0)
    (transporter-can mario__transporter1 P2 mario__transporterSpec0)
    (transporter-can mario__transporter1 P3 mario__transporterSpec0)
    (transporter-can mario__transporter1 P4 mario__transporterSpec0)
    (transporter-can mario__transporter1 P4PCR mario__transporterSpec0)
    (transporter-can mario__transporter1 P5 mario__transporterSpec0)
    (transporter-can mario__transporter1 P5PCR mario__transporterSpec0)
    (transporter-can mario__transporter1 REGRIP mario__transporterSpec0)
    (transporter-can mario__transporter2 CENTRIFUGE_1 mario__transporterSpec0)
    (transporter-can mario__transporter2 CENTRIFUGE_2 mario__transporterSpec0)
    (transporter-can mario__transporter2 CENTRIFUGE_3 mario__transporterSpec0)
    (transporter-can mario__transporter2 CENTRIFUGE_4 mario__transporterSpec0)
    (transporter-can mario__transporter2 P1 mario__transporterSpec0)
    (transporter-can mario__transporter2 P2 mario__transporterSpec0)
    (transporter-can mario__transporter2 P2 mario__transporterSpec2)
    (transporter-can mario__transporter2 P3 mario__transporterSpec0)
    (transporter-can mario__transporter2 P3 mario__transporterSpec2)
    (transporter-can mario__transporter2 P4 mario__transporterSpec0)
    (transporter-can mario__transporter2 P4PCR mario__transporterSpec0)
    (transporter-can mario__transporter2 P5 mario__transporterSpec0)
    (transporter-can mario__transporter2 P5PCR mario__transporterSpec0)
    (transporter-can mario__transporter2 READER mario__transporterSpec2)
    (transporter-can mario__transporter2 REGRIP mario__transporterSpec0)
    (transporter-can mario__transporter2 REGRIP mario__transporterSpec2)
    (transporter-can mario__transporter2 ROBOSEAL mario__transporterSpec0)
    (transporter-can userArm P1 userArmSpec)
    (transporter-can userArm P2 userArmSpec)
    (transporter-can userArm P3 userArmSpec)
    (transporter-can userArm P4PCR userArmSpec)
    (transporter-can userArm P5PCR userArmSpec)
    (transporter-can userArm R1 userArmSpec)
    (transporter-can userArm R2 userArmSpec)
    (transporter-can userArm R3 userArmSpec)
    (transporter-can userArm R4 userArmSpec)
    (transporter-can userArm R5 userArmSpec)
    (transporter-can userArm R6 userArmSpec)
    (transporter-can userArm READER userArmSpec)
    (transporter-can userArm REGRIP userArmSpec)
    (transporter-can userArm T3 userArmSpec)
    (transporter-can userArm offsite userArmSpec)
  )
  (:goals (and
  ))
)