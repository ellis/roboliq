(defdomain domain (
 (:operator (!arm-move-plate ?a ?d ?p ?s2)
  ; preconditions
  (
   (is-agent ?a)
   (is-arm ?d)
   (is-plate ?p)
   (is-site ?s2)
   ; looked-up types
   (is-model ?m)
   (is-site ?s1)
   (is-model ?sm2)
   ; agent
   ;(agent-is-active ?a)
   (agent-has-device ?a ?d)
   ; device
   (device-can-model ?d ?m)
   (device-can-site ?d ?s1)
   (device-can-site ?d ?s2)
   ; site s1
   (location ?p ?s1)
   ;(not (site-is-offsite ?s1))
   ;(not (site-is-closed ?s1))
   ; site s2
   ;(not (site-is-offsite ?s2))
   (model ?s2 ?sm2)
   (stackable ?sm2 ?m)
   ;(not (site-is-closed ?s2))
   ;(not (site-is-occupied ?s2))
   (not (location ?other ?s2)) ; nothing is already at s2
   ; plate
   (model ?p ?m)
   (location ?p ?s1)
   (not (location ?p ?s2))
  )
  ; delete list
  (
   (location ?p ?s1)
  )
  ; add list
  (
   (location ?p ?s2)
  )
 )

 (:method (move-plate ?p ?m ?s1 ?s2)
  move-plate-NULL
  (
   (plate-site ?p ?s2)
  )
  ()

  move-plate-DIRECT
  ; preconditions
  (
   (is-arm ?d)
   (arm-can-site ?d ?s1)
   (arm-can-site ?d ?s2)
  )
  ; task list
  (
   (!arm-move-plate ?a ?d ?p ?m ?s1 ?s2)
  )

  move-plate-INDIRECT
  (is-site ?s3)
  (
   (!arm-move-plate ?a ?d ?p ?m ?s1 ?s3)
   (move-plate ?p ?m ?s3 ?s2)
  )
 )

 (:method (set-plate-site ?p ?m ?s)
  (
   (plate-site ?p ?s1)
  )
  (
   (move-plate ?p ?m ?s1 ?s)
  )
 )

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
