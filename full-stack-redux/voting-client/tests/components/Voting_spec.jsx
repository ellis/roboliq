import {expect} from 'chai';
import React from 'react';
import ReactDOM from 'react-dom';
import ReactTestUtils from 'react-addons-test-utils';
import {Voting} from '../../src/components/Voting';

const {renderIntoDocument, scryRenderedDOMComponentsWithTag, Simulate} = ReactTestUtils;

describe('Voting', () => {

	it('renders a pair of buttons', () => {
		const component = renderIntoDocument(<Voting pair={["Trainspotting", "28 Days Later"]}/>);
		const buttons = scryRenderedDOMComponentsWithTag(component, 'button');

		expect(buttons.length).to.equal(2);
		expect(buttons[0].textContent).to.equal('Trainspotting');
		expect(buttons[1].textContent).to.equal('28 Days Later');
	});

	it('invokes callback when a button is clicked', () => {
		let votedWith;
		const vote = (entry) => votedWith = entry;

		const component = renderIntoDocument(<Voting pair={["Trainspotting", "28 Days Later"]} vote={vote}/>);
		const buttons = scryRenderedDOMComponentsWithTag(component, 'button');
		Simulate.click(buttons[0]);

		expect(votedWith).to.equal('Trainspotting');
	});

	it('disables buttons when user has voted', () => {
		const component = renderIntoDocument(<Voting pair={["Trainspotting", "28 Days Later"]} hasVoted="Trainspotting"/>);
		const buttons = scryRenderedDOMComponentsWithTag(component, 'button');

		expect(buttons.length).to.equal(2);
		expect(buttons[0].hasAttribute('disabled')).to.equal(true);
		expect(buttons[1].hasAttribute('disabled')).to.equal(true);
	});

	it('adds label to the voted entry', () => {
		const component = renderIntoDocument(<Voting pair={["Trainspotting", "28 Days Later"]} hasVoted="Trainspotting"/>);
		const buttons = scryRenderedDOMComponentsWithTag(component, 'button');

		expect(buttons[0].textContent).to.contain('Voted');
	});

	it('renders just the winner when there is one', () => {
		const component = renderIntoDocument(<Voting winner="Trainspotting"/>);
		const buttons = scryRenderedDOMComponentsWithTag(component, 'button');
		expect(buttons.length).to.equal(0);

		const winner = ReactDOM.findDOMNode(component.refs.winner);
		expect(winner).to.be.ok;
		expect(winner.textContent).to.contain('Trainspotting');
	});
});
