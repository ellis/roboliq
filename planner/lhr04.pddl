;; Sepcification in PDDL3 of liquid handling robot domain
;; Constants:
;; * agents: user, robot1
;; Device types:
;; * sealer
;; * peeler
;; * thermocycler

(define (domain lhr03)
 (:requirements :strips :typing :negative-preconditions :equality)
 (:types
  agent ; a robot or human
  arm ; a robot arm for moving plates
  site ; a site which is accessible to the robot
  plateModel ; a plate model
  plate ; a plate
  vessel ; a plate's well or a tube
  plateLid ; a lid on a plate
  ;pipetteDevice
  pipetteSpec

  sealDevice ; sealer device

  peelDevice ; peeler device

  thermocycleDevice
  thermocycleSpec ; specification for thermocycling
 )

 (:constants
  user - agent ; a human user
  userArm - arm ; the user can also move plates around
  elsewhere - site ; an off-robot site which is only accessible by the user
 )

 (:predicates
  (agent-is-active ?a - agent) ; whether the agent can currently be given commands -- robots must be off for humans to interact with the sites on the robot
  (agent-has-arm ?a - agent ?d - arm) ; whether the agent has the given arm

  (arm-can-site ?a - arm ?s - site) ; whether the arm can access the site
  (arm-can-plateModel ?a - arm ?m - plateModel) ; whether the arm can handle the plateModel

  (site-can-plateModel ?s - site ?m - plateModel) ; whether the site can accept the plate model
  (site-is-occupied ?s - site) ; whether the site has a plate on it
  (site-is-closed ?s - site) ; whether the site is "closed" within a device

  (plate-is-undetermined ?p) ; whether the plate has not yet been assigned a model
  (plate-model ?p - plate ?m - plateModel) ; whether the plate is of the given model
  (plate-site ?p - plate ?s - site) ; whether the plate is at the given site
  (plate-is-sealed ?p - plate) ; whether the plate is sealed

  ;(vessel-plate ?v - vessel ?p - plate) ; whether the vessel is on the given plate

  (pipette-done ?spec - pipetteSpec) ; whether the pipetting sequence has been completed

  (seal-site ?d - sealDevice ?s - site)
  (seal-can-plateModel ?d - sealDevice ?m - plateModel)

  (peel-site ?d - peelDevice ?s - site)
  (peel-can-plateModel ?d - sealDevice ?m - plateModel)

  (thermocycle-site ?s - site) ; whether the given plate is a thermocycler site
  (thermocycle-done ?spec - thermocycleSpec ?p - plate) ; whether the given plate has been thermocycled
 )

 (:action user-plate-site
  :parameters (?p - plate ?m - plateModel ?s2 - site)
  :precondition (and
   ; agent
   (not (agent-is-active r1))
   ; device
   (arm-can-plateModel userArm ?m)
   (arm-can-site userArm ?s2)
   ; site
   (not (site-is-occupied ?s2))
   (not (site-is-closed ?s2))
   (site-accepts-plateModel ?s2 ?m)
   ; plate
   (plate-model ?p ?m)
   (plate-site ?p elsewhere)
  )
  :effect (and
   (site-is-occupied ?s2)
   (not (plate-site ?p elsewhere))
   (plate-site ?p ?s2)
  )
 )

 (:action agent-start
  :parameters (?a - agent)
  :precondition (and
   (not (agent-is-active ?a))
  )
  :effect (and
   (agent-is-active ?a)
  )
 )

 (:action agent-stop
  :parameters (?a - agent)
  :precondition (and
   (robot-is-active)
  )
  :effect (and
   (not (agent-is-active ?a))
  )
 )

 (:action arm-move-plate
  :parameters (?a - agent ?d - arm ?p - plate ?m - plateModel ?s1 - site ?s2 - site)
  :precondition (and
   ; agent
   (agent-is-active ?a)
   (agent-has-arm ?a ?d)
   ; device
   (arm-can-plateModel ?d ?m)
   (arm-can-site ?d ?s1)
   (arm-can-site ?d ?s2)
   ; site conditions
   (not (= ?s1 elsewhere))
   (not (= ?s2 elsewhere))
   (site-can-plateModel ?s2 ?m)
   (not (site-is-closed ?s1))
   (not (site-is-closed ?s2))
   (not (site-is-occupied ?s2))
   ; plate
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
  :parameters (?a - agent ?d - sealDevice ?p - plate ?m - plateModel ?s - site)
  :precondition (and
   ; agent
   (agent-is-active ?a)
   ; device
   (seal-site ?d ?s)
   (seal-can-plateModel ?d ?m)
   ; site
   (site-can-plateModel ?s ?m)
   ; plate
   (plate-site ?p ?s)
   (plate-model ?p ?m)
   (not (plate-is-sealed ?p))
  )
  :effect (and
   (plate-is-sealed ?p)
  )
 )

 (:action peel-run
  :parameters (?a - agent ?d - peelDevice ?p - plate ?m - plateModel ?s - site)
  :precondition (and
   ; agent
   (agent-is-active ?a)
   ; device
   (seal-site ?d ?s)
   (seal-can-plateModel ?d ?m)
   ; site
   (site-can-plateModel ?s ?m)
   ; plate
   (plate-site ?p ?s)
   (plate-model ?p ?m)
   (plate-is-sealed ?p)
  )
  :effect (and
   (not (plate-is-sealed ?p))
  )
 )

 ; This is a complex action which involves:
 ; * opening the thermocycler lid
 ; * move plate from ?s1 to ?s
 ; * close lid
 ; * run the thermocycler
 ; * open lid
 ; * move plate to ?s2
 ; * close lid
 ; This is because the thermocycler lid may not be left open, or we risk crashing an arm against it.
 (:action thermocycler-run
  :parameters (?a - agent ?d - thermocycleDevice ?p - plate ?s - site ?arm - arm ?s1 - site ?s2 - site)
  :precondition (and
   ; agent
   (agent-is-active ?a)
   (agent-has-arm ?a ?d)
   ; device
   (thermocycle-can-plateModel ?d ?m)
   (thermocycle-site ?d ?s)
   (arm-can-plateModel ?a ?m)
   (arm-can-site ?a ?s1)
   (arm-can-site ?a ?s2)
   ; site conditions
   (site-can-plateModel ?s2 ?m)
   (not (site-is-closed ?s1))
   (not (site-is-closed ?s2))
   (not (site-is-occupied ?s2))
   ; plate
   (plate-model ?p ?m)
   (plate-site ?p ?s1)
  )
  :effect (and
   (thermocycle-done ?d ?p)
   (not (site-is-occupied ?s1))
   (not (plate-site ?p ?s1))
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
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
