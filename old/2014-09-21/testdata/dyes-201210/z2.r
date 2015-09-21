library('psych')
library('scatterplot3d')

setwd("~/src/roboliq/testdata")

par0 = par(no.readonly=T)

#
# zlevel-01
#

df0 <- read.delim('zlevel-01.tab', header=1)
#df = df0[df0$z < 2100 & (df0$step > 1 | df0$dvol == 0),]
df = df0
attach(df)

describe(z)
plot(z, col=tip)
plot(z ~ tip)
plot(z ~ col)
plot(z ~ row)

aov1 = aov(z ~ as.factor(tip))
summary(aov1)
summary.lm(aov1)

aov2 = aov(z ~ as.factor(col))
summary(aov2)
summary.lm(aov2)

aov3 = aov(z ~ as.factor(row))
summary(aov3)
summary.lm(aov3)

plot(vol, col=tip)
plot(z, col=tip)
plot(dz ~ dvol, col=tip)
abline(lm(z ~ vol))

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

detach(df)

#
# zlevel-02
#

df2 <- read.delim('zlevel-02.tab', header=1)
df2a = df2[df2$tip <= 4,]
df2b = df2[df2$tip > 4,]
df2$idx = seq(1, length(df2$z))
df2a$idx = seq(1, length(df2a$z))
df2b$idx = seq(1, length(df2b$z))

attach(df2a)
describe(z)
plot(z, col=tip)
plot(z ~ tip)
plot(z ~ col)
plot(z ~ row)

aov1 = aov(z ~ as.factor(tip))
summary(aov1)
summary.lm(aov1)

aov2 = aov(z ~ as.factor(col))
summary(aov2)
summary.lm(aov2)

aov3 = aov(z ~ as.factor(row))
summary(aov3)
summary.lm(aov3)

lm1 = lm(z ~ idx)
summary(lm1)
detach(df2a)

attach(df2b)
describe(z)
plot(z, col=tip)
plot(z ~ tip)
plot(z ~ col)
plot(z ~ row)

aov1 = aov(z ~ as.factor(tip))
summary(aov1)
summary.lm(aov1)

aov2 = aov(z ~ as.factor(col))
summary(aov2)
summary.lm(aov2)

aov3 = aov(z ~ as.factor(row))
summary(aov3)
summary.lm(aov3)
detach(df2b)



#
# zlevel-03
#

df3.0 <- read.delim('zlevel-03.tab', header=1)
df3 = df3.0[df3.0$z < 2100 & df3.0$dz > -700,]
df3$idx = seq(1, length(df3$z))
df3b = df3[df3$step > 1,]
df3b$idx = seq(1, length(df3b$z))

attach(df3b)
lm1 = lm(dz ~ dvol)
summary(lm1)

lm2 = lm(dz ~ vol + I(vol - dvol) + I(vol^2) + I((vol - dvol)^2) + I(vol * (vol - dvol)))
summary(lm2)

plot(df3b$dz ~ df3b$dvol)
describe(dz)
plot(z, col=tip)
plot(z ~ tip)
plot(z ~ col)
plot(z ~ row)

aov1 = aov(z ~ as.factor(tip))
summary(aov1)
summary.lm(aov1)

aov2 = aov(z ~ as.factor(col))
summary(aov2)
summary.lm(aov2)

aov3 = aov(z ~ as.factor(row))
summary(aov3)
summary.lm(aov3)

lm1 = lm(z ~ idx)
summary(lm1)
detach(df2a)
