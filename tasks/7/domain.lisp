(defdomain domain (
 (:operator (!sealer-run ?a ?d ?p ?m ?s)
  ; preconditions
  (
   (is-agent ?a)
   (is-sealer ?d)
   (is-plate ?p)
   (is-plateModel ?m)
   (is-site ?s)
   ; agent
   ;(agent-is-active ?a)
   (agent-has-device ?a ?d)
   ; device
   (device-can-site ?d ?s)
   (device-can-model ?d ?m)
   ; plate
   (model ?p ?m)
   (location ?p ?s)
   (not (plate-is-sealed ?p))
   ; site
   (model ?s ?sm)
   (stackable ?sm ?m)
  )
  ; delete list
  ()
  ; add list
  (
   (plate-is-sealed ?p)
  )
 )

 (:method (sealer-run ?a ?d ?p ?m ?s)
  sealer-run-NULL
  (
   (plate-is-sealed ?p)
  )
  ()

  sealer-run-DO
  ()
  ;((set-plate-site ?p ?m ?s) (!sealer-run ?a ?d ?p ?m ?s))
  ((!sealer-run ?a ?d ?p ?m ?s))
 )
))
