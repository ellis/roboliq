== Full description ==

Major steps:

* Clean
* Aspirate
* Dispense

Tips 1 thru 4, either 1000ul or 50ul.

Assign a valid tip to dispense 1.
Assign valid source wells to tips.

Assign a valid tip to dispense 2.
Assign valid source wells to tips.

...

Constraints:

aspVol <= tipVolMax
aspVol <= vesselVol

i: grouping
j: item of grouping

tipXAspWell_i = ...
dispenseX_tip = tipX
dispenseX_source = wellX


Allocate a tip for dispense 1 if possible.
End grouping or continue.

Allocate a tip for dispense 2 if possible.  This may be a new tip or one already used.


Assign a tip to dispense 1.
Assign that tip to source well.
