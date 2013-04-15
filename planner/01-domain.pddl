;; Sepcification in PDDL1 of liquid handling robot domain

(define (domain liquid-handling-robot)
 (:requirements :strips :typing :negative-preconditions)
 (:types
  robot ; a robot
  site ; a site which is accessible to the robot
  plateModel ; a plate model
  plate ; a plate
  vessel ; a plate's well or a tube
  plateLid ; a lid on a plate
  deviceThermocycler ; a thermal cycler for PCR
 )

 (:predicates
  (robot-is-running ?r) ; whether the robot is running -- must be off for humans to interact with bench
  (site-plateModel ?s - site ?m - plateModel) ; whether the site accepts the given plate model
  (plate-model ?p - plate ?m - plateModel) ; whether the plate is of the given model
  (plate-site ?p - plate ?s - site) ; whether the plate is at the given site
  (vessel-plate ?v - vessel ?p - plate) ; whether the vessel is on the given plate
  
  (site-is-occupied ?s - site) ; whether the site has a plate on it
  (site-accepts-plate ?s - site ?p - plate) ; whether the site can accept the plate
  (site-is-closed ?s - site) ; whether the site is "closed" within a device

  (plate-is-sealed ?p - plate) ; whether the plate is sealed

  (site-for-thermocycler ?s - site) ; whether the given plate is a thermocycler site
  (plate-is-thermocycled ?p - plate) ; whether the given plate has been thermocycled
 )

 (:action user-plate-site
  :parameters (?p - plate ?s - site)
  :precondition (and
   (not (site-is-occupied ?s))
   (site-accepts-plate ?s ?p)
  )
  :effect (and 
   (site-is-occupied ?s)
   (plate-site ?p ?s)
  )
 )

 (:action arm-move-plate
  :parameters (?p - plate ?s1 - site ?s2 - site)
  :precondition (and
   (plate-site ?p ?s1)
   (not (site-is-occupied ?s2))
  )
  :effect (and
   (not (site-is-occupied ?s1))
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
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
)
