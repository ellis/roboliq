=======
Labbook
=======

:Author: Ellis Whitehead

Labbook of experiments

2012-04-12
==========

Step 1
------

:Description: Add water to dry primer stock on Invitrogen plate E2215 to reach concentration of 400 uM.
:Location: Robot
:File: protocol-003c-dilutedry
:Plates:
  * Corning plate from Invitrogen

:Notes:
  The following wells did not have any visible content!

  A03,04,
  B01,04,08
  C01,03,08
  D04
  E01

:Result: by visual inspection, liquid levels seem fine

Additional steps:

* sealed E2215 using ``Greiner_384_schwarz.bcf``
* ran E2215 on shaker for 5 minutes

Step 2
------

:Description: Copy plate E2215, diluting from 400 uM to 10 uM.
:Location: Robot
:Time: 16:00
:File: protocol-006a-copy
:Result: by visual inspection

* Destination plate was PCR00093
* Splashes out of PCR00093 wells A12, C06 or C07, C11
* Splashes within various wells

Additional steps:

* sealed E2215
* sealed PCR00093 for shaking
* dropped PCR00093 by mistake, then ran it on the shaker briefly, then "manually" centrifuged it
* now there were quite a few mini droplets on the plate which had escaped the wells -- quite small though -- hopefully no contamination...?

Step 3
------

Mix the PCR together.

:Time: 18:30
:File: protocol-007b-pcrmix

PCR00095 is the plate with the PCR samples in it.

Three hours into the script, got to line 722, at which point the TAQ polymerase well was empty.
The first 36 columns were completely finished, and the remaining columns still needed polymerase.
I sealed the PCR plates and put them (along with E2215) into the fridge.  I put the three 1.5ml tubes with template into the freezer.


Eppendorf Volume Test
=====================

date: 2012-05-04
time: 08:30-09:30
file: TempEllisTestMinAspVol.esc

Manually dispensed 600ul of water in an eppendorf tube *T1*.
Had the robot pipette 20ul from *T1* to an empty eppendorf tube *T2*.
This was repeated until the robot detected that there wasn't enough volume to aspirate anymore.

28 pipetting steps were performed with the following detected volumes:

T1: 673, 662, 650, 639, 627, 612, 600, 589, 569, 558, 542, 531, 515, 492, 477, 458, 438, 419, 400, 377, 354, 331, 304, 273, 246, 207, 173, 130
T2: 0, 30, 88, 134, 173, 211, 246, 273, 300, 331, 350, 377, 400, 419, 438, 454, 473, 492, 508, 523, 539, 554, 569, 585, 596, 612, 619, 635

The final detected volume in *T1* at step 29 was 119ul, at which point the robot claimed that the volume was too low.  I do not understand why it aborts on a detected level of 119ul when it wants to aspirate 20ul.

Manually, I measured final volumes of *T1* = 36ul and *T2* = 550ul.  After 28 steps, I would have expected *T1* = 40ul and *T2* = 560ul.


Test run of primer PCR mix with water
=====================================

The experiment from 2012-04-12 showed no PCR products.
The point of this experiment is to run the script again with water, and afterwards to manually check the volumes of the source wells.

file: protocol-007b-pcrmix


Tube T50water1:
  filled it to near the top, reagents50 row 1 col 1
Tube TEPtemplate1:
  1000ul, eppendorf row 1, col 1
Tube TEPtemplate2:
  1000ul, eppendorf row 2, col 1
Plate P1:
  location: cover,
  volumes: 1000ul water in A02 to C02, representing buffer, dntp, and diluted TAQ
Plate P4:
  location: cooled1,
  empty wells for putting the PCR mixes into
Plate E2215D:
  location: cooled2,
  volumes: 100ul water in all wells representing diluted primers

Execution of script started at 10:11
