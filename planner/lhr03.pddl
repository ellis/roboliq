;; Sepcification in PDDL1 of liquid handling robot domain
;; Assumptions:
;; * single robot
;; * arms move plates

(define (domain lhr03)
 (:requirements :strips :typing :negative-preconditions)
 (:types
  arm ; a robot arm for moving plates
  site ; a site which is accessible to the robot
  plateModel ; a plate model
  plate ; a plate
  vessel ; a plate's well or a tube
  plateLid ; a lid on a plate
  deviceThermocycler ; a thermal cycler for PCR
  
  ;pipetteDevice
  pipetteSpec

  sealDevice ; sealer device

  peelDevice ; peeler device

  thermocycleDevice
  thermocycleSpec ; specification for thermocycling
 )

 (:constants
  elsewhere - site ; an off-robot site which is only accessible by the user
  userArm - arm ; the user can also move plates around
 )

 (:predicates
  (robot-is-running) ; whether the robot is running -- must be off for humans to interact with the sites on the robot
  (robot-is-not-running) ; added because Graphplan apparently can't accept negated goals...
  
  (arm-can-site ?a - arm ?s - site) ; whether the arm can access the site
  (arm-can-plateModel ?a - arm ?m - plateModel) ; whether the arm can handle the plateModel

  (site-accepts-plateModel ?s - site ?m - plateModel) ; whether the site can accept the plate model
  (site-is-occupied ?s - site) ; whether the site has a plate on it
  (site-is-closed ?s - site) ; whether the site is "closed" within a device

  (plate-model ?p - plate ?m - plateModel) ; whether the plate is of the given model
  (plate-site ?p - plate ?s - site) ; whether the plate is at the given site
  (plate-is-sealed ?p - plate) ; whether the plate is sealed

  ;(vessel-plate ?v - vessel ?p - plate) ; whether the vessel is on the given plate

  (pipette-done ?spec - pipetteSpec) ; whether the pipetting sequence has been completed

  (seal-site ?d - sealDevice ?s - site)

  (peel-site ?d - peelDevice ?s - site)

  (thermocycle-site ?s - site) ; whether the given plate is a thermocycler site
  (thermocycle-done ?spec - thermocycleSpec ?p - plate) ; whether the given plate has been thermocycled
 )

 (:action user-plate-site
  :parameters (?p - plate ?m - plateModel ?s2 - site)
  :precondition (and
   (not (robot-is-running))
   (arm-can-plateModel userArm ?m)
   (arm-can-site userArm ?s2)
   (not (site-is-occupied ?s2))
   (not (site-is-closed ?s2))
   (site-accepts-plateModel ?s2 ?m)
   (plate-model ?p ?m)
   (plate-site ?p elsewhere)
  )
  :effect (and
   (site-is-occupied ?s2)
   (not (plate-site ?p elsewhere))
   (plate-site ?p ?s2)
  )
 )

 (:action robot-start
  :parameters ()
  :precondition (and
   (not (robot-is-running))
  )
  :effect (and
   (robot-is-running)
   (not (robot-is-not-running))
  )
 )

 (:action robot-stop
  :parameters ()
  :precondition (and
   (robot-is-running)
  )
  :effect (and
   (not (robot-is-running))
   (robot-is-not-running)
  )
 )

 (:action arm-move-plate
  :parameters (?a - arm ?p - plate ?m - plateModel ?s1 - site ?s2 - site)
  :precondition (and
   ; robot conditions
   (robot-is-running)
   ; device conditions
   (arm-can-plateModel ?a ?m)
   (arm-can-site ?a ?s1)
   (arm-can-site ?a ?s2)
   ; site conditions
   (site-accepts-plateModel ?s2 ?m)
   (not (site-is-closed ?s1))
   (not (site-is-closed ?s2))
   (not (site-is-occupied ?s2))
   ; plate conditions
   (plate-model ?p ?m)
   (plate-site ?p ?s1)
  )
  :effect (and
   (not (site-is-occupied ?s1))
   (not (plate-site ?p ?s1))
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
  )
 )

 (:action seal-run
  :parameters (?d - sealDevice ?p - plate ?m - plateModel ?s - site)
  :precondition (and
   ; robot conditions
   (robot-is-running)
   ; device conditions
   (seal-site ?d ?s)
   ; site conditions
   (site-accepts-plateModel ?s ?m)
   ; plate conditions
   (plate-site ?p ?s)
   (plate-model ?p ?m)
   (not (plate-is-sealed ?p))
  )
  :effect (and
   (plate-is-sealed ?p)
  )
 )

 (:action peel-run
  :parameters (?d - peelDevice ?p - plate ?s - site)
  :precondition (and
   (peel-site ?d ?s)
   (plate-is-sealed ?p)
  )
  :effect (and
   (not (plate-is-sealed ?p))
  )
 )

 (:action thermocycler-run
  :parameters (?p - plate ?s - site)
  :precondition (and
   (site-for-thermocycler ?s)
   (plate-site ?p ?s)
  )
  :effect (and
   (plate-is-thermocycled ?p)
  )
 )

 (:action pipetteA-run
  :parameters (?spec - pipetteSpec ?p1 - plate ?s1 - site)
  :precondition (and
   (robot-is-running)
   (plate-site ?p1 ?s1)
   (not (plate-is-sealed ?p1))
  )
  :effect (pipette-done ?spec)
 )
)
