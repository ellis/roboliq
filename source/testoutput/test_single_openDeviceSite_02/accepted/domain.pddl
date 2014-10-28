(define (domain X)
  (:requirements :strips :typing)
  (:types
    agent - any
    centrifuge - device
    device - any
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
  (:action carousel.close-mario__Centrifuge
    :parameters (
      ?agent - agent
    )
    :precondition (and
      (agent-has-device ?agent mario__Centrifuge)
    )
    :effect (and
      (site-closed CENTRIFUGE_1)
      (site-closed CENTRIFUGE_2)
      (site-closed CENTRIFUGE_3)
      (site-closed CENTRIFUGE_4)
    )
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
  (:action carousel.openSite-CENTRIFUGE_2
    :parameters (
    )
    :precondition (and
    )
    :effect (and
      (site-closed CENTRIFUGE_1)
      (not (site-closed CENTRIFUGE_2))
      (site-closed CENTRIFUGE_3)
      (site-closed CENTRIFUGE_4)
    )
  )
  (:action carousel.openSite-CENTRIFUGE_3
    :parameters (
    )
    :precondition (and
    )
    :effect (and
      (site-closed CENTRIFUGE_1)
      (site-closed CENTRIFUGE_2)
      (not (site-closed CENTRIFUGE_3))
      (site-closed CENTRIFUGE_4)
    )
  )
  (:action carousel.openSite-CENTRIFUGE_4
    :parameters (
    )
    :precondition (and
    )
    :effect (and
      (site-closed CENTRIFUGE_1)
      (site-closed CENTRIFUGE_2)
      (site-closed CENTRIFUGE_3)
      (not (site-closed CENTRIFUGE_4))
    )
  )
  (:action closeDeviceSite
    :parameters (
      ?agent - agent
      ?device - device
      ?site - site
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (device-can-open-site ?device ?site)
    )
    :effect (and
      (site-closed ?site)
    )
  )
  (:action distribute1
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
  (:action distribute2
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
    )
    :effect (and
    )
  )
  (:action distribute3
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
      ?labware3 - labware
      ?model3 - model
      ?site3 - site
      ?siteModel3 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2 ?site3)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
      (device-can-site ?device ?site3)
      (model ?labware3 ?model3)
      (location ?labware3 ?site3)
      (model ?site3 ?siteModel3)
      (stackable ?siteModel3 ?model3)
    )
    :effect (and
    )
  )
  (:action distribute4
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
      ?labware3 - labware
      ?model3 - model
      ?site3 - site
      ?siteModel3 - siteModel
      ?labware4 - labware
      ?model4 - model
      ?site4 - site
      ?siteModel4 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2 ?site3 ?site4)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
      (device-can-site ?device ?site3)
      (model ?labware3 ?model3)
      (location ?labware3 ?site3)
      (model ?site3 ?siteModel3)
      (stackable ?siteModel3 ?model3)
      (device-can-site ?device ?site4)
      (model ?labware4 ?model4)
      (location ?labware4 ?site4)
      (model ?site4 ?siteModel4)
      (stackable ?siteModel4 ?model4)
    )
    :effect (and
    )
  )
  (:action ensureLabwareLocation
    :parameters (
      ?labware - labware
      ?site - site
    )
    :precondition (and
      (location ?labware ?site)
    )
    :effect (and
    )
  )
  (:action evoware.centrifuge.run-mario__Centrifuge
    :parameters (
      ?agent - agent
      ?device - centrifuge
    )
    :precondition (and
      (agent-has-device ?agent ?device)
    )
    :effect (and
      (site-closed CENTRIFUGE_1)
      (site-closed CENTRIFUGE_2)
      (site-closed CENTRIFUGE_3)
      (site-closed CENTRIFUGE_4)
    )
  )
  (:action evoware.timer.start
    :parameters (
      ?agent - agent
    )
    :precondition (and
    )
    :effect (and
    )
  )
  (:action evoware.timer.wait
    :parameters (
      ?agent - agent
    )
    :precondition (and
    )
    :effect (and
    )
  )
  (:action evoware.transportLabware
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
      (not (site-closed ?site1))
      (not (site-closed ?site2))
    )
    :effect (and
      (not (location ?labware ?site1))
      (not (site-blocked ?site1))
      (location ?labware ?site2)
      (site-blocked ?site2)
    )
  )
  (:action getLabwareLocation
    :parameters (
      ?labware - labware
      ?model - model
      ?site - site
      ?siteModel - siteModel
    )
    :precondition (and
      (location ?labware ?site)
      (model ?labware ?model)
      (model ?site ?siteModel)
    )
    :effect (and
    )
  )
  (:action measureAbsorbance
    :parameters (
      ?agent - agent
      ?device - reader
      ?labware - labware
      ?model - model
      ?site - site
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (device-can-site ?device ?site)
      (location ?labware ?site)
    )
    :effect (and
    )
  )
  (:action openDeviceSite
    :parameters (
      ?agent - agent
      ?device - device
      ?site - site
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (device-can-open-site ?device ?site)
    )
    :effect (and
      (not (site-closed ?site))
    )
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
  (:action pipette2
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
    )
    :effect (and
    )
  )
  (:action pipette3
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
      ?labware3 - labware
      ?model3 - model
      ?site3 - site
      ?siteModel3 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2 ?site3)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
      (device-can-site ?device ?site3)
      (model ?labware3 ?model3)
      (location ?labware3 ?site3)
      (model ?site3 ?siteModel3)
      (stackable ?siteModel3 ?model3)
    )
    :effect (and
    )
  )
  (:action pipette4
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
      ?labware3 - labware
      ?model3 - model
      ?site3 - site
      ?siteModel3 - siteModel
      ?labware4 - labware
      ?model4 - model
      ?site4 - site
      ?siteModel4 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2 ?site3 ?site4)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
      (device-can-site ?device ?site3)
      (model ?labware3 ?model3)
      (location ?labware3 ?site3)
      (model ?site3 ?siteModel3)
      (stackable ?siteModel3 ?model3)
      (device-can-site ?device ?site4)
      (model ?labware4 ?model4)
      (location ?labware4 ?site4)
      (model ?site4 ?siteModel4)
      (stackable ?siteModel4 ?model4)
    )
    :effect (and
    )
  )
  (:action promptOperator
    :parameters (
      ?agent - agent
    )
    :precondition (and
    )
    :effect (and
    )
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
  (:action shakePlate
    :parameters (
      ?agent - agent
      ?device - shaker
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
  (:action titrate1
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
  (:action titrate2
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
    )
    :effect (and
    )
  )
  (:action titrate3
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
      ?labware3 - labware
      ?model3 - model
      ?site3 - site
      ?siteModel3 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2 ?site3)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
      (device-can-site ?device ?site3)
      (model ?labware3 ?model3)
      (location ?labware3 ?site3)
      (model ?site3 ?siteModel3)
      (stackable ?siteModel3 ?model3)
    )
    :effect (and
    )
  )
  (:action titrate4
    :parameters (
      ?agent - agent
      ?device - pipetter
      ?labware1 - labware
      ?model1 - model
      ?site1 - site
      ?siteModel1 - siteModel
      ?labware2 - labware
      ?model2 - model
      ?site2 - site
      ?siteModel2 - siteModel
      ?labware3 - labware
      ?model3 - model
      ?site3 - site
      ?siteModel3 - siteModel
      ?labware4 - labware
      ?model4 - model
      ?site4 - site
      ?siteModel4 - siteModel
    )
    :precondition (and
      (agent-has-device ?agent ?device)
      (ne ?site1 ?site2 ?site3 ?site4)
      (device-can-site ?device ?site1)
      (model ?labware1 ?model1)
      (location ?labware1 ?site1)
      (model ?site1 ?siteModel1)
      (stackable ?siteModel1 ?model1)
      (device-can-site ?device ?site2)
      (model ?labware2 ?model2)
      (location ?labware2 ?site2)
      (model ?site2 ?siteModel2)
      (stackable ?siteModel2 ?model2)
      (device-can-site ?device ?site3)
      (model ?labware3 ?model3)
      (location ?labware3 ?site3)
      (model ?site3 ?siteModel3)
      (stackable ?siteModel3 ?model3)
      (device-can-site ?device ?site4)
      (model ?labware4 ?model4)
      (location ?labware4 ?site4)
      (model ?site4 ?siteModel4)
      (stackable ?siteModel4 ?model4)
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
      (not (site-closed ?site1))
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