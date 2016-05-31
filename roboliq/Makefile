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

growthcurve07_vV0block.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve07_vV0block.yaml --print-designs && npm run evoware -- ../testdata/bsse-luigi/Carrier.cfg ../testdata/bsse-luigi/Roboliq_Standard_20160311.ewt protocols/growthcurve07_vV0block.out.json ourlab.luigi.evoware

qc_mario_evaporation.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_evaporation.yaml --print-designs && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt protocols/qc_mario_evaporation.out.json ourlab.mario.evoware #&& cp protocols/qc_mario_evaporation.out.esc 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis'

qc_mario_evaporation2.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_evaporation2.yaml --print-designs && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt protocols/qc_mario_evaporation2.out.json ourlab.mario.evoware #&& cp protocols/qc_mario_evaporation2.out.esc 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis'

qc_mario_evaporation3.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_evaporation3.yaml --print-designs && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt protocols/qc_mario_evaporation3.out.json ourlab.mario.evoware && cp protocols/qc_mario_evaporation3.out.esc 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis'

qc_mario_dye1.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dye1.yaml --print-designs -O 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dye1' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dye1\qc_mario_dye1.out.json' ourlab.mario.evoware

qc_mario_dye2.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dye2.yaml --print-designs -O 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dye2' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dye2\qc_mario_dye2.out.json' ourlab.mario.evoware

qc_mario_dye2b.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dye2b.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dye2b\qc_mario_dye2b.out.json' ourlab.mario.evoware

qc_mario_dyeDebug8.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug8.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' --evoware ../testdata/bsse-mario/Carrier.cfg,../testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug8.yaml --print-designs --evoware ../testdata/bsse-mario/Carrier.cfg,../testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware

qc_mario_dyePrep1.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyePrep1.yaml -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyePrep1\qc_mario_dyePrep1.out.json' ourlab.mario.evoware

qc_mario_dyeDebug1.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug1.yaml -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug1\qc_mario_dyeDebug1.out.json' ourlab.mario.evoware

qc_mario_dyeDebug2.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug2.yaml -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug2\qc_mario_dyeDebug2.out.json' ourlab.mario.evoware

qc_mario_dyeDebug3.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug3.yaml -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug3\qc_mario_dyeDebug3.out.json' ourlab.mario.evoware

qc_mario_dyeDebug4.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug4.yaml -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug4\qc_mario_dyeDebug4.out.json' ourlab.mario.evoware

qc_mario_dyeDebug5.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug5.yaml --evoware ../testdata/bsse-mario/Carrier.cfg,../testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug5.yaml -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug5\qc_mario_dyeDebug5.out.json' ourlab.mario.evoware

qc_mario_dyeDebug6.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug6.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug6\qc_mario_dyeDebug6.out.json' ourlab.mario.evoware

qc_mario_dyeDebug7.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug7.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug7\qc_mario_dyeDebug7.out.json' ourlab.mario.evoware

qc_mario_dyeDebug8.yaml:
	npm start -- --progress --no-ourlab src/config/ourlab.js protocols/qc_mario_dyeDebug8.yaml --print-designs -P 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts' && npm run evoware -- ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt 'C:\ProgramData\Tecan\EVOware\database\scripts\Ellis\EvowareScripts\qc_mario_dyeDebug8\qc_mario_dyeDebug8.out.json' ourlab.mario.evoware

prof:
	node --prof dist/main.js --progress --no-ourlab src/config/ourlab_luigi.js protocols/growthcurve04_firstWithYeast.yaml
	node --prof-process `ls -tr isolate-0000000000* | tail --lines=1` > prof.txt

profDesign_vV0block:
	node --prof src/designMain.js protocols/growthcurve07_vV0block.yaml --path objects.design1
