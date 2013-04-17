;; Sepcification in PDDL3 of liquid handling robot domain
;; Device types:
;; * ROMA
;; * LIHA
;; * sealer
;; * peeler
;; * thermocycler

(define (domain lhr04)
 (:requirements :strips :typing :negative-preconditions :equality)
 (:types
  agent ; a robot or human
  arm ; a robot arm for moving plates
  site ; a site which is accessible to the robot
  plateModel ; a plate model
  plate ; a plate
  vessel ; a plate's well or a tube
  plateLid ; a lid on a plate

  task ; a task to be completed -- can be used to enforce an ordering on tasks

  pipetteDevice
  pipetteSpec

  sealDevice ; sealer device

  peelDevice ; peeler device

  thermocycleDevice
  thermocycleSpec ; specification for thermocycling
 )

 (:constants
  taskA - task
  taskB - task
  taskC - task
 )

 (:predicates
  (agent-is-active ?a - agent) ; whether the agent can currently be given commands -- robots must be off for humans to interact with the sites on the robot
  (agent-has-arm ?a - agent ?d - arm) ; whether the agent has the given arm
  (agent-has-pipetteDevice ?a - agent ?d - pipetteDevice) ; whether the agent has the given pipette device

  (arm-can-site ?a - arm ?s - site) ; whether the arm can access the site
  (arm-can-plateModel ?a - arm ?m - plateModel) ; whether the arm can handle the plateModel

  (site-can-plateModel ?s - site ?m - plateModel) ; whether the site can accept the plate model
  (site-is-occupied ?s - site) ; whether the site has a plate on it
  (site-is-closed ?s - site) ; whether the site is "closed" within a device
  (site-is-offsite ?s - site) ; whether the site can hold multiple plates

  (plate-is-undetermined ?p) ; whether the plate has not yet been assigned a model
  (plate-model ?p - plate ?m - plateModel) ; whether the plate is of the given model
  (plate-site ?p - plate ?s - site) ; whether the plate is at the given site
  (plate-is-sealed ?p - plate) ; whether the plate is sealed

  ;(vessel-plate ?v - vessel ?p - plate) ; whether the vessel is on the given plate

  (pipette-can-site ?d - pipetteDevice ?s - site)
  (pipette-can-plateModel ?d - pipetteDevice ?m - plateModel)
  (pipette-done ?spec - pipetteSpec) ; whether the pipetting sequence has been completed

  (seal-site ?d - sealDevice ?s - site)
  (seal-can-plateModel ?d - sealDevice ?m - plateModel)

  (peel-site ?d - peelDevice ?s - site)
  (peel-can-plateModel ?d - peelDevice ?m - plateModel)

  (thermocycle-site ?d - thermocycleDevice ?s - site) ; whether the given plate is a thermocycler site
  (thermocycle-can-plateModel ?d - thermocycleDevice ?m - plateModel)
  (thermocycle-done ?spec - thermocycleSpec ?p - plate) ; whether the given plate has been thermocycled

  (taskA-done ?p - plate)
  (taskB-done ?p - plate)
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
   (agent-is-active ?a)
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
   ; site s1
   (not (site-is-offsite ?s1))
   (not (site-is-closed ?s1))
   ; site s2
   (not (site-is-offsite ?s2))
   (site-can-plateModel ?s2 ?m)
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

 (:action arm-move-plate-from-offsite
  :parameters (?a - agent ?d - arm ?p - plate ?m - plateModel ?s1 - site ?s2 - site)
  :precondition (and
   ; agent
   (agent-is-active ?a)
   (agent-has-arm ?a ?d)
   ; device
   (arm-can-plateModel ?d ?m)
   (arm-can-site ?d ?s1)
   (arm-can-site ?d ?s2)
   ; site s1
   (site-is-offsite ?s1)
   (not (site-is-closed ?s1))
   ; site s2
   (not (site-is-offsite ?s2))
   (site-can-plateModel ?s2 ?m)
   (not (site-is-closed ?s2))
   (not (site-is-occupied ?s2))
   ; plate
   (plate-model ?p ?m)
   (plate-site ?p ?s1)
  )
  :effect (and
   (not (plate-site ?p ?s1))
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
  )
 )

 (:action arm-move-plate-to-offsite
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
   (not (site-is-offsite ?s1))
   (site-is-offsite ?s2)
   (site-can-plateModel ?s2 ?m)
   (not (site-is-closed ?s1))
   (not (site-is-closed ?s2))
   ; plate
   (plate-model ?p ?m)
   (plate-site ?p ?s1)
  )
  :effect (and
   (not (site-is-occupied ?s1))
   (not (plate-site ?p ?s1))
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
   (peel-site ?d ?s)
   (peel-can-plateModel ?d ?m)
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

 ;
 ; Tasks which are specific to a given protocol
 ;

 (:action taskA-run-pipette
  :parameters (
   ?a - agent
   ?d - pipetteDevice
   ?p1 - plate
   ?m1 - plateModel
   ?s1 - site
   ?p2 - plate
   ?m2 - plateModel
   ?s2 - site
  )
  :precondition (and
   ; agent
   (agent-is-active ?a)
   (agent-has-pipetteDevice ?a ?d)
   ; device
   (pipette-can-plateModel ?d ?m1)
   (pipette-can-plateModel ?d ?m2)
   (pipette-can-site ?d ?s1)
   (pipette-can-site ?d ?s2)
   ; plate
   (not (= ?p1 ?p2))
   (plate-model ?p1 ?m1)
   (plate-site ?p1 ?s1)
   (plate-model ?p2 ?m2)
   (plate-site ?p2 ?s2)
  )
  :effect (taskA-done ?p1)
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
  :parameters (?a - agent ?d - thermocycleDevice ?spec - thermocycleSpec ?p - plate ?m - plateModel ?s - site ?arm - arm ?s1 - site ?s2 - site)
  :precondition (and
   ; task
   (taskA-done ?p)
   ; agent
   (agent-is-active ?a)
   (agent-has-arm ?a ?arm)
   ; device
   (thermocycle-can-plateModel ?d ?m)
   (thermocycle-site ?d ?s)
   (arm-can-plateModel ?arm ?m)
   (arm-can-site ?arm ?s1)
   (arm-can-site ?arm ?s2)
   ; site conditions
   (site-can-plateModel ?s2 ?m)
   (not (site-is-closed ?s1))
   (not (site-is-closed ?s2))
   (not (site-is-occupied ?s2))
   ; plate
   (plate-model ?p ?m)
   (plate-site ?p ?s1)
   (plate-is-sealed ?p)
  )
  :effect (and
   (thermocycle-done ?spec ?p)
   (not (site-is-occupied ?s1))
   (not (plate-site ?p ?s1))
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
   (taskB-done ?p)
  )
 )
)
