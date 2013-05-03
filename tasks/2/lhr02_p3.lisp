; Move p1 from offsite to s1 using a method
;; Want user arm to move from offsite to s1, then robot arm to move from s2 to sealerSite
(defproblem lhr02_p3 lhr02
 ; initial conditions
 (
  (is-agent user)
  (is-arm userArm)
  (is-agent r1)
  (is-arm r1arm)
  (is-sealer sealer)
  (is-site offsite)
  (is-site s1)
  (is-site sealerSite)
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
  ; robot arm
  (arm-can-plateModel r1arm m1)
  (arm-can-site r1arm s1)
  (arm-can-site r1arm sealerSite)
  ; sealer
  (sealer-site sealer sealerSite)
  (sealer-can-plateModel sealer m1)
  ; sites
  (site-can-plateModel offsite m1)
  (site-can-plateModel s1 m1)
  (site-can-plateModel sealerSite m1)

  (plate-model p1 m1)
  (plate-site p1 offsite)
 )
 ; tasks
 (
  (set-plate-site p1 m1 s1)
  (set-plate-site p1 m1 sealerSite)
  (seal-plate-adpms r1 sealer p1 m1 sealerSite)
  ;(seal-plate p1)
  ;(!seal-plate-adpms r1 sealer p1 m1 sealerSite)
 )
)
