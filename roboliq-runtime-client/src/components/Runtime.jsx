import {List, Map} from 'immutable';
import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {connect} from 'react-redux';
import * as actionCreators from '../action_creators';

export const Runtime = React.createClass({
	mixins: [PureRenderMixin],
	render: function() {
    return state.timing.map(entry => {
      CONTINUE
    });
    if (state.timing) {
      var i;
      for (i = 0; i < state.timing.length; i++) {
        var timing = state.timing[i];
        if (timing.type === 0) {
          // Try to find the step in the protocol
          var step = (state.protocol || {}).steps;
          var stepParts = timing.step.split(".");
          for (k = 0; k < stepParts.length; k++) {
            var stepPart = stepParts[k];
            if (step) {
              step = step[stepPart];
            }
          }
          var indent = stepParts.join("");
          var text = (step) ? JSON.stringify(step) : "";
          $('#messages').append($('<li>').text(indent + timing.step + ") "+text));
        }
      }
    }
		return this.props.winner
			? <Winner ref="winner" winner={this.props.winner}/>
			: <div className="results">
				<div className="tally">
					{this.getPair().map(entry => <div key={entry} className="entry">
						<h1>{entry}</h1>
						<div className="voteVisualization">
							<div className="votesBlock" style={{
								width: this.getVotesBlockWidth(entry)
							}}></div>
						</div>
						<div className="voteCount">
							{this.getVotes(entry)}
						</div>
					</div>)}
				</div>
				<div className="management">
					<button ref="restart" onClick={this.props.restart}>
						Restart
					</button>
					<button ref="next" className="next" onClick={this.props.next}>
						Next
					</button>
				</div>
			</div>;
	}
});

function mapStateToProps(state) {
	return {
		timing: state.getIn(["timing"], List()),
		steps: state.getIn(["protocol", "steps"], Map())
	}
}

export const RuntimeContainer = connect(mapStateToProps, actionCreators)(Runtime);
