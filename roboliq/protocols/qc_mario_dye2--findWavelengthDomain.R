suppressWarnings(suppressMessages(library(dplyr)))
suppressWarnings(suppressMessages(library(jsonlite)))

df0 = stream_in(file("absorbance.jsonl"), verbose=F)
df1 = mutate(df0, value = ifelse(is.na(value), max(value, na.rm=T)*1.1, value))
df2 = df1 %>% filter(role != "empty") %>% group_by(step, wavelength) %>% summarize(value = mean(value), dilutionFactor = unique(dilutionFactor), dyeVolume = unique(dyeVolume)) %>% ungroup()

# Plot First vs Control
#dfC1 = subset(df1, (step == 0 & role == "control") | step == 1)
#dfTemp = dfC1 %>% group_by(step, wavelength) %>% summarize(value = mean(value, na.rm=T))
#gvis(data=dfTemp, x=~wavelength, y=~value, stroke=~step) %>% layer_lines() %>% layer_points(data=dfC1, fill:=NA)

# Find range of interesting wavelengths
# Here the difference between the first dye measurement and the control measurement are considered.
# A threshold is calculated a 1/4 the maximum value of the differences, and the range of wavelengths is chosen
# that has differences over the threshold.

dfControlMean = df1 %>% ungroup() %>% filter(role == "control") %>% group_by(wavelength) %>% summarize(value = mean(value)) %>% select(wavelength, value) %>% ungroup()
dfFirst = df1 %>% filter(step == 1) %>% left_join(dfControlMean, by="wavelength") %>% mutate(dvalue = value.x - value.y) %>% rename(value = value.x)

threshold = max(dfFirst$dvalue) / 4
indexes = which(dfFirst$dvalue >= threshold)
indexLower = indexes[1]
indexUpper = indexes[length(indexes)]
wavelengthLower = dfFirst[indexLower,]$wavelength
wavelengthUpper = dfFirst[indexUpper,]$wavelength

#dfFirst$over = dfFirst$wavelength >= wavelengthLower & dfFirst$wavelength <= wavelengthUpper
#dfTemp = dfFirst %>% group_by(step, wavelength) %>% summarize(dvalue = max(dvalue, na.rm=T))
#ggvis(data=dfTemp, x=~wavelength, y=~dvalue) %>% layer_lines() %>% layer_points(data=dfFirst, fill:=NA, stroke=~factor(over))

dfOut = data.frame(wavelengthMin = wavelengthLower, wavelengthMax = wavelengthUpper)
stream_out(dfOut, con=stdout(), verbose=F)
