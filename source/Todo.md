# TODOs for roboliq

- [x] Create titationSeries command
- [x] TitrationSeries: handle multiple volumes for a given step, for grouping
- [x] TitrationSeries: handle reagent without volume, to use as filler
- [?] TitrationSeries: handle multiple reagents for a given step, for grouping
- [ ] Need to create a Transfer command for Pipetting, rather than using distribute!
- [ ] Use 384 well plate
- [ ] small tips
- [ ] Ask Fabian about a verb for the TitrationSeries command: titrate
- [ ] TitrationSeries: filter out 0-volume dispenses
- [ ] TitrationSeries/PipetteSpec: allow for a list of volumes rather than just a single volume
- [ ] TitrationSeries: handle min/max reagent, ranging the volume over the available wells
- [ ] TitrationSeries: handle concentrations in addition to volumes
- [ ] Consider making paths in config file relative to the config file itself
- [ ] Refactor the Converter code, and make more use of parsers
- [ ] Commands should be able to each produce their own reports, e.g. for TitrationSeries, showing a table with the contents of the wells
