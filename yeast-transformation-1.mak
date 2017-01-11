all:
	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-cells.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-dmso.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-dwtransfer.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-lb.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-liac.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-pcrtransfer.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-pegmix.yaml

	npm run processor -- --progress -P compiled --evoware testdata/bsse-mario/Carrier.cfg,testdata/bsse-mario/NewLayout_Feb2015.ewt,ourlab.mario.evoware config/bsse-mario.js protocols/yeast-transformation-waterresusp.yaml
