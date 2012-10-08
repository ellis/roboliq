#library('rjson')
#setwd("~/src/roboliq/testdata/dyes-201210")
#json <- fromJSON(paste(readLines('read10times1a.json'), collapse=""))
#df <- as.data.frame(t(sapply(json, unlist)))

setwd("~/src/roboliq/testdata/dyes-201210")
df1a <- read.delim('read10times1a.tab', header=1)
df1b <- read.delim('read10times1b.tab', header=1)
