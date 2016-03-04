import _ from 'lodash';
import {List, Map} from 'immutable';
import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {connect} from 'react-redux';
import * as actionCreators from '../action_creators';

function buildTimingMap(timing) {
	const timingMap = {};
	timing.forEach(item => {
		const when = (timing.get("type") === 0) ? "begin" : "end";
		_.set(timingMap, [timing.get("step", ""), when], timing.get("time"));
	});
	return timingMap;
}

function handleStep(step, path, timingMap, trs) {
	if (_.isEmpty(step))
		return;
	console.log({path, step});
	const description = step.get("description");
	const command = step.get("command");
	if (description || command) {
		const descriptionTag = (!_.isEmpty(description)) ? <div className="description">{description}</div> : undefined;
		const commandTag = (_.isString(command)) ? <span>{command}: </span> : undefined;
		const params = _.pickBy(_.omit(step.toJS(), ["command", "comment", "description", "steps"]), (value) => _.isString(value) || _.isNumber(value) || _.isBoolean(value));
		const tr = <tr key={path.join(".")}><td>{path.join(".")}</td><td>{descriptionTag}{commandTag}{JSON.stringify(params)}</td></tr>;
		trs.push(tr);
	}

	//console.log("step.keys.filter: "+step.keySeq().filter)
	const keys = step.keySeq().filter(s => s[0] >= "0" && s[0] <= "9");
	keys.forEach(key => {
		handleStep(step.get(key), path.concat(key), timingMap, trs);
	});
}

export const Runtime = React.createClass({
	mixins: [PureRenderMixin],
	render: function() {
		const timingMap = buildTimingMap(this.props.timing);
		const trs = [];
		handleStep(this.props.steps, [], timingMap, trs);
		return (<table className="logTable">
			<thead>
				<tr><th>Step</th><th>Command</th><th>Status</th></tr>
			</thead>
			<tbody>
				{trs}
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

export const RuntimeContainer = connect(mapStateToProps, actionCreators)(Runtime);
