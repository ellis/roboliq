# Temporary file for more conveniently compiling several scripts

luigi_protocol2:
	npm start -- --no-ourlab src/config/ourlab_luigi.js protocols/luigi_protocol2.yaml && npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/DM_WorkTable_June2015.ewt protocols/luigi_protocol2.out.json ourlab.luigi.evoware

growthcurve02_testing:
	npm start -- --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve02_testing.yaml && npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/Roboliq_Standard_20160311.ewt protocols/growthcurve02_testing.out.json ourlab.luigi.evoware

growthcurve03_testing:
	npm start -- --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve03_testing.yaml && npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/Roboliq_Standard_20160311.ewt protocols/growthcurve03_testing.out.json ourlab.luigi.evoware

growthcurve04_firstWithYeast:
	npm start -- --progress --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve04_firstWithYeast.yaml && npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/Roboliq_Standard_20160311.ewt protocols/growthcurve04_firstWithYeast.out.json ourlab.luigi.evoware

growthcurve05_secondWithYeast:
	npm start -- --progress --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve05_secondWithYeast.yaml --print-designs && npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/Roboliq_Standard_20160311.ewt protocols/growthcurve05_secondWithYeast.out.json ourlab.luigi.evoware

prof:
	node --prof dist/main.js -- --progress --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve04_firstWithYeast.yaml
	node --prof-process `ls -tr isolate-0000000000* | tail --lines=1` > prof.txt
