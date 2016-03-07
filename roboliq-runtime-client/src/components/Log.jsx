import _ from 'lodash';
import {List, Map} from 'immutable';
import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {connect} from 'react-redux';
import * as actionCreators from '../action_creators';

export const Log = React.createClass({
	mixins: [PureRenderMixin],
	render: function() {
		let date;
		return (<table className="logTable">
			<thead>
				<tr><th>Time</th><th>Step</th><th>Command</th><th>Log</th></tr>
			</thead>
			<tbody>
				{_.flatMap(this.props.timing.toJS(), (item, index) => {
					// See whether we need a new date row
					const [date0, time0] = item.time.split("T");
					let dateItem;
					if (date !== date0) {
						date = date0;
						dateItem = <tr key={date}><td colSpan="4"><b>{date}</b></td></tr>;
					}

					const time = time0.substr(0, 8);
					const path = item.step.split(".");
					const step = this.props.steps.getIn(path);
					//console.log({item, path, step});
					const params = _.pickBy(step.toJS(), (value) => _.isString(value) || _.isNumber(value) || _.isBoolean(value));
					const logItem = <tr key={index}><td>{time}</td><td>{item.step})</td><td>{JSON.stringify(params)}</td><td>{(item.type === 0) ? "begin" : "end"}</td></tr>;

					return [dateItem, logItem];
				})}
			</tbody>
		</table>);
	}
});

function mapStateToProps(state) {
	return {
		timing: state.getIn(["timing"], List()),
		steps: state.getIn(["protocol", "steps"], Map())
	}
}

export const LogContainer = connect(mapStateToProps, actionCreators)(Log);
