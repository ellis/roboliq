library('psych')
library('scatterplot3d')

setwd("~/src/roboliq/testdata/dyes-201210")

par0 = par(no.readonly=T)

relValue = function(values) {
  m = median(values)
  (values - m) / m
}

# readout = \epsilon * c * d, where \epsilon is a property of the dye, c is concentration, and d is the distance the beam travels through the liquid.
# readout is therefore proportional to totConc * totVol
df <- read.delim('dyes.tab', header=1)
multipipette_012 = as.factor(apply(as.matrix(df$multipipette), 1, function(n) ifelse(n >= 2, '2+', as.character(n))))
df.key = as.factor(paste(df$site, df$plateModel, df$liquidClass, multipipette_012, sep = "|"))
df$well <- (df$row - 1) * 12 + df$col
df$totVol = df$baseVol + df$vol
df$totConc = (df$baseConc * df$baseVol + df$conc * df$vol) / df$totVol
# df$readTotVol is \epsilon * d, and thereby represents a scaled value for the total volume of the liquid in the well.
df$readTotVol = df$readout / df$totConc
# readout ~= totConc * totVol
#  = (df$baseVol + df$vol) * (df$baseConc * df$baseVol + df$conc * df$vol) / df$totVol
#  = df$baseConc * df$baseVol + df$conc * df$vol
# ==>
# vol ~= (readout - baseConc * baseVol) / conc
df$readVol = (df$readout - df$baseConc * df$baseVol) / df$conc
df.vol.levels = levels(as.factor(df$vol))

# Linear models
lm1 = lm(df$readout ~ df$totConc)
lm1.pred = predict(lm1, df)
lm1.d = df$readout - lm1.pred
lm1.dabs = abs(lm1.d)
lm1.rel = lm1.d/lm1.pred
lm1.relabs = lm1.dabs/lm1.pred

lm2 = lm((df$readout[df$multipipette==0] / df$totConc[df$multipipette==0]) ~ df$vol[df$multipipette==0])
lm3 = lm((df$readout[df$multipipette==1] / df$totConc[df$multipipette==1]) ~ df$vol[df$multipipette==1])
lm4 = lm((df$readout[df$multipipette>1] / df$totConc[df$multipipette>1]) ~ df$vol[df$multipipette>1])

# Overview of all conditions
groups <- length(df.vol.levels)
numbox <- length(levels(df.key))
total <- groups * numbox
xpoints <- seq(median(1:numbox),total,numbox)
boxplot((df$readout / df$totConc) ~ interaction(df.key, as.factor(df$vol)), col=2:(length(levels(df.key))+1), frame.plot=1, axes=0)
axis(1, at=xpoints, labels=df.vol.levels)

# Overview of all conditions, by volume
plotOverviewByVol = function(vol) {
  main = paste(as.character(vol), "ul")
  plot(readVol ~ df.key[df$vol == vol], data=df[df$vol == vol,], horizontal=T, col="pink", las=1, ylab='readout / conc', xlab='', main=main)
}
vol_l = c(3, 5, 10, 20, 50, 100, 200)
par(mfcol=c(3,3))
apply(as.array(vol_l), 1, plotOverviewByVol)

# Single pipetting variance vs volume
plotA = function(cond, main) {
  boxplot(readTotVol ~ vol, data=df[cond,], xlab='vol', ylab='readTotVol', main=main)
  plot(readVol ~ vol, data=df[cond,], log='xy')
}
par(mfcol=c(2, 3), oma=par0$oma)
plotA((df$liquidClass == 'Water free dispense' & df$multipipette == 0), "Single-Pipetting")
plotA((df$liquidClass == 'Water free dispense' & df$multipipette == 1), "Multi-Pipetting Step 1")
plotA((df$liquidClass == 'Water free dispense' & df$multipipette >= 2), "Multi-Pipetting Step 2+")

# Multipipetting variance
par(mfcol=c(1,1))
with(df[df$multipipette > 0 & df$tipVolMax > 0,], scatterplot3d(tipVol, tipVolMax, readout / totConc, zlab='', log='xyz'))

# Multipipetting 10ul
par(mfrow=c(1,2))
cond = df$multipipette > 0 & df$tipVolMax > 0 & df$vol == 10
tv = -df$tipVol[cond]
with(df[cond,], plot((readout / totConc) ~ tv, col=as.factor(tipVolMax)), xlab="-tipVol")
x = (1 - df$tipVol[cond] / df$tipVolMax[cond])
with(df[cond,], plot((readout / totConc) ~ x, col=as.factor(tipVolMax)))
# 10ul, 12*7 steps
boxplotMultipipette = function(cond, main) {
  x = (1 - df$tipVol[cond] / df$tipVolMax[cond])
  h = hist(x, plot=F)
  x2 = (1 - df$tipVol[cond] / df$tipVolMax[cond])
  with(df[cond,], plot((readout / totConc) ~ x2, col=as.factor(tipVolMax)), xlab="progress", main=main)
  with(df[cond,], boxplot((readout / totConc) ~ cut(x, h$breaks), notch=T))
}
par(mfcol=c(2,4))
#x = -df$tipVol[cond]
#with(df[cond,], plot((readout / totConc) ~ tipVol, col=as.factor(tipVolMax)))
boxplotMultipipette(cond, "10ul from all")
boxplotMultipipette(cond & df$tipVolMax == 80, "10ul from 80ul")
boxplotMultipipette(cond & df$tipVolMax == 240, "10ul from 240ul")
boxplotMultipipette(cond & df$tipVolMax == 840, "10ul from 840ul")

cond1 = (cond & df$multipipette > 1)
x = (1 - df$tipVol[cond1] / df$tipVolMax[cond1])
lm5 = lm(readTotVol ~ x + tipVol + tipVolMax, data=df[cond1,])

# Compare dye13 and dye14 (10ul multi vs single, air vs wet)
par(mfrow=c(2,2),oma = c(0, 0, 2, 0))
df_id13Aa = df$id == 'dye13Aa'
df_id13Ab = df$id == 'dye13Ab'
df_id14Aa = df$id == 'dye14Aa'
df_id14Ab = df$id == 'dye14Ab'
df_id13or14 = df_id13Aa | df_id13Ab | df_id14Aa | df_id14Ab
df_id_13or14 = df$id[df_id13Aa | df_id13Ab | df_id14Aa | df_id14Ab]
plot(df$readout[df$id == 'dye13Aa'], main="10ul multi/air", ylab='readout')
plot(df$readout[df$id == 'dye13Ab'], main="10ul single/air", ylab='readout')
plot(df$readout[df$id == 'dye14Aa'], main="10ul multi/wet", ylab='readout')
plot(df$readout[df$id == 'dye14Ab'], main="10ul single/wet", ylab='readout')
par(mfrow=c(3,2))
plot(df$well[df$id == 'dye13Aa'], relValue(df$readout[df$id == 'dye13Aa']), main="10ul multi/air", ylab='rel. readout')
plot(df$well[df$id == 'dye13Ab'], relValue(df$readout[df$id == 'dye13Ab']), main="10ul single/air", ylab='rel. readout')
plot(df$well[df$id == 'dye14Aa'], relValue(df$readout[df$id == 'dye14Aa']), main="10ul multi/wet", ylab='rel. readout')
plot(df$well[df$id == 'dye14Ab'], relValue(df$readout[df$id == 'dye14Ab']), main="10ul single/wet", ylab='rel. readout')
boxplot(df$readout[df_id13or14] ~ df$id[df_id13or14])
mtext("10ul multi vs single, air vs wet", outer = T, cex=1)

# Test reader reliability (it appears to be very good)
reader <- read.delim('reader.tab', header=1)
well <- (reader$row - 1) * 12 + reader$col
describe.by(reader$readout, well)
par(mfrow=c(2,2), oma = c(0, 0, 2, 0))
boxplot(reader$readout[reader$vol == 10] ~ well[reader$vol == 10], main="10ul", xlab="well")
boxplot(reader$readout[reader$vol == 20] ~ well[reader$vol == 20], main="20ul", xlab="well")
boxplot(reader$readout[reader$vol == 10] ~ well[reader$vol == 10], ylim=c(0, max(reader$readout[reader$vol == 10])), main="10ul", xlab="well")
boxplot(reader$readout[reader$vol == 20] ~ well[reader$vol == 20], ylim=c(0, max(reader$readout[reader$vol == 20])), main="20ul", xlab="well")
mtext("Reliability of Reader", outer = T, cex=1)

# Multipipetting
plot(lm1.rel ~ multipipette_012, xlab="multipipetting step")
x = df$tipVol / df$vol
plot(lm1.rel ~ x, col=df$vol)

# readout vs tipvol
plot3 = function(vol) {
  #mask = df$vol == vol & df$multipipette == 'true'
  plot(df$readout[df$vol==vol] ~ df$tipVol[df$vol==vol], xlab="tipVol", ylab="readout", main=paste(as.character(vol), "ul"))
}
par(mfrow=c(2,2), oma=c(0, 0, 2, 0))
plot3(5)
plot3(10)
plot3(50)
plot3(100)
mtext("Readout vs Tip Volume, by Volume", outer = T, cex=1)

library(car)
par(mfrow=c(1,1))
scatterplotMatrix(~ df$readout + df$tipVol | df$vol)

# 200 ul direct
par(mfrow=c(2,1), oma=c(0, 0, 2, 0))
# Pipetted from col 1 to 12
plot(df$readout[df$id == 'dye16A'], main="Left-to-Right", ylab="readout", xlab="well", col=df$tip[df$id == 'dye16A'])
# Pipetted from col 12 to 1
plot(df$readout[df$id == 'dye20A'], main="Right-to-Left", ylab="readout", xlab="well", col=df$tip[df$id == 'dye16A'])
mtext("200ul readouts, dispensed left-to-right then reversed", outer = T, cex=1)

par(mfcol=c(3,2), oma=c(0, 0, 2, 0))
cond = (df$id == 'dye16A')
boxplot(df$readout[cond] ~ df$row[cond], main="Left-to-Right by Row", ylab="readout", xlab="row")
boxplot(df$readout[cond] ~ df$col[cond], main="Left-to-Right by Col", ylab="readout", xlab="col")
boxplot(df$readout[cond] ~ df$tip[cond], main="Left-to-Right by Tip", ylab="readout", xlab="tip")
cond = (df$id == 'dye16A' & df$col > 1)
boxplot(df$readout[cond] ~ df$row[cond], main="Left-to-Right by Row", ylab="readout", xlab="row")
boxplot(df$readout[cond] ~ df$col[cond], main="Left-to-Right by Col", ylab="readout", xlab="col")
boxplot(df$readout[cond] ~ df$tip[cond], main="Left-to-Right by Tip", ylab="readout", xlab="tip")


lm(relValue(df$readout[cond]) ~ df$row[cond] + df$col[cond])
lm(relValue(df$readout[df$id == 'dye20A']) ~ df$row[df$id == 'dye20A'] + df$col[df$id == 'dye20A'])
lm(relValue(df$readout[df$id == 'dye20A']) ~ df$row[df$id == 'dye20A'] + df$col[df$id == 'dye20A'])

lm(df$readout ~ df$totConc + df$tip + df$row + df$col + df$tipVol)
lm(scale(df$readout) ~ scale(df$totConc) + scale(df$tip) + scale(df$row) + scale(df$col) + scale(df$tipVol))
lm(scale(df$readout) ~ scale(df$totConc) + (df$tip == 1) + (df$tip == 2) + (df$tip == 3) + (df$tip == 4) + scale(df$row) + scale(df$col) + scale(df$tipVol))

plot(ecdf(lm1.rel))
plot(ecdf(lm1.relabs))
multipipette_012 = as.factor(apply(as.matrix(df$multipipette), 1, function(n) ifelse(n > 2, 2, n)))
plot(lm1.rel ~ multipipette_012)
plot(lm1.rel ~ as.factor(df$vol))
plot(lm1.rel ~ as.factor(df$tip))

x = log(df$vol)
h = hist(x, plot=F)
h2 = hist(df$vol, breaks=exp(h$breaks), plot=F)
boxplot(df$readTotVol ~ df$vol, breaks=h2$breaks)

par(par0)