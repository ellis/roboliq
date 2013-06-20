; Perform pipette2 task
(defproblem pc domain
 ; initial conditions
 (
  ; types
  (is-agent user)
  (is-arm userArm)
  (is-agent r1)
  (is-arm r1arm)
  (is-pipetter pipetter)
  (is-sealer sealer)
  (is-thermocycler thermocycler)
  (is-site offsite)
  (is-site s1)
  (is-site s2)
  (is-site sealerSite)
  (is-plate p1)
  (is-plate p2)
  (is-plateModel m1)
  (is-plateModel m2)
  (is-thermocycleSpec thermocycleSpec1)

  ; user
  (agent-is-active user)
  (agent-has-arm user userArm)
  ; user arm
  (arm-can-plateModel userArm m1)
  (arm-can-plateModel userArm m2)
  (arm-can-site userArm offsite)
  (arm-can-site userArm s1)
  (arm-can-site userArm s2)
  ; robot r1
  (agent-is-active r1)
  (agent-has-arm r1 r1arm)
  (agent-has-pipetter r1 pipetter)
  (agent-has-sealer r1 sealer)
  ; robot arm
  (arm-can-plateModel r1arm m1)
  (arm-can-site r1arm s1)
  (arm-can-site r1arm sealerSite)
  ; pipetter
  (pipetter-can-plateModel pipetter m1)
  (pipetter-can-plateModel pipetter m2)
  (pipetter-can-site pipetter s1)
  (pipetter-can-site pipetter s2)
  ; sealer
  (sealer-site sealer sealerSite)
  (sealer-can-plateModel sealer m1)
  ; sites
  (site-can-plateModel offsite m1)
  (site-can-plateModel offsite m2)
  (site-can-plateModel s1 m1)
  (site-can-plateModel s2 m2)
  (site-can-plateModel sealerSite m1)

  ; Initial state of labware
  (plate-model p1 m1)
  (plate-site p1 offsite)
  (plate-model p2 m2)
  (plate-site p2 offsite)
 )
 ; tasks
 (
  (pipette2 p1 p2)
  (seal-plate p1)
  ;(thermocycle-plate )
 )
)
