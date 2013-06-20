(defdomain domain (
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
   (not (plate-site ?p ?s2))
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
   (agent-is-active ?a)
   (agent-has-sealer ?a ?d)
   ; device
   (sealer-site ?d ?s)
   (sealer-can-plateModel ?d ?m)
   ; site
   (site-can-plateModel ?s ?m)
   ; plate
   (plate-site ?p ?s)
   (plate-model ?p ?m)
   (not (plate-is-sealed ?p))
  )
  ; delete list
  ()
  ; add list
  (
   (plate-is-sealed ?p)
  )
 )

 (:method (seal-plate-adpms ?a ?d ?p ?m ?s)
  seal-plate-adpms-NULL
  (
   (plate-is-sealed ?p)
  )
  ()

  seal-plate-adpms-DO
  ()
  ((set-plate-site ?p ?m ?s) (!sealer-run ?a ?d ?p ?m ?s))
 )

 (:method (seal-plate-pm ?p ?m)
  (
   (is-agent ?a)
   (is-sealer ?d)
   (is-site ?s)
   (agent-has-sealer ?a ?d)
   (sealer-site ?d ?s)
  )
  ((seal-plate-adpms ?a ?d ?p ?m ?s))
 )

 (:method (seal-plate ?p)
  (
   (is-plateModel ?m)
   (plate-model ?p ?m)
  )
  ((seal-plate-pm ?p ?m))
 )

 (:operator (!thermocycler-open ?a ?d)
  ; preconditions
  (
   (is-agent ?a)
   (is-thermocycler ?d)
   (agent-has-thermocycler ?a ?d)
   (not (thermocycler-is-open ?d))
  )
  ; delete list
  ()
  ; add list
  ((thermocycler-is-open ?d))
 )

 (:method (thermocycler-open ?d)
  thermocycler-open-NULL
  (
   (thermocycler-is-open ?p)
  )
  ()

  thermocycler-open-DO
  ()
  ((!thermocycler-open ?a ?d))
 )

 (:operator (!thermocycler-close ?a ?d)
  ; preconditions
  (
   (is-agent ?a)
   (is-thermocycler ?d)
   (agent-has-thermocycler ?a ?d)
   (thermocycler-is-open ?d)
  )
  ; delete list
  ((thermocycler-is-open ?d))
  ; add list
  ()
 )

 (:method (thermocycler-close ?d)
  thermocycler-close-NULL
  (
   (not (thermocycler-is-open ?p))
  )
  ()

  thermocycler-close-DO
  ()
  ((!thermocycler-close ?a ?d))
 )

 (:operator (!thermocycler-run ?a ?d ?spec)
  ; preconditions
  (
   (is-agent ?a)
   (is-thermocycler ?d)
   (is-thermocyclerSpec ?spec)
   (agent-has-thermocycler ?a ?d)
  )
  ; delete list
  ()
  ; add list
  ()
 )

 ; This is a complex action which involves:
 ; * opening the thermocycler lid
 ; * move plate from ?s1 to ?s
 ; * close lid
 ; * run the thermocycler
 ; * open lid
 ; * move plate to ?s2
 ; * close lid
 ; This is because the thermocycler lid may not be left open, or we risk crashing an arm against it.
 (:method (thermocycle-plate ?d ?spec ?p ?s2)
  ; preconditions
  (
   (is-thermocycler ?d)
   (is-thermocyclerSpec ?spec)
   (is-plate ?p)
   (is-site ?s2)

   (is-agent ?a)
   (is-plateModel ?m)
   (is-site ?s)

   (agent-has-thermocycler ?a ?d)
   (plate-model ?p ?m)
   (thermocycler-site ?d ?s)
  )
  ; sub-tasks
  (
   (seal-plate ?p)
   (thermocycler-open ?d)
   (set-plate-site ?p ?m ?s)
   (thermocycler-close ?d)
   (!thermocycler-run ?a ?d ?spec)
   (thermocycler-open ?d)
   (set-plate-site ?p ?m ?s2)
   (thermocycler-close ?d)
  )
 )

 (:operator (!pipette1 ?a ?d ?p ?m ?s)
  ; preconditions
  (
   ; types
   (is-agent ?a)
   (is-pipetter ?d)
   (is-plate ?p)
   (is-plateModel ?m)
   (is-site ?s)
   ; agent
   (agent-is-active ?a)
   (agent-has-pipetter ?a ?d)
   ; device
   (pipetter-can-plateModel ?d ?m)
   (pipetter-can-site ?d ?s)
   ; plate
   (plate-model ?p ?m)
   (plate-site ?p ?s)
  )
  ; delete list
  ()
  ; add list
  ()
 )

 (:method (pipette1-adpms ?a ?d ?p ?m ?s)
  ; preconditions
  ()
  ; task list
  (
   (set-plate-site ?p ?m ?s)
   (!pipette1 ?a ?d ?p ?m ?s)
  )
 )

 (:method (pipette1-pm ?p ?m)
  ; preconditions
  (
   (is-agent ?a)
   (is-pipetter ?d)
   (is-site ?s)
   (agent-has-pipetter ?a ?d)
   (pipetter-can-site ?d ?s)
  )
  ; task list
  (
   (pipette1-adpms ?a ?d ?p ?m ?s)
  )
 )

 (:method (pipette1 ?p)
  ; preconditions
  (
   (is-plateModel ?m)
  )
  ; task list
  (
   (pipette1-pm ?p ?m)
  )
 )

 (:operator (!pipette2 ?a ?d ?p1 ?m1 ?s1 ?p2 ?m2 ?s2)
  ; preconditions
  (
   ; types
   (is-agent ?a)
   (is-pipetter ?d)
   (is-plate ?p1)
   (is-plateModel ?m1)
   (is-site ?s1)
   (is-plate ?p2)
   (is-plateModel ?m2)
   (is-site ?s2)
   ; agent
   (agent-is-active ?a)
   (agent-has-pipetter ?a ?d)
   ; device
   (pipetter-can-plateModel ?d ?m1)
   (pipetter-can-site ?d ?s1)
   (pipetter-can-plateModel ?d ?m2)
   (pipetter-can-site ?d ?s2)
   ; plate
   (plate-model ?p ?m1)
   (plate-site ?p ?s1)
   (plate-model ?p ?m2)
   (plate-site ?p ?s2)
  )
  ; delete list
  ()
  ; add list
  ()
 )

 (:method (pipette2-adpms ?a ?d ?p1 ?m1 ?s1 ?p2 ?m2 ?s2)
  ; preconditions
  ()
  ; task list
  (
   (set-plate-site ?p1 ?m1 ?s1)
   (set-plate-site ?p2 ?m2 ?s2)
   (!pipette2 ?a ?d ?p1 ?m1 ?s1 ?p2 ?m2 ?s2)
  )
 )

 (:method (pipette2-pm ?p1 ?m1 ?p2 ?m2)
  ; preconditions
  (
   (is-agent ?a)
   (is-pipetter ?d)
   (is-site ?s1)
   (is-site ?s2)
   (agent-has-pipetter ?a ?d)
   (pipetter-can-site ?d ?s1)
   (pipetter-can-site ?d ?s2)
  )
  ; task list
  (
   (pipette2-adpms ?a ?d ?p1 ?m1 ?s1 ?p2 ?m2 ?s2)
  )
 )

 (:method (pipette2 ?p1 ?p2)
  ; preconditions
  (
   (is-plateModel ?m1)
   (is-plateModel ?m2)
  )
  ; task list
  (
   (pipette2-pm ?p1 ?m1 ?p2 ?m2)
  )
 )
))
