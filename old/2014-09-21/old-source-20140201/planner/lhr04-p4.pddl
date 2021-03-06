;; Perform pipetting specification `pipetteA` using plates `p1` and `p2`
(define (problem lhr04-p4)
 (:domain lhr04)
 (:requirements :strips :typing :negative-preconditions :equality)

 (:objects
  user - agent ; the human user
  userArm - arm ; the user can also move plates around
  offsite - site ; an off-robot site which is only accessible by the user
  r1 - agent ; robot
  a1 - arm
  s1 - site
  s2 - site
  pipetter - pipetteDevice
  sealer - sealDevice
  sealSite - site
  peeler - peelDevice
  peelSite - site
  p1 - plate
  p2 - plate
  m1 - plateModel
  pipetteA - pipetteSpec
  dummy1 - thermocycleSpec
  dummy2 - thermocycleDevice
 )

 (:init
  ;
  ; generic settings
  ;
  ; user
  (agent-is-active user)
  (agent-has-arm user userArm)
  (arm-can-plateModel userArm m1)
  (arm-can-site userArm offsite)
  (arm-can-site userArm s1)
  ; robot r1
  (agent-has-arm r1 a1)
  (agent-has-pipetteDevice r1 pipetter)
  (arm-can-plateModel a1 m1)
  (arm-can-site a1 s1)
  (arm-can-site a1 s2)
  (arm-can-site a1 sealSite)
  (arm-can-site a1 peelSite)
  (site-is-offsite offsite)
  (site-can-plateModel s1 m1)
  (site-can-plateModel s2 m1)
  (site-can-plateModel sealSite m1)
  (site-can-plateModel peelSite m1)
  ; pipetter
  (pipette-can-site pipetter s1)
  (pipette-can-site pipetter s2)
  (pipette-can-plateModel pipetter m1)
  ; sealer
  (seal-site sealer sealSite)
  (seal-can-plateModel sealer m1)
  (peel-site peeler peelSite)
  ;
  ; problem-specific settings
  ;
  (plate-model p1 m1)
  (plate-site p1 offsite)
  (plate-model p2 m1)
  (plate-site p2 offsite)
 )

 (:goal (and
  ;(plate-is-sealed p1)
  (pipette-done pipetteA)
 ))
)
