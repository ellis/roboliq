(define (problem X-problem)
  (:domain X)
  (:objects
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
    ROBOSEAL - site
    T3 - site
    mario - agent
    mario__Infinite_M200 - reader
    mario__pipetter1 - pipetter
    mario__transporter1 - transporter
    mario__transporter2 - transporter
    mario__transporterSpec0 - transporterSpec
    mario__transporterSpec2 - transporterSpec
    offsite - site
    offsiteModel - siteModel
    plate1 - labware
    plateModel_384_square - model
    plateModel_96_dwp - model
    plateModel_96_nunc_transparent - model
    plateModel_96_pcr - model
    sm1 - siteModel
    sm2 - siteModel
    sm3 - siteModel
    sm4 - siteModel
    sm5 - siteModel
    sm6 - siteModel
    sm7 - siteModel
    troughModel_100ml - model
    troughModel_100ml_lowvol_tips - model
    tubeHolderModel_1500ul - model
    user - agent
    userArm - transporter
    userArmSpec - transporterSpec
  )
  (:init
    (agent-has-device mario mario__Infinite_M200)
    (agent-has-device mario mario__pipetter1)
    (agent-has-device mario mario__transporter1)
    (agent-has-device mario mario__transporter2)
    (agent-has-device user userArm)
    (device-can-model userArm plateModel_384_square)
    (device-can-model userArm plateModel_96_dwp)
    (device-can-model userArm plateModel_96_nunc_transparent)
    (device-can-model userArm plateModel_96_pcr)
    (device-can-model userArm troughModel_100ml)
    (device-can-model userArm troughModel_100ml_lowvol_tips)
    (device-can-model userArm tubeHolderModel_1500ul)
    (device-can-open-site mario__Infinite_M200 READER)
    (device-can-site mario__Infinite_M200 READER)
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
    (location plate1 P3)
    (model P1 sm4)
    (model P2 sm1)
    (model P3 sm6)
    (model P4 sm7)
    (model P4PCR sm5)
    (model P5 sm7)
    (model P5PCR sm5)
    (model R1 sm2)
    (model R2 sm2)
    (model R3 sm2)
    (model R4 sm2)
    (model R5 sm2)
    (model R6 sm2)
    (model ROBOSEAL sm5)
    (model T3 sm3)
    (model plate1 plateModel_96_nunc_transparent)
    (stackable offsiteModel plateModel_384_square)
    (stackable offsiteModel plateModel_96_dwp)
    (stackable offsiteModel plateModel_96_nunc_transparent)
    (stackable offsiteModel plateModel_96_pcr)
    (stackable offsiteModel troughModel_100ml)
    (stackable offsiteModel troughModel_100ml_lowvol_tips)
    (stackable offsiteModel tubeHolderModel_1500ul)
    (stackable sm1 plateModel_384_square)
    (stackable sm1 plateModel_96_dwp)
    (stackable sm1 plateModel_96_nunc_transparent)
    (stackable sm2 troughModel_100ml)
    (stackable sm2 troughModel_100ml_lowvol_tips)
    (stackable sm3 tubeHolderModel_1500ul)
    (stackable sm4 plateModel_96_dwp)
    (stackable sm4 plateModel_96_nunc_transparent)
    (stackable sm4 plateModel_96_pcr)
    (stackable sm5 plateModel_96_pcr)
    (stackable sm6 plateModel_384_square)
    (stackable sm6 plateModel_96_dwp)
    (stackable sm6 plateModel_96_nunc_transparent)
    (stackable sm6 plateModel_96_pcr)
    (stackable sm7 plateModel_384_square)
    (stackable sm7 plateModel_96_nunc_transparent)
    (transporter-can mario__transporter1 P1 mario__transporterSpec0)
    (transporter-can mario__transporter1 P2 mario__transporterSpec0)
    (transporter-can mario__transporter1 P3 mario__transporterSpec0)
    (transporter-can mario__transporter1 P4 mario__transporterSpec0)
    (transporter-can mario__transporter1 P4PCR mario__transporterSpec0)
    (transporter-can mario__transporter1 P5 mario__transporterSpec0)
    (transporter-can mario__transporter1 P5PCR mario__transporterSpec0)
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
    (transporter-can userArm T3 userArmSpec)
    (transporter-can userArm offsite userArmSpec)
  )
  (:goals (and
  ))
)