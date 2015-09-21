library('psych')
library('scatterplot3d')

setwd("~/src/roboliq/testdata/dyes-201210")

par0 = par(no.readonly=T)

df0 <- read.delim('z02b.tab', header=1)
df0$step = df0$step - 1
df = df0[df0$z < 2100 & df0$step > 1,]
#df0 <- read.delim('z02a.tab', header=1)
#df = df0[df0$z < 2100 & (df0$step > 1 | df0$dvol == 0),]
attach(df)

plot(vol, col=tip)
plot(z, col=tip)
plot(dz ~ dvol, col=tip)
abline(lm(dz ~ dvol))

lm1 = lm(dz ~ dvol + vol)
plot(lm1$residuals, col=tip)

tips1 = tip == 1
tips2 = tip == 2
tips3 = tip == 3
tips4 = tip == 4
lm2 = lm(dz ~ dvol + vol + tips2 + tips3 + tips4)
plot(lm2$residuals, col=tip)

lm3 = lm(dz ~ dvol + vol + tips2 + tips3 + tips4 + row + col)
plot(lm3$residuals, col=tip)

df50 = df0[df0$step == 1,]
plot(df50$z, col=df50$tip)
lm50_1 = lm(df50$z ~ df50$row + df50$col)
plot(lm50_1$residuals, col=df50$tip)

tips501 = df50$tip == 1
tips502 = df50$tip == 2
tips503 = df50$tip == 3
tips504 = df50$tip == 4
lm50_2 = lm(df50$z ~ df50$row + df50$col + tips502 + tips503 + tips504)
plot(lm50_2$residuals, col=df50$tip)

lowrows50 = df50$row > 4
lm50_3 = lm(df50$z ~ df50$col + tips501 + tips502 + tips503 + lowrows50)
plot(lm50_3$residuals, col=df50$tip)
