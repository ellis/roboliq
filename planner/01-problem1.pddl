;; a trival problem which will have the user place a plate on the table
(define (problem problem1)
 (:domain liquid-handling-robot)

 (:objects
  p1 - plate
  s1 - site
 )

 (:init
  (site-accepts-p1 s1 p1)
 )

 (:goal (and
  (plate-site p1 s1)
 ))
)
