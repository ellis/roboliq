import React from 'react';
import {List} from 'immutable';

const pair = List.of('Trainspotting', '28 Days Later');

import Results from '../components/Results';

export default React.createClass({
	render: function() {
		console.log("App.render")
		console.log(this.props);
		// return <div>Hello from App!</div>
		//return <div>{this.props.children}</div>;
		// return React.cloneElement(this.props.children, {pair: pair});
		return <div>
			1:
			<div>{this.props.children}</div>
			2:
			{React.cloneElement(this.props.children, {pair: pair})}
		</div>;
	}
});
