suppressWarnings(suppressMessages(library(dplyr)))
suppressWarnings(suppressMessages(library(jsonlite)))

df0 = stream_in(file("absorbance.jsonl"), verbose=F)
varset1 = stream_in(file("dyeAndReader1.json"), verbose=F)

df1 = df0 %>% filter(wavelength >= varset1$wavelengthMin & wavelength <= varset1$wavelengthMax) %>% mutate(value = ifelse(is.na(value), max(value, na.rm=T)*1.1, value), strain = substr(well, 1, 1))

dfControl = df1 %>% filter(role == "control")
# Control means per wavelength
dfControlMean = dfControl %>% group_by(wavelength) %>% summarize(value = mean(value)) %>% select(wavelength, value) %>% ungroup()

# Calculate dvalues
dfDye = df1 %>% filter(step >= 1) %>% left_join(dfControlMean, by="wavelength") %>% mutate(dvalue = value.x - value.y) %>% rename(value = value.x, value0 = value.y)

# Undiluted wells with dvalue (value - control mean at given wavelength)
dfUndiluted = dfDye %>% filter(step == 1)
# Find max undiluted dvalue
dvalueUndilutedMax = max(dfUndiluted$dvalue)

dfDiluted = dfDye %>% filter(step > 1) %>% left_join(dfUndiluted %>% select(strain, wavelength, dvalue), by=c("strain", "wavelength")) %>% rename(dvalue = dvalue.x, dvalue1 = dvalue.y)

dfStepBelowThreshold = dfDiluted %>% group_by(step) %>% summarize(below = all(dvalue < dvalueUndilutedMax / 2))

#dfUndilutedMean = dfUndiluted %>% group_by(wavelength) %>% summarize(dvalue = mean(dvalue)) %>% select(wavelength, dvalue) %>% ungroup()
#dfStepBelowThreshold = dfDiluted %>% group_by(step) %>% summarize(below = all(dvalue.fraction < 0.5))
which1 = which(dfStepBelowThreshold$below)

# If we can find best wavelength from the measurements so far:
if (length(which1) > 0) {
  # Calculate best wavelength
  thresholdStep = dfStepBelowThreshold$step[which1[1]]
  dfThresholdStep = dfDye %>% filter(step == thresholdStep)
  i = which.max(dfThresholdStep$dvalue)
  varset1$wavelength = dfThresholdStep$wavelength[i]
	varset1$offsetControl = dfThresholdStep$value0[i]
}
stream_out(varset1, con=stdout(), verbose=F)
