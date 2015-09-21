(define (domain X)
  (:requirements :strips :typing)
  (:types
    agent - any
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
  (:action sealPlate
    :parameters (
      ?agent - agent
      ?device - sealer
      ?labware - labware
      ?model - model
      ?site - site
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (device-can-site ?device ?site)
      (model ?labware ?model)
      (location ?labware ?site)
    )
    :effect (and
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
    )
    :effect (and
      (not (location ?labware ?site1))
      (not (site-blocked ?site1))
      (location ?labware ?site2)
      (site-blocked ?site2)
    )
  )
)