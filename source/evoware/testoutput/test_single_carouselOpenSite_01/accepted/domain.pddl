(define (domain X)
  (:requirements :strips :typing)
  (:types
    labware - any
    model - any
    peeler - device
    pipetter - device
    reader - device
    sealer - device
    shaker - device
    site - any
    siteModel - any
    thermocycler - device
    transporter - device
  )
  (:predicates
    (agent-has-device ?agent - agent ?device - device)
    (device-can-site ?device - device ?site - site)
    (location ?labware - labware ?site - site)
    (model ?labware - labware ?model - model)
    (stackable ?sm - siteModel ?m - model)
  )
  (:action carousel.openSite-CENTRIFUGE_1
    :parameters (
    )
    :precondition (and
    )
    :effect (and
      (not (site-closed CENTRIFUGE_1))
      (site-closed CENTRIFUGE_2)
      (site-closed CENTRIFUGE_3)
      (site-closed CENTRIFUGE_4)
    )
  )
  (:action transportLabware
    :parameters (
      ?labware - labware
      ?model - model
      ?site1 - site
      ?site2 - site
      ?siteModel2 - siteModel
    )
    :precondition (and
      (location ?labware ?site1)
      (model ?labware ?model)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model)
      (not (site-blocked ?site2))
      (not (site-closed ?site2))
    )
    :effect (and
      (not (location ?labware ?site1))
      (not (site-blocked ?site1))
      (location ?labware ?site2)
      (site-blocked ?site2)
    )
  )
)