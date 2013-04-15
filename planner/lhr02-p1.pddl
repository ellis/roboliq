;; a trival problem which will have the user place a plate on the table
(define (problem lhr02-p1)
 (:domain liquid-handling-robot)
 (:requirements :strips :typing :negative-preconditions)

 (:objects
  elsewhere - site ; an off-robot site which is only accessible by the user
  userArm - arm ; the user can also move plates around
  a1 - arm
  p1 - plate
  m1 - plateModel
  s1 - site
  s2 - site
  mixA - mix
 )

 (:init
  (arm-can-plateModel a1 m1)
  (arm-can-site a1 s1)
  (arm-can-site a1 s2)
  (arm-can-site userArm s1)
  (site-accepts-plateModel s1 m1)
  (plate-model p1 m1)
  (plate-site p1 elsewhere)
 )

 (:goal (and
  (plate-site p1 s1)
;  (mix-done mixA)
;  (not (robot-running))
 ))
)
