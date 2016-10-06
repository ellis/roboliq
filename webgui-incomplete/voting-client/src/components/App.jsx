import React from 'react';
import {List, Map} from 'immutable';

const pair = List.of('Trainspotting', '28 Days Later');
const tally = Map({'Trainspotting': 5, '28 Days Later': 4});

export default React.createClass({
	render: function() {
		console.log("App.render")
		console.log(this.props);
		// return <div>Hello from App!</div>
		//return <div>{this.props.children}</div>;
		// return React.cloneElement(this.props.children, {pair: pair});
		return <div>
			{React.cloneElement(this.props.children, {pair: pair, tally: tally})}
		</div>;
	}
});
