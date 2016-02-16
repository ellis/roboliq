import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
//import YAML from 'yamljs';
import {flattenDesign, design2} from '../design.js';

const Table = ({
	table
}) => {
	// Get column names
	const columnMap = {};
	_.forEach(table, row => _.forEach(_.keys(row), key => { columnMap[key] = true; } ));
	const columns = _.keys(columnMap);

	return <table>
		<thead>
			<tr>{_.map(columns, s => <th key={"column_"+s}>{s}</th>)}</tr>
		</thead>
		<tbody>
			{_.map(table, (row, index) => <tr key={"row"+index}>
				{_.map(columns, key => <td key={"row"+index+"_"+key}>{row[key]}</td>)}
			</tr>)}
		</tbody>
	</table>;
};

export default React.createClass({
	mixins: [PureRenderMixin],
	render: function() {
		const design = design2;
		const table = flattenDesign(design);
		return <div>
			{/*<pre>{YAML.stringify(design, 5, 1)}</pre>*/}
			<pre>{JSON.stringify(design, null, '\t')}</pre>
			<Table table={table}/>
		</div>;
	}
});
