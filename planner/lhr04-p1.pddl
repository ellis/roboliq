;; Seal a plate
(define (problem lhr03-p2)
 (:domain lhr03)
 (:requirements :strips :typing :negative-preconditions)

 (:objects
  elsewhere - site ; an off-robot site which is only accessible by the user
  userArm - arm ; the user can also move plates around
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
  (robot-is-not-running)
  (arm-can-plateModel a1 m1)
  (arm-can-site a1 s1)
  (arm-can-site a1 s2)
  (arm-can-site a1 sealSite)
  (arm-can-site a1 peelSite)
  (arm-can-plateModel userArm m1)
  (arm-can-site userArm s1)
  (site-accepts-plateModel s1 m1)
  (site-accepts-plateModel s2 m1)
  (site-accepts-plateModel sealSite m1)
  (site-accepts-plateModel peelSite m1)
  (seal-site sealer sealSite)
  (peel-site peeler peelSite)
  (plate-model p1 m1)
  (plate-site p1 elsewhere)
  ;(plate-site p1 sealSite)
  ;(plate-is-sealed p1)
 )

 (:goal (and
  (pipette-done pipetteA)
  (plate-is-sealed p1)
 ))
)
