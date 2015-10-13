import _ from 'lodash';
import assert from 'assert';
import * as WellContents from '../src/WellContents.js';

describe('WellContents', function() {
	describe('transferContents', function () {
		it('should transfer contents between wells', function () {
			assert.deepEqual(
				WellContents.transferContents(['100 ul', 'source1'], undefined, '20 ul'),
				[['80 ul', 'source1'], ['20 ul', 'source1']]
			);
			assert.deepEqual(
				WellContents.transferContents(['Infinity l', 'source1'], undefined, '20 ul'),
				[['Infinity l', 'source1'], ['20 ul', 'source1']]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', 'source1'], ['0 ul'], '20 ul'),
				[['80 ul', 'source1'], ['20 ul', 'source1']]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', 'source1'], ['10 ul', 'source2'], '20 ul'),
				[['80 ul', 'source1'], ['30 ul', ['10 ul', 'source2'], ['20 ul', 'source1']]]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', ['50 ul', 'a'], ['50 ul', 'b']], undefined, '20 ul'),
				[['80 ul', ['50 ul', 'a'], ['50 ul', 'b']], ['20 ul', ['50 ul', 'a'], ['50 ul', 'b']]]
			);
			assert.deepEqual(
				WellContents.transferContents(['100 ul', ['50 ul', 'a'], ['50 ul', 'b']], ['10 ul', 'c'], '20 ul'),
				[['80 ul', ['50 ul', 'a'], ['50 ul', 'b']], ['30 ul', ['10 ul', 'c'], ['20 ul', ['50 ul', 'a'], ['50 ul', 'b']]]]
			);
		});
	});
});
