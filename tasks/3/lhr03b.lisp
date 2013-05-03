; Seal with seal-plate
; Want user arm to move from offsite to s1, then robot arm to move from s1 to sealerSite
(defproblem lhr03b lhr03
 ; initial conditions
 (
  ; types
  (is-agent user)
  (is-arm userArm)
  (is-agent r1)
  (is-arm r1arm)
  (is-sealer sealer)
  (is-thermocycler thermocycler)
  (is-thermocyclerSpec thermocyclerSpec1)
  (is-site offsite)
  (is-site s1)
  (is-site sealerSite)
  (is-site thermocyclerSite)
  (is-plate p1)
  (is-plateModel m1)

  ; user
  (agent-is-active user)
  (agent-has-arm user userArm)
  ; user arm
  (arm-can-plateModel userArm m1)
  (arm-can-site userArm offsite)
  (arm-can-site userArm s1)
  ; robot r1
  (agent-is-active r1)
  (agent-has-arm r1 r1arm)
  (agent-has-sealer r1 sealer)
  (agent-has-thermocycler r1 thermocycler)
  ; robot arm
  (arm-can-plateModel r1arm m1)
  (arm-can-site r1arm s1)
  (arm-can-site r1arm sealerSite)
  (arm-can-site r1arm thermocyclerSite)
  ; sealer
  (sealer-site sealer sealerSite)
  (sealer-can-plateModel sealer m1)
  ; thermocycler
  (thermocycler-site thermocycler thermocyclerSite)
  ; sites
  (site-can-plateModel offsite m1)
  (site-can-plateModel s1 m1)
  (site-can-plateModel sealerSite m1)
  (site-can-plateModel thermocyclerSite m1)

  ; Initial state of labware
  (plate-model p1 m1)
  (plate-site p1 offsite)
 )
 ; tasks
 (
  (!thermocycler-open r1 thermocycler)
  (!thermocycler-close r1 thermocycler)
  (!thermocycler-run r1 thermocycler thermocyclerSpec1)
  (thermocycler-open thermocycler)
  (thermocycler-open thermocycler)
  (thermocycler-close thermocycler)
  (thermocycler-close thermocycler)
  ;(thermocycle-plate thermocycler thermocyclerSpec1 p1 s1)
 )
)
