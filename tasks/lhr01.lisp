(defdomain lhr01 (
 (:operator (!arm-move-plate ?a ?d ?p ?m ?s1 ?s2)
  ; preconditions
  (
   (is-agent ?a)
   (is-arm ?d)
   (is-plate ?p)
   (is-plateModel ?m)
   (is-site ?s1)
   (is-site ?s2)
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
  ; delete list
  (
   (site-is-occupied ?s1)
   (plate-site ?p ?s1)
  )
  ; add list
  (
   (site-is-occupied ?s2)
   (plate-site ?p ?s2)
  )
 )

 (:method (move-plate ?p ?m ?s1 ?s2)
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
  ; preconditions
  (
   (is-plate ?p)
   (is-site ?s)
   ;
  )
  ; task list
  (
   (!arm-move-plate ?a ?d ?p ?m ?s1 ?s)
  )
  INDIRECT
  ()
  (
   (!arm-move-plate ?a ?d ?p ?m ?s1 ?s2)
   (move-plate ?p ?m ?s2 ?s)
  )
 )
))
