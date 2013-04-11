(define (domain disjunctive-pb)
	(:requirements :strips :disjunctive-preconditions :conditional-effects)
	(:predicates (p1 ?x ?y) (p2 ?x ?y) (p3 ?x ?y))
	(:action dijunctive_action
  		:parameters (?x ?y)
  		:precondition 
  			(and (or 
  				(p1 ?x ?y) 
  				(p2 ?x ?y)
  			))
  		:effect 
  			(and 
  				(when (p1 ?x ?y) (not (p1 ?x ?y)))
  				(when (p2 ?x ?y) (not (p2 ?x ?y)))
  				(p3 ?x ?y)
  			)
  		)
	)
)