(define (domain X)
  (:requirements :strips :typing)
  (:types
    agent
    labware
    model
    pipetter
    pipetterProgram
    shaker
    shakerProgram
    site
    siteModel
  )
  (:predicates
    (agent-has-device ?agent - agent ?device - device)
    (device-can-site ?device - device ?site - site)
    (location ?labware - labware ?site - site)
    (model ?labware - labware ?model - model)
    (stackable ?sm - siteModel ?m - model)
  )
  (:action pipette1
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
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