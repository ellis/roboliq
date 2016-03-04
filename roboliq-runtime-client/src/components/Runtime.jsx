import _ from 'lodash';
import {List, Map} from 'immutable';
import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {connect} from 'react-redux';
import * as actionCreators from '../action_creators';

export const Runtime = React.createClass({
	mixins: [PureRenderMixin],
	render: function() {
    return (<table>
      <thead>
        <tr><th>Time</th><th>Step</th><th>Command</th></tr>
      </thead>
      <tbody>
        {this.props.timing.toJS().map((item, index) => {
          const path = item.step.split(".");
          const step = this.props.steps.getIn(path);
          console.log({item, path, step});
          const params = _.pickBy(step, (value) => _.isString(value) || _.isNumber(value) || _.isBoolean(value));
          return <tr key={index}><td>{item.time}</td><td>{item.step})</td><td>{JSON.stringify(params)}</td></tr>;
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

export const RuntimeContainer = connect(mapStateToProps, actionCreators)(Runtime);
