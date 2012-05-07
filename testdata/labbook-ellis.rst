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

date: 2012-05-04
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

Execution of script started at 10:11 and went till 13:50.

After execution I measured the following volumes by hand:

buffer in P1(A02): 675ul, expected 712ul

dNTP in P1(B02): 760ul, expected 712ul (I'm guessing that I made a mistake in my measurement here, and that it was actualy 660ul, not 760ul).

TAQ in P1(C02): 665 ul, expected 712ul (this seems to indicate that on average, 3.5ul were aspirated in each step instead of just 3ul)

template1 in eppendorf TEPtemplate1: 940ul, expected 930ul (1ul x 70 aspirations)

template2 in eppendorf TEPtemplate2: 950ul, expected 974ul (1ul x 26 aspirations)

SEQUENCE_01 in EE215D: 70ul, expected 85ul (1.5ul x 10 aspirations)

Sample volumes all appeared approximately equal by visual inspection.
I measured two random wells and got the following unexpected and undesired result.

Samples in plate P4: 22ul, expected 30ul


Test of destination volumes for PCR mix with water
==================================================

date: 2012-05-07
file: TempEllisTestDispenseAccuracy.esc
time: 8:30 - 11:50

I measured a couple sample volumes of only 22ul in the previous experiment, whereas 30ul were expected.
So I wrote a new script to check the final destination volumes.
It dispensed water in the following volumes:

* 17ul to wells A01-H01
* 3ul to wells A01-G01 (simulating buffer, total = 20ul)
* 3ul to wells A01-F01 (simulating dNTP, total = 23ul)
* 1ul to wells A01-E01 (simulating template, total = 24ul)
* 1.5ul to wells A01-D01 (simulating foward primer, total = 25.5ul)
* 1.5ul to wells A01-C01 (simulating backward primer, total = 27ul)
* 3ul to wells A01-B02 (simulating polymerase, total = 30ul)

I repeated this three times used a pipetter to manually test the volume in each well.

====  ========  ========  ========  ========
Well  Expected  Measured  Measured  Measured
====  ========  ========  ========  ========
A01   30ul      28ul      30ul      30
B01   30ul      27ul      30ul      30
C01   27ul      27ul      27ul      27
D01   25.5ul    25.5ul    27ul      25.5
E01   24ul      24ul      25.5ul    24+
F01   23ul      23ul      23ul      23
G01   20ul      20+ul     21ul      20
H01   17ul      17+ul     17ul      17
====  ========  ========  ========  ========

The results are not entirely satisfying.
Some of the divergences are surely due to my amateur pipetting skills, but I suspect that some of them might be genuine divergences.


