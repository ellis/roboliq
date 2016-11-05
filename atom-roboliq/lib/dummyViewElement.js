export default class DummyViewElement {
  constructor (state) {
    this.data = state;
    this.element = document.createElement('div');
    this.message = document.createElement('span');
    this.textNode = document.createTextNode(this.data.content);

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
