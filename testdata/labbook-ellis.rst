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



