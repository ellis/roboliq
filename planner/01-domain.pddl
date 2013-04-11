;; Sepcification in PDDL1 of liquid handling robot domain

(define (domain liquid-handling-robot)
 (:requirements :strips :typing)
 (:types
  site ; a site which is accessible to the robot
  plateModel ; a plate model
  plate ; a plate
  vessel ; a plate's well or a tube
  plateLid ; a lid on a plate
  devicePcr ; a thermal cycler
 )

 (:predicates
  (site-plateModel ?s - site ?m - plateModel) ; whether the site accepts the given plate model
  (plate-model ?p - plate ?m - plateModel) ; whether the plate is of the given model
  (plate-site ?p - plate ?s - site) ; whether the plate is at the given site
  (vessel-plate ?v - vessel ?p -plate) ; whether the vessel is on the given plate
  
  (site-is-occupied ?s - site) ; whether the site has a plate on it
  (site-accepts-plate ?s - site ?p - plate) ; whether the site can accept the plate
 )

 (:action user-plate-site
  :parameters (?p - plate ?s - site)
  :precondition (and
   (not (site-is-occupied ?s)) (site-accepts-plate ?s ?p)
  )
  :effect (and (
   (site-is-occupied ?s)
   (plate-site ?p ?s)
  ))
 )
)
