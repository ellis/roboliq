#library('rjson')
#setwd("~/src/roboliq/testdata/dyes-201210")
#json <- fromJSON(paste(readLines('read10times1a.json'), collapse=""))
#df <- as.data.frame(t(sapply(json, unlist)))

library('psych')
library('scatterplot3d')

setwd("~/src/roboliq/testdata/dyes-201210")

relValue = function(values) {
  m = median(values)
  (values - m) / m
}

df <- read.delim('all.tab', header=1)
dfwell <- (df$row - 1) * 12 + df$col

# Compare dye13 and dye14
par(mfrow=c(2,2))
plot(df$readout[df$id == 'dye13Aa'], main="10ul multi/air", ylab='readout')
plot(df$readout[df$id == 'dye13Ab'], main="10ul single/air", ylab='readout')
plot(df$readout[df$id == 'dye14Aa'], main="10ul multi/wet", ylab='readout')
plot(df$readout[df$id == 'dye14Ab'], main="10ul single/wet", ylab='readout')
par(mfrow=c(2,2))
plot(dfwell[df$id == 'dye13Aa'], relValue(df$readout[df$id == 'dye13Aa']), main="10ul multi/air", ylab='rel. readout')
plot(dfwell[df$id == 'dye13Ab'], relValue(df$readout[df$id == 'dye13Ab']), main="10ul single/air", ylab='rel. readout')
plot(dfwell[df$id == 'dye14Aa'], relValue(df$readout[df$id == 'dye14Aa']), main="10ul multi/wet", ylab='rel. readout')
plot(dfwell[df$id == 'dye14Ab'], relValue(df$readout[df$id == 'dye14Ab']), main="10ul single/wet", ylab='rel. readout')

# Test reader reliability (it appears to be very good)
reader <- read.delim('reader.tab', header=1)
well <- (reader$row - 1) * 12 + reader$col
describe.by(reader$readout, well)
boxplot(reader$readout[reader$vol == 10] ~ well[reader$vol == 10])
boxplot(reader$readout[reader$vol == 20] ~ well[reader$vol == 20])
boxplot(reader$readout[reader$vol == 10] ~ well[reader$vol == 10], ylim=c(0, max(reader$readout[reader$vol == 10])))
