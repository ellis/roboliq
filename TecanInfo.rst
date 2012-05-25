Firmware Commands
=================

``C5,PRZ1,,,,,,,``:
  Device LiHa, position, relative, Z, up by 1 tick for tip 1, none for the rest.
  Expected response: ``C5,0,``

``C5,PRZ,1,,,,,,``:
  Same as above, except that it moves tip 2.

``C6,RPZ0``:
  Device LiHa, read position Z of all tips.

