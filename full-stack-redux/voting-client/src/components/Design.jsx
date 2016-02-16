import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {connect} from 'react-redux';
//import YAML from 'yamljs';
//import {flattenDesign, design2} from '../design.js';

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

function mapStateToProps(state) {
	return {
		designText: state.get("designText"),
		design: state.get("design"),
		table: state.get("table")
	};
}

export const Design = React.createClass({
	mixins: [PureRenderMixin],
	render: function() {
		const design = this.props.design.toJS();
		//console.log("design: "+JSON.stringify(design))
		//const table = flattenDesign(design);
		const table = (this.props.table) ? this.props.table.toJS() : [];
		//console.log("table: "+JSON.stringify(table))
		return <div>
			{/*<pre>{YAML.stringify(design, 5, 1)}</pre>*/}
			<textarea value={this.props.designText} onChange={this.handleChange}></textarea>
			<Table table={table}/>
		</div>;
	},
	handleChange: function(event) {
		console.log("handleChange:");
		console.log(event.target.value);
		this.props.setDesignText(event.target.value);
	},
});

const actions = {
	setDesignText: (text) => {
		return {type: "setDesignText", text};
	}
};

export const DesignContainer = connect(mapStateToProps, actions)(Design);
