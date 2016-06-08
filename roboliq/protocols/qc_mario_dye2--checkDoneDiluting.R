suppressWarnings(suppressMessages(library(dplyr)))
suppressWarnings(suppressMessages(library(jsonlite)))

varset1 = stream_in(file("dyeAndReader2.json"), verbose=F)


# If the wavelength has been set:
if (length(varset1$wavelength) > 0) {
	df0 = stream_in(file("absorbance.jsonl"), verbose=F)
	df1 = df0 %>% filter(wavelength == varset1$wavelength) %>% mutate(value = ifelse(is.na(value), max(value, na.rm=T)*1.1, value), strain = substr(well, 1, 1))
	dfDye = df1 %>% filter(step >= 1) %>% mutate(dvalue = value - varset1$offsetControl)
  # If we have data for additional steps:
  dfStepStop = dfDye %>% group_by(step) %>% summarize(dvalue.mean = mean(dvalue), dvalue.sd = sd(dvalue), dvalue.es = dvalue.mean / dvalue.sd, stop=dvalue.es < 10)
  cat(ifelse(dfStepStop[nrow(dfStepStop),]$stop, "true", "false"))
} else {
	cat('false')
}
