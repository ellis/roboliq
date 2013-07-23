(defdomain domain (
 (:operator (!agent-deactivate ?a)
  ; preconditions
  (agent-is-active ?a)
  ; delete list
  ((agent-is-active ?a))
  ; add list
  ()
 )

 (:method (agent-deactivate)
  agent-deactivate-DO
  ; preconditions
  (
   (agent-is-active ?a)
  )
  ; task list
  ((!agent-deactivate ?a) (agent-deactivate))

  agent-deactivate-NULL
  nil
  nil
 )

 (:operator (!agent-activate ?a)
  ; preconditions
  (
   (forall (?a2) (is-agent ?a2) ((not (agent-is-active ?a2))))
  )
  ; delete list
  ()
  ; add list
  ((agent-is-active ?a))
 )

 (:method (agent-activate ?a)
  agent-active-NULL
  (
   (agent-is-active ?a)
  )
  nil

  agent-activate-DO
  ; preconditions
  ()
  ; task list
  (
   (agent-deactivate)
   (!agent-activate ?a)
  )
 )

 (:operator (!log ?a ?text)
  ((is-agent ?a))
  nil
  nil
 )

 (:operator (!prompt ?a ?text)
  ((is-agent ?a))
  nil
  nil
 )

 (:operator (!transporter-run ?a ?d ?p ?m ?s1 ?s2 ?vectorClass)
  ; preconditions
  (
   (is-agent ?a)
   (is-transporter ?d)
   (is-plate ?p)
   (is-site ?s2)
;   ; looked-up types
;   (is-model ?m)
;   (is-site ?s1)
;   (is-model ?sm2)
;   ; agent
;   (agent-is-active ?a)
;   (agent-has-device ?a ?d)
;   ; device
;   ;(device-can-model ?d ?m)
;   (transporter-can ?d ?s1 ?vectorClass)
;   (transporter-can ?d ?s2 ?vectorClass)
;   ; site s1
;   ;(not (site-is-closed ?s1))
;   ; site s2
;   (model ?s2 ?sm2)
;   (stackable ?sm2 ?m)
;   ;(not (site-is-closed ?s2))
;   (not (location ?other ?s2)) ; nothing is already at s2
;   ; plate
;   (model ?p ?m)
;   (location ?p ?s1)
;   (not (location ?p ?s2))
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

 (:method (transporter-run ?a ?d ?p ?s2)
  transporter-run-NULL
  (
   (location ?p ?s2)
  )
  nil

  transporter-run-DIRECT
  ; preconditions
  (
   (location ?p ?s1)
   (model ?p ?m)
   (transporter-can ?d ?s1 ?vectorClass)
   (transporter-can ?d ?s2 ?vectorClass)
  )
  ; task list
  (
   (agent-activate ?a)
   (!transporter-run ?a ?d ?p ?m ?s1 ?s2 ?vectorClass)
  )
 )

 (:method (move-labware ?p ?s2)
  move-labware-NULL
  (
   (location ?p ?s2)
  )
  nil

  move-labware-DIRECT
  ; preconditions
  (
   (is-agent ?a)
   (agent-has-device ?a ?d)
   (is-transporter ?d)
   (location ?p ?s1)
   (transporter-can ?d ?s1 ?vectorClass)
   (transporter-can ?d ?s2 ?vectorClass)
  )
  ; task list
  (
   (agent-activate ?a)
   (transporter-run ?a ?d ?p ?s2)
  )

  move-labware-TWO
  (
   (is-agent ?a)
   (is-transporter ?d1)
   (is-transporter ?d2)
   (is-site ?s3)
   (location ?p ?s1)
   (agent-has-device ?a ?d1)
   (agent-has-device ?a ?d2)
   (transporter-can ?d1 ?s1 ?vectorClass1)
   (transporter-can ?d1 ?s3 ?vectorClass1)
   (transporter-can ?d2 ?s3 ?vectorClass2)
   (transporter-can ?d2 ?s2 ?vectorClass2)
  )
  (
   (transporter-run ?a ?d1 ?p ?s3)
   (transporter-run ?a ?d2 ?p ?s2)
  )

  move-labware-TWO-DISTINCT
  (
   (is-agent ?a1)
   (is-agent ?a2)
   (is-transporter ?d1)
   (is-transporter ?d2)
   (is-site ?s3)
   (location ?p ?s1)
   (agent-has-device ?a1 ?d1)
   (agent-has-device ?a2 ?d2)
   (transporter-can ?d1 ?s1 ?vectorClass1)
   (transporter-can ?d1 ?s3 ?vectorClass1)
   (transporter-can ?d2 ?s3 ?vectorClass2)
   (transporter-can ?d2 ?s2 ?vectorClass2)
  )
  (
   (transporter-run ?a1 ?d1 ?p ?s3)
   (transporter-run ?a2 ?d2 ?p ?s2)
  )

;  move-plate-INDIRECT
;  (
;   ;(is-agent ?a1)
;   ;(is-agent ?a2)
;   ;(is-transporter ?d1)
;   ;(is-transporter ?d2)
;   (is-site ?s3)
;  )
;  (
;   (transporter-run ?a1 ?d1 ?p ?s3)
;   (move-labware ?p ?s2)
;  )
 )

 (:operator (!sealer-run ?a ?d ?p ?s)
  ; preconditions
  (
   ; types
   (is-agent ?a)
   (is-sealer ?d)
   (is-plate ?p)
   (is-site ?s)
   ; lookup types
   (is-plateModel ?m)
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
   (agent-activate ?a)
   (plate-is-sealed ?p)
  )
 )

 (:method (sealer-run ?a ?d ?p ?s)
  sealer-run-NULL
  (
   (plate-is-sealed ?p)
  )
  ()

  sealer-run-DO
  (
   (is-agent ?a)
   (is-sealer ?d)
   (is-site ?s)
   (agent-has-device ?a ?d)
   (device-can-model ?d ?m)
   (device-can-site ?d ?s)
   (model ?p ?m)
  )
  ((move-labware ?p ?s) (agent-activate ?a) (!sealer-run ?a ?d ?p ?s))
 )
))
