'use babel';

import { CompositeDisposable } from 'atom';


// This is your main singleton.
// The whole state of your package will be stored and managed here.
const RoboliqPackage = {
	subscriptions: null,

	// Activates and restores the previous session of your package.
	activate(state) {
		// Assign a new instance of CompositeDisposable...
		this.subscriptions = new CompositeDisposable();

		// ...and adding commands.
		this.subscriptions.add(
			atom.commands.add('atom-workspace', {
				'your-package:toggle': this.togglePackage
			})
		);
	},

	// When the user or Atom itself kills a window, this method is called.
	deactivate () {
		this.subscriptions.dispose();
	},

	// To save the current package's state, this method should return
	// an object containing all required data.
	serialize () {
	},

	// Code to toggle the package state.
	togglePackage() {
	}
};

export default RoboliqPackage;
