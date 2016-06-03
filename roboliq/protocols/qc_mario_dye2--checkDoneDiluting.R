suppressWarnings(suppressMessages(library(dplyr)))
suppressWarnings(suppressMessages(library(jsonlite)))

df0 = stream_in(file("absorbance.jsonl"), verbose=F)
df1 = mutate(df0, value = ifelse(is.na(value), max(value, na.rm=T)*1.1, value))
df2 = df1 %>% filter(role != "empty") %>% group_by(step, wavelength) %>% summarize(value = mean(value), dilutionFactor = unique(dilutionFactor), dyeVolume = unique(dyeVolume)) %>% ungroup()

varset1 = stream_in(file("dyeAndReader.json"), verbose=F)

# Control means per wavelength
dfControlMean = df1 %>% filter(role == "control") %>% group_by(wavelength) %>% summarize(value = mean(value)) %>% select(wavelength, value) %>% ungroup()

# Undiluted wells with dvalue (value - control mean at given wavelength)
dfUndiluted = df1 %>% filter(step == 1) %>% left_join(dfControlMean, by="wavelength") %>% mutate(dvalue = value.x - value.y) %>% rename(value = value.x)
