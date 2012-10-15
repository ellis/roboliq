#

fab <- read.delim('fabian.tab', header=1)
fab$totVol = fab$baseVol + fab$vol
fab$totConc = (fab$baseConc * fab$baseVol + fab$conc * fab$vol) / fab$totVol
# fab$readTotVol is \epsilon * d, and thereby represents a scaled value for the total volume of the liquid in the well.
fab$readTotVol = fab$readout / fab$totConc
# readout ~= totConc * totVol
#  = (fab$baseVol + fab$vol) * (fab$baseConc * fab$baseVol + fab$conc * fab$vol) / fab$totVol
#  = fab$baseConc * fab$baseVol + fab$conc * fab$vol
# ==>
# vol ~= (readout - baseConc * baseVol) / conc
fab$readVol = (fab$readout - fab$baseConc * fab$baseVol) / fab$conc
