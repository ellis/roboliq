'use babel';

// import Design from '';
// const Design = require('roboliq-processor/src/design.js');
// const Temp = require('roboliq-processor/dist/stripUndefined.js');
// const mathjs = require('mathjs');

export default class DummyViewElement {
  constructor(state = {}) {
    this.data = state;
    this.element = document.createElement('div');
    this.message = document.createElement('span');
		//const table = Design.flattenDesign(state.design);
		//const table = "Hello"
    //this.textNode = document.createTextNode(JSON.stringify(table));
    this.textNode = document.createTextNode("Hello");

    this.element.classList.add('your-package');
    this.message.classList.add('your-package-message');

    this.message.appendChild(this.textNode);
    this.element.appendChild(this.message);
  }

  serialize () {
    return {
      data: this.data
    };
  }

  destroy () {
    this.element.remove();
  }

  getElement () {
    return this.element;
  }

  doSomethingWithData () {}
}
