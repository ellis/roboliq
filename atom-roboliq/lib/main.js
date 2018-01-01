'use babel';

import { CompositeDisposable } from 'atom';
const DummyViewElement = require('./dummyViewElement.js');
const mathjs = require('mathjs');

// This is your main singleton.
// The whole state of your package will be stored and managed here.
const RoboliqPackage = {
	config: {
		"activateHyperMode": {
			"description": "Turns the package into hyper mode.",
			"type": "boolean",
			"default": false
		},
		"setRange": {
			"type": "integer",
			"default": 42,
			"minium": 1,
			"maximum": 9000
		}
	},
	subscriptions: null,
	dummyView: null,
	modal: null,

	// Activates and restores the previous session of your package.
	activate(state) {
		// Assign a new instance of CompositeDisposable...
		this.subscriptions = new CompositeDisposable();

		/*
		// We don't use the serialization system here because we assume
		// that our view won't work with any data.
		this.dummyView = new DummyViewElement(state.viewState);
		// Here we add the custom view to the modal panel of Atom.
		this.modal = atom.workspace.addModalPanel({
			item: this.dummyView.getElement(),
			visible: false
		});
		*/

		// ...and adding commands.
		this.subscriptions.add(
			atom.commands.add('atom-workspace', {
				'roboliq:toggle': () => this.togglePackage()
			})
		);

		this.subscriptions.add(
			atom.config.onDidChange('roboliq', ({oldValue, newValue}) => {
				// do something
			})
		);
	},

	// When the user or Atom itself kills a window, this method is called.
	// We destroy both the custom view and Atom's modal.
	deactivate () {
		this.subscriptions.dispose();
		//this.dummyView.destroy();
		//this.modal.destroy();
	},

	// To save the current package's state, this method should return
	// an object containing all required data.
	serialize () {
		return {
			//viewState: this.dummyView.serialize()
		};
	},

	// Code to toggle the package state.
	togglePackage() {
		console.log("togglePackage:")
		console.log(this)
		/*if (this.modal.isVisible()) {
			this.modal.hide();
		}
		else {
			this.modal.show();
		}*/

		const editor = atom.workspace.getActiveTextEditor();
		if (editor) {
			const state = {
				design: {
					design: {
						"plate*": 2
					}
				}
			};
			const view = new DummyViewElement(state);
			view.getTitle = () => "Design: " + editor.getPath();
			atom.workspace.getActivePane().addItem(view);
			atom.workspace.getActivePane().activateItem(view);
		}
	}
};

export default RoboliqPackage;
