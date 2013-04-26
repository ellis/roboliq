; Want user arm to move from s1 to s2, then robot arm to move from s2 to s3
(defproblem lhr01_p4 lhr01
 ; initial conditions
 (
  (is-agent user)
  (is-arm userArm)
  (is-agent r1)
  (is-arm r1arm)
  (is-plate p1)
  (is-plateModel m1)
  (is-site s1)
  (is-site s2)
  (is-site s3)
  (agent-is-active user)
  (agent-is-active r1)
  (agent-has-arm user userArm)
  (agent-has-arm r1 r1arm)
  (arm-can-plateModel userArm m1)
  (arm-can-plateModel r1arm m1)
  (arm-can-site userArm s1)
  (arm-can-site userArm s2)
  (arm-can-site r1arm s2)
  (arm-can-site r1arm s3)
  (site-can-plateModel s1 m1)
  (site-can-plateModel s2 m1)
  (site-can-plateModel s3 m1)
  (plate-model p1 m1)
  (plate-site p1 s1)
 )
 ; tasks
 (
  (set-plate-site p1 m1 s3)
 )
)
