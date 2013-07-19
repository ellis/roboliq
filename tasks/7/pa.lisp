(defproblem pa domain
 ; initial conditions
 (
  (is-model m2)
  (is-plateModel m2)
  (is-labware shakerSite)
  (is-site shakerSite)
  (is-thermocyclerSpec thermocyclerSpec1)
  (is-transporter r1arm)
  (is-labware s2)
  (is-site s2)
  (is-shakerSpec shakerSpec1)
  (is-thermocycler thermocycler)
  (is-labware s1)
  (is-site s1)
  (is-labware plate1)
  (is-plate plate1)
  (is-model siteModelAll)
  (is-siteModel siteModelAll)
  (is-sealer sealer)
  (is-agent user)
  (is-model siteModel12)
  (is-siteModel siteModel12)
  (is-transporter userArm)
  (is-shaker shaker)
  (is-labware sealerSite)
  (is-site sealerSite)
  (is-labware thermocyclerSite)
  (is-site thermocyclerSite)
  (is-pipetter pipetter)
  (is-model m1)
  (is-plateModel m1)
  (is-model siteModel1)
  (is-siteModel siteModel1)
  (is-agent r1)
  (is-labware offsite)
  (is-site offsite)
  (agent-has-device user userArm)
  (agent-has-device r1 shaker)
  (agent-has-device r1 r1arm)
  (agent-has-device r1 pipetter)
  (agent-has-device r1 sealer)
  (agent-has-device r1 thermocycler)
  (device-can-model r1arm m1)
  (device-can-model r1arm m2)
  (device-can-model thermocycler m1)
  (device-can-model sealer m1)
  (device-can-model userArm m1)
  (device-can-model userArm m2)
  (device-can-model shaker m1)
  (device-can-model shaker m2)
  (device-can-model pipetter m1)
  (device-can-model pipetter m2)
  (device-can-site r1arm s1)
  (device-can-site r1arm s2)
  (device-can-site r1arm sealerSite)
  (device-can-site r1arm thermocyclerSite)
  (device-can-site thermocycler thermocyclerSite)
  (device-can-site sealer sealerSite)
  (device-can-site userArm offsite)
  (device-can-site userArm s1)
  (device-can-site shaker shakerSite)
  (device-can-site pipetter s1)
  (device-can-site pipetter s2)
  (device-can-spec thermocycler thermocyclerSpec1)
  (device-can-spec shaker shakerSpec1)
  (stackable siteModelAll m1)
  (stackable siteModelAll m2)
  (stackable siteModel12 m1)
  (stackable siteModel12 m2)
  (stackable siteModel1 m1)
  (model shakerSite siteModel12)
  (model s2 siteModel12)
  (model s1 siteModel12)
  (model plate1 m1)
  (model thermocyclerSite siteModel1)
  (model sealerSite siteModel1)
  (model offsite siteModelAll)
  (location plate1 offsite)
  ;(agent-is-active user)
  ;(agent-is-active r1)
 )
 ;tasks
 (
  ;(transporter-run user userArm plate1 s1)
  ;(agent-activate r1)
  ;(transporter-run r1 r1arm plate1 sealerSite)
  ;(move-labware plate1 s1)
  ;(transporter-run r1 r1arm plate1 sealerSite)
  ;(move-labware plate1 sealerSite)
  (sealer-run ?a0001 ?d0002 plate1 ?s0003)
 )
)