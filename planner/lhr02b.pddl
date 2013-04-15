;; Sepcification in PDDL1 of liquid handling robot domain
;; Assumptions:
;; * single robot
;; * arms move plates

(define (domain lhr02)
 (:requirements :strips :typing :negative-preconditions)
 (:types
  arm ; a robot arm for moving plates
  site ; a site which is accessible to the robot
  plateModel ; a plate model
  plate ; a plate
  vessel ; a plate's well or a tube
  plateLid ; a lid on a plate
  deviceThermocycler ; a thermal cycler for PCR
  mix ; a series of pipetting actions
 )

 (:constants
  elsewhere - site ; an off-robot site which is only accessible by the user
  userArm - arm ; the user can also move plates around
 )

 (:predicates
  (robot-is-running) ; whether the robot is running -- must be off for humans to interact with the sites on the robot
  (arm-can-site ?a - arm ?s - site) ; whether the arm can access the site
  (arm-can-plateModel ?a - arm ?m - plateModel) ; whether the arm can handle the plateModel

  (plate-model ?p - plate ?m - plateModel) ; whether the plate is of the given model
  (plate-site ?p - plate ?s - site) ; whether the plate is at the given site
  (vessel-plate ?v - vessel ?p - plate) ; whether the vessel is on the given plate
  
  (site-accepts-plateModel ?s - site ?m - plateModel) ; whether the site can accept the plate model
  (site-is-occupied ?s - site) ; whether the site has a plate on it
  (site-is-closed ?s - site) ; whether the site is "closed" within a device

  (plate-is-sealed ?p - plate) ; whether the plate is sealed

  (mix-done ?x - mix) ; whether the pipetting sequence has been completed

  (site-for-thermocycler ?s - site) ; whether the given plate is a thermocycler site
  (plate-is-thermocycled ?p - plate) ; whether the given plate has been thermocycled
 )

 (:action user-plate-site
  :parameters (?p - plate ?m - plateModel ?s2 - site)
  :precondition (and
   (not (robot-is-running))
   (plate-model ?p ?m)
   (plate-site ?p elsewhere)
   (not (site-is-occupied ?s2))
   (not (site-is-closed ?s2))
   (site-accepts-plateModel ?s2 ?m)
  )
  :effect (and 
   (not (plate-site ?p elsewhere))
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
  )
 )

 (:action robot-start
  :parameters ()
  :precondition ()
  :effect (robot-is-running)
 )
 
 (:action robot-stop
  :parameters ()
  :precondition ()
  :effect (not (robot-is-running))
 )
)
