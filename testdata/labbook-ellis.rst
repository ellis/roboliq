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


Fluorescence measurements on PCR plates
=======================================

date: 2012-05-23
time: 9:00 - 11:30
files: TempEllisFluor1Dilute1000.esc, Ellis-20120521a.mdfx

We want to investigate the use of a particular fluorescent substance.
We also want to investigate the properties of our PCR plates for use in the reader.

Names:

:FlA: the fluorescent substance we used
:Buf: the buffer we used
:P1: Deep well plate for FlA dilutions
:P2: PCR plate running the readouts on.

*FlA* has pH requirements, for which we used some buffer *Buf* and diluted it 1:20.
Fabian indicated that the minimum dilution for *FlA* is 1:10000, and the max is 1:100,000,000.
Excite at 460, read out at ~540, with the monochromator.

I prepared the *FlA* dilution as follows:

* Used a deep well plate *P1*.
* All pipetting actions were done in single-pipetting mode.
* Dispensed 25ul of the buffer into A01 an B01.
* Dispensed 5ul of FlA into A01.
* Add 470ul of water to A01 for a total volume of 500ul and fluorescent dilution of 1:100.
* Mix A01 4x370ul.
* Transfer 5ul from A01 to B01.
* Add 470ul of water to B01 for a total volume of 500ul and fluorescent dilution of 1:10000.

I then transferred the 1:10000 dilution to *P2* at volumes of 20ul, 10ul, and 5ul.
The PCR plate was prepared as follows:

* All pipetting actions were done in single-pipetting mode.
* Transfer 20ul from P1(B01) to P2(C03)
* Transfer 10ul from P1(B01) to P2(C06)
* Transfer 5ul from P1(B01) to P2(C09)

The readout program is specified in ``Ellis-20120521a.mdfx`` and the readout values can be found in ``Ellis-20120521a.xlsx``.


date: 2012-05-23
time: 14:15 - 15:15
files: TempEllisFluor2DiluteSeries.esc, Ellis-20120521b.mdfx

Create dilution series of *FlA* and measure fluorescence in the reader.
Each column will contain 1/2 the *FlA* concentration as the previous column, except for the last two columns which have the same concentration.

* Mix 950ul water and 50ul buffer into P1(D01) to create buffer 1:20 solution
* Dispense 50ul buffer(1:20) into P2(F02 r F11)
* Dispense 50ul *FlA* into P2(F01, F02)
* Transfer 50ul and mix from P2(F02) to P2(F03), then 3 to 4, then 4 to 5, ..., then 11 to 12.

Output not good.  Here are the values using a single flash per well.

====  =====
Well  Value
====  =====
F01   OVER
F02   OVER
F03   45981
F04   26783
F05   19659
F06   14250
F07   11447
F08   11950
F09   14165
F10   12126
F11    8652
F12   10932
====  =====

I remeasured with 5 flashes and got essentially the same readout.

date: 2012-05-24

Reran the above pipetting using a white plate.
Haven't run it through the reader yet because it needs an adapter first.

date: 2012-05-24
files: TempEllisFluor3DiluteSeries.esc

Want to run the above experiments again with a black flat-well plate.
However, I need to reprogram the script because a) the plate must be at a different location, b) either liquid detection or z-offset is wrong, so mixing doesn't work, c) we need larger volumes.

* Mix Buf(1/20)
* Mix Buf(1/20)+FlA(1/10000)
* Mix 

(2012-06-11) Hmm, I didn't finish the above notes...  Had lots of problems with volume detection, but fixed them eventually.  The first and second wells got mixed quite a few times due to running the script multiple times till the errors were ironed out, so I don't include the first well in the measurements below.  Anyhow, here are the results as copied from an email I sent to Fabian:

The readout on the black nunc wells is:

32307
21382
16808
13829
11657
 6406
 3483
 2408
 1604
 1155
  834

Empty wells measure around 50.

date: 2012-06-11
files: TempEllisFluor3DiluteSeries.esc, Ellis-20120611.mdfx

Reran the script.  This time there was one error with volume detection in P1(B01), which contained buffer.  I manually transferred some of the buffer from P1(A01) to P1(B01) in order to let the script continue.

Here are the readouts:
OVER
46303
23397
12323
6660
3532
2118
1323
937
689
815
1067

Empty wells measure around 50.

