library('psych')
library('scatterplot3d')

setwd("~/src/roboliq/testdata/dyes-201210")

relValue = function(values) {
  m = median(values)
  (values - m) / m
}

df <- read.delim('dyes.tab', header=1)
dfwell <- (df$row - 1) * 12 + df$col
df.totVol = df$baseVol + df$vol
df.totConc = (df$baseConc * df$baseVol + df$conc * df$vol) / df.totVol

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
plot(dfwell[df$id == 'dye13Aa'], relValue(df$readout[df$id == 'dye13Aa']), main="10ul multi/air", ylab='rel. readout')
plot(dfwell[df$id == 'dye13Ab'], relValue(df$readout[df$id == 'dye13Ab']), main="10ul single/air", ylab='rel. readout')
plot(dfwell[df$id == 'dye14Aa'], relValue(df$readout[df$id == 'dye14Aa']), main="10ul multi/wet", ylab='rel. readout')
plot(dfwell[df$id == 'dye14Ab'], relValue(df$readout[df$id == 'dye14Ab']), main="10ul single/wet", ylab='rel. readout')
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

lm(df$readout ~ df.totConc + df$tip + df$row + df$col + df$tipVol)
lm(scale(df$readout) ~ scale(df.totConc) + scale(df$tip) + scale(df$row) + scale(df$col) + scale(df$tipVol))
lm(scale(df$readout) ~ scale(df.totConc) + (df$tip == 1) + (df$tip == 2) + (df$tip == 3) + (df$tip == 4) + scale(df$row) + scale(df$col) + scale(df$tipVol))

lm1 = lm(df$readout ~ df.totConc)
lm1.pred = predict(x, df)
lm1.d = abs(df$readout - df.pred1)
lm1.rel = lm1.d/df.pred1
plot(ecdf(lm1.rel))
plot(lm1.rel ~ as.factor(df$multipipette > 0))
plot(lm1.rel[df$multipipette > 0] ~ as.factor(df$multipipette[df$multipipette > 0] == 1))
plot(lm1.rel ~ as.factor(df$tip))
