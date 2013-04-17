(define (domain testd)
 (:requirements :strips :typing :negative-preconditions :equality)
 (:types thing)
 (:predicates
  (state ?a - thing)
 )

 (:action act1
  :parameters (?a ?b)
  :precondition (and
   (not (= ?a ?b))
   (not (state ?a))
  )
  :effect (and
   (not (state ?b))
  )
 )
)
