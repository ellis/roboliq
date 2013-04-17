(define (problem testp)
 (:domain testd)
 (:requirements :strips :typing :negative-preconditions :equality)

 (:objects a - thing b - thing)

 (:init
  (state b)
 )

 (:goal (and
  (not (state b))
 ))
)

