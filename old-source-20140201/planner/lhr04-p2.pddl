;; Place plate `p1` on site `s2`
(define (problem lhr04-p1)
 (:domain lhr04)
 (:requirements :strips :typing :negative-preconditions)

 (:objects
  user - agent ; the human user
  userArm - arm ; the user can also move plates around
  offsite - site ; an off-robot site which is only accessible by the user
  r1 - agent ; robot
  a1 - arm
  s1 - site
  s2 - site
  sealer - sealDevice
  sealSite - site
  peeler - peelDevice
  peelSite - site
  p1 - plate
  m1 - plateModel
  pipetteA - pipetteSpec
 )

 (:init
  ; user
  (agent-is-active user)
  (agent-has-arm user userArm)
  (arm-can-plateModel userArm m1)
  (arm-can-site userArm offsite)
  (arm-can-site userArm s1)
  ; robot r1
  (agent-has-arm r1 a1)
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
  (seal-site sealer sealSite)
  (peel-site peeler peelSite)
  (plate-model p1 m1)
  (plate-site p1 offsite)
  ;(plate-site p1 sealSite)
  ;(plate-is-sealed p1)
 )

 (:goal (and
  (plate-site p1 s2)
 ))
)
